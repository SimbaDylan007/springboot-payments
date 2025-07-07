package com.yourcompany.payments.repository;

import com.yourcompany.payments.dto.TransactionRecordDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class TransactionRepository {

    private final JdbcTemplate jdbcTemplate;

    public void save(TransactionRecordDto tx) {
        String sql = """
            INSERT INTO transactions (
                transaction_record_uuid, user_id, operation_type, event_timestamp,
                client_reference, biller_id, customer_account_number, amount,
                currency, payment_method, request_details1, request_details2,
                request_details3, status, successful_flag, gateway_transaction_id,
                gateway_message, token_details, error_details
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try {
            // FIX: Convert UUID objects to strings before passing them to the database.
            jdbcTemplate.update(sql,
                    tx.id().toString(), // Convert UUID to String
                    tx.userId() != null ? tx.userId().toString() : null, // Convert UUID to String safely
                    tx.operationType(),
                    tx.timestamp(),
                    tx.clientReference(),
                    tx.billerId(),
                    tx.customerAccountNumber(),
                    tx.amount(),
                    tx.currency(),
                    tx.paymentMethod(),
                    tx.requestDetails1(),
                    tx.requestDetails2(),
                    tx.requestDetails3(),
                    tx.status(),
                    tx.successfulFlag(),
                    tx.gatewayTransactionId(),
                    tx.gatewayMessage(),
                    tx.tokenDetails(),
                    tx.errorDetails()
            );
        } catch (Exception e) {
            log.error("Failed to save transaction log record for client reference: {}. Error: {}",
                    tx.clientReference(), e.getMessage(), e);
        }
    }
}