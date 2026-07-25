package com.howie.pharmacy.pharmacy_store.serviceImpl;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.howie.pharmacy.pharmacy_store.dto.order.OrderResponseDto;

@Service
public class ExportService {
    public ByteArrayInputStream exportOrdersToExcel(List<OrderResponseDto> orders) throws IOException {
        String[] columns = { "Mã đơn hàng", "Tên khách hàng", "Số điện thoại", "Tổng tiền", "Phương thức thanh toán",
                "Trạng thái" };
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Orders");

            // Create header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
            }

            // Create data rows
            int rowIdx = 1;
            for (OrderResponseDto order : orders) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(order.getId().toString());
                row.createCell(1).setCellValue(order.getShippingAddress().getFullname());
                row.createCell(2).setCellValue(order.getShippingAddress().getPhone());
                row.createCell(3).setCellValue(order.getTotalPrice().doubleValue());
                row.createCell(4).setCellValue(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "");
                row.createCell(5).setCellValue(order.getStatus() != null ? order.getStatus().name() : "");
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}
