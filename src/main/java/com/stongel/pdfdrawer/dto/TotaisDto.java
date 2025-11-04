package com.stongel.pdfdrawer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TotaisDto {

    @JsonAlias({"subtotal"})
    private BigDecimal subtotal;

    @JsonAlias({"desconto"})
    private BigDecimal desconto;

    @JsonAlias({"totalMateriais","materiais"})
    private BigDecimal totalMateriais;

    @JsonAlias({"totalServicos","servicos"})
    private BigDecimal totalServicos;

    @JsonAlias({"totalGeral","total"})
    private BigDecimal totalGeral;

    @JsonAlias({"mobilizacao","totalMobilizacao"})
    private BigDecimal mobilizacao;

    public BigDecimal getSubtotal() { return nz(subtotal); }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = nz(subtotal); }

    public BigDecimal getDesconto() { return nz(desconto); }
    public void setDesconto(BigDecimal desconto) { this.desconto = nz(desconto); }

    public BigDecimal getTotalMateriais() { return nz(totalMateriais); }
    public void setTotalMateriais(BigDecimal totalMateriais) { this.totalMateriais = nz(totalMateriais); }

    public BigDecimal getTotalServicos() { return nz(totalServicos); }
    public void setTotalServicos(BigDecimal totalServicos) { this.totalServicos = nz(totalServicos); }

    public BigDecimal getTotalGeral() { return nz(totalGeral); }
    public void setTotalGeral(BigDecimal totalGeral) { this.totalGeral = nz(totalGeral); }

    public BigDecimal getMobilizacao() { return nz(mobilizacao); }
    public void setMobilizacao(BigDecimal mobilizacao) { this.mobilizacao = nz(mobilizacao); }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
