package com.yourcompany.payments.service.biller;

// --- Imports for DTOs and Entities ---
import com.yourcompany.payments.dto.TransactionRecordDto;
import com.yourcompany.payments.dto.nyaradzo.NyaradzoValidationResponse;
import com.yourcompany.payments.dto.nyaradzo.PolicyStatus;
import com.yourcompany.payments.dto.payment.PaymentCreateRequest;
import com.yourcompany.payments.dto.payment.PaymentResponse;
import com.yourcompany.payments.dto.school.SchoolType;
import com.yourcompany.payments.dto.school.SchoolValidationResponse;
import com.yourcompany.payments.dto.school.ZbSchoolApiResponse;
import com.yourcompany.payments.entity.User;
import com.yourcompany.payments.exception.BillerValidationException;
import com.yourcompany.payments.exception.PaymentFailedException;
import com.yourcompany.payments.repository.PaymentRepository;
import com.yourcompany.payments.repository.TransactionRepository;
import com.yourcompany.payments.repository.UserRepository;

// --- Imports for JAXB (WSDL-generated classes) ---
import com.yourcompany.payments.wsdl.ObjectFactory;
import com.yourcompany.payments.wsdl.PostPayment;
import com.yourcompany.payments.wsdl.PostPaymentResponse;
import com.yourcompany.payments.wsdl.ValidateCustomerAccount;
import com.yourcompany.payments.wsdl.ValidateCustomerAccountResponse;
import com.yourcompany.payments.wsdl.ValidationResponse; // Import ValidationResponse explicitly
import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

