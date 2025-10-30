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

/**
 * Renderer com:
 * - Leitura de coordenadas de templates/stongel-coords.json (sem recompilar)
 * - Preenche pág. 1 (header/totais) e pág. 7 (tabelas) — apenas DADOS
 * - Origem normalizada para CropBox
 * - Grade de calibração: -Dpdf.grid=1 (passo configurável via -Dpdf.gridStep e -Dpdf.gridMajor)
 * - Probes (alfinetes): lidos do JSON, desenham cruzes + rótulos em (x,y) exatos
 */
@Component
public class StongelTemplateRenderer {

    // ---- Tipografia ----
    private static final PDFont FONT_REG = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont FONT_B   = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final float FONT_H      = 10f;
    private static final float FONT_H_BOLD = 12f;

    // ---- Config carregada do JSON ----
    private static final JsonNode CFG = loadConfig(); // lazy static
    private static final int PAGE_IDX_HEADER_TOTAIS = getInt(CFG, "pageIndexes.headerTotais", 0);
    private static final int PAGE_IDX_TABELAS       = getInt(CFG, "pageIndexes.tables", 6);

    // Empresa (página 1)
    private static final float X_EMPRESA   = getF(CFG, "empresa.x", 60f);
    private static final float Y_EMP_RAZAO = getF(CFG, "empresa.yRazao", 740f);
    private static final float Y_EMP_CNPJ  = getF(CFG, "empresa.yCnpj", 725f);
    private static final float Y_EMP_TEL   = getF(CFG, "empresa.yTel", 710f);
    private static final float Y_EMP_EMAIL = getF(CFG, "empresa.yEmail", 695f);

    // Obra (página 1)
    private static final float X_OBRA_LABEL = getF(CFG, "obra.xLabel", 60f);
    private static final float X_OBRA_VAL   = getF(CFG, "obra.xVal", 100f);
    private static final float Y_OBRA       = getF(CFG, "obra.y", 665f);
    private static final float OBRA_MAX_W   = getF(CFG, "obra.maxW", 460f);

    // Tabela (página 7) — apenas DADOS
    private static final float X_COL_DESC     = getF(CFG, "tabelas.cols.desc", 60f);
    private static final float X_COL_COMP     = getF(CFG, "tabelas.cols.comp", 300f);
    private static final float X_COL_UN       = getF(CFG, "tabelas.cols.un", 340f);
    private static final float X_COL_QTD      = getF(CFG, "tabelas.cols.qtd", 380f);
    private static final float X_COL_CUSTO    = getF(CFG, "tabelas.cols.custo", 420f);
    private static final float X_COL_PRECO    = getF(CFG, "tabelas.cols.preco", 470f);
    private static final float X_COL_DESC_LIM = getF(CFG, "tabelas.cols.descLim", 530f);
    private static final float X_COL_PRECO_KG = getF(CFG, "tabelas.cols.precoKg", 585f);

    private static final float Y_MAT_FIRSTLINE = getF(CFG, "tabelas.materiais.yFirst", 598f);
    private static final float Y_SRV_FIRSTLINE = getF(CFG, "tabelas.servicos.yFirst", 328f);
    private static final float Y_ROW_STEP      = getF(CFG, "tabelas.materiais.rowStep", 16f);
    private static final float Y_MIN_MAT       = getF(CFG, "tabelas.materiais.yMin", 380f);
    private static final float Y_MIN_SRV       = getF(CFG, "tabelas.servicos.yMin", 190f);

    // Totais (página 1)
    private static final float X_TOT_LABEL = getF(CFG, "totais.xLabel", 420f);
    private static final float X_TOT_VAL   = getF(CFG, "totais.xVal", 560f);
    private static final float Y_TOT_TOP   = getF(CFG, "totais.yTop", 200f);
    private static final float Y_TOT_STEP  = getF(CFG, "totais.step", 16f);

    // Grade de calibração
    private static final boolean DEBUG_GRID   = "1".equals(System.getProperty("pdf.grid"));
    private static final float   GRID_STEP    = getSysF("pdf.gridStep", 10f);   // subgraduação fina
    private static final float   GRID_MAJOR   = getSysF("pdf.gridMajor", 100f); // linhas grossas/numeradas

