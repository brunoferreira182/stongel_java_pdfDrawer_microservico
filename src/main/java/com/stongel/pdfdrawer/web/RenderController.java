package com.stongel.pdfdrawer.web;

import com.stongel.pdfdrawer.dto.BudgetDto;
import com.stongel.pdfdrawer.service.PdfRenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/render")
public class RenderController {
    private static final Logger log = LoggerFactory.getLogger(RenderController.class);
    private final PdfRenderService service;

    public RenderController(PdfRenderService service) {
        this.service = service;
    }

    @PostMapping(path = "/from-payload", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> renderFromPayload(@RequestBody BudgetDto dto) throws Exception {
        log.debug("Recebido payload para renderização");
        byte[] pdf = service.renderFromTemplate(dto);

        // força download com o nome do ORIGINAL
        String cd = ContentDisposition.attachment()
                .filename("STONGEL - PDF-ORIGINAL.pdf")
                .build()
                .toString();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, cd)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
