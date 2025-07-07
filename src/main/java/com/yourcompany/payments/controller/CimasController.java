package com.yourcompany.payments.controller;

import com.yourcompany.payments.dto.payment.PaymentCreateRequest;
import com.yourcompany.payments.dto.payment.PaymentResponse;
import com.yourcompany.payments.entity.User;
import com.yourcompany.payments.service.biller.EgressPaymentService;
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
    public ResponseEntity<?> validateAccount(@RequestParam("customer_account") String customerAccount) {
        // TODO: Call a specific validation method in EgressPaymentService
        // Map<String, Object> result = egressPaymentService.validateCimasAccount(customerAccount);
        // return ResponseEntity.ok(result);
        return ResponseEntity.ok("Validation endpoint not yet implemented.");
    }

    @PostMapping("/pay")
    public ResponseEntity<PaymentResponse> postPayment(
            @Valid @RequestBody PaymentCreateRequest payment,
            @AuthenticationPrincipal User currentUser) {

        if (!BILLER_ID.equalsIgnoreCase(payment.billerId())) {
            throw new IllegalArgumentException("Invalid billerId for this endpoint.");
        }

        if (!"M".equals(payment.customerPaymentDetails2()) && !"E".equals(payment.customerPaymentDetails2())) {
            throw new IllegalArgumentException("customerPaymentDetails2 must be either 'M' (Member) or 'E' (Payer)");
        }

        PaymentResponse response = egressPaymentService.postPayment(payment, currentUser.getUserUuid());
        return ResponseEntity.ok(response);
    }

    // TODO: Implement /retry and /reverse endpoints, calling corresponding service methods
}