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

      // -------- Empresa / Obra (página 1) – valores --------
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

      float X_OBRA_LABEL_FALL = getF(cfg, "obra.xLabel", 60f);
      float X_OBRA_VAL        = getF(cfg, "obra.xVal",   100f);
      float Y_OBRA            = getF(cfg, "obra.y",      665f);
      float OBRA_MAX_W        = getF(cfg, "obra.maxW",   460f);

      // -------- Página 1 – labels (explícitos) --------
      float L_RAZAO_X = getF(cfg, "labels.page1.razao.x", 60f);
      float L_RAZAO_Y = getF(cfg, "labels.page1.razao.y", 175f);
      float L_CNPJ_X  = getF(cfg, "labels.page1.cnpj.x",  60f);
      float L_CNPJ_Y  = getF(cfg, "labels.page1.cnpj.y",  155f);
      float L_CONT_X  = getF(cfg, "labels.page1.contato.x",60f);
      float L_CONT_Y  = getF(cfg, "labels.page1.contato.y",135f);
      float L_TEL_X   = getF(cfg, "labels.page1.telefone.x",60f);
      float L_TEL_Y   = getF(cfg, "labels.page1.telefone.y",115f);
      float L_MAIL_X  = getF(cfg, "labels.page1.email.x", 60f);
      float L_MAIL_Y  = getF(cfg, "labels.page1.email.y", 95f);
      float L_OBRA_X  = getF(cfg, "labels.page1.obra.x",  X_OBRA_LABEL_FALL);
      float L_OBRA_Y  = getF(cfg, "labels.page1.obra.y",  Y_OBRA);

      // -------- Totais (fallback antigo) --------
      float X_TOT_LABEL_FALLBACK = getF(cfg, "totais.xLabel", 420f);
      float X_TOT_VAL_FALLBACK   = getF(cfg, "totais.xVal",   560f);
      float Y_TOT_TOP_FALLBACK   = getF(cfg, "totais.yTop",   200f);
      float Y_TOT_STEP_FALLBACK  = getF(cfg, "totais.step",    16f);

      // -------- Layout ANTIGO (fallback) --------
      float X_COL_DESC_OLD     = getF(cfg, "tabelas.cols.desc", 60f);
      float X_COL_COMP_OLD     = getF(cfg, "tabelas.cols.comp", 300f);
      float X_COL_UN_OLD       = getF(cfg, "tabelas.cols.un",   340f);
      float X_COL_QTD_OLD      = getF(cfg, "tabelas.cols.qtd",  380f);
      float X_COL_CUSTO_OLD    = getF(cfg, "tabelas.cols.custo",420f);
      float X_COL_PRECO_OLD    = getF(cfg, "tabelas.cols.preco",470f);
      float X_COL_DESC_LIM_OLD = getF(cfg, "tabelas.cols.descLim",530f);
      float X_COL_PRECO_KG_OLD = getF(cfg, "tabelas.cols.precoKg",585f);

      float Y_MAT_FIRSTLINE_OLD = getF(cfg, "tabelas.materiais.yFirst", 598f);
      float Y_SRV_FIRSTLINE_OLD = getF(cfg, "tabelas.servicos.yFirst",  328f);
      float Y_ROW_STEP_OLD      = getF(cfg, "tabelas.materiais.rowStep",16f);
      float Y_MIN_MAT_OLD       = getF(cfg, "tabelas.materiais.yMin",   380f);
      float Y_MIN_SRV_OLD       = getF(cfg, "tabelas.servicos.yMin",    190f);

      // -------- NOVO layout (como no print) --------
      JsonNode L_MAT = at(cfg, "layout.materiais");
      float MAT_QTD_RIGHT     = getF(L_MAT, "xQtdRight", 55f);
      float MAT_UN_X          = getF(L_MAT, "xUn",       105f);
      float MAT_DESC_X        = getF(L_MAT, "xDesc",     200f);
      float MAT_DESC_MAXW     = getF(L_MAT, "descMaxWidth", 360f);
      float MAT_SUBT_RIGHT    = getF(L_MAT, "xSubtotalRight", 510f);
      float MAT_Y_FIRST       = getF(L_MAT, "yFirst",    555f);
      float MAT_Y_MIN         = getF(L_MAT, "yMin",      380f);
      float MAT_ROW_STEP      = getF(L_MAT, "rowStep",   16f);
      // >>> offsets SÓ da descrição de Materiais <<<
      float MAT_DESC_DX       = getF(L_MAT, "descDx",    0f);
      float MAT_DESC_DY       = getF(L_MAT, "descDy",    0f);

      JsonNode L_SRV = at(cfg, "layout.servicos");
      float SRV_QTD_RIGHT     = getF(L_SRV, "xQtdRight", 55f);
      float SRV_UN_X          = getF(L_SRV, "xUn",       105f);
      float SRV_DESC_X        = getF(L_SRV, "xDesc",     200f);
      float SRV_DESC_MAXW     = getF(L_SRV, "descMaxWidth", 340f);
      float SRV_CUSTO_U_RIGHT = getF(L_SRV, "xCustoUnitRight", 470f);
      float SRV_SUBT_RIGHT    = getF(L_SRV, "xSubtotalRight",   565f);
      float SRV_Y_FIRST       = getF(L_SRV, "yFirst",    380f);
      float SRV_Y_MIN         = getF(L_SRV, "yMin",      190f);
      float SRV_ROW_STEP      = getF(L_SRV, "rowStep",   16f);
      // >>> offsets SÓ da descrição de Serviços (opcional) <<<
      float SRV_DESC_DX       = getF(L_SRV, "descDx",    0f);
      float SRV_DESC_DY       = getF(L_SRV, "descDy",    0f);

      // -------- Totais (página 7) --------
      float X_TOT_TAB_LABEL = getF(cfg, "totaisTables.xLabel", X_TOT_LABEL_FALLBACK);
      float X_TOT_TAB_VAL   = getF(cfg, "totaisTables.xVal",   X_TOT_VAL_FALLBACK);
      float Y_TOT_TAB_TOP   = getF(cfg, "totaisTables.yTop",   Y_TOT_TOP_FALLBACK);
      float Y_TOT_TAB_STEP  = getF(cfg, "totaisTables.step",   Y_TOT_STEP_FALLBACK);
      float VAL_DX          = getF(cfg, "totaisTables.valDx",  0f);
      float VAL_DY          = getF(cfg, "totaisTables.valDy",  0f);
      JsonNode AJUSTES_INDIVIDUAIS = at(cfg, "totaisTables.ajustesIndividuais");

      // --- Flags de GRID ---
      boolean debugGrid = "1".equals(System.getProperty("pdf.grid"))
              || cfg.path("debug").path("grid").asBoolean(false);
      float gridStep  = getSysF("pdf.gridStep", (float) cfg.path("debug").path("gridStep").asDouble(10.0));
      float gridMajor = getSysF("pdf.gridMajor", (float) cfg.path("debug").path("gridMajor").asDouble(100.0));

      try (PDDocument doc = Loader.loadPDF(templateBytes)) {

          // ===== Página 1 =====
          PDPage pageHeader = doc.getPage(PAGE_IDX_HEADER_TOTAIS);
          try (PDPageContentStream cs = new PDPageContentStream(doc, pageHeader, AppendMode.APPEND, true, true)) {
              normalizeToCropBox(cs, pageHeader);

              if (debugGrid) {
                  drawGrid(cs, pageHeader, gridStep, gridMajor);
                  drawProbes(cs, cfg.path("probes").path("page1"));
              }

              // LABELS
              BR.drawText(cs, FONT_REG, FONT_H, L_RAZAO_X, L_RAZAO_Y, "Razão Social:");
              BR.drawText(cs, FONT_REG, FONT_H, L_CNPJ_X,  L_CNPJ_Y,  "CNPJ:");
              BR.drawText(cs, FONT_REG, FONT_H, L_CONT_X,  L_CONT_Y,  "Contato:");
              BR.drawText(cs, FONT_REG, FONT_H, L_TEL_X,   L_TEL_Y,   "Telefone:");
              BR.drawText(cs, FONT_REG, FONT_H, L_MAIL_X,  L_MAIL_Y,  "E-mail:");
              BR.drawText(cs, FONT_REG, FONT_H, L_OBRA_X,  L_OBRA_Y,  "Obra:");

              // VALORES
              var emp = dto.getEmpresa();
              BR.drawText(cs, FONT_REG, FONT_H, X_EMP_RAZAO, Y_EMP_RAZAO, emp != null ? emp.getRazaoSocial() : "-");
              BR.drawText(cs, FONT_REG, FONT_H, X_EMP_CNPJ,  Y_EMP_CNPJ,  emp != null ? emp.getCnpj()        : "-");
              BR.drawText(cs, FONT_REG, FONT_H, X_EMP_CONT,  Y_EMP_CONT,  emp != null ? emp.getContato()     : "-");
              BR.drawText(cs, FONT_REG, FONT_H, X_EMP_TEL,   Y_EMP_TEL,   emp != null ? emp.getTelefone()    : "-");
              BR.drawText(cs, FONT_REG, FONT_H, X_EMP_EMAIL, Y_EMP_EMAIL, emp != null ? emp.getEmail()       : "-");

              drawParagraph(cs, FONT_REG, FONT_H, X_OBRA_VAL, Y_OBRA, OBRA_MAX_W, safe(dto.getObra()), 12f);
          }

          // ===== Página 7 =====
          PDPage pageTab = doc.getPage(PAGE_IDX_TABELAS);
          try (PDPageContentStream cs = new PDPageContentStream(doc, pageTab, AppendMode.APPEND, true, true)) {
              normalizeToCropBox(cs, pageTab);

              if (debugGrid) {
                  drawGrid(cs, pageTab, gridStep, gridMajor);
                  drawProbes(cs, cfg.path("probes").path("pageTables"));
              }

              boolean hasNewLayout = (L_MAT != null && !L_MAT.isMissingNode()) || (L_SRV != null && !L_SRV.isMissingNode());

              if (hasNewLayout) {
                  if (hasItems(dto.getMateriais())) {
                      drawMaterialsRowsV2(cs, dto.getMateriais(),
                              MAT_Y_FIRST, MAT_ROW_STEP, MAT_Y_MIN,
                              MAT_QTD_RIGHT, MAT_UN_X,
                              MAT_DESC_X + MAT_DESC_DX, MAT_DESC_MAXW, // X com offset
                              MAT_SUBT_RIGHT,
                              MAT_DESC_DY // Y da descrição (offset)
                      );
                  }
                  if (hasItems(dto.getServicos())) {
                      drawServicesRowsV2(cs, dto.getServicos(),
                              SRV_Y_FIRST, SRV_ROW_STEP, SRV_Y_MIN,
                              SRV_QTD_RIGHT, SRV_UN_X,
                              SRV_DESC_X + SRV_DESC_DX, SRV_DESC_MAXW,
                              SRV_CUSTO_U_RIGHT, SRV_SUBT_RIGHT,
                              SRV_DESC_DY
                      );
                  }
              } else {
                  // Fallback (layout antigo)
                  if (hasItems(dto.getMateriais())) {
                      drawTableRowsOld(cs, dto.getMateriais(),
                              Y_MAT_FIRSTLINE_OLD, Y_ROW_STEP_OLD, Y_MIN_MAT_OLD,
                              X_COL_DESC_OLD, X_COL_COMP_OLD, X_COL_UN_OLD, X_COL_QTD_OLD,
                              X_COL_CUSTO_OLD, X_COL_PRECO_OLD, X_COL_DESC_LIM_OLD, X_COL_PRECO_KG_OLD);
                  }
                  if (hasItems(dto.getServicos())) {
                      drawTableRowsOld(cs, dto.getServicos(),
                              Y_SRV_FIRSTLINE_OLD, Y_ROW_STEP_OLD, Y_MIN_SRV_OLD,
                              X_COL_DESC_OLD, X_COL_COMP_OLD, X_COL_UN_OLD, X_COL_QTD_OLD,
                              X_COL_CUSTO_OLD, X_COL_PRECO_OLD, X_COL_DESC_LIM_OLD, X_COL_PRECO_KG_OLD);
                  }
              }

              // Totais (mantém ajustesIndividuais)
              drawTotals(cs, dto.getTotais(),
                      X_TOT_TAB_LABEL, X_TOT_TAB_VAL,
                      Y_TOT_TAB_TOP, Y_TOT_TAB_STEP,
                      VAL_DX, VAL_DY,
                      AJUSTES_INDIVIDUAIS);
          }

          // Exporta
          try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
              doc.save(baos);
              return baos.toByteArray();
          }
      }
  }

  // ----------------- Helpers de desenho -----------------

  private static void normalizeToCropBox(PDPageContentStream cs, PDPage page) throws IOException {
      PDRectangle crop = page.getCropBox();
      if (crop != null) {
          cs.transform(Matrix.getTranslateInstance(-crop.getLowerLeftX(), -crop.getLowerLeftY()));
      }
  }

  /** Layout NOVO – Materiais */
  private static void drawMaterialsRowsV2(
          PDPageContentStream cs,
          List<ItemDto> itens,
          float yStart, float rowStep, float yMin,
          float xQtdRight, float xUn,
          float xDesc, float descMaxWidth,
          float xSubtotalRight,
          float descDy
  ) throws IOException {
      float y = yStart;
      for (ItemDto it : itens) {
          if (y < yMin) break;

          drawRightAligned(cs, FONT_REG, FONT_H, xQtdRight, y, BR.numero(it.getQuantidade())); // Qtde
          BR.drawText(cs, FONT_REG, FONT_H, xUn, y, safe(it.getUnidade()));                    // Unidade
          drawWrapped(cs, FONT_REG, FONT_H, xDesc, y + descDy, descMaxWidth, safe(it.getDescricao()), 10f); // Descrição (X/Y ajustáveis)
          double subtotal = (it.getPrecoVenda() != null && it.getPrecoVenda().doubleValue() > 0)
                  ? it.getPrecoVenda().doubleValue()
                  : ((it.getCusto() != null && it.getQuantidade() != null)
                        ? it.getCusto().doubleValue() * it.getQuantidade().doubleValue()
                        : 0.0);
          drawRightAligned(cs, FONT_REG, FONT_H, xSubtotalRight, y, BR.moeda(subtotal));       // Subtotal

          y -= rowStep;
      }
  }

  /** Layout NOVO – Serviços */
  private static void drawServicesRowsV2(
          PDPageContentStream cs,
          List<ItemDto> itens,
          float yStart, float rowStep, float yMin,
          float xQtdRight, float xUn,
          float xDesc, float descMaxWidth,
          float xCustoUnitRight, float xSubtotalRight,
          float descDy
  ) throws IOException {
      float y = yStart;
      for (ItemDto it : itens) {
          if (y < yMin) break;

          drawRightAligned(cs, FONT_REG, FONT_H, xQtdRight, y, BR.numero(it.getQuantidade())); // Qtde
          BR.drawText(cs, FONT_REG, FONT_H, xUn, y, safe(it.getUnidade()));                    // Unidade
          drawWrapped(cs, FONT_REG, FONT_H, xDesc, y + descDy, descMaxWidth, safe(it.getDescricao()), 10f); // Serviço (X/Y ajustáveis)
          double custoU = 0.0;
          if (it.getPrecoVenda() != null && it.getQuantidade() != null && it.getQuantidade().doubleValue() > 0) {
              custoU = it.getPrecoVenda().doubleValue() / it.getQuantidade().doubleValue();
          } else if (it.getCusto() != null && it.getQuantidade() != null && it.getQuantidade().doubleValue() > 0) {
              custoU = it.getCusto().doubleValue() / it.getQuantidade().doubleValue();
          }
          drawRightAligned(cs, FONT_REG, FONT_H, xCustoUnitRight, y, BR.moeda(custoU));       // Custo Unit.
          double subtotal = (it.getPrecoVenda() != null && it.getPrecoVenda().doubleValue() > 0)
                  ? it.getPrecoVenda().doubleValue()
                  : ((it.getCusto() != null && it.getQuantidade() != null)
                        ? it.getCusto().doubleValue() * it.getQuantidade().doubleValue()
                        : 0.0);
          drawRightAligned(cs, FONT_REG, FONT_H, xSubtotalRight, y, BR.moeda(subtotal));      // Subtotal

          y -= rowStep;
      }
  }

  /** Fallback genérico (8 colunas) */
  private static void drawTableRowsOld(
          PDPageContentStream cs,
          List<ItemDto> itens,
          float yStart, float rowStep, float yMin,
          float xDesc, float xComp, float xUn, float xQtd,
          float xCusto, float xPreco, float xDescLim, float xPrecoKg
  ) throws IOException {
      float y = yStart;
      for (ItemDto it : itens) {
          if (y < yMin) break;

          drawWrapped(cs, FONT_REG, FONT_H, xDesc, y, 230f, safe(it.getDescricao()), 10f);
          BR.drawText(cs, FONT_REG, FONT_H, xComp, y, safe(it.getComp()));
          BR.drawText(cs, FONT_REG, FONT_H, xUn,   y, safe(it.getUnidade()));
          drawRightAligned(cs, FONT_REG, FONT_H, xQtd,     y, BR.numero(it.getQuantidade()));
          drawRightAligned(cs, FONT_REG, FONT_H, xCusto,   y, BR.moeda(it.getCusto()));
          drawRightAligned(cs, FONT_REG, FONT_H, xPreco,   y, BR.moeda(it.getPrecoVenda()));
          String descPerc = (it.getLimiteDesconto() != null) ? it.getLimiteDesconto().toString() + "%" : "-";
          drawRightAligned(cs, FONT_REG, FONT_H, xDescLim, y, descPerc);
          drawRightAligned(cs, FONT_REG, FONT_H, xPrecoKg, y, BR.moeda(it.getPrecoKg()));
          y -= rowStep;
      }
  }

  private static void drawRightAligned(PDPageContentStream cs, PDFont font, float fontSize,
                                       float xRight, float y, String text) throws IOException {
      if (text == null) text = "-";
      float w = font.getStringWidth(text) / 1000f * fontSize;
      BR.drawText(cs, font, fontSize, xRight - w, y, text);
  }

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

  private static void drawTotals(PDPageContentStream cs, TotaisDto t,
                                 float X_TOT_LABEL, float X_TOT_VAL,
                                 float Y_TOT_TOP, float Y_TOT_STEP,
                                 float VAL_DX, float VAL_DY,
                                 JsonNode AJUSTES) throws IOException {
      if (t == null) return;
      float y = Y_TOT_TOP;

      drawTotalLine(cs, "Subtotal",        t.getSubtotal(),       y, X_TOT_LABEL, X_TOT_VAL,
              VAL_DX + getAdj(AJUSTES, "subtotal", "dx"),
              VAL_DY + getAdj(AJUSTES, "subtotal", "dy"));
      y -= Y_TOT_STEP;

      drawTotalLine(cs, "Desconto",        t.getDesconto(),       y, X_TOT_LABEL, X_TOT_VAL,
              VAL_DX + getAdj(AJUSTES, "desconto", "dx"),
              VAL_DY + getAdj(AJUSTES, "desconto", "dy"));
      y -= Y_TOT_STEP;

      drawTotalLine(cs, "Total Materiais", t.getTotalMateriais(), y, X_TOT_LABEL, X_TOT_VAL,
              VAL_DX + getAdj(AJUSTES, "totalMateriais", "dx"),
              VAL_DY + getAdj(AJUSTES, "totalMateriais", "dy"));
      y -= Y_TOT_STEP;

      drawTotalLine(cs, "Total Serviços",  t.getTotalServicos(),  y, X_TOT_LABEL, X_TOT_VAL,
              VAL_DX + getAdj(AJUSTES, "totalServicos", "dx"),
              VAL_DY + getAdj(AJUSTES, "totalServicos", "dy"));
      y -= Y_TOT_STEP;

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

  // ----------------- DEBUG -----------------

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

      cs.setStrokingColor(Color.DARK_GRAY);
      cs.setLineWidth(0.6f);
      cs.moveTo(0, 0); cs.lineTo(w, 0); cs.stroke(); // X
      cs.moveTo(0, 0); cs.lineTo(0, h); cs.stroke(); // Y
  }

  private static void drawProbes(PDPageContentStream cs, JsonNode arr) throws IOException {
      if (arr == null || !arr.isArray()) return;
      for (Iterator<JsonNode> it = arr.elements(); it.hasNext();) {
          JsonNode p = it.next();
          float x = (float) p.path("x").asDouble();
          float y = (float) p.path("y").asDouble();
          String label = p.path("label").asText("(" + (int)x + "," + (int)y + ")");

          cs.setStrokingColor(new Color(180, 0, 0));
          cs.setLineWidth(0.8f);
          cs.moveTo(x - 4, y); cs.lineTo(x + 4, y); cs.stroke();
          cs.moveTo(x, y - 4); cs.lineTo(x, y + 4); cs.stroke();

          BR.drawText(cs, FONT_REG, 8f, x + 6, y + 2, label);
      }
  }

  // ----------------- Util -----------------

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
      if (n == null) return def;
      JsonNode j = at(n, path);
      return (j != null && j.isNumber()) ? (float) j.asDouble() : def;
  }

  private static JsonNode at(JsonNode n, String path) {
      if (n == null) return null;
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

  private static float getAdj(JsonNode ajustes, String key, String axis) {
      if (ajustes == null || ajustes.isMissingNode()) return 0f;
      JsonNode node = ajustes.path(key).path(axis);
      return node.isNumber() ? (float) node.asDouble() : 0f;
  }
}
