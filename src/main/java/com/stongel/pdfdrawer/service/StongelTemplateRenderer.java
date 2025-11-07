package com.stongel.pdfdrawer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stongel.pdfdrawer.dto.*;
import com.stongel.pdfdrawer.util.BR;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.util.Matrix;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Component
public class StongelTemplateRenderer {

    // ================== FONTES INTER (EMBUTIDAS) ==================
    private static final byte[] FONT_REG_BYTES = loadFontBytes("fonts/Inter_24pt-Regular.ttf");
    private static final byte[] FONT_BOLD_BYTES = loadFontBytes("fonts/Inter_24pt-Bold.ttf");
    private static final byte[] FONT_ITALIC_BYTES = FONT_REG_BYTES;
    private static final byte[] FONT_MONO_BYTES = FONT_REG_BYTES;
    private static final float FONT_H = 10f;

    private final ThreadLocal<Fonts> fontContext = new ThreadLocal<>();

    private final ObjectMapper om = new ObjectMapper();

    public byte[] renderFromOrcamentoJson(String json) throws Exception {
        JsonNode root = om.readTree(json);
        BudgetDto dto = mapRealJsonToBudgetDto(root);
        JsonNode cfg = loadDynamicConfig();
        return renderCore(dto, root, cfg);
    }

    public byte[] renderFromTemplate(BudgetDto dto) throws Exception {
        JsonNode cfg = loadDynamicConfig();
        return renderCore(dto, null, cfg);
    }

