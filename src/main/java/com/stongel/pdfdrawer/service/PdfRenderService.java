package com.stongel.pdfdrawer.service;

import com.stongel.pdfdrawer.dto.BudgetDto;
import org.springframework.stereotype.Service;

@Service
public class PdfRenderService {

    private final StongelTemplateRenderer renderer;

    public PdfRenderService(StongelTemplateRenderer renderer) {
        this.renderer = renderer;
    }

    public String ping() { return "ok"; }

    public byte[] renderFromTemplate(BudgetDto dto) throws Exception {
        return renderer.renderFromTemplate(dto);
    }

    public byte[] render(BudgetDto dto) throws Exception {
        return renderer.render(dto);
    }
}
