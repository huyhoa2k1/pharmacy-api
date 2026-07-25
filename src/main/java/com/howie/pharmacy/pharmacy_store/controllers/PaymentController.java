package com.howie.pharmacy.pharmacy_store.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PayOS payOS;

    public PaymentController(PayOS payOS) {
        this.payOS = payOS;
    }

    @PostMapping({ "/create-payment-link", "/create-embedded-link" })
    public ResponseEntity<?> createPaymentLink(@RequestBody Map<String, Object> requestData) {
        try {
            long orderCode = System.currentTimeMillis();
            long amount = ((Number) requestData.get("amount")).longValue();

            CreatePaymentLinkRequest request = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .description("Thanh toán đơn hàng")
                    .amount(amount)
                    .cancelUrl("http://localhost:5173")
                    .returnUrl("http://localhost:5173")
                    .build();

            CreatePaymentLinkResponse response = payOS.paymentRequests().create(request);
            System.out.println("Response: " + response);
            return ResponseEntity.ok(Map.of(
                    "checkoutUrl", response.getCheckoutUrl(),
                    "orderCode", orderCode));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid request data: " + e.getMessage());
        }
    }
}