    private byte[] renderCore(BudgetDto dto, JsonNode rawJson, JsonNode cfg) throws Exception {
        byte[] templateBytes;
        try (InputStream is = new ClassPathResource("templates/STONGEL - PDF.pdf").getInputStream()) {
            templateBytes = is.readAllBytes();
        }

        int PAGE_IDX_HEADER_TOTAIS = getInt(cfg, "pageIndexes.headerTotais", 0);
        int PAGE_IDX_TABELAS = 5;

        // Coordenadas empresa
        float X_EMP_RAZAO = getF(cfg, "empresa.razao.x", -10000f);
        float Y_EMP_RAZAO = getF(cfg, "empresa.razao.y", -10000f);
        float X_EMP_CNPJ = getF(cfg, "empresa.cnpj.x", -10000f);
        float Y_EMP_CNPJ = getF(cfg, "empresa.cnpj.y", -10000f);
        float X_EMP_CONT = getF(cfg, "empresa.contato.x", -10000f);
        float Y_EMP_CONT = getF(cfg, "empresa.contato.y", -10000f);
        float X_EMP_TEL = getF(cfg, "empresa.tel.x", -10000f);
        float Y_EMP_TEL = getF(cfg, "empresa.tel.y", -10000f);
        float X_EMP_EMAIL = getF(cfg, "empresa.email.x", -10000f);
        float Y_EMP_EMAIL = getF(cfg, "empresa.email.y", -10000f);

        float L_RAZAO_X = getF(cfg, "labels.page1.razao.x", -10000f);
        float L_RAZAO_Y = getF(cfg, "labels.page1.razao.y", -10000f);
        float L_CNPJ_X = getF(cfg, "labels.page1.cnpj.x", -10000f);
        float L_CNPJ_Y = getF(cfg, "labels.page1.cnpj.y", -10000f);
        float L_CONT_X = getF(cfg, "labels.page1.contato.x", -10000f);
        float L_CONT_Y = getF(cfg, "labels.page1.contato.y", -10000f);
        float L_TEL_X = getF(cfg, "labels.page1.telefone.x", -10000f);
        float L_TEL_Y = getF(cfg, "labels.page1.telefone.y", -10000f);
        float L_MAIL_X = getF(cfg, "labels.page1.email.x", -10000f);
        float L_MAIL_Y = getF(cfg, "labels.page1.email.y", -10000f);

        // Layout antigo
        float X_COL_DESC_OLD = getF(cfg, "tabelas.cols.desc", 60f);
        float X_COL_COMP_OLD = getF(cfg, "tabelas.cols.comp", 300f);
        float X_COL_UN_OLD = getF(cfg, "tabelas.cols.un", 340f);
        float X_COL_QTD_OLD = getF(cfg, "tabelas.cols.qtd", 380f);
        float X_COL_CUSTO_OLD = getF(cfg, "tabelas.cols.custo", 420f);
        float X_COL_PRECO_OLD = getF(cfg, "tabelas.cols.preco", 470f);
        float X_COL_DESC_LIM_OLD = getF(cfg, "tabelas.cols.descLim", 530f);
        float X_COL_PRECO_KG_OLD = getF(cfg, "tabelas.cols.precoKg", 585f);
        float Y_MAT_FIRSTLINE_OLD = getF(cfg, "tabelas.materiais.yFirst", 598f);
        float Y_SRV_FIRSTLINE_OLD = getF(cfg, "tabelas.servicos.yFirst", 328f);
        float Y_ROW_STEP_OLD = getF(cfg, "tabelas.materiais.rowStep", 16f);
        float Y_MIN_MAT_OLD = getF(cfg, "tabelas.materiais.yMin", 380f);
        float Y_MIN_SRV_OLD = getF(cfg, "tabelas.servicos.yMin", 190f);

        // Layout novo
        JsonNode L_MAT = at(cfg, "layout.materiais");
        float MAT_QTD_RIGHT = getF(L_MAT, "xQtdRight", 65f);
        float MAT_UN_X = getF(L_MAT, "xUn", 105f);
        float MAT_DESC_X = getF(L_MAT, "xDesc", 200f);
        float MAT_DESC_MAXW = getF(L_MAT, "descMaxWidth", 360f);
        float MAT_SUBT_RIGHT = getF(L_MAT, "xSubtotalRight", 510f);
        float MAT_Y_FIRST = getF(L_MAT, "yFirst", 555f);
        float MAT_Y_MIN = getF(L_MAT, "yMin", 380f);
        float MAT_ROW_STEP = getF(L_MAT, "rowStep", 16f);
        float MAT_DESC_DX = getF(L_MAT, "descDx", -30f);
        float MAT_DESC_DY = getF(L_MAT, "descDy", 0f);

        JsonNode L_SRV = at(cfg, "layout.servicos");
        float SRV_QTD_RIGHT = getF(L_SRV, "xQtdRight", 65f);
        float SRV_UN_X = getF(L_SRV, "xUn", 105f);
        float SRV_DESC_X = getF(L_SRV, "xDesc", 200f);
        float SRV_DESC_MAXW = getF(L_SRV, "descMaxWidth", 340f);
        float SRV_CUSTO_U_RIGHT = getF(L_SRV, "xCustoUnitRight", 470f);
        float SRV_SUBT_RIGHT = getF(L_SRV, "xSubtotalRight", 565f);
        float SRV_Y_FIRST = getF(L_SRV, "yFirst", 380f);
        float SRV_Y_MIN = getF(L_SRV, "yMin", 190f);
        float SRV_ROW_STEP = getF(L_SRV, "rowStep", 16f);
        float SRV_DESC_DX = getF(L_SRV, "descDx", -30f);
        float SRV_DESC_DY = getF(L_SRV, "descDy", 0f);

        // Totais
        float X_TOT_TAB_LABEL = getF(cfg, "totaisTables.xLabel", 420f);
        float X_TOT_TAB_VAL = getF(cfg, "totaisTables.xVal", 560f);
        float Y_TOT_TAB_TOP = getF(cfg, "totaisTables.yTop", 200f);
        float Y_TOT_TAB_STEP = getF(cfg, "totaisTables.step", 16f);
        float VAL_DX = getF(cfg, "totaisTables.valDx", 0f);
        float VAL_DY = getF(cfg, "totaisTables.valDy", 0f);
        JsonNode AJUSTES_IND = at(cfg, "totaisTables.ajustesIndividuais");

        boolean debugGrid = "1".equals(System.getProperty("pdf.grid")) || cfg.path("debug").path("grid").asBoolean(false);
        float gridStep = getSysF("pdf.gridStep", (float) cfg.path("debug").path("gridStep").asDouble(10.0));
        float gridMajor = getSysF("pdf.gridMajor", (float) cfg.path("debug").path("gridMajor").asDouble(100.0));

        try (PDDocument doc = Loader.loadPDF(templateBytes)) {
            fontContext.set(loadFonts(doc));
            try {
                // Página 1 - Cabeçalho
                PDPage pageHeader = doc.getPage(PAGE_IDX_HEADER_TOTAIS);
                try (PDPageContentStream cs = new PDPageContentStream(doc, pageHeader, AppendMode.APPEND, true, true)) {
                    normalizeToCropBox(cs, pageHeader);
                    if (debugGrid) {
                        drawGrid(cs, pageHeader, gridStep, gridMajor);
                        drawProbes(cs, cfg.path("probes").path("page1"));
                    }
                    EmpresaDto emp = dto.getEmpresa();
                    BR.drawText(cs, fontReg(), FONT_H, L_RAZAO_X, L_RAZAO_Y, "Razão Social:");
                    BR.drawText(cs, fontReg(), FONT_H, L_CNPJ_X, L_CNPJ_Y, "CNPJ:");
                    BR.drawText(cs, fontReg(), FONT_H, L_CONT_X, L_CONT_Y, "Contato:");
                    BR.drawText(cs, fontReg(), FONT_H, L_TEL_X, L_TEL_Y, "Telefone:");
                    BR.drawText(cs, fontReg(), FONT_H, L_MAIL_X, L_MAIL_Y, "E-mail:");
                    BR.drawText(cs, fontReg(), FONT_H, X_EMP_RAZAO, Y_EMP_RAZAO, emp != null ? emp.getRazaoSocial() : "-");
                    BR.drawText(cs, fontReg(), FONT_H, X_EMP_CNPJ, Y_EMP_CNPJ, emp != null ? emp.getCnpj() : "-");
                    BR.drawText(cs, fontReg(), FONT_H, X_EMP_CONT, Y_EMP_CONT, emp != null ? emp.getContato() : "-");
                    BR.drawText(cs, fontReg(), FONT_H, X_EMP_TEL, Y_EMP_TEL, emp != null ? emp.getTelefone() : "-");
                    BR.drawText(cs, fontReg(), FONT_H, X_EMP_EMAIL, Y_EMP_EMAIL, emp != null ? emp.getEmail() : "-");
                }

                // Página de tabelas (forçada página 6)
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
                            drawMaterialsRowsV2(cs, dto.getMateriais(), MAT_Y_FIRST, MAT_ROW_STEP, MAT_Y_MIN,
                                    MAT_QTD_RIGHT, MAT_UN_X, MAT_DESC_X + MAT_DESC_DX, MAT_DESC_MAXW, MAT_SUBT_RIGHT, MAT_DESC_DY);
                        }
                        if (hasItems(dto.getServicos())) {
                            drawServicesRowsV2(cs, dto.getServicos(), SRV_Y_FIRST, SRV_ROW_STEP, SRV_Y_MIN,
                                    SRV_QTD_RIGHT, SRV_UN_X, SRV_DESC_X + SRV_DESC_DX, SRV_DESC_MAXW,
                                    SRV_CUSTO_U_RIGHT, SRV_SUBT_RIGHT, SRV_DESC_DY);
                        }
                    } else {
                        if (hasItems(dto.getMateriais())) {
                            drawTableRowsOld(cs, dto.getMateriais(), Y_MAT_FIRSTLINE_OLD, Y_ROW_STEP_OLD, Y_MIN_MAT_OLD,
                                    X_COL_DESC_OLD, X_COL_COMP_OLD, X_COL_UN_OLD, X_COL_QTD_OLD,
                                    X_COL_CUSTO_OLD, X_COL_PRECO_OLD, X_COL_DESC_LIM_OLD, X_COL_PRECO_KG_OLD);
                        }
                        if (hasItems(dto.getServicos())) {
                            drawTableRowsOld(cs, dto.getServicos(), Y_SRV_FIRSTLINE_OLD, Y_ROW_STEP_OLD, Y_MIN_SRV_OLD,
                                    X_COL_DESC_OLD, X_COL_COMP_OLD, X_COL_UN_OLD, X_COL_QTD_OLD,
                                    X_COL_CUSTO_OLD, X_COL_PRECO_OLD, X_COL_DESC_LIM_OLD, X_COL_PRECO_KG_OLD);
                        }
                    }

                    drawTotalsValuesOnlyAdjusted(cs, dto.getTotais(), X_TOT_TAB_LABEL, X_TOT_TAB_VAL,
                            Y_TOT_TAB_TOP, Y_TOT_TAB_STEP, VAL_DX, VAL_DY, AJUSTES_IND, at(cfg, "totaisTables"));
                }

