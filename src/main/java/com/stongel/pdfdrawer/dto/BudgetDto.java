package com.stongel.pdfdrawer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BudgetDto {

    @JsonAlias({"empresa","dadosEmpresa"})
    private EmpresaDto empresa;

    @JsonAlias({"obra","enderecoObra","localObra"})
    private String obra;

    @JsonAlias({"materiais","itensMateriais","produtos"})
    private List<ItemDto> materiais;

    @JsonAlias({"servicos","itensServicos"})
    private List<ItemDto> servicos;

    @JsonAlias({"totais","resumoTotais"})
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
