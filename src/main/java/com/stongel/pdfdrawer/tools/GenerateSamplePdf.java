package com.stongel.pdfdrawer.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stongel.pdfdrawer.dto.BudgetDto;
import com.stongel.pdfdrawer.service.StongelTemplateRenderer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Small CLI helper to render a sample PDF (useful for local smoke tests).
 */
public final class GenerateSamplePdf {

    public static void main(String[] args) throws Exception {
        String inputPath = args.length > 0 ? args[0] : "mini-ok.json";
        String outputPath = args.length > 1 ? args[1] : "src/main/resources/templates/orcamento-34.pdf";

        ObjectMapper mapper = new ObjectMapper();
        BudgetDto dto = mapper.readValue(new File(inputPath), BudgetDto.class);

        StongelTemplateRenderer renderer = new StongelTemplateRenderer();
        byte[] pdfBytes = renderer.renderFromTemplate(dto);

        Path out = Path.of(outputPath);
        Files.createDirectories(out.getParent());
        Files.write(out, pdfBytes);

        System.out.printf("PDF gerado com sucesso em %s (%d bytes)%n", out, pdfBytes.length);
    }
}
