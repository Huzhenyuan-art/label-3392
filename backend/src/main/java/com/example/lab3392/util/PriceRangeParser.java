package com.example.lab3392.util;

import java.math.BigDecimal;

public final class PriceRangeParser {

    private PriceRangeParser() {
    }

    public static PriceRangeResult parse(String minPrice, String maxPrice) {
        String minRaw = trimToNull(minPrice);
        String maxRaw = trimToNull(maxPrice);

        boolean minPresent = minRaw != null;
        boolean maxPresent = maxRaw != null;

        if (minPresent ^ maxPresent) {
            return new PriceRangeResult("价格区间需同时填写最小价和最大价（或同时留空）", null, null);
        }

        if (!minPresent) {
            return new PriceRangeResult(null, null, null);
        }

        try {
            BigDecimal minVal = new BigDecimal(minRaw);
            BigDecimal maxVal = new BigDecimal(maxRaw);
            if (minVal.compareTo(maxVal) > 0) {
                return new PriceRangeResult("价格区间不合法：最小价不能大于最大价", null, null);
            }
            return new PriceRangeResult(null, minVal, maxVal);
        } catch (NumberFormatException ex) {
            return new PriceRangeResult("价格区间请输入数字，例如：199.00", null, null);
        }
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public record PriceRangeResult(String errorMessage, BigDecimal minPrice, BigDecimal maxPrice) {
        public boolean hasError() {
            return errorMessage != null;
        }
    }
}
