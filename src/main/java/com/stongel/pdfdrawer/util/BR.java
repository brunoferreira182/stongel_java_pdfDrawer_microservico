package com.stongel.pdfdrawer.util;

import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

public final class BR {

    private static final Locale PTBR = Locale.of("pt", "BR");
    private static final NumberFormat NF_MOEDA = NumberFormat.getCurrencyInstance(PTBR);
    private static final NumberFormat NF_NUM = NumberFormat.getNumberInstance(PTBR);

    private BR() {}

    public static String texto(String s) {
        return s == null ? "-" : s;
    }

    public static String numero(Number n) {
        if (n == null) return "-";
        return NF_NUM.format(n);
    }

    public static String moeda(BigDecimal v) {
        if (v == null) return "-";
        return NF_MOEDA.format(v);
    }

    public static String moeda(Number v) {
        if (v == null) return "-";
        return NF_MOEDA.format(new BigDecimal(String.valueOf(v)));
    }

    /** Utilitário simples pra escrever uma linha única já com setFont + moveto. */
    public static void drawText(PDPageContentStream cs, PDFont font, float fontSize,
                                float x, float y, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(Objects.toString(text, "-"));
        cs.endText();
    }
}