    public byte[] renderFromTemplate(BudgetDto dto) throws Exception {
        // 1) Carrega template
        byte[] templateBytes;
        try (InputStream is = new ClassPathResource("templates/STONGEL - PDF.pdf").getInputStream()) {
            templateBytes = is.readAllBytes();
        }

        try (PDDocument doc = Loader.loadPDF(templateBytes)) {

            // ===== Página 1: Cabeçalho/Obra/Totais =====
            PDPage pageHeader = doc.getPage(PAGE_IDX_HEADER_TOTAIS);
            try (PDPageContentStream cs = new PDPageContentStream(doc, pageHeader, AppendMode.APPEND, true, true)) {
                normalizeToCropBox(cs, pageHeader);
                if (DEBUG_GRID) {
                    drawGrid(cs, pageHeader, GRID_STEP, GRID_MAJOR);
                    drawProbes(cs, CFG.path("probes").path("page1"));
                }

                // Empresa
                var emp = dto.getEmpresa();
                BR.drawText(cs, FONT_B,   FONT_H_BOLD, X_EMPRESA, Y_EMP_RAZAO,  emp != null ? emp.getRazaoSocial() : "-");
                BR.drawText(cs, FONT_REG, FONT_H,      X_EMPRESA, Y_EMP_CNPJ,   emp != null ? emp.getCnpj()        : "-");
                BR.drawText(cs, FONT_REG, FONT_H,      X_EMPRESA, Y_EMP_TEL,    emp != null ? emp.getTelefone()    : "-");
                BR.drawText(cs, FONT_REG, FONT_H,      X_EMPRESA, Y_EMP_EMAIL,  emp != null ? emp.getEmail()       : "-");

                // Obra
                BR.drawText(cs, FONT_B,   FONT_H, X_OBRA_LABEL, Y_OBRA, "Obra:");
                drawParagraph(cs, FONT_REG, FONT_H, X_OBRA_VAL, Y_OBRA, OBRA_MAX_W, safe(dto.getObra()), 12f);

                // Totais
                drawTotals(cs, dto.getTotais());
            }

            // ===== Página 7: Materiais/Serviços =====
            PDPage pageTab = doc.getPage(PAGE_IDX_TABELAS);
            try (PDPageContentStream cs = new PDPageContentStream(doc, pageTab, AppendMode.APPEND, true, true)) {
                normalizeToCropBox(cs, pageTab);
                if (DEBUG_GRID) {
                    drawGrid(cs, pageTab, GRID_STEP, GRID_MAJOR);
                    drawProbes(cs, CFG.path("probes").path("pageTables"));
                }

                // Materiais
                if (hasItems(dto.getMateriais())) {
                    drawItems(cs, Y_MAT_FIRSTLINE + Y_ROW_STEP, dto.getMateriais(), true);
                }
                // Serviços
                if (hasItems(dto.getServicos())) {
                    drawItems(cs, Y_SRV_FIRSTLINE + Y_ROW_STEP, dto.getServicos(), false);
                }
            }

            // 3) Exporta
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                doc.save(baos);
                return baos.toByteArray();
            }
        }
    }

    // ----------------- Helpers -----------------

    private static JsonNode loadConfig() {
        try (InputStream is = new ClassPathResource("templates/stongel-coords.json").getInputStream()) {
            return new ObjectMapper().readTree(is);
        } catch (Exception e) {
            // fallback mínima
            ObjectMapper om = new ObjectMapper();
            try {
                return om.readTree("{\"pageIndexes\":{\"headerTotais\":0,\"tables\":6}}");
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

    private static float getSysF(String prop, float def) {
        String v = System.getProperty(prop);
        if (v == null || v.isBlank()) return def;
        try { return Float.parseFloat(v); } catch (Exception e) { return def; }
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

    /** Normaliza origem (0,0) para a CropBox. */
    private static void normalizeToCropBox(PDPageContentStream cs, PDPage page) throws IOException {
        PDRectangle crop = page.getCropBox();
        if (crop != null) {
            cs.transform(Matrix.getTranslateInstance(-crop.getLowerLeftX(), -crop.getLowerLeftY()));
        }
        // Rotação (habilite se necessário):
        // int rot = page.getRotation();
        // if (rot == 90)  cs.transform(Matrix.getRotateInstance(Math.toRadians(90), crop.getWidth(), 0));
        // if (rot == 180) cs.transform(Matrix.getRotateInstance(Math.toRadians(180), crop.getWidth(), crop.getHeight()));
        // if (rot == 270) cs.transform(Matrix.getRotateInstance(Math.toRadians(270), 0, crop.getHeight()));
    }

    /** Desenha linhas de itens (somente DADOS). */
    private static float drawItems(PDPageContentStream cs, float yStart, List<ItemDto> itens, boolean materiais) throws IOException {
        if (!hasItems(itens)) return yStart;

        float y = yStart - Y_ROW_STEP;
        for (ItemDto it : itens) {
            BR.drawText(cs, FONT_REG, FONT_H, X_COL_DESC,     y, safe(it.getDescricao()));
            BR.drawText(cs, FONT_REG, FONT_H, X_COL_COMP,     y, safe(it.getComp()));
            BR.drawText(cs, FONT_REG, FONT_H, X_COL_UN,       y, safe(it.getUnidade()));
            BR.drawText(cs, FONT_REG, FONT_H, X_COL_QTD,      y, BR.numero(it.getQuantidade()));
            BR.drawText(cs, FONT_REG, FONT_H, X_COL_CUSTO,    y, BR.moeda(it.getCusto()));
            BR.drawText(cs, FONT_REG, FONT_H, X_COL_PRECO,    y, BR.moeda(it.getPrecoVenda()));

            String limDesc = (it.getLimiteDesconto() != null) ? it.getLimiteDesconto().toString() + "%" : "-";
            BR.drawText(cs, FONT_REG, FONT_H, X_COL_DESC_LIM, y, limDesc);

            BR.drawText(cs, FONT_REG, FONT_H, X_COL_PRECO_KG, y, BR.moeda(it.getPrecoKg()));

            y -= Y_ROW_STEP;

            // Limites pra não invadir outros blocos
            if (materiais && y < Y_MIN_MAT) break;
            if (!materiais && y < Y_MIN_SRV) break;
        }
        return y;
    }

    /** Totais (página headerTotais). */
    private static void drawTotals(PDPageContentStream cs, TotaisDto t) throws IOException {
        if (t == null) return;
        float y = Y_TOT_TOP;

        drawTotalLine(cs, "Subtotal",        t.getSubtotal(),       y); y -= Y_TOT_STEP;
        drawTotalLine(cs, "Desconto",        t.getDesconto(),       y); y -= Y_TOT_STEP;
        drawTotalLine(cs, "Total Materiais", t.getTotalMateriais(), y); y -= Y_TOT_STEP;
        drawTotalLine(cs, "Total Serviços",  t.getTotalServicos(),  y); y -= Y_TOT_STEP;
        drawTotalBold(cs, "TOTAL GERAL",     t.getTotalGeral(),     y);
    }

    private static void drawTotalLine(PDPageContentStream cs, String label, Number val, float y) throws IOException {
        BR.drawText(cs, FONT_REG, FONT_H, X_TOT_LABEL, y, label);
        BR.drawText(cs, FONT_B,   FONT_H, X_TOT_VAL,   y, BR.moeda(val));
    }

    private static void drawTotalBold(PDPageContentStream cs, String label, Number val, float y) throws IOException {
        BR.drawText(cs, FONT_B,   FONT_H, X_TOT_LABEL, y, label);
        BR.drawText(cs, FONT_B,   FONT_H, X_TOT_VAL,   y, BR.moeda(val));
    }

    /** Parágrafo com quebra automática respeitando largura máxima. */
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

    private static boolean hasItems(List<?> list) {
        return list != null && !list.isEmpty();
    }

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    // ======= DEBUG / Calibração =======

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

        // Eixos enfatizados
        cs.setStrokingColor(Color.DARK_GRAY);
        cs.setLineWidth(0.6f);
        cs.moveTo(0, 0); cs.lineTo(w, 0); cs.stroke(); // eixo X
        cs.moveTo(0, 0); cs.lineTo(0, h); cs.stroke(); // eixo Y
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
}