                if (rawJson != null) {
                    renderSections(doc, rawJson, cfg, PAGE_IDX_HEADER_TOTAIS, PAGE_IDX_TABELAS, debugGrid, gridStep, gridMajor);
                }

                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    doc.save(baos);
                    return baos.toByteArray();
                }
            } finally {
                fontContext.remove();
            }
        }
    }

    // ====================== MÉTODOS AUXILIARES ======================
    private static void normalizeToCropBox(PDPageContentStream cs, PDPage page) throws IOException {
        PDRectangle crop = page.getCropBox();
        if (crop != null) {
            cs.transform(Matrix.getTranslateInstance(-crop.getLowerLeftX(), -crop.getLowerLeftY()));
        }
    }

    private void drawMaterialsRowsV2(PDPageContentStream cs, List<ItemDto> itens,
                                             float yStart, float rowStep, float yMin,
                                             float xQtdRight, float xUn, float xDesc, float descMaxWidth,
                                             float xSubtotalRight, float descDy) throws IOException {
        if (itens == null || itens.isEmpty()) return;
        float y = yStart;
        for (ItemDto it : itens) {
            if (y < yMin) break;
            drawRightAligned(cs, fontReg(), FONT_H, xQtdRight, y, BR.numero(it.getQuantidade()));
            BR.drawText(cs, fontReg(), FONT_H, xUn, y, safe(it.getUnidade()));
            drawWrapped(cs, fontReg(), FONT_H, xDesc, y + descDy, descMaxWidth, safe(it.getDescricao()), 10f);
            double subtotal = (it.getPrecoVenda() != null && it.getPrecoVenda().doubleValue() > 0)
                    ? it.getPrecoVenda().doubleValue()
                    : ((it.getCusto() != null && it.getQuantidade() != null)
                    ? it.getCusto().doubleValue() * it.getQuantidade().doubleValue()
                    : 0.0);
            drawRightAligned(cs, fontReg(), FONT_H, xSubtotalRight, y, BR.moeda(subtotal));
            y -= rowStep;
        }
    }

    private void drawServicesRowsV2(PDPageContentStream cs, List<ItemDto> itens,
                                            float yStart, float rowStep, float yMin,
                                            float xQtdRight, float xUn, float xDesc, float descMaxWidth,
                                            float xCustoUnitRight, float xSubtotalRight, float descDy) throws IOException {
        if (itens == null || itens.isEmpty()) return;
        float y = yStart;
        for (ItemDto it : itens) {
            if (y < yMin) break;
            drawRightAligned(cs, fontReg(), FONT_H, xQtdRight, y, BR.numero(it.getQuantidade()));
            BR.drawText(cs, fontReg(), FONT_H, xUn, y, safe(it.getUnidade()));
            drawWrapped(cs, fontReg(), FONT_H, xDesc, y + descDy, descMaxWidth, safe(it.getDescricao()), 10f);
            double custoU = 0.0;
            if (it.getPrecoVenda() != null && it.getQuantidade() != null && it.getQuantidade().doubleValue() > 0) {
                custoU = it.getPrecoVenda().doubleValue() / it.getQuantidade().doubleValue();
            } else if (it.getCusto() != null && it.getQuantidade() != null && it.getQuantidade().doubleValue() > 0) {
                custoU = it.getCusto().doubleValue() / it.getQuantidade().doubleValue();
            }
            drawRightAligned(cs, fontReg(), FONT_H, xCustoUnitRight, y, BR.moeda(custoU));
            double subtotal = (it.getPrecoVenda() != null && it.getPrecoVenda().doubleValue() > 0)
                    ? it.getPrecoVenda().doubleValue()
                    : ((it.getCusto() != null && it.getQuantidade() != null)
                    ? it.getCusto().doubleValue() * it.getQuantidade().doubleValue()
                    : 0.0);
            drawRightAligned(cs, fontReg(), FONT_H, xSubtotalRight, y, BR.moeda(subtotal));
            y -= rowStep;
        }
    }

    private void drawTableRowsOld(PDPageContentStream cs, List<ItemDto> itens,
                                          float yStart, float rowStep, float yMin,
                                          float xDesc, float xComp, float xUn, float xQtd,
                                          float xCusto, float xPreco, float xDescLim, float xPrecoKg) throws IOException {
        if (itens == null || itens.isEmpty()) return;
        float y = yStart;
        for (ItemDto it : itens) {
            if (y < yMin) break;
            drawWrapped(cs, fontReg(), FONT_H, xDesc, y, 230f, safe(it.getDescricao()), 10f);
            BR.drawText(cs, fontReg(), FONT_H, xComp, y, safe(it.getComp()));
            BR.drawText(cs, fontReg(), FONT_H, xUn, y, safe(it.getUnidade()));
            drawRightAligned(cs, fontReg(), FONT_H, xQtd, y, BR.numero(it.getQuantidade()));
            drawRightAligned(cs, fontReg(), FONT_H, xCusto, y, BR.moeda(it.getCusto()));
            drawRightAligned(cs, fontReg(), FONT_H, xPreco, y, BR.moeda(it.getPrecoVenda()));
            String descPerc = (it.getLimiteDesconto() != null) ? it.getLimiteDesconto().toString() + "%" : "-";
            drawRightAligned(cs, fontReg(), FONT_H, xDescLim, y, descPerc);
            drawRightAligned(cs, fontReg(), FONT_H, xPrecoKg, y, BR.moeda(it.getPrecoKg()));
            y -= rowStep;
        }
    }

    private static void drawRightAligned(PDPageContentStream cs, PDFont font, float fontSize,
                                         float xRight, float y, String text) throws IOException {
        if (text == null) text = "-";
        text = sanitizeText(text);
        float w = font.getStringWidth(text) / 1000f * fontSize;
        BR.drawText(cs, font, fontSize, xRight - w, y, text);
    }

    private static void drawWrapped(PDPageContentStream cs, PDFont font, float fontSize,
                                    float x, float y, float maxWidth,
                                    String text, float lineStep) throws IOException {
        if (text == null || text.isBlank()) return;
        text = sanitizeText(text);
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

    private void renderSections(PDDocument doc, JsonNode raw, JsonNode cfg,
                                int idxHeader, int idxTables, boolean debugGrid,
                                float gridStep, float gridMajor) throws IOException {
        JsonNode sectionsCfg = at(cfg, "sections");
        if (sectionsCfg == null || !sectionsCfg.fieldNames().hasNext()) return;

        Map<String, JsonNode> bySlug = new HashMap<>();
        JsonNode arr = raw.path("sections");
        if (arr != null && arr.isArray()) {
            for (JsonNode s : arr) {
                String slug = s.path("slug").asText(null);
                if (slug != null) bySlug.put(slug, s);
            }
        }

        Map<Integer, Float> pageCursorY = new HashMap<>();
        final float MARGIN_BOTTOM = 60f;
        final float BETWEEN_PARAGRAPHS = 6f;

        Iterator<String> it = sectionsCfg.fieldNames();
        while (it.hasNext()) {
            String slug = it.next();
            JsonNode cfgSec = sectionsCfg.path(slug);
            int pageIndex = cfgSec.path("page").asInt();
            if (pageIndex == idxHeader || pageIndex == idxTables) continue;
            if (pageIndex < 0 || pageIndex >= doc.getNumberOfPages()) continue;

            float x = (float) cfgSec.path("x").asDouble(60);
            float y = (float) cfgSec.path("y").asDouble(640);
            float maxW = (float) cfgSec.path("maxW").asDouble(480);
            float lineStep = (float) cfgSec.path("lineStep").asDouble(12);
            float fontSize = (float) cfgSec.path("fontSize").asDouble(10);

            float startY = y;
            if (pageCursorY.containsKey(pageIndex)) {
                startY = Math.min(pageCursorY.get(pageIndex) - BETWEEN_PARAGRAPHS, startY);
            }

            PDPage page = doc.getPage(pageIndex);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page, AppendMode.APPEND, true, true)) {
                normalizeToCropBox(cs, page);
                if (debugGrid) drawGrid(cs, page, gridStep, gridMajor);

                JsonNode sec = bySlug.get(slug);
                if (sec == null) {
                    pageCursorY.put(pageIndex, startY);
                    continue;
                }

                String html = sec.path("descricao").asText("");
                float cursorY = startY;


                cursorY = renderHtmlBlock(cs, html, x, cursorY, maxW, fontSize, lineStep, MARGIN_BOTTOM);
                pageCursorY.put(pageIndex, cursorY);
            }
        }
    }

    private static class Span {
        final String text;
        final PDFont font;
        final float fs;
        final boolean underline;
        final float indent;
        Span(String text, PDFont font, float fs, boolean underline, float indent) {
            this.text = text;
            this.font = font;
            this.fs = fs;
            this.underline = underline;
            this.indent = indent;
        }
    }

    private enum BlockType { P, H1, H2, H3, H4, UL, OL, PRE, BR }

    private static class Block {
        final BlockType type;
        final String innerHtml;
        Block(BlockType t, String h) { this.type = t; this.innerHtml = h; }
    }

    private float renderHtmlBlock(PDPageContentStream cs, String rawHtml,
                                  float x, float startY, float maxW,
                                  float baseFs, float lineStep, float marginBottom) throws IOException {
        if (rawHtml == null || rawHtml.isBlank()) return startY;
        String html = rawHtml
                .replaceAll("(?i)<br\\s*/?>", "<br/>")
                .replaceAll("(?i)</p>", "</p>\n")
                .replaceAll("(?i)<p[^>]*>", "<p>")
                .replaceAll("(?i)</h([1-4])>", "</h$1>\n")
                .replace("\u00A0", " ");
        List<Block> blocks = splitTopBlocks(html);
        float y = startY;
        int orderedListCounter = 0;
        for (Block b : blocks) {
            if (y < marginBottom) break;
            switch (b.type) {
                case H1: case H2: case H3: case H4: {
                    float fs = switch (b.type) {
                        case H1 -> baseFs + 6;
                        case H2 -> baseFs + 4;
                        case H3 -> baseFs + 2;
                        default -> baseFs + 1;
                    };
                    List<Span> line = tokenizeInline(b.innerHtml, fontBold(), fs);
                    y = drawWrappedStyledLine(cs, x, y, maxW, line, lineStep + 2);
                    y -= 4f;
                    break;
                }
                case P: {
                    List<List<Span>> lines = paragraphsFromHtml(b.innerHtml, baseFs);
                    for (List<Span> line : lines) {
                        y = drawWrappedStyledLine(cs, x, y, maxW, line, lineStep);
                        if (y < marginBottom) break;
                    }
                    y -= 4f;
                    break;
                }
                case BR: y -= lineStep; break;
                case UL: {
                    List<String> lis = extractLi(b.innerHtml);
                    for (String li : lis) {
                        List<Span> spans = new ArrayList<>();
                        spans.add(new Span("• ", fontBold(), baseFs, false, 0));
                        spans.addAll(tokenizeInline(li, fontReg(), baseFs));
                        y = drawWrappedStyledLine(cs, x + 12f, y, maxW - 12f, spans, lineStep);
                        if (y < marginBottom) break;
                    }
                    y -= 4f;
                    break;
                }
                case OL: {
                    List<String> lis = extractLi(b.innerHtml);
                    orderedListCounter = 0;
                    for (String li : lis) {
                        orderedListCounter++;
                        List<Span> spans = new ArrayList<>();
                        spans.add(new Span(orderedListCounter + ". ", fontBold(), baseFs, false, 0));
                        spans.addAll(tokenizeInline(li, fontReg(), baseFs));
                        y = drawWrappedStyledLine(cs, x + 12f, y, maxW - 12f, spans, lineStep);
                        if (y < marginBottom) break;
                    }
                    y -= 4f;
                    break;
                }
                case PRE: {
                    String mono = stripTags(b.innerHtml).replace("\r", "").replace("\t", " ");
                    String[] lines = mono.split("\n");
                    for (String ln : lines) {
                        String t = sanitizeText(ln);
                        BR.drawText(cs, fontMono(), baseFs, x, y, t);
                        y -= lineStep;
                        if (y < marginBottom) break;
                    }
                    y -= 2f;
                    break;
                }
            }
        }
        return y;
    }

    private List<Block> splitTopBlocks(String html) {
        List<Block> out = new ArrayList<>();
        String rest = html;
        while (!rest.isEmpty()) {
            rest = rest.trim();
            if (rest.isEmpty()) break;
            if (rest.matches("(?is)^<br\\s*/?>.*")) {
                out.add(new Block(BlockType.BR, ""));
                rest = rest.replaceFirst("(?is)^<br\\s*/?>", "");
                continue;
            }
            String[] heads = {"h1","h2","h3","h4"};
            boolean consumed = false;
            for (String h : heads) {
                String open = "(?is)^<" + h + "[^>]*>";
                if (rest.matches(open + ".*")) {
                    int iClose = idxOfClosing(rest, h);
                    if (iClose >= 0) {
                        String inner = rest.substring(rest.indexOf('>')+1, iClose);
                        BlockType t = switch (h) {
                            case "h1" -> BlockType.H1;
                            case "h2" -> BlockType.H2;
                            case "h3" -> BlockType.H3;
                            default -> BlockType.H4;
                        };
                        out.add(new Block(t, inner));
                        rest = rest.substring(iClose + ("</"+h+">").length());
                        consumed = true;
                        break;
                    }
                }
            }
            if (consumed) continue;
            if (rest.matches("(?is)^<ul[^>]*>.*")) {
                int iClose = idxOfClosing(rest, "ul");
                if (iClose >= 0) {
                    out.add(new Block(BlockType.UL, rest.substring(rest.indexOf('>')+1, iClose)));
                    rest = rest.substring(iClose + "</ul>".length());
                    continue;
                }
            }
            if (rest.matches("(?is)^<ol[^>]*>.*")) {
                int iClose = idxOfClosing(rest, "ol");
                if (iClose >= 0) {
                    out.add(new Block(BlockType.OL, rest.substring(rest.indexOf('>')+1, iClose)));
                    rest = rest.substring(iClose + "</ol>".length());
                    continue;
                }
            }
            if (rest.matches("(?is)^<pre[^>]*>.*")) {
                int iClose = idxOfClosing(rest, "pre");
                if (iClose >= 0) {
                    out.add(new Block(BlockType.PRE, rest.substring(rest.indexOf('>')+1, iClose)));
                    rest = rest.substring(iClose + "</pre>".length());
                    continue;
                }
            }
            if (rest.matches("(?is)^<p[^>]*>.*")) {
                int iClose = idxOfClosing(rest, "p");
                if (iClose >= 0) {
                    out.add(new Block(BlockType.P, rest.substring(rest.indexOf('>')+1, iClose)));
                    rest = rest.substring(iClose + "</p>".length());
                    continue;
                }
            }
            int nx = nextTopTagIndex(rest);
            String chunk = (nx > 0 ? rest.substring(0, nx) : rest);
            out.add(new Block(BlockType.P, chunk));
            rest = (nx > 0 ? rest.substring(nx) : "");
        }
        return out;
    }

    private int idxOfClosing(String s, String tag) {
        String close = "</" + tag + ">";
        return s.toLowerCase().indexOf(close);
    }

    private int nextTopTagIndex(String s) {
        String[] tags = {"<h1","<h2","<h3","<h4","<ul","<ol","<pre","<p","<br"};
        int min = -1;
        String sl = s.toLowerCase();
        for (String t : tags) {
            int i = sl.indexOf(t);
            if (i >= 0) min = (min < 0 ? i : Math.min(min, i));
        }
        return min;
    }

    private List<String> extractLi(String innerHtml) {
        List<String> out = new ArrayList<>();
        String rest = innerHtml;
        while (true) {
            int i = rest.toLowerCase().indexOf("<li");
            if (i < 0) break;
            int iGt = rest.indexOf('>', i);
            if (iGt < 0) break;
            int iClose = rest.toLowerCase().indexOf("</li>", iGt);
            if (iClose < 0) break;
            String li = rest.substring(iGt + 1, iClose);
            out.add(li);
            rest = rest.substring(iClose + 5);
        }
        return out;
    }

    private String stripTags(String html) {
        return html.replaceAll("(?is)<[^>]+>", "");
    }

    private List<Span> tokenizeInline(String html, PDFont defaultFont, float fs) {
        List<Span> spans = new ArrayList<>();
        if (html == null || html.isBlank()) return spans;
        String s = html
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'");
        boolean bold = (defaultFont == fontBold());
        boolean italic = (defaultFont == fontItalic());
        boolean underline = false;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < s.length();) {
            char c = s.charAt(i);
            if (c == '<') {
                if (buf.length() > 0) {
                    PDFont f = (bold && italic) ? fontBold() : (bold ? fontBold() : (italic ? fontItalic() : defaultFont));
                    spans.add(new Span(sanitizeText(buf.toString()), f, fs, underline, 0));
                    buf.setLength(0);
                }
                int j = s.indexOf('>', i);
                if (j < 0) break;
                String tag = s.substring(i+1, j).trim().toLowerCase(Locale.ROOT);
                boolean closing = tag.startsWith("/");
                String name = closing ? tag.substring(1) : tag;
                switch (name) {
                    case "b": case "strong": bold = !closing; break;
                    case "i": case "em": italic = !closing; break;
                    case "u": underline = !closing; break;
                    case "br": spans.add(new Span("\n", defaultFont, fs, false, 0)); break;
                    default: break;
                }
                i = j + 1;
            } else {
                buf.append(c);
                i++;
            }
        }
        if (buf.length() > 0) {
            PDFont f = (bold && italic) ? fontBold() : (bold ? fontBold() : (italic ? fontItalic() : defaultFont));
            spans.add(new Span(sanitizeText(buf.toString()), f, fs, underline, 0));
        }
        return spans;
    }

    private List<List<Span>> paragraphsFromHtml(String html, float baseFs) {
        List<List<Span>> out = new ArrayList<>();
        String normalized = html.replaceAll("(?i)</p>", "\n\n")
                .replaceAll("(?i)<p[^>]*>", "");
        String[] paras = normalized.split("\\n\\s*\\n");
        for (String p : paras) {
            out.add(tokenizeInline(p, fontReg(), baseFs));
        }
        return out;
    }

    private float drawWrappedStyledLine(PDPageContentStream cs,
                                        float x, float y, float maxW,
                                        List<Span> spans, float lineStep) throws IOException {
        List<Span> lineAccum = new ArrayList<>();
        float lineWidth = 0f;
        for (Span sp : spans) {
            String[] parts = sp.text.split("(?<=\\s)|(?=\\s)");
            for (String part : parts) {
                if (part.equals("\n")) {
                    if (!lineAccum.isEmpty()) {
                        drawSpans(cs, x, y, lineAccum);
                        y -= lineStep;
                        lineAccum.clear();
                        lineWidth = 0f;
                    } else {
                        y -= lineStep;
                    }
                    continue;
                }
                String probe = part;
                float w = sp.font.getStringWidth(probe) / 1000f * sp.fs;
                if (lineWidth + w > maxW && !lineAccum.isEmpty()) {
                    drawSpans(cs, x, y, lineAccum);
                    y -= lineStep;
                    lineAccum.clear();
                    lineWidth = 0f;
                }
                lineAccum.add(new Span(probe, sp.font, sp.fs, sp.underline, sp.indent));
                lineWidth += w;
            }
        }
        if (!lineAccum.isEmpty()) {
            drawSpans(cs, x, y, lineAccum);
            y -= lineStep;
        }
        return y;
    }

    private void drawSpans(PDPageContentStream cs, float x, float y, List<Span> spans) throws IOException {
        float cursorX = x;
        for (Span sp : spans) {
            BR.drawText(cs, sp.font, sp.fs, cursorX, y, sp.text);
            float w = sp.font.getStringWidth(sp.text) / 1000f * sp.fs;
            if (sp.underline) {
                cs.setLineWidth(0.5f);
                cs.moveTo(cursorX, y - 2);
                cs.lineTo(cursorX + w, y - 2);
                cs.stroke();
            }
            cursorX += w;
        }
    }

    private static String sanitizeText(String s) {
        if (s == null) return "";
        String t = s;
        t = t.replace('•', '•');
        t = t.replace('→', '→');
        t = t.replace('–', '-');
        t = t.replace('—', '-');
        t = t.replace('“', '"').replace('”', '"');
        t = t.replace('‘', '\'').replace('’', '\'');
        t = t.replace('\u00A0', ' ');
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c >= 32 && c <= 126 || c == '\n' || c == '\t') {
                sb.append(c);
            } else if (c >= 160 && c <= 255) {
                sb.append(c);
            } else {
                sb.append('?');
            }
        }
        return sb.toString();
    }

    private void drawTotalsValuesOnlyAdjusted(PDPageContentStream cs, TotaisDto t,
                                                     float baseXLabel, float baseXVal,
                                                     float baseYTop, float stepY,
                                                     float globalDx, float globalDy,
                                                     JsonNode ajustesInd, JsonNode totalTableCfg) throws IOException {
        if (t == null) return;
        String[] keys = new String[]{ "subtotal", "desconto", "totalMateriais", "totalServicos", "mobilizacao", "totalGeral" };
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            BigDecimal valor = getValorPorChave(t, key);
            if (valor == null) valor = BigDecimal.ZERO;
            float yLabel = baseYTop - (i * stepY);
            String label = formatLabel(key);
            BR.drawText(cs, fontReg(), FONT_H, baseXLabel, yLabel, label);
            float dx = globalDx + getAdj(ajustesInd, key, "dx");
            float dy = globalDy + getAdj(ajustesInd, key, "dy");
            float xVal = baseXVal + dx;
            float yVal = yLabel + dy;
            BR.drawText(cs, fontReg(), FONT_H, xVal, yVal, BR.moeda(valor));
        }
    }

    private static String formatLabel(String key) {
        return switch (key) {
            case "subtotal" -> "Subtotal";
            case "desconto" -> "Desconto";
            case "totalMateriais" -> "Total Materiais";
            case "totalServicos" -> "Total Serviços";
            case "mobilizacao" -> "Mobilização";
            case "totalGeral" -> "TOTAL GERAL";
            default -> key;
        };
    }

    private static BigDecimal getValorPorChave(TotaisDto t, String key) {
        return switch (key) {
            case "subtotal" -> toBD(t.getSubtotal());
            case "desconto" -> toBD(t.getDesconto());
            case "totalMateriais" -> toBD(t.getTotalMateriais());
            case "totalServicos" -> toBD(t.getTotalServicos());
            case "mobilizacao" -> toBD(t.getMobilizacao());
            case "totalGeral" -> toBD(t.getTotalGeral());
            default -> BigDecimal.ZERO;
        };
    }

    private static BigDecimal toBD(Number n) {
        if (n == null) return BigDecimal.ZERO;
        if (n instanceof BigDecimal) return (BigDecimal) n;
        try { return new BigDecimal(String.valueOf(n)); } catch (Exception e) { return BigDecimal.ZERO; }
    }

    private void drawGrid(PDPageContentStream cs, PDPage page, float step, float major) throws IOException {
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
                BR.drawText(cs, fontReg(), 7f, x + 2, 3, String.valueOf((int) x));
            }
        }
        for (float y = 0; y <= h + 0.1f; y += step) {
            boolean isMajor = (Math.round(y) % Math.round(major) == 0);
            cs.setStrokingColor(isMajor ? Color.GRAY : new Color(200,200,200));
            cs.moveTo(0, y);
            cs.lineTo(w, y);
            cs.stroke();
            if (isMajor) {
                BR.drawText(cs, fontReg(), 7f, 2, y + 2, String.valueOf((int) y));
            }
        }
        cs.setStrokingColor(Color.DARK_GRAY);
        cs.setLineWidth(0.6f);
        cs.moveTo(0, 0); cs.lineTo(w, 0); cs.stroke();
        cs.moveTo(0, 0); cs.lineTo(0, h); cs.stroke();
    }

    private void drawProbes(PDPageContentStream cs, JsonNode arr) throws IOException {
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
            BR.drawText(cs, fontReg(), 8f, x + 6, y + 2, label);
        }
    }

    private Fonts loadFonts(PDDocument doc) {
        try {
            PDFont reg = PDType0Font.load(doc, new ByteArrayInputStream(FONT_REG_BYTES));
            PDFont bold = PDType0Font.load(doc, new ByteArrayInputStream(FONT_BOLD_BYTES));
            PDFont italic = PDType0Font.load(doc, new ByteArrayInputStream(FONT_ITALIC_BYTES));
            PDFont mono = PDType0Font.load(doc, new ByteArrayInputStream(FONT_MONO_BYTES));
            return new Fonts(reg, bold, italic, mono);
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao carregar fontes Inter. Verifique src/main/resources/fonts/", e);
        }
    }

    private Fonts fonts() {
        Fonts f = fontContext.get();
        if (f == null) {
            throw new IllegalStateException("Contexto de fontes não inicializado");
        }
        return f;
    }

    private PDFont fontReg() { return fonts().reg(); }
    private PDFont fontBold() { return fonts().bold(); }
    private PDFont fontItalic() { return fonts().italic(); }
    private PDFont fontMono() { return fonts().mono(); }

    private static byte[] loadFontBytes(String path) {
        try (InputStream is = new ClassPathResource(path).getInputStream()) {
            return is.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao carregar fonte: " + path, e);
        }
    }

    private record Fonts(PDFont reg, PDFont bold, PDFont italic, PDFont mono) {}

    private static JsonNode loadDynamicConfig() {
        try (InputStream is = new ClassPathResource("templates/stongel-coords.json").getInputStream()) {
            return new ObjectMapper().readTree(is);
        } catch (Exception e) {
            try {
                return new ObjectMapper().readTree("{\"pageIndexes\":{\"headerTotais\":0,\"tables\":5}}");
            } catch (IOException ex) {
                throw new RuntimeException("Falha ao carregar stongel-coords.json", ex);
            }
        }
    }

    private BudgetDto mapRealJsonToBudgetDto(JsonNode root) {
        String razao = text(root, "dadosEmpresa.razaoSocial");
        String cnpj = text(root, "dadosEmpresa.cnpj");
        String contato = text(root, "dadosEmpresa.dadosResponsavel.nomeResponsavel");
        if (contato == null || (contato != null && contato.isBlank()))
            contato = text(root, "dadosEmpresa.nomeFantasia");
        String tel = coalesce(
                text(root, "dadosEmpresa.dadosResponsavel.raw.dadosContato.telefoneResponsavel"),
                text(root, "dadosEmpresa.telefone")
        );
        String mail = coalesce(
                text(root, "dadosEmpresa.dadosResponsavel.raw.dadosContato.emailResponsavel"),
                text(root, "dadosEmpresa.email")
        );
        String endereco = coalesce(text(root, "dadosObra.endereco"), "");
        String ref = coalesce(text(root, "dadosObra.referencia"), "");
        String metragem = Optional.ofNullable(root.at("/dadosObra/metragemTotal").numberValue())
                .map(BR::numero).orElse(null);
        String obra = joinNonEmpty(endereco, ref, " - ");
        if (metragem != null && !metragem.isBlank()) {
            obra = joinNonEmpty(obra, metragem + " m²", " • ");
        }

        Map<String, String> unidadePorMaterialId = new HashMap<>();
        for (JsonNode area : arr(root, "areasConfiguradas")) {
            for (JsonNode m : arr(area, "materiais")) {
                String id = text(m, "id");
                String un = text(m, "unidade");
                if (id != null && un != null) unidadePorMaterialId.put(id, un);
            }
        }

        List<ItemDto> materiais = new ArrayList<>();
        for (JsonNode p : arr(root, "precificacao.produtos")) {
            ItemDto it = new ItemDto();
            it.setDescricao(text(p, "descricao"));
            it.setComp("-");
            String un = unidadePorMaterialId.get(text(p, "id"));
            it.setUnidade(coalesce(un, "CJ"));
            it.setQuantidade(number(p, "quantidadeCJ"));
            if (it.getQuantidade() == null || it.getQuantidade().doubleValue() == 0d) {
                it.setQuantidade(number(p, "quantidade"));
            }
            it.setCusto(number(p, "precoBase"));
            it.setPrecoVenda(number(p, "valorTotal"));
            it.setLimiteDesconto(BigDecimal.ZERO);
            it.setPrecoKg(BigDecimal.ZERO);
            materiais.add(it);
        }

        List<ItemDto> servicos = new ArrayList<>();
        for (JsonNode s : arr(root, "precificacao.servicos")) {
            ItemDto it = new ItemDto();
            it.setDescricao(text(s, "descricao"));
            it.setUnidade("m²");
            it.setQuantidade(number(s, "quantidade"));
            it.setCusto(number(s, "valorUnitario"));
            it.setPrecoVenda(number(s, "valorTotal"));
            it.setLimiteDesconto(BigDecimal.ZERO);
            it.setPrecoKg(BigDecimal.ZERO);
            it.setComp("-");
            servicos.add(it);
        }

        TotaisDto tot = new TotaisDto();
        tot.setTotalMateriais(number(root, "precificacao.totais.material"));
        tot.setTotalServicos(number(root, "precificacao.totais.servico"));
        BigDecimal desconto = BigDecimal.ZERO;
        tot.setDesconto(desconto);
        BigDecimal subtotal = safeBD(tot.getTotalMateriais()).add(safeBD(tot.getTotalServicos()));
        tot.setSubtotal(subtotal);
        BigDecimal mobil = number(root, "precificacao.dadosMobilizacao.total.totalMobilizacao");
        if (mobil == null || mobil.compareTo(BigDecimal.ZERO) == 0) {
            mobil = number(root, "precificacao.mobilizacao");
        }
        tot.setMobilizacao(mobil);
        BigDecimal totalGeral = subtotal.add(safeBD(mobil)).subtract(safeBD(desconto));
        tot.setTotalGeral(totalGeral);

        BudgetDto dto = new BudgetDto();
        EmpresaDto empDto = new EmpresaDto();
        empDto.setRazaoSocial(razao);
        empDto.setCnpj(cnpj);
        empDto.setContato(contato);
        empDto.setTelefone(tel);
        empDto.setEmail(mail);
        dto.setEmpresa(empDto);
        dto.setObra(obra);
        dto.setMateriais(materiais);
        dto.setServicos(servicos);
        dto.setTotais(tot);
        return dto;
    }

    private Iterable<JsonNode> arr(JsonNode root, String path) {
        JsonNode n = at(root, path);
        return (n != null && n.isArray()) ? n : Collections.emptyList();
    }

    private String text(JsonNode n, String path) {
        JsonNode j = at(n, path);
        return (j != null && !j.isMissingNode() && !j.isNull()) ? j.asText() : null;
    }

    private BigDecimal number(JsonNode n, String path) {
        JsonNode j = at(n, path);
        if (j == null || j.isNull() || j.isMissingNode()) return BigDecimal.ZERO;
        if (j.isNumber()) return new BigDecimal(j.asText());
        try { return new BigDecimal(j.asText().replace(",", ".")); } catch (Exception e) { return BigDecimal.ZERO; }
    }

    private BigDecimal safeBD(Number n) {
        if (n == null) return BigDecimal.ZERO;
        if (n instanceof BigDecimal) return (BigDecimal) n;
        try { return new BigDecimal(String.valueOf(n)); } catch (Exception e) { return BigDecimal.ZERO; }
    }

    private String joinNonEmpty(String a, String b, String sep) {
        a = (a == null ? "" : a.trim());
        b = (b == null ? "" : b.trim());
        if (a.isEmpty() && b.isEmpty()) return "";
        if (a.isEmpty()) return b;
        if (b.isEmpty()) return a;
        return a + sep + b;
    }

    private String coalesce(String a, String b) {
        return (a != null && !a.isBlank()) ? a : (b != null ? b : null);
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
