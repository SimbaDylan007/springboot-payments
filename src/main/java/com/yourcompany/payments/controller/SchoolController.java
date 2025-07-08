package com.yourcompany.payments.controller;

import com.yourcompany.payments.dto.payment.PaymentCreateRequest;
import com.yourcompany.payments.dto.payment.PaymentResponse;
import com.yourcompany.payments.dto.school.SchoolType;
import com.yourcompany.payments.dto.school.SchoolValidationResponse;
import com.yourcompany.payments.entity.User;
import com.yourcompany.payments.service.biller.EgressPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/school")
@RequiredArgsConstructor
public class SchoolController {

    private final EgressPaymentService egressPaymentService;

    @GetMapping("/validate")
    public ResponseEntity<SchoolValidationResponse> validateAccount(
            @RequestParam("student_id") String studentId,
            @RequestParam(required = false) String term,
            @RequestParam("school_type") SchoolType schoolType,
            @RequestParam(required = false) String institutionId
    ) {
        SchoolValidationResponse validationResult = egressPaymentService.validateSchoolAccount(
                studentId,
                term,
                schoolType,
                institutionId
        );
        return ResponseEntity.ok(validationResult);
    }

    @PostMapping("/pay")
    public ResponseEntity<PaymentResponse> postPayment(
            @Valid @RequestBody PaymentCreateRequest payment,
            @AuthenticationPrincipal User currentUser) {

        // The logic for parsing customerPaymentDetails1 and setting the dynamic billerId
        // should be handled within the EgressPaymentService.postPayment method
        // to keep the controller clean.
        if (payment.customerPaymentDetails1() == null || payment.customerPaymentDetails1().isEmpty()) {
            throw new IllegalArgumentException("customerPaymentDetails1 is required for school payments.");
        }

        PaymentResponse response = egressPaymentService.postPayment(payment, currentUser.getUserUuid());
        return ResponseEntity.ok(response);
    }

    // TODO: Implement /retry and /reverse endpoints based on Python logic
}