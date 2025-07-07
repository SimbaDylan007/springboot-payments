package com.yourcompany.payments.service.biller;

// --- Imports for JAXB (WSDL-generated classes) ---
import com.yourcompany.payments.wsdl.ObjectFactory;
import com.yourcompany.payments.wsdl.Payment;
import com.yourcompany.payments.wsdl.PostPayment;
import com.yourcompany.payments.wsdl.PostPaymentResponse;
import com.yourcompany.payments.wsdl.ValidateCustomerAccount;
import com.yourcompany.payments.wsdl.ValidateCustomerAccountResponse;
// --- End of JAXB imports ---

// --- Imports for JAXBElement Wrapper ---
import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
// --- End of Wrapper imports ---

import com.yourcompany.payments.dto.payment.PaymentCreateRequest;
import com.yourcompany.payments.dto.payment.PaymentResponse;
import com.yourcompany.payments.dto.TransactionRecordDto;
import com.yourcompany.payments.entity.User;
import com.yourcompany.payments.exception.PaymentFailedException;
import com.yourcompany.payments.repository.PaymentRepository;
import com.yourcompany.payments.repository.TransactionRepository;
import com.yourcompany.payments.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ws.client.core.WebServiceTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class EgressPaymentService {

    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final WebServiceTemplate webServiceTemplate;

    @Value("${egress.source}")
    private String egressSource;

    private static final String WSDL_NAMESPACE_URI = "http://ws.billpayment.zb.co.zw/";

    /**
     * Makes a REAL SOAP call to validate a customer account.
     */
    public ValidateCustomerAccountResponse validateAccount(String billerId, String customerAccount) {
        log.info("[EGRESS-VALIDATE] START - Biller: {}, Account: {}", billerId, customerAccount);

        ObjectFactory factory = new ObjectFactory();
        ValidateCustomerAccount requestBody = factory.createValidateCustomerAccount();
        requestBody.setSource(egressSource);
        requestBody.setBillerId(billerId);
        requestBody.setCustomerAccount(customerAccount);

        // Wrap the request object in a JAXBElement to define the root XML element.
        QName qName = new QName(WSDL_NAMESPACE_URI, "validateCustomerAccount");
        JAXBElement<ValidateCustomerAccount> jaxbRequest = new JAXBElement<>(qName, ValidateCustomerAccount.class, requestBody);

        try {
            log.debug("Sending real SOAP request for validateAccount to gateway.");

            // The response might also need to be unwrapped from a JAXBElement.
            Object rawResponse = webServiceTemplate.marshalSendAndReceive(jaxbRequest);

            if (rawResponse instanceof JAXBElement) {
                rawResponse = ((JAXBElement<?>) rawResponse).getValue();
            }

            ValidateCustomerAccountResponse response = (ValidateCustomerAccountResponse) rawResponse;

            if (response == null || response.getReturn() == null) {
                throw new PaymentFailedException("Received null or empty return object from SOAP gateway.", null);
            }

            log.info("[EGRESS-VALIDATE] SUCCESS - Biller: {}, Successful Flag: {}", billerId, response.getReturn().isSuccessful());
            return response;

        } catch (Exception e) {
            log.error("[EGRESS-VALIDATE] FAILED - Biller: {}, Account: {}. Error: {}", billerId, customerAccount, e.getMessage(), e);
            throw new PaymentFailedException("Validation call to gateway failed.", e);
        }
    }

    /**
     * Makes a REAL SOAP call to post a payment and persists the entire transaction flow.
     */
    @Transactional
    public PaymentResponse postPayment(PaymentCreateRequest request, UUID userUuid) {
        String clientReference = generateUniqueReference();
        User user = userRepository.findByUserUuid(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found with UUID: " + userUuid));

        try {
            log.info("[EGRESS-PAY] START - Biller: {}, Ref: {}", request.billerId(), clientReference);

            ObjectFactory factory = new ObjectFactory();
            PostPayment requestPayload = factory.createPostPayment();
            Payment wsdlPayment = createWsdlPaymentObject(factory, request, user, clientReference);
            requestPayload.setPayment(wsdlPayment);

            // Wrap the request object in a JAXBElement.
            QName qName = new QName(WSDL_NAMESPACE_URI, "postPayment");
            JAXBElement<PostPayment> jaxbRequest = new JAXBElement<>(qName, PostPayment.class, requestPayload);

            Object rawResponse = webServiceTemplate.marshalSendAndReceive(jaxbRequest);

            if (rawResponse instanceof JAXBElement) {
                rawResponse = ((JAXBElement<?>) rawResponse).getValue();
            }

            PostPaymentResponse soapResponse = (PostPaymentResponse) rawResponse;

            if (soapResponse == null || soapResponse.getReturn() == null) {
                throw new PaymentFailedException("Received empty response from payment gateway.", null);
            }

            com.yourcompany.payments.wsdl.PaymentResponse gatewayResponseData = soapResponse.getReturn();

            boolean isSuccess = gatewayResponseData.isSuccessful();
            String status = isSuccess ? "SUCCESSFUL" : "FAILED";
            String message = gatewayResponseData.getReceiptDetails() != null ? gatewayResponseData.getReceiptDetails() : "No details provided.";

            PaymentResponse responseDto = buildPaymentResponseDto(request, user, clientReference, gatewayResponseData, status, message);

            saveTransactionRecord("post", request, responseDto, clientReference);
            savePaymentEntity(responseDto, user);

            log.info("[EGRESS-PAY] {} - Biller: {}, Ref: {}", status, request.billerId(), clientReference);
            return responseDto;

        } catch (Exception e) {
            log.error("[EGRESS-PAY] CRITICAL ERROR - Ref: {}. Error: {}", clientReference, e.getMessage(), e);

            PaymentResponse failedResponse = buildFailedPaymentResponse(request, user, clientReference, e.getMessage());

            saveTransactionRecord("post", request, failedResponse, clientReference);
            savePaymentEntity(failedResponse, user);

            throw new PaymentFailedException("Payment processing failed. Ref: " + clientReference, e);
        }
    }

    // --- Private Helper Methods ---

    private Payment createWsdlPaymentObject(ObjectFactory factory, PaymentCreateRequest request, User user, String clientReference) {
        Payment wsdlPayment = factory.createPayment();
        wsdlPayment.setSource(egressSource);
        wsdlPayment.setBillerId(request.billerId());
        wsdlPayment.setCustomerAccount(request.customerAccountNumber());
        wsdlPayment.setAmount(request.amount().doubleValue());
        wsdlPayment.setCurrency(request.currency());
        wsdlPayment.setPaymentReference(clientReference);
        wsdlPayment.setPaymentMethod(request.paymentMethod());
        wsdlPayment.setCustomerName(user.getFullName());
        wsdlPayment.setCustomerPaymentDetails1(request.customerPaymentDetails1());
        wsdlPayment.setCustomerPaymentDetails2(request.customerPaymentDetails2());
        wsdlPayment.setCustomerPaymentDetails3(request.customerPaymentDetails3());
        return wsdlPayment;
    }

    private PaymentResponse buildPaymentResponseDto(PaymentCreateRequest request, User user, String clientRef, com.yourcompany.payments.wsdl.PaymentResponse gatewayResponse, String status, String message) {
        Payment wsdlPayment = gatewayResponse.getPayment();
        String gatewayRef = String.valueOf((wsdlPayment != null) ? wsdlPayment.getGatewayReference() : null);
        String narrative = (wsdlPayment != null) ? wsdlPayment.getNarrative() : null;
        String tokenDetails = extractZetdcToken(message, request.billerId());

        return new PaymentResponse(
                gatewayResponse.isSuccessful(), status, message, gatewayRef, clientRef,
                gatewayResponse.getReceiptNumber(), request.billerId(), request.customerAccountNumber(),
                user.getFullName(), request.amount(), request.currency(), egressSource,
                request.paymentMethod(), null, LocalDateTime.now(), gatewayResponse.getReceiptDetails(),
                narrative, tokenDetails, user.getUserUuid().toString(), request.customerPaymentDetails1(),
                request.customerPaymentDetails2(), request.customerPaymentDetails3(), null
        );
    }

    private PaymentResponse buildFailedPaymentResponse(PaymentCreateRequest request, User user, String clientRef, String errorMessage) {
        return new PaymentResponse(
                false, "FAILED", errorMessage, null, clientRef, null, request.billerId(),
                request.customerAccountNumber(), user.getFullName(), request.amount(), request.currency(),
                egressSource, request.paymentMethod(), null, LocalDateTime.now(), errorMessage, errorMessage,
                null, user.getUserUuid().toString(), request.customerPaymentDetails1(),
                request.customerPaymentDetails2(), request.customerPaymentDetails3(), null
        );
    }

    private void saveTransactionRecord(String operation, PaymentCreateRequest request, PaymentResponse response, String clientRef) {
        TransactionRecordDto txRecord = new TransactionRecordDto(
                UUID.randomUUID(), UUID.fromString(response.userId()), operation, LocalDateTime.now(),
                clientRef, request.billerId(), request.customerAccountNumber(), request.amount(),
                request.currency(), request.paymentMethod(), request.customerPaymentDetails1(),
                request.customerPaymentDetails2(), request.customerPaymentDetails3(), response.status(),
                response.successful(), response.gatewayReference(), response.message(),
                response.tokenDetails(), !response.successful() ? response.message() : null
        );
        transactionRepository.save(txRecord);
    }

    private void savePaymentEntity(PaymentResponse response, User user) {
        com.yourcompany.payments.entity.Payment paymentEntity = new com.yourcompany.payments.entity.Payment();
        paymentEntity.setUser(user);
        paymentEntity.setGatewayReference(response.gatewayReference());
        paymentEntity.setPaymentReference(response.paymentReference());
        paymentEntity.setBillerId(response.billerId());
        paymentEntity.setCustomerAccount(response.customerAccount());
        paymentEntity.setAmount(response.amount());
        paymentEntity.setCurrency(response.currency());
        paymentEntity.setStatus(response.status());
        paymentEntity.setNarrative(response.narrative());
        paymentEntity.setPaymentMethod(response.paymentMethod());
        paymentEntity.setPaymentDate(response.paymentDate());
        paymentEntity.setSuccessful(response.successful());
        paymentEntity.setReceiptNumber(response.receiptNumber());
        paymentEntity.setReceiptDetails(response.receiptDetails());
        paymentEntity.setCustomerPaymentDetails1(response.tokenDetails());
        paymentEntity.setSource(egressSource);
        paymentEntity.setCustomerName(response.customerName());
        paymentRepository.save(paymentEntity);
    }

    private String generateUniqueReference() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String shortUuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("%s%s%s", egressSource.substring(0, 3).toUpperCase(), timestamp, shortUuid);
    }

    private String extractZetdcToken(String message, String billerId) {
        if ("ZETDC".equalsIgnoreCase(billerId) && message != null) {
            Pattern pattern = Pattern.compile("\\b\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}\\b");
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                log.info("ZETDC token extracted from message.");
                return matcher.group(0).replaceAll("[-\\s]", "");
            }
        }
        return null;
    }
}