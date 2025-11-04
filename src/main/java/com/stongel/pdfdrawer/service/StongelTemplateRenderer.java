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

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

@Component
public class StongelTemplateRenderer {

    // Fonte
    private static final PDFont FONT_REG = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final float  FONT_H   = 10f;

    private final ObjectMapper om = new ObjectMapper();

    /* ===========================
       ENTRADA 1: JSON REAL
       =========================== */

    public byte[] renderFromOrcamentoJson(String json) throws Exception {
        JsonNode root = om.readTree(json);
        BudgetDto dto = mapRealJsonToBudgetDto(root);
        return renderFromTemplate(dto);
    }

    /* ===========================
       ENTRADA 2: DTO
       =========================== */
    public byte[] renderFromTemplate(BudgetDto dto) throws Exception {

        // Template
        byte[] templateBytes;
        try (InputStream is = new ClassPathResource("templates/STONGEL - PDF.pdf").getInputStream()) {
            templateBytes = is.readAllBytes();
        }

        // Coords dinâmicas
        JsonNode cfg = loadDynamicConfig();

        int PAGE_IDX_HEADER_TOTAIS = getInt(cfg, "pageIndexes.headerTotais", 0);
        int PAGE_IDX_TABELAS       = getInt(cfg, "pageIndexes.tables", 6);

        // ---------- Página 1 (labels fixos + valores) ----------
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

        float L_RAZAO_X = 60f, L_RAZAO_Y = 175f;
        float L_CNPJ_X  = 60f, L_CNPJ_Y  = 155f;
        float L_CONT_X  = 60f, L_CONT_Y  = 135f;
        float L_TEL_X   = 60f, L_TEL_Y   = 115f;
        float L_MAIL_X  = 60f, L_MAIL_Y  = 95f;
        float L_OBRA_X  = X_OBRA_LABEL_FALL, L_OBRA_Y = Y_OBRA;

        // ---------- Layout novo (página 7) ----------
        JsonNode L_MAT = at(cfg, "layout.materiais");
        float MAT_QTD_RIGHT     = getF(L_MAT, "xQtdRight", 55f);
        float MAT_UN_X          = getF(L_MAT, "xUn",       105f);
        float MAT_DESC_X        = getF(L_MAT, "xDesc",     200f);
        float MAT_DESC_MAXW     = getF(L_MAT, "descMaxWidth", 360f);
        float MAT_SUBT_RIGHT    = getF(L_MAT, "xSubtotalRight", 510f);
        float MAT_Y_FIRST       = getF(L_MAT, "yFirst",    555f);
        float MAT_Y_MIN         = getF(L_MAT, "yMin",      380f);
        float MAT_ROW_STEP      = getF(L_MAT, "rowStep",   16f);
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
        float SRV_DESC_DX       = getF(L_SRV, "descDx",    0f);
        float SRV_DESC_DY       = getF(L_SRV, "descDy",    0f);

        // Totais (pág. 7)
        float X_TOT_TAB_LABEL = getF(cfg, "totaisTables.xLabel", 420f);
        float X_TOT_TAB_VAL   = getF(cfg, "totaisTables.xVal",   560f);
        float Y_TOT_TAB_TOP   = getF(cfg, "totaisTables.yTop",   200f);
        float Y_TOT_TAB_STEP  = getF(cfg, "totaisTables.step",    16f);
        float VAL_DX          = getF(cfg, "totaisTables.valDx",    0f);
        float VAL_DY          = getF(cfg, "totaisTables.valDy",    0f);
        JsonNode AJUSTES_INDIVIDUAIS = at(cfg, "totaisTables.ajustesIndividuais");

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
                }

                // LABELS fixos (não mexer)
                BR.drawText(cs, FONT_REG, FONT_H, L_RAZAO_X, L_RAZAO_Y, "Razão Social:");
                BR.drawText(cs, FONT_REG, FONT_H, L_CNPJ_X,  L_CNPJ_Y,  "CNPJ:");
                BR.drawText(cs, FONT_REG, FONT_H, L_CONT_X,  L_CONT_Y,  "Contato:");
                BR.drawText(cs, FONT_REG, FONT_H, L_TEL_X,   L_TEL_Y,   "Telefone:");
                BR.drawText(cs, FONT_REG, FONT_H, L_MAIL_X,  L_MAIL_Y,  "E-mail:");
                BR.drawText(cs, FONT_REG, FONT_H, L_OBRA_X,  L_OBRA_Y,  "Obra:");

