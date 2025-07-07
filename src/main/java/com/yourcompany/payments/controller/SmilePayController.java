package com.yourcompany.payments.controller;

import com.yourcompany.payments.dto.smilepay.PaymentInitiateRequest;
import com.yourcompany.payments.dto.smilepay.PaymentInitiateResponse;
import com.yourcompany.payments.entity.User;
import com.yourcompany.payments.service.biller.SmilePayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/smilepay")
@RequiredArgsConstructor
public class SmilePayController {

    private final SmilePayService smilePayService;

    @PostMapping("/initiate-payment")
    public ResponseEntity<PaymentInitiateResponse> initiatePayment(
            @RequestBody PaymentInitiateRequest request,
            @AuthenticationPrincipal User currentUser) {

        PaymentInitiateResponse response = smilePayService.initiateStandardPayment(request, currentUser);
        return ResponseEntity.ok(response);
    }
}