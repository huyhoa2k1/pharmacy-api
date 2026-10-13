package com.howie.pharmacy.pharmacy_store.services;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.howie.pharmacy.pharmacy_store.dto.product.ProductResponseDto;

record ProductPriceCriteria(BigDecimal minimum, BigDecimal maximum) {

    private static final String AMOUNT =
            "([0-9]+(?:(?:[.,][0-9]{3})+|[.,][0-9]{1,2})?)\\s*(trieu|million|m|nghin|ngan|thousand|k)?(?=\\s|$|d)";
    private static final Pattern MAXIMUM = Pattern.compile(
            "(?:duoi|toi da|khong qua|den|toi|under|below|less than|at most|max(?:imum)?)\\s*" + AMOUNT);
    private static final Pattern MINIMUM = Pattern.compile(
            "(?:tren|tu|above|over|more than|at least|min(?:imum)?)\\s*" + AMOUNT);

    static Optional<ProductPriceCriteria> parse(String query) {
        String normalized = Normalizer.normalize(query.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd');
        BigDecimal maximum = findAmount(MAXIMUM.matcher(normalized));
        BigDecimal minimum = findAmount(MINIMUM.matcher(normalized));
        if (maximum == null && minimum == null) {
            return Optional.empty();
        }
        return Optional.of(new ProductPriceCriteria(minimum, maximum));
    }

    boolean matches(ProductResponseDto product) {
        if (product.getPrice() == null) {
            return false;
        }
        BigDecimal currentPrice = product.getPrice();
        if (Boolean.TRUE.equals(product.getIsSale()) && product.getSaleEndTime() != null
                && product.getSaleEndTime().isAfter(java.time.LocalDateTime.now())
                && product.getDiscount() != null && product.getDiscount() > 0
                && product.getDiscount() <= 100) {
            currentPrice = currentPrice.multiply(BigDecimal.valueOf(100 - product.getDiscount()).movePointLeft(2));
        }
        return (minimum == null || currentPrice.compareTo(minimum) >= 0)
                && (maximum == null || currentPrice.compareTo(maximum) <= 0);
    }

    private static BigDecimal findAmount(Matcher matcher) {
        if (!matcher.find()) {
            return null;
        }
        String number = matcher.group(1);
        String unit = matcher.group(2);
        BigDecimal value;
        if (unit == null) {
            value = new BigDecimal(number.replace(".", "").replace(",", ""));
        } else {
            value = new BigDecimal(number.replace(",", "."));
            if (List.of("trieu", "million", "m").contains(unit)) {
                value = value.multiply(BigDecimal.valueOf(1_000_000));
            } else {
                value = value.multiply(BigDecimal.valueOf(1_000));
            }
        }
        return value;
    }
}
