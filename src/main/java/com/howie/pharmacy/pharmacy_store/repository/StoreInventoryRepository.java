package com.howie.pharmacy.pharmacy_store.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.howie.pharmacy.pharmacy_store.entity.StoreInventory;

import jakarta.persistence.LockModeType;

public interface StoreInventoryRepository extends JpaRepository<StoreInventory, Integer> {

    @Query("SELECT i FROM StoreInventory i JOIN FETCH i.product p WHERE i.store.id = :storeId ORDER BY p.name")
    List<StoreInventory> findByStoreIdWithProduct(@Param("storeId") Integer storeId);

    @Query("SELECT i FROM StoreInventory i JOIN FETCH i.store s WHERE i.product.id IN :productIds AND s.isActive = true")
    List<StoreInventory> findActiveByProductIds(@Param("productIds") Collection<Integer> productIds);

    // Khoá dòng tồn kho để hai đơn đồng thời không cùng trừ một lượng hàng
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM StoreInventory i WHERE i.store.id = :storeId AND i.product.id = :productId")
    Optional<StoreInventory> findForUpdate(@Param("storeId") Integer storeId, @Param("productId") Integer productId);

    @Query("SELECT i FROM StoreInventory i WHERE i.product.id = :productId ORDER BY i.store.id")
    List<StoreInventory> findByProductIdOrdered(@Param("productId") Integer productId);

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM StoreInventory i WHERE i.product.id = :productId")
    long sumAmountByProductId(@Param("productId") Integer productId);
}
