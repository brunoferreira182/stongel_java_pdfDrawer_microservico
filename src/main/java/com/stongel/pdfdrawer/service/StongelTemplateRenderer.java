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
 * Preenche:
 *  - Página de cabeçalho (header/totais), normalmente página 1 (index 0)
 *  - Página de tabelas (materiais/serviços), normalmente página 7 (index 6)
 *
 * Lê coordenadas de "src/main/resources/templates/stongel-coords.json" a cada chamada.
 * O template base é "src/main/resources/templates/STONGEL - PDF.pdf".
 *
 * Recursos:
 *  - Apenas DADOS (cabeçalhos/títulos já estão no template)
 *  - Origem normalizada para CropBox
 *  - Grade de calibração: -Dpdf.grid=1  (opções: -Dpdf.gridStep=10, -Dpdf.gridMajor=100)
 *  - Probes (alfinetes): definidos no JSON para marcar (x,y) exatos
 */
@Component
public class StongelTemplateRenderer {

    // Tipografia
    private static final PDFont FONT_REG = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont FONT_B   = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final float FONT_H      = 10f;
    private static final float FONT_H_BOLD = 12f;

    // Flags/props de grade (podem ser lidas uma vez; valores padrão caso não setado)
    private static final boolean DEBUG_GRID = "1".equals(System.getProperty("pdf.grid"));
    private static final float   GRID_STEP  = getSysF("pdf.gridStep", 10f);
    private static final float   GRID_MAJOR = getSysF("pdf.gridMajor", 100f);

