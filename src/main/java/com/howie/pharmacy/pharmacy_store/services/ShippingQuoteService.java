package com.howie.pharmacy.pharmacy_store.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.howie.pharmacy.pharmacy_store.dto.geo.GeoPlaceDto;
import com.howie.pharmacy.pharmacy_store.dto.order.OrderItemCreateDto;
import com.howie.pharmacy.pharmacy_store.dto.shipping.ShippingQuoteDto;
import com.howie.pharmacy.pharmacy_store.entity.Product;
import com.howie.pharmacy.pharmacy_store.entity.Province;
import com.howie.pharmacy.pharmacy_store.entity.Store;
import com.howie.pharmacy.pharmacy_store.entity.StoreInventory;
import com.howie.pharmacy.pharmacy_store.entity.Ward;
import com.howie.pharmacy.pharmacy_store.exception.AppExceptions.BadRequestException;
import com.howie.pharmacy.pharmacy_store.repository.ProductRepository;
import com.howie.pharmacy.pharmacy_store.repository.ProvinceRepository;
import com.howie.pharmacy.pharmacy_store.repository.StoreInventoryRepository;
import com.howie.pharmacy.pharmacy_store.repository.WardRepository;
import com.howie.pharmacy.pharmacy_store.services.RoutingService.Route;

/** Chọn cửa hàng gần nhất còn đủ hàng cho cả giỏ và tính phí vận chuyển. */
@Service
public class ShippingQuoteService {

    public record Quote(Store store, Route route, BigDecimal subtotal, BigDecimal shippingFee, double latitude,
            double longitude) {
        public BigDecimal distanceKm() {
            return BigDecimal.valueOf(route.distanceKm()).setScale(2, RoundingMode.HALF_UP);
        }

        // Khách được miễn phí ship: shippingFee là chi phí cửa hàng trả cho đơn vị vận
        // chuyển
        public BigDecimal total() {
            return subtotal;
        }
    }

    private final StoreInventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final ProvinceRepository provinceRepository;
    private final WardRepository wardRepository;
    private final GeocodingService geocodingService;
    private final RoutingService routingService;
    private final BigDecimal baseFee;
    private final double freeDistanceKm;
    private final BigDecimal perKmFee;
    private final double maxDistanceKm;
    private final int routeCandidates;

