package com.yourcompany.payments.controller;

import com.yourcompany.payments.dto.payment.PaymentCreateRequest;
import com.yourcompany.payments.dto.payment.PaymentResponse;
import com.yourcompany.payments.entity.User;
import com.yourcompany.payments.service.biller.EgressPaymentService;
import com.yourcompany.payments.wsdl.ValidateCustomerAccountResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/coh")
@RequiredArgsConstructor
public class CohController {

    private final EgressPaymentService egressPaymentService;
    private static final String BILLER_ID = "COH";

    @GetMapping("/validate")
    public ResponseEntity<ValidateCustomerAccountResponse> validateAccount(@RequestParam("customer_account") String customerAccount) {
        ValidateCustomerAccountResponse validationResult = egressPaymentService.validateAccount(BILLER_ID, customerAccount);
        return ResponseEntity.ok(validationResult);
    }

    @PostMapping("/pay")
    public ResponseEntity<PaymentResponse> postPayment(
            @Valid @RequestBody PaymentCreateRequest payment,
            @AuthenticationPrincipal User currentUser) {

        if (!BILLER_ID.equalsIgnoreCase(payment.billerId())) {
            throw new IllegalArgumentException("Invalid billerId for this endpoint.");
        }

        // This COH-specific validation is preserved
        List<String> validMethods = List.of("ATM", "BATCH", "INTERNET", "MOBILE", "POS", "OTHER");
        if (!validMethods.contains(payment.customerPaymentDetails1())) {
            throw new IllegalArgumentException("customerPaymentDetails1 must be one of: " + String.join(", ", validMethods));
        }

        List<String> validTypes = List.of("ACCOUNT", "CASH", "CARD", "OTHER");
        if (!validTypes.contains(payment.customerPaymentDetails2())) {
            throw new IllegalArgumentException("customerPaymentDetails2 must be one of: " + String.join(", ", validTypes));
        }

        PaymentResponse response = egressPaymentService.postPayment(payment, currentUser.getUserUuid());
        return ResponseEntity.ok(response);
    }

    // TODO: Implement /retry and /reverse endpoints
}