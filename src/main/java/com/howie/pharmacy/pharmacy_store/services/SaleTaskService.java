package com.howie.pharmacy.pharmacy_store.services;

import java.time.LocalDateTime;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.howie.pharmacy.pharmacy_store.repository.ProductRepository;

@Service
public class SaleTaskService {
    private final ProductRepository productRepository;

    public SaleTaskService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        System.out.println("Ứng dụng đã khởi động. Kiểm tra và cập nhật các sản phẩm hết hạn khuyến mãi...");
    }

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void updateExpiredSales() {
        System.out.println("Đang kiểm tra và cập nhật các sản phẩm hết hạn khuyến mãi...");

        LocalDateTime currentTime = LocalDateTime.now();

        if (!productRepository.existsExpiredSales(currentTime)) {
            System.out.println("Không có sản phẩm nào hết hạn khuyến mãi cần cập nhật.");
            return;
        }

        int updatedProducts = productRepository.closeExpiredSales(currentTime);

        if (updatedProducts > 0) {
            System.out.println("Đã cập nhật " + updatedProducts + " sản phẩm hết hạn khuyến mãi.");
        }
    }

}
