package com.yourcompany.payments.service;

import com.yourcompany.payments.dto.transaction.TransactionResponse;
import com.yourcompany.payments.entity.Payment;
import com.yourcompany.payments.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final PaymentRepository paymentRepository;

    /**
     * Fetches the transaction history for a given user.
     * The data is retrieved from the 'payments' table, as this table holds the final
     * state of each transaction attempt.
     *
     * @param userUuid The UUID of the user whose transaction history is being requested.
     * @return A list of TransactionResponse DTOs, ordered by the most recent first.
     */
    @Transactional(readOnly = true) // Use a read-only transaction for query efficiency
    public List<TransactionResponse> getHistoryForUser(UUID userUuid) {
        log.info("Fetching transaction history for user UUID: {}", userUuid);

        // Use the repository method to find all payments for the user, ordered by date
        List<Payment> payments = paymentRepository.findByUser_UserUuidOrderByPaymentDateDesc(userUuid);

        log.debug("Found {} payment records for user UUID: {}", payments.size(), userUuid);

        // Map the list of Payment entities to a list of TransactionResponse DTOs
        return payments.stream()
                .map(this::mapPaymentToTransactionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Helper method to map a Payment entity to a TransactionResponse DTO.
     * This centralizes the conversion logic.
     *
     * @param payment The Payment entity from the database.
     * @return A TransactionResponse DTO.
     */
    private TransactionResponse mapPaymentToTransactionResponse(Payment payment) {
        return new TransactionResponse(
                payment.getId(),
                payment.getPaymentDate(), // or payment.getCreatedAt() depending on business logic
                payment.getBillerId(),
                payment.getAmount(),
                payment.getCustomerAccount(),
                payment.getStatus(),
                payment.getPaymentReference(),
                payment.getPaymentMethod(),
                payment.getCurrency(),
                payment.getCustomerPaymentDetails1(), // This field was used for the token in your python code
                payment.getReceiptNumber()
        );
    }
}