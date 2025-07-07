-- This script will be run automatically by Spring Boot on startup if the table doesn't exist.
-- It creates the 'transactions' table for detailed logging, which is managed by JdbcTemplate.

CREATE TABLE IF NOT EXISTS transactions (
                                            transaction_record_uuid VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id VARCHAR(36),
    operation_type VARCHAR(50),
    event_timestamp TIMESTAMP,
    client_reference VARCHAR(100),
    biller_id VARCHAR(50),
    customer_account_number VARCHAR(100),
    amount DECIMAL(19, 2),
    currency VARCHAR(10),
    payment_method VARCHAR(50),
    request_details1 TEXT,
    request_details2 TEXT,
    request_details3 TEXT,
    status VARCHAR(50),
    successful_flag BOOLEAN,
    gateway_transaction_id VARCHAR(100),
    gateway_message TEXT,
    token_details TEXT,
    error_details TEXT
    );