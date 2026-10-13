package com.howie.pharmacy.pharmacy_store.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import com.howie.pharmacy.pharmacy_store.dto.product.ProductResponseDto;

class ProductCatalogRetrievalServiceTest {

    @Test
    void combinesExactAndSemanticResultsAndReloadsCurrentCatalogProducts() {
        VectorStore vectorStore = mock(VectorStore.class);
        ProductService productService = mock(ProductService.class);
        ProductResponseDto exactProduct = product(1, "Exact result");
        ProductResponseDto semanticProduct = product(2, "Semantic result");

        when(productService.searchProducts("vitamin c")).thenReturn(List.of(exactProduct));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                new Document("semantic-id", "Vitamin C product", Map.of("productId", 2)),
                new Document("stale-id", "Deleted product", Map.of("productId", 3))));
        when(productService.findByIds(List.of(1, 2, 3))).thenReturn(List.of(exactProduct, semanticProduct));

        ProductCatalogRetrievalService retrievalService =
                new ProductCatalogRetrievalService(vectorStore, productService, 5, 0.72);

        assertEquals(List.of(exactProduct, semanticProduct), retrievalService.retrieve("Tìm vitamin C"));
    }

    @Test
    void usesCurrentDatabasePricesForBudgetQueries() {
        VectorStore vectorStore = mock(VectorStore.class);
        ProductService productService = mock(ProductService.class);
        ProductResponseDto product = product(7, "Affordable product");
        product.setPrice(new BigDecimal("100000"));

        when(productService.searchProducts("sản phẩm dưới 100k")).thenReturn(List.of());
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        when(productService.findByPriceRange(null, new BigDecimal("100000"), 5)).thenReturn(List.of(product));
        when(productService.findByIds(List.of(7))).thenReturn(List.of(product));

        ProductCatalogRetrievalService retrievalService =
                new ProductCatalogRetrievalService(vectorStore, productService, 5, 0.72);

        assertEquals(List.of(product), retrievalService.retrieve("sản phẩm dưới 100k"));
    }

    @Test
    void retrievesCatalogItemsForNaturalLanguageAvailabilityQuestion() {
        VectorStore vectorStore = mock(VectorStore.class);
        ProductService productService = mock(ProductService.class);
        ProductResponseDto toothpaste = product(11, "Kem đánh răng bạc hà");

        when(productService.searchProducts("kem đánh răng")).thenReturn(List.of(toothpaste));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        when(productService.findByIds(List.of(11))).thenReturn(List.of(toothpaste));

        ProductCatalogRetrievalService retrievalService =
                new ProductCatalogRetrievalService(vectorStore, productService, 5, 0.72);

        assertEquals(List.of(toothpaste), retrievalService.retrieve(
                PharmacyChatService.normalizeProductQuery("Có loại kem đánh răng nào không?")));
    }

    private ProductResponseDto product(Integer id, String name) {
        ProductResponseDto product = new ProductResponseDto();
        product.setId(id);
        product.setName(name);
        return product;
    }
}
