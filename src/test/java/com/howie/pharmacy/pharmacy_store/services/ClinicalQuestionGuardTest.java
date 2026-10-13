package com.howie.pharmacy.pharmacy_store.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ClinicalQuestionGuardTest {

    @Test
    void blocksRequestsForClinicalAdviceRatherThanMereSymptomMentions() {
        assertTrue(ClinicalQuestionGuard.isClinicalQuestion("Tôi bị ho, nên uống thuốc gì?"));
        assertTrue(ClinicalQuestionGuard.isClinicalQuestion("What dose should I take for a headache?"));
        assertTrue(ClinicalQuestionGuard.isClinicalQuestion("What medication can I take for a cold?"));
        assertTrue(ClinicalQuestionGuard.isClinicalQuestion("Tôi bị nghẹt mũi, nên dùng gì?"));
        assertTrue(ClinicalQuestionGuard.isClinicalQuestion("Thuốc nào phù hợp cho tôi khi bị ho?"));
        assertTrue(ClinicalQuestionGuard.isClinicalQuestion(
                "What should I take?", List.of("I have a fever and a sore throat.")));
        assertFalse(ClinicalQuestionGuard.isClinicalQuestion(
                "Có loại kem đánh răng nào không?", List.of("Tôi bị ho, nên uống thuốc gì?")));
    }

    @Test
    void allowsCatalogSearchForProductsIncludingMedicinesAndHealthSupplies() {
        assertFalse(ClinicalQuestionGuard.isClinicalQuestion("Tôi cần tìm thuốc ho"));
        assertFalse(ClinicalQuestionGuard.isClinicalQuestion("Tìm thuốc cho trẻ bị sốt"));
        assertFalse(ClinicalQuestionGuard.isClinicalQuestion("Có loại kem đánh răng nào không?"));
        assertFalse(ClinicalQuestionGuard.isClinicalQuestion("Có bao cao su không?"));
        assertFalse(ClinicalQuestionGuard.isClinicalQuestion("Có bao cao su không?"));
        assertFalse(ClinicalQuestionGuard.isClinicalQuestion("Các loại sản phẩm hỗ trợ sức khỏe"));
        assertFalse(ClinicalQuestionGuard.isClinicalQuestion("Tìm vitamin C của thương hiệu ABC"));
        assertFalse(ClinicalQuestionGuard.isClinicalQuestion("Sản phẩm nào đang giảm giá?"));
        assertFalse(ClinicalQuestionGuard.isClinicalQuestion("Dầu gội nào có trong cửa hàng?"));
        assertFalse(ClinicalQuestionGuard.isClinicalQuestion("What medicines are available in the catalog?"));
    }
}
