package com.stongel.pdfdrawer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stongel.pdfdrawer.dto.BudgetDto;
import com.stongel.pdfdrawer.dto.ItemDto;
import com.stongel.pdfdrawer.dto.TotaisDto;
import com.stongel.pdfdrawer.util.BR;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.util.Matrix;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

@Component
public class StongelTemplateRenderer {

  // Tipografia
  private static final PDFont FONT_REG = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
  private static final float  FONT_H   = 10f;

  public byte[] renderFromTemplate(BudgetDto dto) throws Exception {

      // --- Carrega template ---
      byte[] templateBytes;
      try (InputStream is = new ClassPathResource("templates/STONGEL - PDF.pdf").getInputStream()) {
          templateBytes = is.readAllBytes();
      }

      // --- Carrega coordenadas JSON (reload a cada chamada) ---
      JsonNode cfg = loadDynamicConfig();

      // Páginas (0-based)
      int PAGE_IDX_HEADER_TOTAIS = getInt(cfg, "pageIndexes.headerTotais", 0);
      int PAGE_IDX_TABELAS       = getInt(cfg, "pageIndexes.tables", 6);

      // -------- Empresa / Obra (página header) ------------
      float X_EMP_RAZAO = getF(cfg, "empresa.razao.x", 120f);
      float Y_EMP_RAZAO = getF(cfg, "empresa.razao.y", 175f);
      float X_EMP_CNPJ  = getF(cfg, "empresa.cnpj.x", 95f);
      float Y_EMP_CNPJ  = getF(cfg, "empresa.cnpj.y", 155f);
      float X_EMP_CONT  = getF(cfg, "empresa.contato.x", 100f);
      float Y_EMP_CONT  = getF(cfg, "empresa.contato.y", 135f);
      float X_EMP_TEL   = getF(cfg, "empresa.tel.x", 100f);
      float Y_EMP_TEL   = getF(cfg, "empresa.tel.y", 115f);
      float X_EMP_EMAIL = getF(cfg, "empresa.email.x", 100f);
      float Y_EMP_EMAIL = getF(cfg, "empresa.email.y", 95f);

      float X_OBRA_LABEL = getF(cfg, "obra.xLabel", 60f);
      float X_OBRA_VAL   = getF(cfg, "obra.xVal", 100f);
      float Y_OBRA       = getF(cfg, "obra.y", 665f);
      float OBRA_MAX_W   = getF(cfg, "obra.maxW", 460f);

      // -------- Totais (fallback) ------------
      float X_TOT_LABEL_FALLBACK = getF(cfg, "totais.xLabel", 420f);
      float X_TOT_VAL_FALLBACK   = getF(cfg, "totais.xVal", 560f);
      float Y_TOT_TOP_FALLBACK   = getF(cfg, "totais.yTop", 200f);
      float Y_TOT_STEP_FALLBACK  = getF(cfg, "totais.step", 16f);

      // -------- Tabela (página 7) ------------
      float X_COL_DESC     = getF(cfg, "tabelas.cols.desc", 60f);
      float X_COL_COMP     = getF(cfg, "tabelas.cols.comp", 300f);
      float X_COL_UN       = getF(cfg, "tabelas.cols.un", 340f);
      float X_COL_QTD      = getF(cfg, "tabelas.cols.qtd", 380f);   // borda direita
      float X_COL_CUSTO    = getF(cfg, "tabelas.cols.custo", 420f); // borda direita
      float X_COL_PRECO    = getF(cfg, "tabelas.cols.preco", 470f); // borda direita
      float X_COL_DESC_LIM = getF(cfg, "tabelas.cols.descLim", 530f); // borda direita
      float X_COL_PRECO_KG = getF(cfg, "tabelas.cols.precoKg", 585f); // borda direita

      float Y_MAT_FIRSTLINE = getF(cfg, "tabelas.materiais.yFirst", 598f);
      float Y_SRV_FIRSTLINE = getF(cfg, "tabelas.servicos.yFirst", 328f);
      float Y_ROW_STEP      = getF(cfg, "tabelas.materiais.rowStep", 16f);
      float Y_MIN_MAT       = getF(cfg, "tabelas.materiais.yMin", 380f);
      float Y_MIN_SRV       = getF(cfg, "tabelas.servicos.yMin", 190f);

      // -------- Totais (página 7) ------------
      float X_TOT_TAB_LABEL = getF(cfg, "totaisTables.xLabel", X_TOT_LABEL_FALLBACK);
      float X_TOT_TAB_VAL   = getF(cfg, "totaisTables.xVal",   X_TOT_VAL_FALLBACK);
      float Y_TOT_TAB_TOP   = getF(cfg, "totaisTables.yTop",   Y_TOT_TOP_FALLBACK);
      float Y_TOT_TAB_STEP  = getF(cfg, "totaisTables.step",   Y_TOT_STEP_FALLBACK);

      // >>> OFFSETS globais apenas para os VALORES (não afetam rótulos) <<<
      float VAL_DX = getF(cfg, "totaisTables.valDx", 0f);
      float VAL_DY = getF(cfg, "totaisTables.valDy", 0f);

      // >>> Ajustes individuais (por linha) <<<
      JsonNode AJUSTES_INDIVIDUAIS = at(cfg, "totaisTables.ajustesIndividuais");

      // --- Flags de GRID (System property OU JSON) ---
      boolean debugGrid = "1".equals(System.getProperty("pdf.grid"))
              || cfg.path("debug").path("grid").asBoolean(false);
      float gridStep  = getSysF("pdf.gridStep", (float) cfg.path("debug").path("gridStep").asDouble(10.0));
      float gridMajor = getSysF("pdf.gridMajor", (float) cfg.path("debug").path("gridMajor").asDouble(100.0));

      try (PDDocument doc = Loader.loadPDF(templateBytes)) {

          // ===== Página 1: Cabeçalho / Obra (SEM TOTAIS) =====
          PDPage pageHeader = doc.getPage(PAGE_IDX_HEADER_TOTAIS);
          try (PDPageContentStream cs = new PDPageContentStream(doc, pageHeader, AppendMode.APPEND, true, true)) {
              normalizeToCropBox(cs, pageHeader);

              if (debugGrid) {
                  drawGrid(cs, pageHeader, gridStep, gridMajor);
                  drawProbes(cs, cfg.path("probes").path("page1"));
              }

              var emp = dto.getEmpresa();
              BR.drawText(cs, FONT_REG, FONT_H, X_EMP_RAZAO, Y_EMP_RAZAO, emp != null ? emp.getRazaoSocial() : "-");
              BR.drawText(cs, FONT_REG, FONT_H, X_EMP_CNPJ,  Y_EMP_CNPJ,  emp != null ? emp.getCnpj()        : "-");
              BR.drawText(cs, FONT_REG, FONT_H, X_EMP_CONT,  Y_EMP_CONT,  emp != null ? emp.getContato()     : "-");
              BR.drawText(cs, FONT_REG, FONT_H, X_EMP_TEL,   Y_EMP_TEL,   emp != null ? emp.getTelefone()    : "-");
              BR.drawText(cs, FONT_REG, FONT_H, X_EMP_EMAIL, Y_EMP_EMAIL, emp != null ? emp.getEmail()       : "-");

              BR.drawText(cs, FONT_REG, FONT_H, X_OBRA_LABEL, Y_OBRA, "Obra:");
              drawParagraph(cs, FONT_REG, FONT_H, X_OBRA_VAL, Y_OBRA, OBRA_MAX_W, safe(dto.getObra()), 12f);

              // (Sem totais na página 1)
          }

          // ===== Página 7: Materiais / Serviços + TOTAIS =====
          PDPage pageTab = doc.getPage(PAGE_IDX_TABELAS);
          try (PDPageContentStream cs = new PDPageContentStream(doc, pageTab, AppendMode.APPEND, true, true)) {
              normalizeToCropBox(cs, pageTab);

              if (debugGrid) {
                  drawGrid(cs, pageTab, gridStep, gridMajor);
                  drawProbes(cs, cfg.path("probes").path("pageTables"));
              }

              // Materiais
              if (hasItems(dto.getMateriais())) {
                  drawTableRows(
                          cs,
                          dto.getMateriais(),
                          Y_MAT_FIRSTLINE, Y_ROW_STEP, Y_MIN_MAT,
                          X_COL_DESC, X_COL_COMP, X_COL_UN, X_COL_QTD,
                          X_COL_CUSTO, X_COL_PRECO, X_COL_DESC_LIM, X_COL_PRECO_KG
                  );
              }

              // Serviços
              if (hasItems(dto.getServicos())) {
                  drawTableRows(
                          cs,
                          dto.getServicos(),
                          Y_SRV_FIRSTLINE, Y_ROW_STEP, Y_MIN_SRV,
                          X_COL_DESC, X_COL_COMP, X_COL_UN, X_COL_QTD,
                          X_COL_CUSTO, X_COL_PRECO, X_COL_DESC_LIM, X_COL_PRECO_KG
                  );
              }

              // Totais na página 7 (com offsets globais + individuais)
              drawTotals(
                      cs,
                      dto.getTotais(),
                      X_TOT_TAB_LABEL, X_TOT_TAB_VAL,
                      Y_TOT_TAB_TOP, Y_TOT_TAB_STEP,
                      VAL_DX, VAL_DY,
                      AJUSTES_INDIVIDUAIS
              );
          }

          // Exporta
          try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
              doc.save(baos);
              return baos.toByteArray();
          }
      }
  }

  // ----------------- Helpers de desenho -----------------

  /** Normaliza a origem (0,0) para a CropBox da página. */
  private static void normalizeToCropBox(PDPageContentStream cs, PDPage page) throws IOException {
      PDRectangle crop = page.getCropBox();
      if (crop != null) {
          cs.transform(Matrix.getTranslateInstance(-crop.getLowerLeftX(), -crop.getLowerLeftY()));
      }
  }

  /** Tabela em colunas alinhadas (página 7). */
  private static void drawTableRows(
          PDPageContentStream cs,
          List<ItemDto> itens,
          float yStart, float rowStep, float yMin,
          float xDesc, float xComp, float xUn, float xQtd,
          float xCusto, float xPreco, float xDescLim, float xPrecoKg
  ) throws IOException {

      float y = yStart;
      for (ItemDto it : itens) {
          if (y < yMin) break;

          // Descrição (quebra automática, ~230 px de largura a partir de xDesc)
          drawWrapped(cs, FONT_REG, FONT_H, xDesc, y, 230f, safe(it.getDescricao()), 10f);

          // Colunas textuais (esquerda)
          BR.drawText(cs, FONT_REG, FONT_H, xComp, y, safe(it.getComp()));
          BR.drawText(cs, FONT_REG, FONT_H, xUn,   y, safe(it.getUnidade()));

          // Colunas numéricas (direita) — usar os x* como BORDA DIREITA
          drawRightAligned(cs, FONT_REG, FONT_H, xQtd,     y, BR.numero(it.getQuantidade()));
          drawRightAligned(cs, FONT_REG, FONT_H, xCusto,   y, BR.moeda(it.getCusto()));
          drawRightAligned(cs, FONT_REG, FONT_H, xPreco,   y, BR.moeda(it.getPrecoVenda()));

          String descPerc = (it.getLimiteDesconto() != null) ? it.getLimiteDesconto().toString() + "%" : "-";
          drawRightAligned(cs, FONT_REG, FONT_H, xDescLim, y, descPerc);

          drawRightAligned(cs, FONT_REG, FONT_H, xPrecoKg, y, BR.moeda(it.getPrecoKg()));

          y -= rowStep;
      }
  }

  /** Texto alinhado à direita, usando xRight como borda direita. */
  private static void drawRightAligned(PDPageContentStream cs, PDFont font, float fontSize,
                                       float xRight, float y, String text) throws IOException {
      if (text == null) text = "-";
      float w = font.getStringWidth(text) / 1000f * fontSize;
      BR.drawText(cs, font, fontSize, xRight - w, y, text);
  }

  /** Texto com quebra automática até largura máxima. */
  private static void drawWrapped(PDPageContentStream cs, PDFont font, float fontSize,
                                  float x, float y, float maxWidth,
                                  String text, float lineStep) throws IOException {
      if (text == null || text.isBlank()) return;
      String[] words = text.split("\\s+");
      String line = "";
      float cursorY = y;
      for (String w : words) {
          String probe = line.isEmpty() ? w : line + " " + w;
          float width = font.getStringWidth(probe) / 1000f * fontSize;
          if (width > maxWidth) {
              BR.drawText(cs, font, fontSize, x, cursorY, line);
              cursorY -= lineStep;
              line = w;
          } else {
              line = probe;
          }
      }
      if (!line.isEmpty()) {
          BR.drawText(cs, font, fontSize, x, cursorY, line);
      }
  }

  /** Parágrafo com quebra automática (página 1: Obra). */
  private static void drawParagraph(PDPageContentStream cs, PDFont font, float fontSize,
                                    float x, float y, float maxWidth,
                                    String text, float lineStep) throws IOException {
      if (text == null || text.isBlank()) {
          BR.drawText(cs, font, fontSize, x, y, "-");
          return;
      }
      String[] words = text.split("\\s+");
      String line = "";
      float cursorY = y;

      for (String w : words) {
          String probe = line.isEmpty() ? w : line + " " + w;
          float width = font.getStringWidth(probe) / 1000f * fontSize;
          if (width > maxWidth) {
              BR.drawText(cs, font, fontSize, x, cursorY, line);
              cursorY -= lineStep;
              line = w;
          } else {
              line = probe;
          }
      }
      if (!line.isEmpty()) {
          BR.drawText(cs, font, fontSize, x, cursorY, line);
      }
  }

  /** Totais (página 7): aplica offsets globais e individuais de valores. */
  private static void drawTotals(PDPageContentStream cs, TotaisDto t,
                                 float X_TOT_LABEL, float X_TOT_VAL,
                                 float Y_TOT_TOP, float Y_TOT_STEP,
                                 float VAL_DX, float VAL_DY,
                                 JsonNode AJUSTES) throws IOException {
      if (t == null) return;
      float y = Y_TOT_TOP;

      // Subtotal
      drawTotalLine(cs, "Subtotal",        t.getSubtotal(),       y, X_TOT_LABEL, X_TOT_VAL,
              VAL_DX + getAdj(AJUSTES, "subtotal", "dx"),
              VAL_DY + getAdj(AJUSTES, "subtotal", "dy"));
      y -= Y_TOT_STEP;

      // Desconto
      drawTotalLine(cs, "Desconto",        t.getDesconto(),       y, X_TOT_LABEL, X_TOT_VAL,
              VAL_DX + getAdj(AJUSTES, "desconto", "dx"),
              VAL_DY + getAdj(AJUSTES, "desconto", "dy"));
      y -= Y_TOT_STEP;

      // Total Materiais
      drawTotalLine(cs, "Total Materiais", t.getTotalMateriais(), y, X_TOT_LABEL, X_TOT_VAL,
              VAL_DX + getAdj(AJUSTES, "totalMateriais", "dx"),
              VAL_DY + getAdj(AJUSTES, "totalMateriais", "dy"));
      y -= Y_TOT_STEP;

      // Total Serviços
      drawTotalLine(cs, "Total Serviços",  t.getTotalServicos(),  y, X_TOT_LABEL, X_TOT_VAL,
              VAL_DX + getAdj(AJUSTES, "totalServicos", "dx"),
              VAL_DY + getAdj(AJUSTES, "totalServicos", "dy"));
      y -= Y_TOT_STEP;

      // TOTAL GERAL (em negrito lógico — mesma fonte padrão aqui)
      drawTotalBold(cs, "TOTAL GERAL",     t.getTotalGeral(),     y, X_TOT_LABEL, X_TOT_VAL,
              VAL_DX + getAdj(AJUSTES, "totalGeral", "dx"),
              VAL_DY + getAdj(AJUSTES, "totalGeral", "dy"));
  }

  private static void drawTotalLine(PDPageContentStream cs, String label, Number val, float y,
                                    float X_TOT_LABEL, float X_TOT_VAL,
                                    float VAL_DX, float VAL_DY) throws IOException {
      BR.drawText(cs, FONT_REG, FONT_H, X_TOT_LABEL, y, label);
      BR.drawText(cs, FONT_REG, FONT_H, X_TOT_VAL + VAL_DX, y + VAL_DY, BR.moeda(val));
  }

  private static void drawTotalBold(PDPageContentStream cs, String label, Number val, float y,
                                    float X_TOT_LABEL, float X_TOT_VAL,
                                    float VAL_DX, float VAL_DY) throws IOException {
      BR.drawText(cs, FONT_REG, FONT_H, X_TOT_LABEL, y, label);
      BR.drawText(cs, FONT_REG, FONT_H, X_TOT_VAL + VAL_DX, y + VAL_DY, BR.moeda(val));
  }

  // ----------------- DEBUG / Calibração -----------------

  /** Grade com subgraduação (step) e linhas maiores (major). */
  private static void drawGrid(PDPageContentStream cs, PDPage page, float step, float major) throws IOException {
      PDRectangle b = page.getCropBox();
      float w = b.getWidth();
      float h = b.getHeight();

      cs.setLineWidth(0.25f);

      for (float x = 0; x <= w + 0.1f; x += step) {
          boolean isMajor = (Math.round(x) % Math.round(major) == 0);
          cs.setStrokingColor(isMajor ? Color.GRAY : new Color(200,200,200));
          cs.moveTo(x, 0);
          cs.lineTo(x, h);
          cs.stroke();
          if (isMajor) {
              BR.drawText(cs, FONT_REG, 7f, x + 2, 3, String.valueOf((int) x));
          }
      }
      for (float y = 0; y <= h + 0.1f; y += step) {
          boolean isMajor = (Math.round(y) % Math.round(major) == 0);
          cs.setStrokingColor(isMajor ? Color.GRAY : new Color(200,200,200));
          cs.moveTo(0, y);
          cs.lineTo(w, y);
          cs.stroke();
          if (isMajor) {
              BR.drawText(cs, FONT_REG, 7f, 2, y + 2, String.valueOf((int) y));
          }
      }

      // Eixos
      cs.setStrokingColor(Color.DARK_GRAY);
      cs.setLineWidth(0.6f);
      cs.moveTo(0, 0); cs.lineTo(w, 0); cs.stroke(); // X
      cs.moveTo(0, 0); cs.lineTo(0, h); cs.stroke(); // Y
  }

  /** Desenha probes de JSON: cruz + rótulo em (x,y). */
  private static void drawProbes(PDPageContentStream cs, JsonNode arr) throws IOException {
      if (arr == null || !arr.isArray()) return;
      for (Iterator<JsonNode> it = arr.elements(); it.hasNext();) {
          JsonNode p = it.next();
          float x = (float) p.path("x").asDouble();
          float y = (float) p.path("y").asDouble();
          String label = p.path("label").asText("(" + (int)x + "," + (int)y + ")");

          // cruz
          cs.setStrokingColor(new Color(180, 0, 0));
          cs.setLineWidth(0.8f);
          cs.moveTo(x - 4, y); cs.lineTo(x + 4, y); cs.stroke();
          cs.moveTo(x, y - 4); cs.lineTo(x, y + 4); cs.stroke();

          // label
          BR.drawText(cs, FONT_REG, 8f, x + 6, y + 2, label);
      }
  }

  // ----------------- Util -----------------

  /** Lê o stongel-coords.json do classpath a cada chamada. */
  private static JsonNode loadDynamicConfig() {
      try (InputStream is = new ClassPathResource("templates/stongel-coords.json").getInputStream()) {
          return new ObjectMapper().readTree(is);
      } catch (Exception e) {
          try {
              return new ObjectMapper().readTree("{\"pageIndexes\":{\"headerTotais\":0,\"tables\":6}}");
          } catch (IOException ex) {
              throw new RuntimeException("Falha ao carregar stongel-coords.json", ex);
          }
      }
  }

  private static int getInt(JsonNode n, String path, int def) {
      JsonNode j = at(n, path);
      return (j != null && j.isInt()) ? j.asInt() : def;
  }

  private static float getF(JsonNode n, String path, float def) {
      JsonNode j = at(n, path);
      return (j != null && j.isNumber()) ? (float) j.asDouble() : def;
  }

  private static JsonNode at(JsonNode n, String path) {
      String[] ps = path.split("\\.");
      JsonNode cur = n;
      for (String p : ps) {
          if (cur == null) return null;
          cur = cur.get(p);
      }
      return cur;
  }

  private static float getSysF(String prop, float def) {
      String v = System.getProperty(prop);
      if (v == null || v.isBlank()) return def;
      try { return Float.parseFloat(v); } catch (Exception e) { return def; }
  }

  private static boolean hasItems(List<?> list) {
      return list != null && !list.isEmpty();
  }

  private static String safe(String s) {
      return (s == null || s.isBlank()) ? "-" : s;
  }

  /** Lê dx/dy individuais de 'totaisTables.ajustesIndividuais'. */
  private static float getAdj(JsonNode ajustes, String key, String axis) {
      if (ajustes == null || ajustes.isMissingNode()) return 0f;
      JsonNode node = ajustes.path(key).path(axis);
      return node.isNumber() ? (float) node.asDouble() : 0f;
  }
}