    public byte[] renderFromTemplate(BudgetDto dto) throws Exception {

        // 1) Carrega o template
        byte[] templateBytes;
        try (InputStream is = new ClassPathResource("templates/STONGEL - PDF.pdf").getInputStream()) {
            templateBytes = is.readAllBytes();
        }

        // 2) Carrega coordenadas do JSON (reload a cada chamada)
        JsonNode cfg = loadDynamicConfig();

        // Páginas alvo (0-based)
        int PAGE_IDX_HEADER_TOTAIS = getInt(cfg, "pageIndexes.headerTotais", 0);
        int PAGE_IDX_TABELAS       = getInt(cfg, "pageIndexes.tables", 6);

        // Empresa / Obra (página header)
        float X_EMPRESA   = getF(cfg, "empresa.x", 60f);
        float Y_EMP_RAZAO = getF(cfg, "empresa.yRazao", 740f);
        float Y_EMP_CNPJ  = getF(cfg, "empresa.yCnpj", 725f);
        float Y_EMP_TEL   = getF(cfg, "empresa.yTel", 710f);
        float Y_EMP_EMAIL = getF(cfg, "empresa.yEmail", 695f);

        float X_OBRA_LABEL = getF(cfg, "obra.xLabel", 60f);
        float X_OBRA_VAL   = getF(cfg, "obra.xVal", 100f);
        float Y_OBRA       = getF(cfg, "obra.y", 665f);
        float OBRA_MAX_W   = getF(cfg, "obra.maxW", 460f);

        // Tabela (página de tabelas) — apenas DADOS
        float X_COL_DESC     = getF(cfg, "tabelas.cols.desc", 60f);
        float X_COL_COMP     = getF(cfg, "tabelas.cols.comp", 300f);
        float X_COL_UN       = getF(cfg, "tabelas.cols.un", 340f);
        float X_COL_QTD      = getF(cfg, "tabelas.cols.qtd", 380f);
        float X_COL_CUSTO    = getF(cfg, "tabelas.cols.custo", 420f);
        float X_COL_PRECO    = getF(cfg, "tabelas.cols.preco", 470f);
        float X_COL_DESC_LIM = getF(cfg, "tabelas.cols.descLim", 530f);
        float X_COL_PRECO_KG = getF(cfg, "tabelas.cols.precoKg", 585f);

        float Y_MAT_FIRSTLINE = getF(cfg, "tabelas.materiais.yFirst", 598f);
        float Y_SRV_FIRSTLINE = getF(cfg, "tabelas.servicos.yFirst", 328f);
        float Y_ROW_STEP      = getF(cfg, "tabelas.materiais.rowStep", 16f);
        float Y_MIN_MAT       = getF(cfg, "tabelas.materiais.yMin", 380f);
        float Y_MIN_SRV       = getF(cfg, "tabelas.servicos.yMin", 190f);

        // Totais (página header)
        float X_TOT_LABEL = getF(cfg, "totais.xLabel", 420f);
        float X_TOT_VAL   = getF(cfg, "totais.xVal", 560f);
        float Y_TOT_TOP   = getF(cfg, "totais.yTop", 200f);
        float Y_TOT_STEP  = getF(cfg, "totais.step", 16f);

        try (PDDocument doc = Loader.loadPDF(templateBytes)) {

            // ===== Página de Cabeçalho/Totais =====
            PDPage pageHeader = doc.getPage(PAGE_IDX_HEADER_TOTAIS);
            try (PDPageContentStream cs = new PDPageContentStream(doc, pageHeader, AppendMode.APPEND, true, true)) {
                normalizeToCropBox(cs, pageHeader);

                if (DEBUG_GRID) {
                    drawGrid(cs, pageHeader, GRID_STEP, GRID_MAJOR);
                    drawProbes(cs, cfg.path("probes").path("page1"));
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
                drawTotals(cs, dto.getTotais(), X_TOT_LABEL, X_TOT_VAL, Y_TOT_TOP, Y_TOT_STEP);
            }

            // ===== Página de Tabelas (Materiais/Serviços) =====
            PDPage pageTab = doc.getPage(PAGE_IDX_TABELAS);
            try (PDPageContentStream cs = new PDPageContentStream(doc, pageTab, AppendMode.APPEND, true, true)) {
                normalizeToCropBox(cs, pageTab);

                if (DEBUG_GRID) {
                    drawGrid(cs, pageTab, GRID_STEP, GRID_MAJOR);
                    drawProbes(cs, cfg.path("probes").path("pageTables"));
                }

                // Materiais (todas as linhas, se houver)
                if (hasItems(dto.getMateriais())) {
                    drawItems(cs, Y_MAT_FIRSTLINE + Y_ROW_STEP, dto.getMateriais(), true,
                            X_COL_DESC, X_COL_COMP, X_COL_UN, X_COL_QTD, X_COL_CUSTO, X_COL_PRECO, X_COL_DESC_LIM, X_COL_PRECO_KG,
                            Y_ROW_STEP, Y_MIN_MAT, Y_MIN_SRV);
                }
                // Serviços (todas as linhas, se houver)
                if (hasItems(dto.getServicos())) {
                    drawItems(cs, Y_SRV_FIRSTLINE + Y_ROW_STEP, dto.getServicos(), false,
                            X_COL_DESC, X_COL_COMP, X_COL_UN, X_COL_QTD, X_COL_CUSTO, X_COL_PRECO, X_COL_DESC_LIM, X_COL_PRECO_KG,
                            Y_ROW_STEP, Y_MIN_MAT, Y_MIN_SRV);
                }
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
        // Rotação (habilite se necessário):
        // int rot = page.getRotation();
        // if (rot == 90)  cs.transform(Matrix.getRotateInstance(Math.toRadians(90), crop.getWidth(), 0));
        // if (rot == 180) cs.transform(Matrix.getRotateInstance(Math.toRadians(180), crop.getWidth(), crop.getHeight()));
        // if (rot == 270) cs.transform(Matrix.getRotateInstance(Math.toRadians(270), 0, crop.getHeight()));
    }

    /** Desenha linhas de itens (somente DADOS). */
    private static float drawItems(
            PDPageContentStream cs, float yStart, List<ItemDto> itens, boolean materiais,
            float X_COL_DESC, float X_COL_COMP, float X_COL_UN, float X_COL_QTD, float X_COL_CUSTO,
            float X_COL_PRECO, float X_COL_DESC_LIM, float X_COL_PRECO_KG,
            float Y_ROW_STEP, float Y_MIN_MAT, float Y_MIN_SRV
    ) throws IOException {
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

            // Limites para não invadir outros blocos do template
            if (materiais && y < Y_MIN_MAT) break;
            if (!materiais && y < Y_MIN_SRV) break;
        }
        return y;
    }

    /** Totais na página de cabeçalho. */
    private static void drawTotals(PDPageContentStream cs, TotaisDto t,
                                   float X_TOT_LABEL, float X_TOT_VAL,
                                   float Y_TOT_TOP, float Y_TOT_STEP) throws IOException {
        if (t == null) return;
        float y = Y_TOT_TOP;

        drawTotalLine(cs, "Subtotal",        t.getSubtotal(),       y, X_TOT_LABEL, X_TOT_VAL); y -= Y_TOT_STEP;
        drawTotalLine(cs, "Desconto",        t.getDesconto(),       y, X_TOT_LABEL, X_TOT_VAL); y -= Y_TOT_STEP;
        drawTotalLine(cs, "Total Materiais", t.getTotalMateriais(), y, X_TOT_LABEL, X_TOT_VAL); y -= Y_TOT_STEP;
        drawTotalLine(cs, "Total Serviços",  t.getTotalServicos(),  y, X_TOT_LABEL, X_TOT_VAL); y -= Y_TOT_STEP;
        drawTotalBold(cs, "TOTAL GERAL",     t.getTotalGeral(),     y, X_TOT_LABEL, X_TOT_VAL);
    }

    private static void drawTotalLine(PDPageContentStream cs, String label, Number val, float y,
                                      float X_TOT_LABEL, float X_TOT_VAL) throws IOException {
        BR.drawText(cs, FONT_REG, FONT_H, X_TOT_LABEL, y, label);
        BR.drawText(cs, FONT_B,   FONT_H, X_TOT_VAL,   y, BR.moeda(val));
    }

    private static void drawTotalBold(PDPageContentStream cs, String label, Number val, float y,
                                      float X_TOT_LABEL, float X_TOT_VAL) throws IOException {
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

    // ----------------- Util -----------------

    /** Lê o stongel-coords.json do classpath a cada chamada. */
    private static JsonNode loadDynamicConfig() {
        try (InputStream is = new ClassPathResource("templates/stongel-coords.json").getInputStream()) {
            return new ObjectMapper().readTree(is);
        } catch (Exception e) {
            // fallback mínima
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
}
