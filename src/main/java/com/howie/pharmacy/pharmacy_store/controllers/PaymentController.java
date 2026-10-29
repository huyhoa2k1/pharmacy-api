package com.howie.pharmacy.pharmacy_store.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.howie.pharmacy.pharmacy_store.dto.payment.PaymentLinkDto;
import com.howie.pharmacy.pharmacy_store.dto.payment.PaymentStatusDto;
import com.howie.pharmacy.pharmacy_store.exception.AppExceptions.BadRequestException;
import com.howie.pharmacy.pharmacy_store.services.PaymentService;

import vn.payos.model.webhooks.Webhook;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // Tạo link thanh toán cho đơn hàng đã tạo (paymentMethod = CARD)
    @PostMapping("/create-payment-link")
    public ResponseEntity<?> createPaymentLink(@RequestBody Map<String, Object> requestData) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!(requestData.get("orderId") instanceof Number orderId)) {
            throw new BadRequestException("orderId is required");
        }
        PaymentLinkDto link = paymentService.createPaymentLink(orderId.intValue());
        return ResponseEntity.ok(link);
    }

    // PayOS gọi endpoint này khi thanh toán xong; chữ ký được kiểm tra bằng checksum-key
    @PostMapping("/payos-webhook")
    public ResponseEntity<?> handleWebhook(@RequestBody Webhook webhook) {
        paymentService.handleWebhook(webhook);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // Frontend gọi sau khi PayOS chuyển về returnUrl/cancelUrl (query param orderCode)
    @GetMapping("/status/{payosOrderCode}")
    public ResponseEntity<PaymentStatusDto> getPaymentStatus(@PathVariable Long payosOrderCode) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(payosOrderCode));
    }
}
