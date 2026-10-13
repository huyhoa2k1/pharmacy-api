package com.howie.pharmacy.pharmacy_store.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.howie.pharmacy.pharmacy_store.entity.Product;
import com.howie.pharmacy.pharmacy_store.repository.ProductRepository;

@Service
public class ProductVectorIndexService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductVectorIndexService.class);
    private static final int BACKFILL_PAGE_SIZE = 500;

    private final JdbcTemplate jdbcTemplate;
    private final ProductRepository productRepository;
    private final VectorStore vectorStore;
    private final TransactionTemplate transactionTemplate;
    private final int batchSize;

    public ProductVectorIndexService(JdbcTemplate jdbcTemplate, ProductRepository productRepository,
            VectorStore vectorStore, PlatformTransactionManager transactionManager,
            @Value("${pharmacy.rag.indexer.batch-size:20}") int batchSize) {
        this.jdbcTemplate = jdbcTemplate;
        this.productRepository = productRepository;
        this.vectorStore = vectorStore;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.batchSize = Math.max(1, batchSize);
    }

    public void enqueue(Integer productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID is required for vector indexing");
        }
        jdbcTemplate.update("""
                INSERT INTO product_index_outbox(product_id)
                VALUES (?)
                ON CONFLICT (product_id) DO UPDATE
                SET requested_at = CURRENT_TIMESTAMP,
                    attempts = 0,
                    available_at = CURRENT_TIMESTAMP,
                    locked_until = NULL
                """, productId);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void enqueueCatalogBackfill() {
        int page = 0;
        List<Integer> productIds;
        do {
            productIds = productRepository.findAllIds(PageRequest.of(page++, BACKFILL_PAGE_SIZE));
            productIds.forEach(this::enqueue);
        } while (productIds.size() == BACKFILL_PAGE_SIZE);
    }

    @Scheduled(fixedDelayString = "${pharmacy.rag.indexer.delay:10000}")
    public void processPendingProducts() {
        for (int count = 0; count < batchSize; count++) {
            PendingProduct pending = claimNext();
            if (pending == null) {
                return;
            }
            try {
                indexProduct(pending.productId());
                jdbcTemplate.update("""
                        DELETE FROM product_index_outbox
                        WHERE product_id = ? AND requested_at = ?
                        """, pending.productId(), pending.requestedAt());
            } catch (RuntimeException exception) {
                LOGGER.error("Unable to update product vector index for product {}", pending.productId(), exception);
                jdbcTemplate.update("""
                        UPDATE product_index_outbox
                        SET attempts = attempts + 1,
                            available_at = CURRENT_TIMESTAMP
                                + LEAST(300, 5 * power(2, LEAST(attempts, 6))) * INTERVAL '1 second',
                            locked_until = NULL
                        WHERE product_id = ? AND requested_at = ?
                        """, pending.productId(), pending.requestedAt());
            }
        }
    }

    private PendingProduct claimNext() {
        return transactionTemplate.execute(status -> {
            List<PendingProduct> pendingProducts = jdbcTemplate.query("""
                    SELECT product_id, requested_at
                    FROM product_index_outbox
                    WHERE available_at <= CURRENT_TIMESTAMP
                      AND (locked_until IS NULL OR locked_until < CURRENT_TIMESTAMP)
                    ORDER BY requested_at
                    LIMIT 1
                    FOR UPDATE SKIP LOCKED
                    """, (resultSet, rowNumber) -> new PendingProduct(
                    resultSet.getInt("product_id"),
                    resultSet.getTimestamp("requested_at")));
            if (pendingProducts.isEmpty()) {
                return null;
            }

            PendingProduct pending = pendingProducts.get(0);
            jdbcTemplate.update("""
                    UPDATE product_index_outbox
                    SET locked_until = CURRENT_TIMESTAMP + INTERVAL '2 minutes'
                    WHERE product_id = ? AND requested_at = ?
                    """, pending.productId(), pending.requestedAt());
            return pending;
        });
    }

    private void indexProduct(Integer productId) {
        Product product = productRepository.findDetailedById(productId).orElse(null);
        String documentId = documentId(productId);
        if (product == null) {
            vectorStore.delete(List.of(documentId));
            jdbcTemplate.update("DELETE FROM product_vector_index_state WHERE product_id = ?", productId);
            return;
        }

        String content = buildContent(product);
        String contentHash = sha256(content);
        List<String> existingHashes = jdbcTemplate.query(
                "SELECT content_hash FROM product_vector_index_state WHERE product_id = ?",
                (resultSet, rowNumber) -> resultSet.getString(1), productId);
        if (!existingHashes.isEmpty() && contentHash.equals(existingHashes.get(0))) {
            return;
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("productId", productId);
        metadata.put("productName", product.getName());
        if (product.getBrand() != null) {
            metadata.put("brandId", product.getBrand().getId());
            metadata.put("brandName", product.getBrand().getName());
            if (product.getBrand().getCategory() != null) {
                metadata.put("categoryId", product.getBrand().getCategory().getId());
                metadata.put("categoryName", product.getBrand().getCategory().getName());
            }
        }
        vectorStore.add(List.of(new Document(documentId, content, metadata)));
        jdbcTemplate.update("""
                INSERT INTO product_vector_index_state(product_id, content_hash, indexed_at)
                VALUES (?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (product_id) DO UPDATE
                SET content_hash = EXCLUDED.content_hash, indexed_at = CURRENT_TIMESTAMP
                """, productId, contentHash);
    }

    private String buildContent(Product product) {
        List<String> fields = new ArrayList<>();
        fields.add("Product name: " + product.getName());
        fields.add("Product description: " + product.getDescription());
        if (product.getBrand() != null) {
            fields.add("Brand: " + product.getBrand().getName());
            if (product.getBrand().getCategory() != null) {
                fields.add("Category: " + product.getBrand().getCategory().getName());
            }
        }
        return String.join("\n", fields);
    }

    private String sha256(String content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String documentId(Integer productId) {
        return UUID.nameUUIDFromBytes(("pharmacy-product-" + productId).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private record PendingProduct(Integer productId, Timestamp requestedAt) {
    }
}
