package com.howie.pharmacy.pharmacy_store.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.howie.pharmacy.pharmacy_store.entity.Order;

import jakarta.persistence.LockModeType;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    @Query("SELECT o FROM Order o JOIN o.shippingAddress s WHERE o.user IS NULL AND s.phone = :phone")
    List<Order> findByPhoneAndUserIsNull(@Param("phone") String phone);

    List<Order> findByUserId(Integer userId);

    // Khoá bản ghi để webhook và API truy vấn trạng thái không cập nhật trùng đồng thời
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.payosOrderCode = :code")
    Optional<Order> findByPayosOrderCodeForUpdate(@Param("code") Long code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Integer id);

    @Query("SELECT o.id FROM Order o WHERE o.paymentMethod = com.howie.pharmacy.pharmacy_store.entity.Order.Payment.CARD "
            + "AND o.isPaid = false AND o.status <> com.howie.pharmacy.pharmacy_store.entity.Order.Status.CANCEL "
            + "AND o.updatedAt < :cutoff")
    List<Integer> findStaleUnpaidOnlineOrderIds(@Param("cutoff") LocalDateTime cutoff);
}
