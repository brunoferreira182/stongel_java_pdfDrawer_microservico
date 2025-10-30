package com.stongel.pdfdrawer.util;

import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

public class TableDrawer {

  public static class Col<T> {
    public String header;
    public float width;
    public Align align = Align.LEFT;
    public Function<T, String> map;

    public Col(String header, float width, Align align, Function<T,String> map) {
      this.header = header; this.width = width; this.align = align; this.map = map;
    }
  }
  public enum Align { LEFT, CENTER, RIGHT }

  public static <T> float draw(
      PDPageContentStream cs, PDFont font, PDFont bold,
      float x, float y, float rowH, float fontSize,
      List<Col<T>> cols, List<T> rows,
      float headerBgGray // 0=no header bg; 0.9=light gray like template
  ) throws IOException {

    float totalW = 0f;
    for (var c: cols) totalW += c.width;

    // Header background
    if (headerBgGray > 0f) {
      cs.addRect(x, y - rowH + 2f, totalW, rowH);
      cs.setNonStrokingColor((int)(headerBgGray*255));
      cs.fill();
      cs.setNonStrokingColor(0);
    }

    // Header text
    float curX = x;
    for (var c: cols) {
      PdfText.drawText(cs, c.header, curX + 3f, y - rowH + 5f, bold, fontSize);
      curX += c.width;
    }
    y -= rowH;

    // Rows
    for (T r: rows) {
      curX = x;
      for (var c: cols) {
        String v = c.map.apply(r);
        float txtW = PdfText.stringWidth(font, fontSize, v);
        float tx = curX + 3f;
        if (c.align == Align.CENTER) {
          tx = curX + (c.width - txtW)/2f;
        } else if (c.align == Align.RIGHT) {
          tx = curX + c.width - 3f - txtW;
        }
        PdfText.drawText(cs, v, tx, y - rowH + 5f, font, fontSize);
        curX += c.width;
      }
      y -= rowH;
    }
    return y;
  }
}
