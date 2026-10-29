package com.howie.pharmacy.pharmacy_store.services;

import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.howie.pharmacy.pharmacy_store.dto.payment.PaymentLinkDto;
import com.howie.pharmacy.pharmacy_store.dto.payment.PaymentStatusDto;
import com.howie.pharmacy.pharmacy_store.entity.Order;
import com.howie.pharmacy.pharmacy_store.exception.AppExceptions.BadRequestException;
import com.howie.pharmacy.pharmacy_store.exception.AppExceptions.ResourceNotFoundException;
import com.howie.pharmacy.pharmacy_store.repository.OrderRepository;

import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    // PayOS gửi webhook thử với orderCode này khi xác nhận webhook URL
    private static final long PAYOS_CONFIRM_TEST_ORDER_CODE = 123L;
    private static final long LINK_TTL_SECONDS = 15 * 60;
    private static final long STALE_AFTER_MINUTES = 20;

    private final PayOS payOS;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final NotificationService notificationService;
    private final String frontendUrl;

    public PaymentService(PayOS payOS, OrderRepository orderRepository, OrderService orderService,
            NotificationService notificationService,
            @Value("${pharmacy.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.payOS = payOS;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.notificationService = notificationService;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    /** Tạo link thanh toán PayOS cho một đơn hàng online; số tiền lấy từ đơn hàng, không tin client. */
    @Transactional
    public PaymentLinkDto createPaymentLink(Integer orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (order.getPaymentMethod() != Order.Payment.CARD) {
            throw new BadRequestException("Order is not an online payment order");
        }

        Long previousCode = order.getPayosOrderCode();
        if (previousCode != null) {
            syncWithPayOS(order);
        }
        if (Boolean.TRUE.equals(order.getIsPaid())) {
            throw new BadRequestException("Order is already paid");
        }
        if (order.getStatus() == Order.Status.CANCEL) {
            throw new BadRequestException("Order was cancelled");
        }

        long newCode = newPayosOrderCode();
        CreatePaymentLinkRequest request = CreatePaymentLinkRequest.builder()
                .orderCode(newCode)
                .amount(order.getTotalPrice().setScale(0, RoundingMode.HALF_UP).longValueExact())
                .description("DH " + order.getOrderCode())
                .returnUrl(frontendUrl + "/payment/result")
                .cancelUrl(frontendUrl + "/payment/result")
                .expiredAt(Instant.now().getEpochSecond() + LINK_TTL_SECONDS)
                .build();
        CreatePaymentLinkResponse response = createLink(request);

        order.setPayosOrderCode(newCode);
        orderRepository.save(order);

        if (previousCode != null) {
            try {
                payOS.paymentRequests().cancel(previousCode);
            } catch (Exception e) {
                log.debug("Could not cancel previous PayOS link {}: {}", previousCode, e.getMessage());
            }
        }
        return new PaymentLinkDto(response.getCheckoutUrl(), newCode);
    }

    /** Xử lý webhook PayOS: xác thực chữ ký, đánh dấu đã thanh toán (idempotent). */
    @Transactional
    public void handleWebhook(Webhook webhook) {
        WebhookData data;
        try {
            data = payOS.webhooks().verify(webhook);
        } catch (Exception e) {
            throw new BadRequestException("Invalid webhook signature");
        }
        if (data.getOrderCode() == null || data.getOrderCode() == PAYOS_CONFIRM_TEST_ORDER_CODE) {
            return;
        }
        Order order = orderRepository.findByPayosOrderCodeForUpdate(data.getOrderCode()).orElse(null);
        if (order == null) {
            log.warn("PayOS webhook for unknown orderCode {}", data.getOrderCode());
            return;
        }
        if (!"00".equals(data.getCode())) {
            return;
        }
        long expected = order.getTotalPrice().setScale(0, RoundingMode.HALF_UP).longValue();
        if (data.getAmount() == null || data.getAmount() != expected) {
            log.error("PayOS webhook amount mismatch for order {}: expected {}, got {}", order.getId(), expected,
                    data.getAmount());
            return;
        }
        markPaid(order);
    }

    /** Truy vấn trạng thái thanh toán; nếu chưa có kết quả thì đối chiếu trực tiếp với PayOS. */
    @Transactional
    public PaymentStatusDto getPaymentStatus(Long payosOrderCode) {
        Order order = orderRepository.findByPayosOrderCodeForUpdate(payosOrderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (isPending(order)) {
            syncWithPayOS(order);
        }
        return toStatusDto(order);
    }

    /** Đơn online quá hạn: đối chiếu PayOS hoặc huỷ để trả lại tồn kho. */
    @Transactional
    public void settleStaleOrder(Integer orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElse(null);
        if (order == null || !isPending(order)) {
            return;
        }
        if (order.getPayosOrderCode() == null) {
            cancelUnpaid(order);
            return;
        }
        syncWithPayOS(order);
    }

    public List<Integer> findStaleOrderIds() {
        return orderRepository.findStaleUnpaidOnlineOrderIds(LocalDateTime.now().minusMinutes(STALE_AFTER_MINUTES));
    }

    private CreatePaymentLinkResponse createLink(CreatePaymentLinkRequest request) {
        try {
            return payOS.paymentRequests().create(request);
        } catch (Exception e) {
            log.error("Failed to create PayOS payment link", e);
            throw new BadRequestException("Could not create payment link: " + e.getMessage());
        }
    }

    private void syncWithPayOS(Order order) {
        try {
            PaymentLink link = payOS.paymentRequests().get(order.getPayosOrderCode());
            PaymentLinkStatus status = link.getStatus();
            if (status == PaymentLinkStatus.PAID) {
                markPaid(order);
            } else if (status == PaymentLinkStatus.CANCELLED || status == PaymentLinkStatus.EXPIRED
                    || status == PaymentLinkStatus.FAILED) {
                cancelUnpaid(order);
            }
        } catch (Exception e) {
            log.warn("Could not sync order {} with PayOS: {}", order.getId(), e.getMessage());
        }
    }

    private void markPaid(Order order) {
        if (Boolean.TRUE.equals(order.getIsPaid())) {
            return;
        }
        if (order.getStatus() == Order.Status.CANCEL) {
            log.warn("Order {} was already cancelled but a payment was received; manual refund may be needed",
                    order.getId());
        }
        order.setIsPaid(true);
        orderRepository.save(order);
        try {
            notificationService.createNotification("Order paid",
                    Map.of("orderId", order.getId(), "orderCode", order.getOrderCode()));
        } catch (Exception e) {
            log.warn("Failed to create paid notification for order {}: {}", order.getId(), e.getMessage());
        }
    }

    private void cancelUnpaid(Order order) {
        if (Boolean.TRUE.equals(order.getIsPaid()) || order.getStatus() == Order.Status.CANCEL) {
            return;
        }
        orderService.updateOrderStatus(order.getId(), Order.Status.CANCEL);
    }

    private boolean isPending(Order order) {
        return !Boolean.TRUE.equals(order.getIsPaid()) && order.getStatus() != Order.Status.CANCEL;
    }

    private PaymentStatusDto toStatusDto(Order order) {
        String paymentStatus = Boolean.TRUE.equals(order.getIsPaid()) ? "PAID"
                : order.getStatus() == Order.Status.CANCEL ? "CANCELLED" : "PENDING";
        return new PaymentStatusDto(order.getId(), order.getOrderCode(), order.getPayosOrderCode(), paymentStatus,
                order.getStatus(), order.getTotalPrice());
    }

    // Giữ trong giới hạn số nguyên an toàn của JavaScript (2^53 - 1)
    private long newPayosOrderCode() {
        return System.currentTimeMillis() * 1000 + ThreadLocalRandom.current().nextInt(1000);
    }
}
