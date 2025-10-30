package com.stongel.pdfdrawer.dto;

import java.util.List;

public class BudgetDto {
    private EmpresaDto empresa;
    private String obra;
    private List<ItemDto> materiais;
    private List<ItemDto> servicos;
    private TotaisDto totais;

    public EmpresaDto getEmpresa() { return empresa; }
    public void setEmpresa(EmpresaDto empresa) { this.empresa = empresa; }

    public String getObra() { return obra; }
    public void setObra(String obra) { this.obra = obra; }

    public List<ItemDto> getMateriais() { return materiais; }
    public void setMateriais(List<ItemDto> materiais) { this.materiais = materiais; }

    public List<ItemDto> getServicos() { return servicos; }
    public void setServicos(List<ItemDto> servicos) { this.servicos = servicos; }

    public TotaisDto getTotais() { return totais; }
    public void setTotais(TotaisDto totais) { this.totais = totais; }
}