    public ShippingQuoteService(StoreInventoryRepository inventoryRepository, ProductRepository productRepository,
            ProvinceRepository provinceRepository, WardRepository wardRepository, GeocodingService geocodingService,
            RoutingService routingService,
            @Value("${pharmacy.shipping.base-fee:15000}") BigDecimal baseFee,
            @Value("${pharmacy.shipping.free-distance-km:3}") double freeDistanceKm,
            @Value("${pharmacy.shipping.per-km-fee:5000}") BigDecimal perKmFee,
            @Value("${pharmacy.shipping.max-distance-km:200}") double maxDistanceKm,
            @Value("${pharmacy.shipping.route-candidates:3}") int routeCandidates) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.provinceRepository = provinceRepository;
        this.wardRepository = wardRepository;
        this.geocodingService = geocodingService;
        this.routingService = routingService;
        this.baseFee = baseFee;
        this.freeDistanceKm = freeDistanceKm;
        this.perKmFee = perKmFee;
        this.maxDistanceKm = maxDistanceKm;
        this.routeCandidates = Math.max(1, routeCandidates);
    }

    /**
     * Dùng toạ độ khách chọn trên bản đồ; nếu không có thì tìm toạ độ từ địa chỉ đã
     * điền,
     * rồi chọn cửa hàng gần nhất.
     */
    public Quote calculateForAddress(Double latitude, Double longitude, String provinceCode, String wardCode,
            String address, List<OrderItemCreateDto> items) {
        if (latitude != null && longitude != null) {
            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                throw new BadRequestException("Invalid delivery location");
            }
            return calculate(latitude, longitude, items);
        }
        if (provinceCode == null || provinceCode.isBlank() || wardCode == null || wardCode.isBlank()) {
            throw new BadRequestException("Delivery location is required");
        }
        GeoPlaceDto place = locate(provinceCode, wardCode, address);
        return calculate(place.latitude(), place.longitude(), items);
    }

    // Thử từng mức chi tiết giảm dần vì OSM thường không có đủ số nhà/tên đường
    private GeoPlaceDto locate(String provinceCode, String wardCode, String address) {
        String province = provinceRepository.findByProvinceCode(provinceCode).map(Province::getName)
                .orElse(provinceCode);
        String ward = wardRepository.findByWardCode(wardCode).map(Ward::getName).orElse(wardCode);

        List<String> queries = new ArrayList<>();
        if (address != null && !address.isBlank()) {
            queries.add(String.join(", ", address.trim(), ward, province, "Việt Nam"));
        }
        queries.add(String.join(", ", ward, province, "Việt Nam"));
        queries.add(province + ", Việt Nam");

        for (String query : queries) {
            List<GeoPlaceDto> places = geocodingService.search(query);
            if (!places.isEmpty()) {
                return places.get(0);
            }
        }
        throw new BadRequestException("Could not locate the delivery address");
    }

    @Transactional(readOnly = true)
    public Quote calculate(double latitude, double longitude, List<OrderItemCreateDto> items) {
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("At least one order item is required");
        }
        Map<Integer, Integer> wanted = new LinkedHashMap<>();
        for (OrderItemCreateDto item : items) {
            if (item.getProductId() == null || item.getAmount() == null || item.getAmount() < 1) {
                throw new BadRequestException("Invalid order item");
            }
            wanted.merge(item.getProductId(), item.getAmount(), Integer::sum);
        }

        Map<Integer, Product> products = productRepository.findAllById(wanted.keySet()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        if (products.size() != wanted.size()) {
            throw new BadRequestException("Product not found");
        }
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Map.Entry<Integer, Integer> e : wanted.entrySet()) {
            subtotal = subtotal.add(products.get(e.getKey()).getPrice().multiply(BigDecimal.valueOf(e.getValue())));
        }

        Map<Integer, Store> stores = new LinkedHashMap<>();
        Map<Integer, Map<Integer, Integer>> stock = new LinkedHashMap<>();
        for (StoreInventory inv : inventoryRepository.findActiveByProductIds(wanted.keySet())) {
            Integer storeId = inv.getStore().getId();
            stores.putIfAbsent(storeId, inv.getStore());
            stock.computeIfAbsent(storeId, k -> new LinkedHashMap<>()).put(inv.getProduct().getId(), inv.getAmount());
        }

        List<Store> candidates = stores.values().stream()
                .filter(s -> wanted.entrySet().stream()
                        .allMatch(e -> stock.get(s.getId()).getOrDefault(e.getKey(), 0) >= e.getValue()))
                .sorted(Comparator.comparingDouble(
                        s -> RoutingService.haversineKm(latitude, longitude, s.getLatitude(), s.getLongitude())))
                .limit(routeCandidates)
                .toList();
        if (candidates.isEmpty()) {
            throw new BadRequestException("No store currently has enough stock for this order");
        }

        Store bestStore = null;
        Route bestRoute = null;
        for (Store candidate : candidates) {
            Route route = routingService.route(candidate.getLatitude(), candidate.getLongitude(), latitude, longitude);
            if (bestRoute == null || route.distanceKm() < bestRoute.distanceKm()) {
                bestStore = candidate;
                bestRoute = route;
            }
        }
        if (bestRoute.distanceKm() > maxDistanceKm) {
            throw new BadRequestException("Delivery location is too far from our nearest store");
        }

        return new Quote(bestStore, bestRoute, subtotal, computeFee(bestRoute.distanceKm()), latitude,
                longitude);
    }

    // Chi phí vận chuyển cửa hàng phải trả cho đơn vị vận chuyển
    public BigDecimal computeFee(double distanceKm) {
        long extraKm = (long) Math.ceil(Math.max(0, distanceKm - freeDistanceKm));
        return baseFee.add(perKmFee.multiply(BigDecimal.valueOf(extraKm)));
    }

    public ShippingQuoteDto toDto(Quote quote) {
        Store s = quote.store();
        return new ShippingQuoteDto(s.getId(), s.getName(), s.getAddress(), s.getLatitude(), s.getLongitude(),
                quote.distanceKm(), quote.route().durationMinutes(), quote.route().estimated(), quote.subtotal(),
                quote.shippingFee(), quote.total());
    }
}
