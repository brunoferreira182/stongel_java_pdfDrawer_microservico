package com.stongel.pdfdrawer.web;

import com.stongel.pdfdrawer.service.StongelTemplateRenderer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/render")
public class RenderController {

    private static final Logger log = LoggerFactory.getLogger(RenderController.class);
    private final StongelTemplateRenderer renderer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RenderController(StongelTemplateRenderer renderer) {
        this.renderer = renderer;
    }

    /**
     * Gera o PDF a partir do JSON do orçamento.
     * - Consome application/json
     * - Produz application/pdf
     * - Retorna Content-Disposition e Content-Length para facilitar download via proxy/browser
     */
    @PostMapping(
        value = "/from-payload",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> renderFromPayload(@RequestBody String json) {
        try {
            if (json == null || json.isBlank()) {
                return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(("{\"error\":\"Payload JSON vazio\"}").getBytes());
            }

            byte[] pdf = renderer.renderFromOrcamentoJson(json);

            if (pdf == null || pdf.length == 0) {
                // Renderer não gerou nada — trate como erro de servidor
                return ResponseEntity.internalServerError()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(("{\"error\":\"Falha ao gerar PDF (vazio)\"}").getBytes());
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentLength(pdf.length);
            // inline = abre no navegador; use "attachment" se quiser forçar download
            String filename = resolveFilename(json);
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + ".pdf\"");
            // Expor esse header ao JS (além do Nginx já expor)
            headers.add("Access-Control-Expose-Headers", "Content-Disposition");

            return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);

        } catch (IllegalArgumentException e) {
            // Erros previsíveis de validação/conteúdo
            log.warn("Falha de validação ao renderizar PDF: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(("{\"error\":\"Payload inválido: " + escapeJson(e.getMessage()) + "\"}").getBytes());

        } catch (Exception e) {
            // Qualquer outra exceção vira 500 com log completo
            log.error("Erro ao gerar PDF a partir do payload", e);
            return ResponseEntity.internalServerError()
                .contentType(MediaType.APPLICATION_JSON)
                .body(("{\"error\":\"Erro interno ao gerar PDF\"}").getBytes());
        }
    }

    @GetMapping("/health")
    public String health() {
        return "ok";
    }

    /**
     * Escapa aspas para mensagem JSON simples (evita quebrar o JSON de erro).
     */
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String resolveFilename(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String revisionCode = textValue(root, "codigoOrcamentoRevisao");
            String budgetCode = textValue(root, "codigoOrcamento");
            String raw = revisionCode != null && !revisionCode.isBlank()
                ? revisionCode
                : budgetCode;
            String normalized = normalizeCode(raw);
            return normalized.isBlank() ? "orcamento" : normalized;
        } catch (Exception e) {
            return "orcamento";
        }
    }

    private String textValue(JsonNode root, String field) {
        if (root == null || field == null) return null;
        JsonNode node = root.get(field);
        return node != null && !node.isNull() ? node.asText() : null;
    }

    private String normalizeCode(String code) {
        if (code == null) return "";
        String value = code.trim();
        if (value.isEmpty()) return "";
        int slash = value.indexOf('/');
        if (slash >= 0) value = value.substring(0, slash).trim();
        value = value.replaceAll("[_\\s]+", "-");
        value = value.replaceAll("[^A-Za-z0-9-]", "");
        value = value.replaceAll("-+", "-");
        value = value.replaceAll("^-+|-+$", "");
        return value.toUpperCase();
    }
}
