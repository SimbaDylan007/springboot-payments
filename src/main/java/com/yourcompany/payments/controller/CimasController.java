package com.yourcompany.payments.controller;

import com.yourcompany.payments.dto.payment.PaymentCreateRequest;
import com.yourcompany.payments.dto.payment.PaymentResponse;
import com.yourcompany.payments.entity.User;
import com.yourcompany.payments.service.biller.EgressPaymentService;
// FIX: Import the correct response class, just like in DovesController
import com.yourcompany.payments.wsdl.ValidateCustomerAccountResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cimas")
@RequiredArgsConstructor
public class CimasController {

    private final EgressPaymentService egressPaymentService;
    private static final String BILLER_ID = "CIMAS";

    @GetMapping("/validate")
    // FIX: Changed return type from ResponseEntity<?> to the specific response object
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

        // This Cimas-specific validation is preserved
        if (!"M".equals(payment.customerPaymentDetails2()) && !"E".equals(payment.customerPaymentDetails2())) {
            throw new IllegalArgumentException("customerPaymentDetails2 must be either 'M' (Member) or 'E' (Payer)");
        }

        PaymentResponse response = egressPaymentService.postPayment(payment, currentUser.getUserUuid());
        return ResponseEntity.ok(response);
    }

    // TODO: Implement /retry and /reverse endpoints, calling corresponding service methods
}