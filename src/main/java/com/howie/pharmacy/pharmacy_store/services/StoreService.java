package com.howie.pharmacy.pharmacy_store.services;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.howie.pharmacy.pharmacy_store.dto.store.StoreDto;
import com.howie.pharmacy.pharmacy_store.dto.store.StoreInventoryDto;
import com.howie.pharmacy.pharmacy_store.dto.store.StoreInventoryUpdateDto;
import com.howie.pharmacy.pharmacy_store.dto.store.StoreRequestDto;
import com.howie.pharmacy.pharmacy_store.entity.Product;
import com.howie.pharmacy.pharmacy_store.entity.Store;
import com.howie.pharmacy.pharmacy_store.entity.StoreInventory;
import com.howie.pharmacy.pharmacy_store.exception.AppExceptions.ResourceNotFoundException;
import com.howie.pharmacy.pharmacy_store.repository.ProductRepository;
import com.howie.pharmacy.pharmacy_store.repository.StoreInventoryRepository;
import com.howie.pharmacy.pharmacy_store.repository.StoreRepository;

@Service
public class StoreService {

    private final StoreRepository storeRepository;
    private final StoreInventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public StoreService(StoreRepository storeRepository, StoreInventoryRepository inventoryRepository,
            ProductRepository productRepository) {
        this.storeRepository = storeRepository;
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<StoreDto> list(String provinceCode, boolean includeInactive) {
        boolean hasProvince = provinceCode != null && !provinceCode.isBlank();
        List<Store> stores;
        if (includeInactive) {
            stores = hasProvince ? storeRepository.findByProvinceCodeOrderByNameAsc(provinceCode)
                    : storeRepository.findAllByOrderByNameAsc();
        } else {
            stores = hasProvince ? storeRepository.findByProvinceCodeAndIsActiveTrueOrderByNameAsc(provinceCode)
                    : storeRepository.findByIsActiveTrueOrderByNameAsc();
        }
        return stores.stream().map(StoreService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public StoreDto get(Integer id) {
        return toDto(findStore(id));
    }

    @Transactional
    public StoreDto create(StoreRequestDto request) {
        Store store = new Store();
        apply(store, request);
        return toDto(storeRepository.save(store));
    }

    @Transactional
    public StoreDto update(Integer id, StoreRequestDto request) {
        Store store = findStore(id);
        apply(store, request);
        return toDto(storeRepository.save(store));
    }

    @Transactional(readOnly = true)
    public List<StoreInventoryDto> getInventory(Integer storeId) {
        findStore(storeId);
        return inventoryRepository.findByStoreIdWithProduct(storeId).stream()
                .map(i -> new StoreInventoryDto(i.getProduct().getId(), i.getProduct().getName(), i.getAmount()))
                .toList();
    }

    @Transactional
    public StoreInventoryDto setInventory(Integer storeId, StoreInventoryUpdateDto request) {
        Store store = findStore(storeId);
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        StoreInventory row = inventoryRepository.findForUpdate(storeId, product.getId())
                .orElseGet(() -> newRow(store, product));
        row.setAmount(request.amount());
        inventoryRepository.save(row);

        // Product.amount luôn bằng tổng tồn kho các cửa hàng
        product.setAmount((int) inventoryRepository.sumAmountByProductId(product.getId()));
        productRepository.save(product);
        return new StoreInventoryDto(product.getId(), product.getName(), row.getAmount());
    }

    /**
     * Đồng bộ tồn kho cửa hàng khi admin sửa trực tiếp Product.amount: phần chênh
     * lệch
     * được cộng/trừ vào cửa hàng mặc định (active có id nhỏ nhất) trước.
     */
    @Transactional
    public void reconcileStock(Product product) {
        Store defaultStore = storeRepository.findFirstByIsActiveTrueOrderByIdAsc().orElse(null);
        if (defaultStore == null || product.getAmount() == null) {
            return;
        }
        List<StoreInventory> rows = inventoryRepository.findByProductIdOrdered(product.getId());
        long total = rows.stream().mapToLong(StoreInventory::getAmount).sum();
        long delta = Math.max(0, product.getAmount()) - total;
        if (delta == 0) {
            return;
        }

        if (delta > 0) {
            StoreInventory row = rows.stream()
                    .filter(r -> r.getStore().getId().equals(defaultStore.getId()))
                    .findFirst()
                    .orElseGet(() -> newRow(defaultStore, product));
            row.setAmount((int) (row.getAmount() + delta));
            inventoryRepository.save(row);
            return;
        }

        long toRemove = -delta;
        List<StoreInventory> ordered = rows.stream()
                .sorted(Comparator.comparingInt(r -> r.getStore().getId().equals(defaultStore.getId()) ? 0 : 1))
                .toList();
        for (StoreInventory row : ordered) {
            if (toRemove == 0) {
                break;
            }
            int take = (int) Math.min(row.getAmount(), toRemove);
            row.setAmount(row.getAmount() - take);
            toRemove -= take;
            inventoryRepository.save(row);
        }
    }

    private Store findStore(Integer id) {
        return storeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Store not found"));
    }

    private static StoreInventory newRow(Store store, Product product) {
        StoreInventory row = new StoreInventory();
        row.setStore(store);
        row.setProduct(product);
        row.setAmount(0);
        return row;
    }

    private static void apply(Store store, StoreRequestDto request) {
        store.setName(request.name().trim());
        store.setProvinceCode(request.provinceCode() == null || request.provinceCode().isBlank() ? null
                : request.provinceCode().trim());
        store.setAddress(request.address().trim());
        store.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
        store.setLatitude(request.latitude());
        store.setLongitude(request.longitude());
        if (request.isActive() != null) {
            store.setIsActive(request.isActive());
        }
    }

    private static StoreDto toDto(Store s) {
        return new StoreDto(s.getId(), s.getName(), s.getProvinceCode(), s.getAddress(), s.getPhone(),
                s.getLatitude(), s.getLongitude(), s.getIsActive());
    }
}
