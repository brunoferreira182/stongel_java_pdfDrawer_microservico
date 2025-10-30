package com.stongel.pdfdrawer.service;

import com.stongel.pdfdrawer.dto.BudgetDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PdfRenderService {
    private static final Logger log = LoggerFactory.getLogger(PdfRenderService.class);
    private final StongelTemplateRenderer renderer;

    public PdfRenderService(StongelTemplateRenderer renderer) {
        this.renderer = renderer;
    }

    public byte[] renderFromTemplate(BudgetDto dto) throws Exception {
        if (dto == null) {
            log.warn("DTO nulo recebido!");
            return new byte[0];
        }
        log.debug("Empresa: {}", dto.getEmpresa() != null ? dto.getEmpresa().getRazaoSocial() : "(sem empresa)");
        log.debug("Obra: {}", dto.getObra());
        log.debug("Materiais: {}", dto.getMateriais() != null ? dto.getMateriais().size() : 0);
        log.debug("Serviços: {}", dto.getServicos() != null ? dto.getServicos().size() : 0);
        return renderer.renderFromTemplate(dto);
    }
}
