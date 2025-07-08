package com.yourcompany.payments.controller;

import com.yourcompany.payments.dto.doves.DovesValidationResponse;
import com.yourcompany.payments.dto.payment.PaymentCreateRequest;
import com.yourcompany.payments.dto.payment.PaymentResponse;
import com.yourcompany.payments.entity.User;
import com.yourcompany.payments.service.biller.EgressPaymentService;
// FIX: Import the correct response class
import com.yourcompany.payments.wsdl.ValidateCustomerAccountResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/doves")
@RequiredArgsConstructor
public class DovesController {

    private final EgressPaymentService egressPaymentService;
    private static final String BILLER_ID = "DOVES";

    @GetMapping("/validate")
    // FIX: The method now returns the full response object
    public ResponseEntity<DovesValidationResponse> validateAccount(@RequestParam("customer_account") String customerAccount) {
        DovesValidationResponse validationResult = egressPaymentService.validateDovesPolicy(customerAccount);
        return ResponseEntity.ok(validationResult);
    }

    @PostMapping("/pay")
    public ResponseEntity<PaymentResponse> postPayment(
            @Valid @RequestBody PaymentCreateRequest payment,
            @AuthenticationPrincipal User currentUser) {

        if (!BILLER_ID.equalsIgnoreCase(payment.billerId())) {
            throw new IllegalArgumentException("Invalid billerId for this endpoint.");
        }

        PaymentResponse response = egressPaymentService.postPayment(payment, currentUser.getUserUuid());
        return ResponseEntity.ok(response);
    }
}