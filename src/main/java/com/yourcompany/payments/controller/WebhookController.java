package com.yourcompany.payments.controller;

import com.yourcompany.payments.dto.smilepay.SmilePayWebhookPayload;
import com.yourcompany.payments.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/smilepay")
    public ResponseEntity<String> handleSmilePayWebhook(@RequestBody SmilePayWebhookPayload payload) {
        log.info("Received SmilePay webhook for order: {}", payload.orderReference());
        webhookService.processSmilePayNotification(payload);
        return ResponseEntity.ok("Webhook received.");
    }
}