// --- Standard Java and Spring Imports ---
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.ws.client.core.WebServiceTemplate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class EgressPaymentService {

    // --- Injected Dependencies ---
    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final WebServiceTemplate webServiceTemplate;
    private final RestTemplate restTemplate;

    // --- Configuration Properties ---
    @Value("${payments.school.primary.endpoint.url}")
    private String schoolApiUrl;

    @Value("${egress.source}")
    private String egressSource;

    // --- Constants ---
    private static final String WSDL_NAMESPACE_URI = "http://ws.billpayment.zb.co.zw/";
    private static final String NYARADZO_BILLER_ID = "NYARADZO";

    // ===================================================================================
    // CORE SOAP METHODS (Your existing, correct implementations)
    // ===================================================================================

    public ValidateCustomerAccountResponse validateAccount(String billerId, String customerAccount) {
        log.info("[EGRESS-VALIDATE] START - Biller: {}, Account: {}", billerId, customerAccount);
        ObjectFactory factory = new ObjectFactory();
        ValidateCustomerAccount requestBody = factory.createValidateCustomerAccount();
        requestBody.setSource(egressSource);
        requestBody.setBillerId(billerId);
        requestBody.setCustomerAccount(customerAccount);

        QName qName = new QName(WSDL_NAMESPACE_URI, "validateCustomerAccount");
        JAXBElement<ValidateCustomerAccount> jaxbRequest = new JAXBElement<>(qName, ValidateCustomerAccount.class, requestBody);

        try {
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

    @Transactional
    public PaymentResponse postPayment(PaymentCreateRequest request, UUID userUuid) {
        String clientReference = generateUniqueReference();
        User user = userRepository.findByUserUuid(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found with UUID: " + userUuid));

        try {
            log.info("[EGRESS-PAY] START - Biller: {}, Ref: {}", request.billerId(), clientReference);
            ObjectFactory factory = new ObjectFactory();
            PostPayment requestPayload = factory.createPostPayment();
            com.yourcompany.payments.wsdl.Payment wsdlPayment = createWsdlPaymentObject(factory, request, user, clientReference);
            requestPayload.setPayment(wsdlPayment);

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

    // ===================================================================================
    // BILLER-SPECIFIC PUBLIC METHODS
    // ===================================================================================

    // --- School Methods ---
    public SchoolValidationResponse validateSchoolAccount(String studentId, String term, SchoolType schoolType, String institutionId) {
        if (schoolType == SchoolType.UNIVERSITY) {
            return validateUniversityStudent(studentId, institutionId);
        } else {
            return validatePrimaryOrSecondarySchool(studentId, term);
        }
    }

    // --- Nyaradzo Methods ---
    public NyaradzoValidationResponse validateNyaradzoPolicy(String policyNumber, int months) {
        String validationAccountString = String.format("%s|%s", policyNumber, months);
        log.info("Initiating validation for Nyaradzo policy: {}", validationAccountString);
        ValidateCustomerAccountResponse rawResponse = this.validateAccount(NYARADZO_BILLER_ID, validationAccountString);

        // Get the ValidationResponse object.
        ValidationResponse responseData = rawResponse.getReturn();

        // Check for success using the 'isSuccessful()' method from the WSDL
        if (!responseData.isSuccessful()) {
            // The WSDL shows 'responseDetails' will contain the error message.
            String errorMessage = responseData.getResponseDetails();
            log.warn("Validation SOAP call failed for {}. Biller response: {}", validationAccountString, errorMessage);
            throw new BillerValidationException("Nyaradzo policy validation failed. Details: " + errorMessage);
        }
        return mapToNyaradzoValidationResponse(rawResponse, months);
    }

    public PaymentResponse postNyaradzoPayment(PaymentCreateRequest payment, UUID userUuid) {
        log.info("Initiating payment process for Nyaradzo policy: {}", payment.customerAccountNumber());
        if (payment.months() == null || payment.months() < 1 || payment.months() > 12) {
            throw new IllegalArgumentException("Field 'months' is required and must be between 1 and 12.");
        }
        int monthsToPay = payment.months();
        String policyNumber = payment.customerAccountNumber();
        NyaradzoValidationResponse validation = this.validateNyaradzoPolicy(policyNumber, monthsToPay);

        final Set<PolicyStatus> payableStatuses = Set.of(PolicyStatus.ACTIVE, PolicyStatus.LAPSED);
        if (!payableStatuses.contains(validation.getPolicyStatus())) {
            throw new BillerValidationException("Cannot process payment for policy with status: " + validation.getPolicyStatus());
        }

        BigDecimal clientAmount = payment.amount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal amountDueFromValidation = validation.getAmountDue().setScale(2, RoundingMode.HALF_UP);

        if (clientAmount.compareTo(amountDueFromValidation) != 0) {
            throw new BillerValidationException(String.format("Payment amount %s does not match the amount due (%s) for %d months.", clientAmount, amountDueFromValidation, monthsToPay));
        }

        String paymentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String customerPaymentDetails3 = String.join("|",
                validation.getPolicyNumber(),
                paymentDate,
                String.valueOf(monthsToPay),
                policyNumber,
                clientAmount.toPlainString(),
                validation.getCurrency(),
                validation.getPolicyHolder(),
                clientAmount.toPlainString());

        String soapCustomerAccount = String.format("%s|%s", policyNumber, monthsToPay);

        // FIX: Reordered arguments to EXACTLY match the PaymentCreateRequest constructor from the compiler error.
        // It's: (customerAccountNumber, amount, paymentMethod, currency, billerId, months, d1, d2, d3, d4, d5, notes)
        PaymentCreateRequest finalPaymentRequest = new PaymentCreateRequest(
                soapCustomerAccount,        // 1. String customerAccountNumber
                clientAmount,               // 2. BigDecimal amount
                payment.paymentMethod(),    // 3. String paymentMethod
                validation.getCurrency(),   // 4. String currency
                NYARADZO_BILLER_ID,         // 5. String billerId (This was the missing String at position 5!)
                monthsToPay,                // 6. Integer months
                payment.customerPaymentDetails1(), // 7. String customerPaymentDetails1
                payment.customerPaymentDetails2(), // 8. String customerPaymentDetails2
                customerPaymentDetails3,           // 9. String customerPaymentDetails3
                payment.customerPaymentDetails4(), // 10. String customerPaymentDetails4
                payment.customerPaymentDetails5(), // 11. String customerPaymentDetails5
                null // 12. String notes (or other final String field, if any)
        );

        log.info("All validations passed. Posting payment for policy {}.", policyNumber);
        return this.postPayment(finalPaymentRequest, userUuid);
    }

    // Added generic retry and reverse method stubs to satisfy the compiler
    public PaymentResponse retryNyaradzoPayment(String originalReference) {
        log.info("Retrying Nyaradzo payment for reference: {}", originalReference);
        // TODO: Implement actual SOAP call for retry using the originalReference
        throw new UnsupportedOperationException("Retry functionality not yet implemented.");
    }

    public PaymentResponse reverseNyaradzoPayment(String originalReference) {
        log.info("Reversing Nyaradzo payment for reference: {}", originalReference);
        // TODO: Implement actual SOAP call for reversal using the originalReference
        throw new UnsupportedOperationException("Reversal functionality not yet implemented.");
    }

    // ===================================================================================
    // PRIVATE HELPER METHODS
    // ===================================================================================

    private SchoolValidationResponse validateUniversityStudent(String studentId, String institutionId) {
        if (institutionId == null || institutionId.isBlank()) {
            throw new IllegalArgumentException("Institution ID is required for university validation");
        }
        ValidateCustomerAccountResponse soapResponse = this.validateAccount(institutionId, studentId);

        // Get the ValidationResponse object.
        ValidationResponse responseData = soapResponse.getReturn();

        // Check for success using the 'isSuccessful()' method from the WSDL
        if (!responseData.isSuccessful()) {
            log.warn("University validation failed for student {}. Biller response: {}", studentId, responseData.getResponseDetails());
            throw new RuntimeException("University validation failed. Details: " + responseData.getResponseDetails());
        }

        String[] parts = responseData.getResponseDetails().split("\\|");
        if (parts.length < 3) {
            throw new RuntimeException("Could not parse university validation response.");
        }

        return SchoolValidationResponse.builder()
                .successful(true)
                .studentId(parts[0].trim())
                .studentName(parts[1].trim())
                .grade(parts[2].trim())
                .term(parts[2].trim())
                // The `validationResponse` in WSDL does not have a 'currency' field directly.
                // Based on Python code, it defaults to USD for Universities.
                .currency("USD")
                .feeAmount(0.0)
                .outstandingBalance(0.0)
                .build();
    }

    private SchoolValidationResponse validatePrimaryOrSecondarySchool(String schoolAccountNumber, String term) {
        try {
            log.info("Calling primary/secondary school API for account: {}", schoolAccountNumber);
            ZbSchoolApiResponse apiResponse = restTemplate.getForObject(schoolApiUrl, ZbSchoolApiResponse.class, schoolAccountNumber);
            if (apiResponse == null || !"00".equals(apiResponse.getResponseCode()) || apiResponse.getBody() == null) {
                String errorMsg = apiResponse != null ? apiResponse.getMessage() : "Unknown error from school service";
                log.warn("School validation failed for account {}: {}", schoolAccountNumber, errorMsg);
                throw new RuntimeException("School validation failed: " + errorMsg);
            }
            ZbSchoolApiResponse.Body body = apiResponse.getBody();
            return SchoolValidationResponse.builder()
                    .successful(true)
                    .studentId(schoolAccountNumber)
                    .studentName("To be provided by user")
                    .grade("To be provided by user")
                    .term(term != null ? term : "Current Term")
                    .schoolName(body.getAccountName())
                    .accountNumber(body.getAccountNumber())
                    .currency(body.getCurrency())
                    .feeAmount(0.0)
                    .outstandingBalance(0.0)
                    .build();
        } catch (RestClientException e) {
            log.error("Error calling school validation REST API: {}", e.getMessage());
            throw new RuntimeException("Could not connect to the school validation service.");
        }
    }

    private NyaradzoValidationResponse mapToNyaradzoValidationResponse(ValidateCustomerAccountResponse rawResponse, int months) {
        // Get the ValidationResponse object.
        ValidationResponse responseData = rawResponse.getReturn();
        String[] details = responseData.getResponseDetails().split("\\|");

        // Based on the log: Sam Doe|5.0|5.0|USD|1|SCPK1111|ACTIVE|2024-11-07|5.0|NY1111|200
        // Corrected indices based on the provided log format
        // details[0] = Sam Doe (policyHolder)
        // details[1] = 5.0 (monthlyPremium)
        // details[2] = 5.0 (amountDue)
        // details[3] = USD (currency)
        // details[6] = ACTIVE (policyStatus)
        // policyNumber can come from getCustomerAccount() or details[5]

        PolicyStatus status = Arrays.stream(PolicyStatus.values())
                .filter(s -> s.getValue().equalsIgnoreCase(details[6])) // Correct index for status
                .findFirst()
                .orElse(PolicyStatus.UNKNOWN);

        return NyaradzoValidationResponse.builder()
                .successful(true)
                .message("Validation successful.")
                .policyNumber(responseData.getCustomerAccount()) // Or details[5] if preferred from string
                .monthsValidated(months)
                .policyStatus(status)
                .policyHolder(details[0]) // Correct index for policy holder
                .monthlyPremium(new BigDecimal(details[1])) // Correct index for monthly premium
                .amountDue(new BigDecimal(details[2])) // Correct index for amount due
                .currency(details[3]) // Correct index for currency
                .build();
    }

    // --- Your original helper methods (unchanged) ---
    private com.yourcompany.payments.wsdl.Payment createWsdlPaymentObject(ObjectFactory factory, PaymentCreateRequest request, User user, String clientReference) {
        com.yourcompany.payments.wsdl.Payment wsdlPayment = factory.createPayment();
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
        com.yourcompany.payments.wsdl.Payment wsdlPayment = gatewayResponse.getPayment();
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