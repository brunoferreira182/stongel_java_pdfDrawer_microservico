package com.stongel.pdfdrawer.web;

import com.stongel.pdfdrawer.dto.BudgetDto;
import com.stongel.pdfdrawer.service.PdfRenderService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class RenderController {

    private final PdfRenderService service;

    public RenderController(PdfRenderService service) {
        this.service = service;
    }

    @GetMapping("/ping")
    public String ping() {
        return service.ping();
    }

    @PostMapping(path = "/render/from-payload", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> renderFromPayload(@RequestBody BudgetDto dto) throws Exception {
        byte[] pdf = service.renderFromTemplate(dto);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"orcamento-stongel.pdf\"")
                .body(pdf);
    }
}
