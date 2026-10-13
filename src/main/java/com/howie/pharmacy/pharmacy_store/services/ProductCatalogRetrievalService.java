package com.howie.pharmacy.pharmacy_store.services;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.howie.pharmacy.pharmacy_store.dto.product.ProductResponseDto;

@Service
public class ProductCatalogRetrievalService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductCatalogRetrievalService.class);

    private final VectorStore vectorStore;
    private final ProductService productService;
    private final int topK;
    private final double similarityThreshold;

    public ProductCatalogRetrievalService(VectorStore vectorStore, ProductService productService,
            @Value("${pharmacy.rag.retrieval.top-k:5}") int topK,
            @Value("${pharmacy.rag.retrieval.similarity-threshold:0.72}") double similarityThreshold) {
        if (topK < 1 || topK > 20) {
            throw new IllegalArgumentException("RAG retrieval top-k must be between 1 and 20");
        }
        if (similarityThreshold < 0 || similarityThreshold > 1) {
            throw new IllegalArgumentException("RAG similarity threshold must be between 0 and 1");
        }
        this.vectorStore = vectorStore;
        this.productService = productService;
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> retrieve(String query) {
        LinkedHashSet<Integer> productIds = new LinkedHashSet<>();

        ProductPriceCriteria priceCriteria = ProductPriceCriteria.parse(query).orElse(null);
        productService.searchProducts(PharmacyChatService.normalizeProductQuery(query))
                .stream()
                .limit(Math.min(3, topK))
                .map(ProductResponseDto::getId)
                .filter(java.util.Objects::nonNull)
                .forEach(productIds::add);

        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build();
        List<Document> documents = vectorStore.similaritySearch(searchRequest);
        if (documents != null) {
            documents.stream()
                    .map(Document::getMetadata)
                    .map(this::productId)
                    .filter(java.util.Objects::nonNull)
                    .forEach(productIds::add);
        }

        if (priceCriteria != null) {
            productService.findByPriceRange(priceCriteria.minimum(), priceCriteria.maximum(), topK)
                    .stream()
                    .map(ProductResponseDto::getId)
                    .filter(java.util.Objects::nonNull)
                    .forEach(productIds::add);
        }

        if (productIds.isEmpty()) {
            return List.of();
        }

        List<Integer> orderedIds = new ArrayList<>(productIds);
        Map<Integer, ProductResponseDto> currentProducts = productService.findByIds(orderedIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        ProductResponseDto::getId,
                        product -> product,
                        (first, ignored) -> first));
        return orderedIds.stream()
                .map(currentProducts::get)
                .filter(java.util.Objects::nonNull)
                .filter(product -> priceCriteria == null || priceCriteria.matches(product))
                .limit(topK)
                .toList();
    }

    private Integer productId(Map<String, Object> metadata) {
        Object value = metadata.get("productId");
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.valueOf(text);
            } catch (NumberFormatException exception) {
                LOGGER.warn("Ignoring product vector with a non-numeric productId metadata value");
                return null;
            }
        }
        LOGGER.warn("Ignoring product vector without productId metadata");
        return null;
    }
}
