package com.stongel.pdfdrawer.util;

import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.IOException;

public class PdfText {

  public static void drawText(PDPageContentStream cs, String text, float x, float y,
                              PDFont font, float fontSize) throws IOException {
    if (text == null) text = "";
    cs.beginText();
    cs.setFont(font, fontSize);
    cs.newLineAtOffset(x, y);
    cs.showText(text);
    cs.endText();
  }

  public static float stringWidth(PDFont font, float size, String text) throws IOException {
    if (text == null) text = "";
    return font.getStringWidth(text) / 1000f * size;
  }
}
