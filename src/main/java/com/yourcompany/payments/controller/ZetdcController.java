package com.yourcompany.payments.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yourcompany.payments.dto.payment.PaymentCreateRequest;
import com.yourcompany.payments.dto.payment.PaymentResponse;
import com.yourcompany.payments.dto.zetdc.ZetdcValidationResponse;
import com.yourcompany.payments.entity.User;
import com.yourcompany.payments.service.biller.EgressPaymentService;
import com.yourcompany.payments.wsdl.ValidateCustomerAccountResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/zetdc")
@RequiredArgsConstructor
public class ZetdcController {

    private final EgressPaymentService egressPaymentService;
    private static final String BILLER_ID = "ZETDC";

    @GetMapping("/validate")
    public ResponseEntity<ZetdcValidationResponse> validateAccount(@RequestParam("account_number") String accountNumber) throws JsonProcessingException {
        ZetdcValidationResponse validationResult = egressPaymentService.validateZetdc(accountNumber);
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

    // TODO: Implement /retry and /reverse endpoints
}