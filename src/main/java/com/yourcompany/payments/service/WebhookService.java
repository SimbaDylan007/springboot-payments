package com.yourcompany.payments.service;

import com.yourcompany.payments.dto.smilepay.SmilePayWebhookPayload;
import com.yourcompany.payments.entity.Payment;
import com.yourcompany.payments.repository.PaymentRepository;
import com.yourcompany.payments.service.biller.EgressPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yourcompany.payments.dto.payment.PaymentCreateRequest;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final PaymentRepository paymentRepository;
    private final EgressPaymentService egressPaymentService;

    /**
     * Handles the incoming webhook notification from SmilePay.
     * 1. Finds the original 'INITIATED' payment record.
     * 2. Updates its status based on the webhook payload.
     * 3. If successful, triggers the asynchronous post-payment processing.
     */
    @Transactional
    public void processSmilePayNotification(SmilePayWebhookPayload payload) {
        String orderReference = payload.orderReference();
        log.info("[WEBHOOK] Processing notification for OrderRef: {}. Status: {}", orderReference, payload.status());

        Optional<Payment> paymentOptional = paymentRepository.findByGatewayReference(orderReference);
        if (paymentOptional.isEmpty()) {
            log.error("[WEBHOOK] CRITICAL: Received webhook for unknown OrderRef: {}", orderReference);
            // In a real app, you might save this to an "unmatched_webhooks" table for investigation.
            return;
        }

        Payment payment = paymentOptional.get();

        // Update the payment record with details from the webhook.
        payment.setStatus(payload.status().toUpperCase());
        payment.setPaymentReference(payload.transactionReference()); // This is the gateway's final transaction ID
        payment.setSuccessful("SUCCESSFUL".equalsIgnoreCase(payload.status()));

        // Fill in any missing details if the webhook provides them
        if (payload.billerId() != null) payment.setBillerId(payload.billerId());
        if (payload.customerAccount() != null) payment.setCustomerAccount(payload.customerAccount());

        paymentRepository.save(payment);

        // If the payment was successful, trigger the final settlement with the biller.
        if (payment.isSuccessful()) {
            log.info("[WEBHOOK] Payment for OrderRef: {} was successful. Triggering post-payment processing.", orderReference);
            triggerPostPaymentProcessing(payment);
        } else {
            log.warn("[WEBHOOK] Payment for OrderRef: {} was not successful (Status: {}). No post-payment action taken.",
                    orderReference, payload.status());
        }
    }

    /**
     * This method runs in a background thread (@Async) so the webhook can return immediately.
     * It calls the EgressPaymentService to perform the final SOAP call to the biller.
     */
    @Async
    @Transactional
    public void triggerPostPaymentProcessing(Payment payment) {
        log.info("[POST-PAYMENT-ASYNC] Starting Egress settlement for OrderRef: {}", payment.getGatewayReference());

        try {
            // Create the request object needed by the EgressPaymentService
            PaymentCreateRequest egressRequest = new PaymentCreateRequest(
                    payment.getCustomerAccount(),
                    payment.getAmount(),
                    payment.getPaymentMethod(),
                    payment.getCurrency(),
                    payment.getBillerId(),
                    null, // months
                    null, null, null, null, null, // customer details
                    payment.getPaymentReference() // Use the final transaction reference from the gateway
            );

            // Call the Egress service to perform the final SOAP transaction
            egressPaymentService.postPayment(egressRequest, payment.getUser().getUserUuid());

            // If the above call is successful, the payment status will be updated to COMPLETED inside it.
            // If it fails, it will throw an exception.
            payment.setStatus("COMPLETED"); // Mark as fully completed in our system.
            log.info("[POST-PAYMENT-ASYNC] Egress settlement successful for OrderRef: {}", payment.getGatewayReference());

        } catch (Exception e) {
            log.error("[POST-PAYMENT-ASYNC] Egress settlement FAILED for OrderRef: {}. Error: {}",
                    payment.getGatewayReference(), e.getMessage(), e);
            payment.setStatus("POST_PAYMENT_FAILED");
            payment.setNarrative("Failed to settle with biller after successful gateway payment. Reason: " + e.getMessage());
        } finally {
            // Always save the final state of the payment record after async processing.
            paymentRepository.save(payment);
        }
    }
}