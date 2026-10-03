package com.howie.pharmacy.pharmacy_store.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PharmacyChatServiceTest {

    @Test
    void normalizesNaturalLanguageMedicineSearchToProductKeywords() {
        assertEquals("ho", PharmacyChatService.normalizeProductQuery("tìm cho tôi loại thuốc trị ho"));
        assertEquals("ho", PharmacyChatService.normalizeProductQuery("điều trị ho"));
    }

    @Test
    void preservesProductSpecificKeywords() {
        assertEquals("vitamin c", PharmacyChatService.normalizeProductQuery("Tìm giúp tôi vitamin C"));
        assertEquals("viêm họng", PharmacyChatService.normalizeProductQuery("thuốc trị viêm họng"));
    }
}