                // Valores
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
                }

                // Materiais
                if (hasItems(dto.getMateriais())) {
                    drawMaterialsRowsV2(cs, dto.getMateriais(),
                            MAT_Y_FIRST, MAT_ROW_STEP, MAT_Y_MIN,
                            MAT_QTD_RIGHT, MAT_UN_X,
                            MAT_DESC_X + MAT_DESC_DX, MAT_DESC_MAXW,
                            MAT_SUBT_RIGHT,
                            MAT_DESC_DY
                    );
                }

                // Serviços
                if (hasItems(dto.getServicos())) {
                    drawServicesRowsV2(cs, dto.getServicos(),
                            SRV_Y_FIRST, SRV_ROW_STEP, SRV_Y_MIN,
                            SRV_QTD_RIGHT, SRV_UN_X,
                            SRV_DESC_X + SRV_DESC_DX, SRV_DESC_MAXW,
                            SRV_CUSTO_U_RIGHT, SRV_SUBT_RIGHT,
                            SRV_DESC_DY
                    );
                }

                // Totais
                drawTotals(cs, dto.getTotais(),
                        X_TOT_TAB_LABEL, X_TOT_TAB_VAL,
                        Y_TOT_TAB_TOP, Y_TOT_TAB_STEP,
                        VAL_DX, VAL_DY,
                        AJUSTES_INDIVIDUAIS);
            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                doc.save(baos);
                return baos.toByteArray();
            }
        }
    }

    /* ============== Desenho ============== */

    private static void normalizeToCropBox(PDPageContentStream cs, PDPage page) throws IOException {
        PDRectangle crop = page.getCropBox();
        if (crop != null) {
            cs.transform(Matrix.getTranslateInstance(-crop.getLowerLeftX(), -crop.getLowerLeftY()));
        }
    }

    private static void drawMaterialsRowsV2(
            PDPageContentStream cs,
            java.util.List<ItemDto> itens,
            float yStart, float rowStep, float yMin,
            float xQtdRight, float xUn,
            float xDesc, float descMaxWidth,
            float xSubtotalRight,
            float descDy
    ) throws IOException {
        if (itens == null || itens.isEmpty()) return; // evita NPE
        float y = yStart;
        for (ItemDto it : itens) {
            if (y < yMin) break;

            drawRightAligned(cs, FONT_REG, FONT_H, xQtdRight, y, BR.numero(it.getQuantidade())); // Qtde
            BR.drawText(cs, FONT_REG, FONT_H, xUn, y, safe(it.getUnidade()));                    // Unidade
            drawWrapped(cs, FONT_REG, FONT_H, xDesc, y + descDy, descMaxWidth, safe(it.getDescricao()), 10f); // Descrição

            double subtotal = (it.getPrecoVenda() != null && it.getPrecoVenda().doubleValue() > 0)
                    ? it.getPrecoVenda().doubleValue()
                    : ((it.getCusto() != null && it.getQuantidade() != null)
                        ? it.getCusto().doubleValue() * it.getQuantidade().doubleValue()
                        : 0.0);
            drawRightAligned(cs, FONT_REG, FONT_H, xSubtotalRight, y, BR.moeda(subtotal));       // Subtotal

            y -= rowStep;
        }
    }

    private static void drawServicesRowsV2(
            PDPageContentStream cs,
            java.util.List<ItemDto> itens,
            float yStart, float rowStep, float yMin,
            float xQtdRight, float xUn,
            float xDesc, float descMaxWidth,
            float xCustoUnitRight, float xSubtotalRight,
            float descDy
    ) throws IOException {
        if (itens == null || itens.isEmpty()) return; // evita NPE
        float y = yStart;
        for (ItemDto it : itens) {
            if (y < yMin) break;

            drawRightAligned(cs, FONT_REG, FONT_H, xQtdRight, y, BR.numero(it.getQuantidade())); // Qtde
            BR.drawText(cs, FONT_REG, FONT_H, xUn, y, safe(it.getUnidade()));                    // Unidade
            drawWrapped(cs, FONT_REG, FONT_H, xDesc, y + descDy, descMaxWidth, safe(it.getDescricao()), 10f); // Serviço

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

    /* ============== DEBUG ============== */

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

    /* ============== JSON / Mapeamento ============== */

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

    private BudgetDto mapRealJsonToBudgetDto(JsonNode root) {
        // Empresa
        String razao = text(root, "dadosEmpresa.razaoSocial");
        String cnpj  = text(root, "dadosEmpresa.cnpj");
        String contato = text(root, "dadosEmpresa.dadosResponsavel.nomeResponsavel");
        if (isBlank(contato)) contato = text(root, "dadosEmpresa.nomeFantasia");

        String tel = coalesce(
                text(root, "dadosEmpresa.dadosResponsavel.raw.dadosContato.telefoneResponsavel"),
                text(root, "dadosEmpresa.telefone")
        );
        String mail = coalesce(
                text(root, "dadosEmpresa.dadosResponsavel.raw.dadosContato.emailResponsavel"),
                text(root, "dadosEmpresa.email")
        );

        // Obra compacta
        String endereco = coalesce(text(root, "dadosObra.endereco"), "");
        String ref      = coalesce(text(root, "dadosObra.referencia"), "");
        String metragem = null;
        JsonNode metrNode = at(root, "dadosObra.metragemTotal");
        if (metrNode != null && !metrNode.isNull() && !metrNode.isMissingNode()) {
            BigDecimal m = parseNumeric(metrNode);
            if (m.compareTo(BigDecimal.ZERO) > 0) {
                metragem = BR.numero(m);
            }
        }
        String obra = joinNonEmpty(
                joinNonEmpty(endereco, ref, " - "),
                (metragem != null ? metragem + " m²" : ""),
                " • "
        );

        // índice unidade por material (áreasConfiguradas)
        Map<String, String> unidadePorMaterialId = new HashMap<>();
        JsonNode areasCfg = at(root, "areasConfiguradas");
        if (areasCfg != null && areasCfg.isArray()) {
            for (JsonNode area : areasCfg) {
                JsonNode mats = at(area, "materiais");
                if (mats != null && mats.isArray()) {
                    for (JsonNode m : mats) {
                        String id = text(m, "id");
                        String un = text(m, "unidade");
                        if (!isBlank(id) && !isBlank(un)) unidadePorMaterialId.put(id, un);
                    }
                }
            }
        }

        // Materiais (precificacao.produtos)
        java.util.List<ItemDto> materiais = new ArrayList<>();
        JsonNode produtos = at(root, "precificacao.produtos");
        if (produtos != null && produtos.isArray()) {
            for (JsonNode p : produtos) {
                ItemDto it = new ItemDto();
                it.setDescricao(text(p, "descricao"));
                it.setComp("-");

                String matId = text(p, "id");
                String unidade = unidadePorMaterialId.get(matId);
                if (isBlank(unidade)) unidade = "CJ";
                it.setUnidade(unidade);

                BigDecimal qtd = firstNonZero(
                        parseNumeric(p.path("quantidadeCJ")),
                        parseNumeric(p.path("quantidade")),
                        parseNumeric(p.path("quantidadeMetragem"))
                );
                it.setQuantidade(qtd);

                it.setCusto(parseNumeric(p.path("precoBase")));   // custo base
                it.setPrecoVenda(parseNumeric(p.path("valorTotal"))); // subtotal do item

                it.setLimiteDesconto(BigDecimal.ZERO);
                it.setPrecoKg(BigDecimal.ZERO);

                materiais.add(it);
            }
        }

        // Serviços (precificacao.servicos)
        java.util.List<ItemDto> servicos = new ArrayList<>();
        JsonNode servs = at(root, "precificacao.servicos");
        if (servs != null && servs.isArray()) {
            for (JsonNode s : servs) {
                ItemDto it = new ItemDto();
                it.setDescricao(text(s, "descricao"));
                it.setComp("-");
                it.setUnidade("m\u00B2");

                BigDecimal qtd = firstNonZero(
                        parseNumeric(s.path("quantidade")),
                        parseNumeric(s.path("metragem"))
                );
                it.setQuantidade(qtd);

                it.setCusto(parseNumeric(s.path("valorUnitario"))); // usado para custo unitário
                it.setPrecoVenda(parseNumeric(s.path("valorTotal"))); // subtotal

                it.setLimiteDesconto(BigDecimal.ZERO);
                it.setPrecoKg(BigDecimal.ZERO);

                servicos.add(it);
            }
        }

        // Totais
        TotaisDto tot = new TotaisDto();
        tot.setTotalMateriais(parseNumeric(at(root, "precificacao.totais.material")));
        tot.setTotalServicos(parseNumeric(at(root, "precificacao.totais.servico")));
        tot.setDesconto(BigDecimal.ZERO);

        BigDecimal subtotal = tot.getTotalMateriais().add(tot.getTotalServicos());
        tot.setSubtotal(subtotal);
        tot.setTotalGeral(subtotal.subtract(tot.getDesconto()));

        BudgetDto dto = new BudgetDto();

        // Empresa no DTO
        try {
            var empClass = dto.getClass().getMethod("getEmpresa").getReturnType();
            Object emp = dto.getClass().getMethod("getEmpresa").invoke(dto);
            if (emp == null) {
                emp = empClass.getDeclaredConstructor().newInstance();
            }
            empClass.getMethod("setRazaoSocial", String.class).invoke(emp, razao);
            empClass.getMethod("setCnpj", String.class).invoke(emp, cnpj);
            empClass.getMethod("setContato", String.class).invoke(emp, contato);
            empClass.getMethod("setTelefone", String.class).invoke(emp, tel);
            empClass.getMethod("setEmail", String.class).invoke(emp, mail);
            dto.getClass().getMethod("setEmpresa", empClass).invoke(dto, emp);
        } catch (Exception ignore) {
            // se o DTO tiver setters diretos, adapte aqui
        }

        dto.setObra(obra);
        dto.setMateriais(materiais); // nunca null
        dto.setServicos(servicos);   // nunca null
        dto.setTotais(tot);
        return dto;
    }

    /* ============== Utils num/strings/paths ============== */

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String coalesce(String a, String b) {
        return !isBlank(a) ? a : (!isBlank(b) ? b : null);
    }

    private static String joinNonEmpty(String a, String b, String sep) {
        a = (a == null ? "" : a.trim());
        b = (b == null ? "" : b.trim());
        if (a.isEmpty() && b.isEmpty()) return "";
        if (a.isEmpty()) return b;
        if (b.isEmpty()) return a;
        return a + sep + b;
    }

    private Iterable<JsonNode> arr(JsonNode root, String path) {
        JsonNode n = at(root, path);
        return (n != null && n.isArray()) ? n : Collections.emptyList();
    }

    private String text(JsonNode n, String path) {
        JsonNode j = at(n, path);
        return (j != null && !j.isMissingNode() && !j.isNull()) ? j.asText() : null;
    }

    private static BigDecimal parseNumeric(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return BigDecimal.ZERO;
        try {
            if (node.isNumber()) return new BigDecimal(node.asText());
            String s = node.asText().trim().replace(",", ".");
            if (s.isEmpty()) return BigDecimal.ZERO;
            return new BigDecimal(s);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static BigDecimal firstNonZero(BigDecimal... vals) {
        for (BigDecimal v : vals) {
            if (v != null && v.compareTo(BigDecimal.ZERO) > 0) return v;
        }
        return BigDecimal.ZERO;
    }

    private static BigDecimal safeBD(Number n) {
        if (n == null) return BigDecimal.ZERO;
        if (n instanceof BigDecimal bd) return bd;
        try { return new BigDecimal(String.valueOf(n)); } catch (Exception e) { return BigDecimal.ZERO; }
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
        if (isBlank(v)) return def;
        try { return Float.parseFloat(v); } catch (Exception e) { return def; }
    }

    private static boolean hasItems(java.util.List<?> list) {
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
