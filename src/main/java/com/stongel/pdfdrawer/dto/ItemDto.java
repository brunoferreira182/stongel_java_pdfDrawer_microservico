package com.stongel.pdfdrawer.dto;

import java.math.BigDecimal;

public class ItemDto {
    private String descricao;
    private String comp;          // A, B, A+B...
    private String unidade;       // UN/Kg...
    private BigDecimal quantidade;
    private BigDecimal custo;
    private BigDecimal precoVenda;
    private BigDecimal limiteDesconto; // em %
    private BigDecimal precoKg;

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getComp() { return comp; }
    public void setComp(String comp) { this.comp = comp; }

    public String getUnidade() { return unidade; }
    public void setUnidade(String unidade) { this.unidade = unidade; }

    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }

    public BigDecimal getCusto() { return custo; }
    public void setCusto(BigDecimal custo) { this.custo = custo; }

    public BigDecimal getPrecoVenda() { return precoVenda; }
    public void setPrecoVenda(BigDecimal precoVenda) { this.precoVenda = precoVenda; }

    public BigDecimal getLimiteDesconto() { return limiteDesconto; }
    public void setLimiteDesconto(BigDecimal limiteDesconto) { this.limiteDesconto = limiteDesconto; }

    public BigDecimal getPrecoKg() { return precoKg; }
    public void setPrecoKg(BigDecimal precoKg) { this.precoKg = precoKg; }
}
