package com.howie.pharmacy.pharmacy_store.rabbitmq.event;

import java.io.Serializable;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateEvent implements Serializable {
    private Integer orderId;
    private String orderCode;
    private Integer userId;
}
