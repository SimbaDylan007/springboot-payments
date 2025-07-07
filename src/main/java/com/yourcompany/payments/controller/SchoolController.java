package com.yourcompany.payments.controller;

import com.yourcompany.payments.entity.User;
import com.yourcompany.payments.service.biller.EgressPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.yourcompany.payments.dto.payment.PaymentCreateRequest;
import com.yourcompany.payments.dto.payment.PaymentResponse;

@RestController
@RequestMapping("/api/v1/school")
@RequiredArgsConstructor
public class SchoolController {

    private final EgressPaymentService egressPaymentService;
    // Biller ID for schools is dynamic, so we won't use a static final here.

    @GetMapping("/validate")
    public ResponseEntity<?> validateAccount(
            @RequestParam("student_id") String studentId,
            @RequestParam(required = false) String term,
            @RequestParam("school_type") String schoolType,
            @RequestParam(required = false) String institutionId
    ) {
        // TODO: Call validation method in EgressPaymentService
        return ResponseEntity.ok("Validation endpoint not yet implemented.");
    }

    @PostMapping("/pay")
    public ResponseEntity<PaymentResponse> postPayment(
            @Valid @RequestBody PaymentCreateRequest payment,
            @AuthenticationPrincipal User currentUser) {

        if (payment.customerPaymentDetails1() == null || payment.customerPaymentDetails1().split("\\|").length < 6) {
            throw new IllegalArgumentException("customerPaymentDetails1 must contain at least 6 pipe-delimited values.");
        }

        // Logic to determine the real billerId from customerPaymentDetails1
        // This should be done inside the EgressPaymentService before making the SOAP call.

        PaymentResponse response = egressPaymentService.postPayment(payment, currentUser.getUserUuid());
        return ResponseEntity.ok(response);
    }

    // TODO: Implement /retry and /reverse endpoints
}