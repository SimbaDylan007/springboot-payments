package com.yourcompany.payments.service.biller;

import com.yourcompany.payments.dto.smilepay.PaymentInitiateRequest;
import com.yourcompany.payments.dto.smilepay.PaymentInitiateResponse;
import com.yourcompany.payments.entity.Payment;
import com.yourcompany.payments.entity.User;
import com.yourcompany.payments.exception.PaymentFailedException;
import com.yourcompany.payments.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmilePayService {

    private final RestTemplate restTemplate;
    private final PaymentRepository paymentRepository;

    @Value("${smilepay.base-url}")
    private String smilePayBaseUrl;

    @Value("${smilepay.api-key}")
    private String smilePayApiKey;

    @Value("${smilepay.api-secret}")
    private String smilePayApiSecret;

    @Value("${api.base-url}") // Your backend's public URL for webhooks
    private String apiBaseUrl;

    /**
     * Initiates a payment with the SmilePay gateway.
     * 1. Generates a unique order reference.
     * 2. Creates an 'INITIATED' payment record in the local database.
     * 3. Calls the SmilePay API.
     * 4. Returns the payment URL and order reference to the frontend for redirection.
     */
    @Transactional
    public PaymentInitiateResponse initiateStandardPayment(PaymentInitiateRequest request, User currentUser) {
        String orderReference = "SP_" + UUID.randomUUID().toString();
        log.info("[SMILEPAY-INIT] Initiating payment for Biller: {}, OrderRef: {}", request.billerId(), orderReference);

        // Step 1: Create and save the initial Payment record to our DB
        Payment payment = new Payment();
        payment.setUser(currentUser);
        payment.setGatewayReference(orderReference); // Use our generated ref as the gateway ref initially
        payment.setBillerId(request.billerId());
        payment.setCustomerAccount(request.customerAccount());
        payment.setAmount(request.amount());
        payment.setCurrency(request.currency());
        payment.setStatus("INITIATED");
        payment.setPaymentMethod(request.paymentMethod());
        payment.setSuccessful(false);
        payment.setPaymentDate(LocalDateTime.now());
        paymentRepository.save(payment);

        // Step 2: Prepare the request for the SmilePay API
        String webhookUrl = apiBaseUrl + "/api/v1/webhooks/smilepay";
        Map<String, Object> smilePayPayload = Map.of(
                "orderReference", orderReference,
                "amount", request.amount(),
                "currencyCode", getCurrencyCode(request.currency()),
                "resultUrl", webhookUrl,
                "returnUrl", "https://your-frontend.com/payment-complete/" + orderReference, // TODO: Get from config
                "itemName", "Payment for " + request.billerId(),
                "itemDescription", "Account: " + request.customerAccount(),
                "email", currentUser.getEmail(),
                "mobilePhoneNumber", request.mobilePhoneNumber() != null ? request.mobilePhoneNumber() : ""
        );

        // Step 3: Call the SmilePay API
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", smilePayApiKey);
        headers.set("x-api-secret", smilePayApiSecret);
        headers.set("Content-Type", "application/json");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(smilePayPayload, headers);
        String smilePayEndpoint = smilePayBaseUrl + "/payments/initiate-transaction";

        try {
            log.debug("[SMILEPAY-INIT] Sending request to {}: {}", smilePayEndpoint, smilePayPayload);
            ResponseEntity<Map> response = restTemplate.exchange(smilePayEndpoint, HttpMethod.POST, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody == null || !"00".equals(responseBody.get("responseCode"))) {
                String errorMsg = responseBody != null ? (String) responseBody.get("responseMessage") : "Empty response from gateway";
                log.error("[SMILEPAY-INIT] Failed to initiate payment for OrderRef: {}. Reason: {}", orderReference, errorMsg);
                throw new PaymentFailedException("SmilePay gateway rejected the payment initiation: " + errorMsg, null);
            }

            String paymentUrl = (String) responseBody.get("paymentUrl");
            log.info("[SMILEPAY-INIT] Successfully initiated payment for OrderRef: {}. URL: {}", orderReference, paymentUrl);

            return new PaymentInitiateResponse(paymentUrl, orderReference);

        } catch (HttpClientErrorException e) {
            log.error("[SMILEPAY-INIT] HTTP error during initiation for OrderRef: {}. Status: {}, Body: {}",
                    orderReference, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new PaymentFailedException("Failed to communicate with SmilePay gateway.", e);
        }
    }

    private String getCurrencyCode(String currency) {
        return "USD".equalsIgnoreCase(currency) ? "840" : "924"; // Simple mapping
    }
}