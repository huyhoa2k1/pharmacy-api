package com.howie.pharmacy.pharmacy_store.controllers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.howie.pharmacy.pharmacy_store.dto.order.OrderResponseDto;
import com.howie.pharmacy.pharmacy_store.serviceImpl.ExportService;
import com.howie.pharmacy.pharmacy_store.serviceImpl.OrderServiceImpl;

@RestController
@RequestMapping("/api/export")
public class ExportController {
    private final OrderServiceImpl orderService;
    private final ExportService exportService;

    public ExportController(OrderServiceImpl orderService, ExportService exportService) {
        this.orderService = orderService;
        this.exportService = exportService;
    }

    @PostMapping("/orders")
    public ResponseEntity<Resource> exportAllOrders() throws IOException {
        List<OrderResponseDto> orders = orderService.getAllOrders();
        ByteArrayInputStream excelData = exportService.exportOrdersToExcel(orders);
        InputStreamResource resource = new InputStreamResource(excelData);

        String filename = "orders-list.xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }
}
