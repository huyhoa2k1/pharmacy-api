package com.howie.pharmacy.pharmacy_store.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.howie.pharmacy.pharmacy_store.dto.geo.GeoPlaceDto;
import com.howie.pharmacy.pharmacy_store.dto.order.OrderItemCreateDto;
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

class ShippingQuoteServiceTest {

        private StoreInventoryRepository inventoryRepository;
        private ProductRepository productRepository;
        private RoutingService routingService;
        private ProvinceRepository provinceRepository;
        private WardRepository wardRepository;
        private GeocodingService geocodingService;
        private ShippingQuoteService service;
        private Product product;

        @BeforeEach
        void setUp() {
                inventoryRepository = mock(StoreInventoryRepository.class);
                productRepository = mock(ProductRepository.class);
                routingService = mock(RoutingService.class);
                provinceRepository = mock(ProvinceRepository.class);
                wardRepository = mock(WardRepository.class);
                geocodingService = mock(GeocodingService.class);
                service = new ShippingQuoteService(inventoryRepository, productRepository, provinceRepository,
                                wardRepository, geocodingService, routingService,
                                new BigDecimal("15000"), 3, new BigDecimal("5000"), 100, 3);

                product = new Product();
                product.setId(1);
                product.setPrice(new BigDecimal("100000"));
                when(productRepository.findAllById(java.util.Set.of(1))).thenReturn(List.of(product));
        }

        @Test
        void computeFeeAppliesBaseFeeAndFreeDistance() {
                assertEquals(new BigDecimal("15000"), service.computeFee(2.0));
                assertEquals(new BigDecimal("20000"), service.computeFee(3.2));
                assertEquals(new BigDecimal("50000"), service.computeFee(10.0));
        }

        @Test
        void picksNearestInStockStoreByRouteDistance() {
                Store a = store(1, 10.0, 106.0);
                Store b = store(2, 10.5, 106.5);
                Store outOfStock = store(3, 10.41, 106.41);
                when(inventoryRepository.findActiveByProductIds(java.util.Set.of(1))).thenReturn(List.of(
                                inventory(a, 5), inventory(b, 5), inventory(outOfStock, 1)));
                when(routingService.route(eq(10.5), eq(106.5), anyDouble(), anyDouble()))
                                .thenReturn(new Route(12.5, 30, false));
                when(routingService.route(eq(10.0), eq(106.0), anyDouble(), anyDouble()))
                                .thenReturn(new Route(60.0, 90, false));

                ShippingQuoteService.Quote quote = service.calculate(10.4, 106.4,
                                List.of(new OrderItemCreateDto(1, 2)));

                assertEquals(2, quote.store().getId());
                assertEquals(0, new BigDecimal("200000").compareTo(quote.subtotal()));
                // 15000 + ceil(12.5 - 3) * 5000
                assertEquals(new BigDecimal("65000"), quote.shippingFee());
        }

        @Test
        void rejectsOrderWhenNoStoreHasEnoughStock() {
                when(inventoryRepository.findActiveByProductIds(java.util.Set.of(1)))
                                .thenReturn(List.of(inventory(store(1, 10.0, 106.0), 1)));

                assertThrows(BadRequestException.class,
                                () -> service.calculate(10.4, 106.4, List.of(new OrderItemCreateDto(1, 2))));
        }

        @Test
        void fallsBackToWardWhenStreetAddressIsNotFound() {
                Province province = new Province();
                province.setName("Thanh pho Ho Chi Minh");
                Ward ward = new Ward();
                ward.setName("Phuong Ben Nghe");
                when(provinceRepository.findByProvinceCode("79")).thenReturn(java.util.Optional.of(province));
                when(wardRepository.findByWardCode("26740")).thenReturn(java.util.Optional.of(ward));
                when(geocodingService.search("1 Khong co, Phuong Ben Nghe, Thanh pho Ho Chi Minh, Việt Nam"))
                                .thenReturn(List.of());
                when(geocodingService.search("Phuong Ben Nghe, Thanh pho Ho Chi Minh, Việt Nam"))
                                .thenReturn(List.of(new GeoPlaceDto("Ben Nghe", 10.77, 106.70)));
                Store a = store(1, 10.0, 106.0);
                when(inventoryRepository.findActiveByProductIds(java.util.Set.of(1)))
                                .thenReturn(List.of(inventory(a, 5)));
                when(routingService.route(anyDouble(), anyDouble(), eq(10.77), eq(106.70)))
                                .thenReturn(new Route(2.0, 10, false));

                ShippingQuoteService.Quote quote = service.calculateForAddress(null, null, "79", "26740", "1 Khong co",
                                List.of(new OrderItemCreateDto(1, 1)));

                assertEquals(10.77, quote.latitude());
                assertEquals(1, quote.store().getId());
        }

        @Test
        void usesPickedCoordinatesWithoutGeocodingTheAddress() {
                Store a = store(1, 10.0, 106.0);
                when(inventoryRepository.findActiveByProductIds(java.util.Set.of(1)))
                                .thenReturn(List.of(inventory(a, 5)));
                when(routingService.route(anyDouble(), anyDouble(), eq(10.05), eq(106.05)))
                                .thenReturn(new Route(1.0, 5, false));

                ShippingQuoteService.Quote quote = service.calculateForAddress(10.05, 106.05, "79", "26740", "abc",
                                List.of(new OrderItemCreateDto(1, 1)));

                assertEquals(106.05, quote.longitude());
                org.mockito.Mockito.verifyNoInteractions(geocodingService);
        }

        @Test
        void rejectsOutOfRangeCoordinates() {
                assertThrows(BadRequestException.class, () -> service.calculateForAddress(91.0, 106.0, null, null, null,
                                List.of(new OrderItemCreateDto(1, 1))));
        }

        @Test
        void rejectsWhenAddressCannotBeLocated() {
                when(geocodingService.search(org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of());

                assertThrows(BadRequestException.class, () -> service.calculateForAddress(null, null, "79", "26740",
                                "abc xyz", List.of(new OrderItemCreateDto(1, 1))));
        }

        @Test
        void rejectsNonPositiveAmounts() {
                assertThrows(BadRequestException.class,
                                () -> service.calculate(10.4, 106.4, List.of(new OrderItemCreateDto(1, 0))));
        }

        private Store store(int id, double lat, double lng) {
                Store s = new Store();
                s.setId(id);
                s.setLatitude(lat);
                s.setLongitude(lng);
                s.setName("Store " + id);
                return s;
        }

        private StoreInventory inventory(Store store, int amount) {
                StoreInventory i = new StoreInventory();
                i.setStore(store);
                i.setProduct(product);
                i.setAmount(amount);
                return i;
        }
}
