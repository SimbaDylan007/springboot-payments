package com.yourcompany.payments.repository;

import com.yourcompany.payments.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    Optional<Payment> findByGatewayReference(String gatewayReference);
    List<Payment> findByUser_UserUuidOrderByPaymentDateDesc(UUID userUuid);
}