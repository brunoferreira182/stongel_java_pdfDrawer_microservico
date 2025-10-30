package com.stongel.pdfdrawer.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class BR {
    private static final Locale PT_BR = new Locale("pt","BR");
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(PT_BR);

    private BR() {}

    public static String texto(String v) {
        return (v == null || v.isBlank()) ? "-" : v;
    }

    public static String moeda(BigDecimal v) {
        if (v == null) return "-";
        return CURRENCY.format(v);
    }

    public static String numero(BigDecimal v) {
        if (v == null) return "-";
        return v.stripTrailingZeros().toPlainString();
    }
}
