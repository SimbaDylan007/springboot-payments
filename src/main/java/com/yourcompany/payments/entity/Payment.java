package com.yourcompany.payments.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_gateway_ref", columnList = "gatewayReference"),
        @Index(name = "idx_payment_payment_ref", columnList = "paymentReference")
})

@Data
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    private String gatewayReference;

    private String paymentReference;

    private String billerId;
    private String source;
    private String customerAccount;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String narrative;
    private String customerName;
    private String paymentMethod;
    private String paymentType;

    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime paymentDate;

    private String customerPaymentDetails1;
    private String customerPaymentDetails2;
    private String customerPaymentDetails3;
    private String customerPaymentDetails4;
    private String customerPaymentDetails5;

    private boolean successful;
    private String receiptNumber;
    private String receiptDetails;

    @Column(updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}