package com.howie.pharmacy.pharmacy_store.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.howie.pharmacy.pharmacy_store.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    @Query("SELECT o FROM Order o JOIN o.shippingAddress s WHERE o.user IS NULL AND s.phone = :phone")
    List<Order> findByPhoneAndUserIsNull(@Param("phone") String phone);

    List<Order> findByUserId(Integer userId);
}
