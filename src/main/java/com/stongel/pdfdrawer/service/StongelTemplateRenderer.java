package com.stongel.pdfdrawer.service;

import com.stongel.pdfdrawer.dto.BudgetDto;
import com.stongel.pdfdrawer.dto.ItemDto;
import com.stongel.pdfdrawer.util.BR;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class StongelTemplateRenderer {

    // ====== FONTS (PDFBox 3.x) ======
    private static final PDFont HELV    = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont HELV_B  = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private static final float FONT_H = 10f;
    private static final float FONT_H_BOLD = 10.5f;

    private static final float X_EMPRESA = 48f;
    private static final float Y_EMPRESA_RAZAO = 742f;
    private static final float Y_EMPRESA_CNPJ   = 730f;
    private static final float Y_EMPRESA_TEL    = 718f;
    private static final float Y_EMPRESA_EMAIL  = 706f;

    private static final float X_OBRA_LABEL = 48f;
    private static final float X_OBRA_VALUE = 90f;
    private static final float Y_OBRA       = 680f;
    private static final float OBRA_MAX_WIDTH = 520f;

    private static final float Y_TIT_MAT    = 650f;
    private static final float Y_TIT_SRV    = 420f;

    private static final float X_COL_DESC   = 48f;
    private static final float X_COL_COMP   = 332f;
    private static final float X_COL_UN     = 362f;
    private static final float X_COL_QTD    = 400f;
    private static final float X_COL_CUSTO  = 440f;
    private static final float X_COL_PRECO  = 490f;
    private static final float X_COL_DESC_LIM = 540f;
    private static final float X_COL_PRECO_KG = 590f;

    private static final float ROW_H        = 14.0f;
    private static final int   MAX_ROWS_MAT = 14;
    private static final int   MAX_ROWS_SRV = 12;

    private static final float X_TOTAIS_LABEL = 370f;
    private static final float X_TOTAIS_VAL   = 540f;
    private static final float Y_TOTAIS_INI   = 180f;
    private static final float GAP_TOTAIS     = 14f;

    public byte[] renderFromTemplate(BudgetDto dto) throws Exception {
        return render(dto);
    }

    public byte[] render(BudgetDto dto) throws Exception {
        ClassPathResource cpr = new ClassPathResource("templates/STONGEL - PDF.pdf");
        try (InputStream in = cpr.getInputStream()) {
            byte[] bytes = in.readAllBytes();

            try (PDDocument doc = Loader.loadPDF(bytes);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                PDPage page = doc.getPage(0);
                PDRectangle media = page.getMediaBox();

                try (PDPageContentStream cs = new PDPageContentStream(doc, page,
                        PDPageContentStream.AppendMode.APPEND, true)) {

                    // ===== Cabeçalho Empresa =====
                    drawText(cs, HELV_B, FONT_H_BOLD, X_EMPRESA, Y_EMPRESA_RAZAO,
                            BR.texto(dto.getEmpresa() != null ? dto.getEmpresa().getRazaoSocial() : null));
                    drawText(cs, HELV,      FONT_H,      X_EMPRESA, Y_EMPRESA_CNPJ,
                            "CNPJ: " + BR.texto(dto.getEmpresa() != null ? dto.getEmpresa().getCnpj() : null));
                    drawText(cs, HELV,      FONT_H,      X_EMPRESA, Y_EMPRESA_TEL,
                            "Telefone: " + BR.texto(dto.getEmpresa() != null ? dto.getEmpresa().getTelefone() : null));
                    drawText(cs, HELV,      FONT_H,      X_EMPRESA, Y_EMPRESA_EMAIL,
                            "E-mail: " + BR.texto(dto.getEmpresa() != null ? dto.getEmpresa().getEmail() : null));

                    // ===== Obra =====
                    drawText(cs, HELV_B, FONT_H, X_OBRA_LABEL, Y_OBRA, "Obra:");
                    String obra = BR.texto(dto.getObra());
                    drawParagraph(cs, X_OBRA_VALUE, Y_OBRA, OBRA_MAX_WIDTH, obra, FONT_H + 2,
                            HELV, FONT_H);

                    // ===== Materiais =====
                    drawText(cs, HELV_B, FONT_H, X_COL_DESC, Y_TIT_MAT, "MATERIAIS");
                    float yHeadMat = Y_TIT_MAT - 14;
                    drawHeaders(cs, yHeadMat);
                    float y = yHeadMat - ROW_H;
                    List<ItemDto> mats = dto.getMateriais();
                    if (mats != null) {
                        for (int i = 0; i < Math.min(mats.size(), MAX_ROWS_MAT); i++) {
                            y = drawItemRow(cs, mats.get(i), y);
                        }
                    }

                    // ===== Serviços =====
                    drawText(cs, HELV_B, FONT_H, X_COL_DESC, Y_TIT_SRV, "SERVIÇOS");
                    float yHeadSrv = Y_TIT_SRV - 14;
                    drawHeaders(cs, yHeadSrv);
                    float ySrv = yHeadSrv - ROW_H;
                    List<ItemDto> srvs = dto.getServicos();
                    if (srvs != null) {
                        for (int i = 0; i < Math.min(srvs.size(), MAX_ROWS_SRV); i++) {
                            ySrv = drawItemRow(cs, srvs.get(i), ySrv);
                        }
                    }

                    // ===== Totais =====
                    BigDecimal totalMateriais = dto.getTotais() != null ? dto.getTotais().getTotalMateriais() : null;
                    BigDecimal totalServicos  = dto.getTotais() != null ? dto.getTotais().getTotalServicos()  : null;
                    BigDecimal subtotal       = dto.getTotais() != null ? dto.getTotais().getSubtotal()       : null;
                    BigDecimal desconto       = dto.getTotais() != null ? dto.getTotais().getDesconto()       : null;
                    BigDecimal totalGeral     = dto.getTotais() != null ? dto.getTotais().getTotalGeral()     : null;

                    int idx = 0;
                    drawTotal(cs,     "Total Materiais:", BR.moeda(totalMateriais), Y_TOTAIS_INI - (idx++)*GAP_TOTAIS);
                    drawTotal(cs,     "Total Serviços:",  BR.moeda(totalServicos),  Y_TOTAIS_INI - (idx++)*GAP_TOTAIS);
                    drawTotal(cs,     "Subtotal:",        BR.moeda(subtotal),       Y_TOTAIS_INI - (idx++)*GAP_TOTAIS);
                    drawTotal(cs,     "Desconto:",        BR.moeda(desconto),       Y_TOTAIS_INI - (idx++)*GAP_TOTAIS);
                    drawTotalBold(cs, "TOTAL GERAL:",     BR.moeda(totalGeral),     Y_TOTAIS_INI - (idx)*GAP_TOTAIS);
                }

                doc.save(out);
                return out.toByteArray();
            }
        }
    }

    private void drawHeaders(PDPageContentStream cs, float y) throws Exception {
        drawText(cs, HELV_B, FONT_H, X_COL_DESC,     y, "Produto / Descrição");
        drawText(cs, HELV_B, FONT_H, X_COL_COMP,     y, "Comp");
        drawText(cs, HELV_B, FONT_H, X_COL_UN,       y, "Un/Kg");
        drawText(cs, HELV_B, FONT_H, X_COL_QTD,      y, "Qtd");
        drawText(cs, HELV_B, FONT_H, X_COL_CUSTO,    y, "Custo");
        drawText(cs, HELV_B, FONT_H, X_COL_PRECO,    y, "Preço Venda");
        drawText(cs, HELV_B, FONT_H, X_COL_DESC_LIM, y, "Limite Desc.");
        drawText(cs, HELV_B, FONT_H, X_COL_PRECO_KG, y, "Preço/Kg");
    }

    private float drawItemRow(PDPageContentStream cs, ItemDto it, float y) throws Exception {
        String desc = it != null ? safe(it.getDescricao()) : "-";
        if (desc.length() > 50) desc = desc.substring(0, 50);

        drawText(cs, HELV, FONT_H, X_COL_DESC,     y, desc);
        drawText(cs, HELV, FONT_H, X_COL_COMP,     y, safe(it != null ? it.getComp() : null));
        drawText(cs, HELV, FONT_H, X_COL_UN,       y, safe(it != null ? it.getUnidade() : null));
        drawText(cs, HELV, FONT_H, X_COL_QTD,      y, BR.numero(it != null ? it.getQuantidade() : null));
        drawText(cs, HELV, FONT_H, X_COL_CUSTO,    y, BR.moeda(it != null ? it.getCusto() : null));
        drawText(cs, HELV, FONT_H, X_COL_PRECO,    y, BR.moeda(it != null ? it.getPrecoVenda() : null));
        drawText(cs, HELV, FONT_H, X_COL_DESC_LIM, y, it != null && it.getLimiteDesconto()!=null ? it.getLimiteDesconto().toPlainString()+"%" : "-");
        drawText(cs, HELV, FONT_H, X_COL_PRECO_KG, y, BR.moeda(it != null ? it.getPrecoKg() : null));

        return y - ROW_H;
    }

    private void drawTotal(PDPageContentStream cs, String label, String value, float y) throws Exception {
        drawText(cs, HELV,   FONT_H, X_TOTAIS_LABEL, y, label);
        drawText(cs, HELV_B, FONT_H, X_TOTAIS_VAL,   y, value);
    }

    private void drawTotalBold(PDPageContentStream cs, String label, String value, float y) throws Exception {
        drawText(cs, HELV_B, FONT_H, X_TOTAIS_LABEL, y, label);
        drawText(cs, HELV_B, FONT_H, X_TOTAIS_VAL,   y, value);
    }

    private void drawText(PDPageContentStream cs, PDFont font, float size, float x, float y, String text) throws Exception {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text != null ? text : "-");
        cs.endText();
    }

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    // Parágrafo (wrap por largura)
    public void drawParagraph(PDPageContentStream cs, float x, float y, float maxWidth,
                              String text, float leading, PDFont font, float fontSize) throws Exception {
        if (text == null) text = "-";
        List<String> lines = wrapText(text, font, fontSize, maxWidth);
        float cursorY = y;
        for (String line : lines) {
            drawText(cs, font, fontSize, x, cursorY, line);
            cursorY -= leading;
        }
    }

    private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws Exception {
        List<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();

        for (String w : words) {
            String candidate = line.length() == 0 ? w : line + " " + w;
            float width = stringWidth(font, fontSize, candidate);
            if (width <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
            } else {
                if (line.length() > 0) {
                    lines.add(line.toString());
                    line.setLength(0);
                    line.append(w);
                } else {
                    lines.add(w);
                }
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    private float stringWidth(PDFont font, float fontSize, String text) throws Exception {
        return font.getStringWidth(text) / 1000f * fontSize;
    }
}
