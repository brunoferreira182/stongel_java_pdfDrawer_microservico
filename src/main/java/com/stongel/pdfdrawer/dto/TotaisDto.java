package com.stongel.pdfdrawer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TotaisDto {
    @JsonAlias({"totalMateriais","materiaisTotal","valorMateriais"})
    private BigDecimal totalMateriais;

    @JsonAlias({"totalServicos","servicosTotal","valorServicos"})
    private BigDecimal totalServicos;

    @JsonAlias({"subtotal","subTotal"})
    private BigDecimal subtotal;

    @JsonAlias({"desconto","valorDesconto"})
    private BigDecimal desconto;

    @JsonAlias({"totalGeral","valorTotal","valorTotalGeral"})
    private BigDecimal totalGeral;

    public BigDecimal getTotalMateriais() { return totalMateriais; }
    public void setTotalMateriais(BigDecimal totalMateriais) { this.totalMateriais = totalMateriais; }
    public BigDecimal getTotalServicos() { return totalServicos; }
    public void setTotalServicos(BigDecimal totalServicos) { this.totalServicos = totalServicos; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getDesconto() { return desconto; }
    public void setDesconto(BigDecimal desconto) { this.desconto = desconto; }
    public BigDecimal getTotalGeral() { return totalGeral; }
    public void setTotalGeral(BigDecimal totalGeral) { this.totalGeral = totalGeral; }
}
