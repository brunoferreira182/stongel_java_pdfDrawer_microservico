package com.stongel.pdfdrawer.dto;

import java.math.BigDecimal;

public class TotaisDto {
    private BigDecimal totalMateriais;
    private BigDecimal totalServicos;
    private BigDecimal subtotal;
    private BigDecimal desconto;
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
