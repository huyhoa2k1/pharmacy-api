package com.howie.pharmacy.pharmacy_store.services;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public final class ClinicalQuestionGuard {

    private static final List<String> CLINICAL_TERMS = List.of(
            "trieu chung", "chan doan", "lieu dung", "lieu uong", "uong bao nhieu", "cach dung",
            "cach uong", "tac dung phu", "tuong tac thuoc", "chong chi dinh", "mang thai",
            "cho con bu", "pregnant", "dosage", "dose", "diagnose", "diagnosis", "side effect",
            "drug interaction", "contraindication", "lieu");

    private static final List<String> CLINICAL_RECOMMENDATION_TERMS = List.of(
            "nen uong", "nen dung", "nen chon", "nen su dung", "thuoc nao phu hop",
            "thuoc nao tot nhat", "uong thuoc gi", "dung thuoc gi", "nen uong gi",
            "nen dung gi", "nen chon thuoc", "thuoc nao nen", "thuoc nao de tri",
            "thuoc nao de chua", "loai nao phu hop", "loai nao nen dung",
            "nen mua thuoc gi", "recommend a medicine", "recommend medication",
            "which medicine should", "what medicine should", "which medication should",
            "what medication should", "which drug should", "what drug should",
            "medicine should i take", "which medicine to take", "what medicine to take",
            "best medicine", "best medication", "should i take", "can i take",
            "what should i take", "what can i take", "which medicine", "which medication",
            "which drug", "lieu uong", "lieu dung", "cach dung", "cach uong",
            "what dose", "how much should i take", "how to take");

    private static final List<String> CATALOG_SEARCH_TERMS = List.of(
            "tim", "tra cuu", "co ban", "co hang", "co loai", "co san pham",
            "danh sach", "liet ke", "dang ban", "con hang", "available", "in stock",
            "find", "search", "show me", "list", "do you sell", "do you have");

    private static final List<String> SYMPTOM_TERMS = List.of(
            "dau dau", "dau bung", "dau hong", "dau rang", "dau nguc", "dau lung",
            "sot", "ho", "kho tho", "kho thở", "viem", "di ung", "ngua", "chong mat",
            "buon non", "tieu chay", "tao bon", "mat ngu", "phat ban", "met moi",
            "nghet mui", "so mui", "cam lanh", "cum", "dau nhuc", "kho tieu",
            "headache", "fever", "cough", "rash", "nausea", "diarrhea", "shortness of breath",
            "runny nose", "stuffy nose", "cold", "flu", "sore throat", "itching",
            "stomach ache", "insomnia", "high blood pressure", "blood sugar",
            "tieu duong", "diabetes", "hen suyen", "asthma", "ung thu", "cancer",
            "benh than", "kidney disease", "migraine");

    private ClinicalQuestionGuard() {
    }

    public static boolean isClinicalQuestion(String question) {
        return isClinicalQuestion(question, List.of());
    }

    public static boolean isClinicalQuestion(String question, List<String> recentUserMessages) {
        int historyStart = Math.max(0, recentUserMessages.size() - 4);
        String normalized = normalize(question);
        if (CLINICAL_TERMS.stream().anyMatch(term -> containsPhrase(normalized, normalize(term)))) {
            return true;
        }

        boolean asksToFindCatalogItems = CATALOG_SEARCH_TERMS.stream()
                .anyMatch(term -> containsPhrase(normalized, normalize(term)));
        if (asksToFindCatalogItems) {
            return false;
        }

        if (CLINICAL_RECOMMENDATION_TERMS.stream()
                .anyMatch(term -> containsPhrase(normalized, normalize(term)))) {
            return true;
        }

        boolean mentionsSymptom = SYMPTOM_TERMS.stream()
                .anyMatch(term -> containsPhrase(normalized, normalize(term)));
        if (!mentionsSymptom) {
            return false;
        }

        boolean personalSymptom = containsPhrase(normalized, "bi")
                || containsPhrase(normalized, "mac")
                || containsPhrase(normalized, "suffering from")
                || containsPhrase(normalized, "i have");
        if (personalSymptom) {
            return true;
        }

        List<String> recentSymptoms = recentUserMessages.stream()
                .skip(historyStart)
                .map(ClinicalQuestionGuard::normalize)
                .filter(message -> SYMPTOM_TERMS.stream()
                        .anyMatch(term -> containsPhrase(message, normalize(term))))
                .toList();
        boolean ambiguousFollowUp = containsPhrase(normalized, "loai nao")
                || containsPhrase(normalized, "cai nao")
                || containsPhrase(normalized, "which one")
                || containsPhrase(normalized, "what about this");
        return ambiguousFollowUp && !recentSymptoms.isEmpty();
    }

    private static boolean containsPhrase(String text, String phrase) {
        return (" " + text + " ").contains(" " + phrase + " ");
    }

    private static String normalize(String value) {
        String decomposed = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd');
        return decomposed.replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }
}
