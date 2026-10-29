package com.howie.pharmacy.pharmacy_store.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentReconciliationTask {
    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationTask.class);

    private final PaymentService paymentService;

    public PaymentReconciliationTask(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // Bắt các đơn thanh toán online bị bỏ dở hoặc lỡ webhook, và trả lại tồn kho khi đơn hết hạn
    @Scheduled(fixedDelayString = "${pharmacy.payment.reconcile-delay:300000}")
    public void reconcile() {
        for (Integer orderId : paymentService.findStaleOrderIds()) {
            try {
                paymentService.settleStaleOrder(orderId);
            } catch (Exception e) {
                log.warn("Failed to settle stale order {}: {}", orderId, e.getMessage());
            }
        }
    }
}
