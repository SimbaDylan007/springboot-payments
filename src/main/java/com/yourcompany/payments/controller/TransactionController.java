package com.yourcompany.payments.controller;

import com.yourcompany.payments.dto.transaction.TransactionResponse;
import com.yourcompany.payments.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yourcompany.payments.entity.User; // Your custom User entity

import java.util.List;

@RestController
@RequestMapping("/api/v1/transaction")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/history")
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory(
            @AuthenticationPrincipal User currentUser) { // Assuming your UserDetails is your User entity

        List<TransactionResponse> history = transactionService.getHistoryForUser(currentUser.getUserUuid());
        return ResponseEntity.ok(history);
    }

    // ... other transaction-related endpoints like /store-pop
}