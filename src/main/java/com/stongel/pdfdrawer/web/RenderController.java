package com.stongel.pdfdrawer.web;

import com.stongel.pdfdrawer.service.StongelTemplateRenderer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/render")
public class RenderController {

    private final StongelTemplateRenderer renderer;

    public RenderController(StongelTemplateRenderer renderer) {
        this.renderer = renderer;
    }

    @PostMapping(value = "/from-payload", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> renderFromPayload(@RequestBody String json) throws Exception {
        byte[] pdf = renderer.renderFromOrcamentoJson(json);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=orcamento.pdf")
                .body(pdf);
    }

    @GetMapping("/health")
    public String health() { return "ok"; }
}
