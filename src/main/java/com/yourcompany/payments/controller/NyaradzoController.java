package com.yourcompany.payments.controller;

import com.yourcompany.payments.dto.nyaradzo.NyaradzoValidationResponse;
import com.yourcompany.payments.dto.payment.PaymentCreateRequest;
import com.yourcompany.payments.dto.payment.PaymentResponse;
import com.yourcompany.payments.entity.User; // Ensure User is imported
import com.yourcompany.payments.service.biller.EgressPaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // Ensure this is imported
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/nyaradzo")
@RequiredArgsConstructor
@Validated
public class NyaradzoController {

    private final EgressPaymentService egressPaymentService;

    @GetMapping("/validate")
    public ResponseEntity<NyaradzoValidationResponse> validateAccount(
            @RequestParam("policy_number") String policyNumber,
            @RequestParam("months") @Min(1) @Max(12) int months) {

        NyaradzoValidationResponse response = egressPaymentService.validateNyaradzoPolicy(policyNumber, months);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/pay")
    // FIX: Add @AuthenticationPrincipal to get the current user
    public ResponseEntity<PaymentResponse> postPayment(
            @Valid @RequestBody PaymentCreateRequest payment,
            @AuthenticationPrincipal User currentUser) {

        // FIX: Pass the user's UUID as the second argument to the service method
        PaymentResponse response = egressPaymentService.postNyaradzoPayment(payment, currentUser.getUserUuid());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/retry/{original_payment_reference}")
    public ResponseEntity<PaymentResponse> retryPayment(@PathVariable String original_payment_reference) {
        PaymentResponse response = egressPaymentService.retryNyaradzoPayment(original_payment_reference);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reverse/{original_payment_reference}")
    public ResponseEntity<PaymentResponse> reversePayment(@PathVariable String original_payment_reference) {
        PaymentResponse response = egressPaymentService.reverseNyaradzoPayment(original_payment_reference);
        return ResponseEntity.ok(response);
    }
}