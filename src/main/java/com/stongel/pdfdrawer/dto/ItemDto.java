package com.stongel.pdfdrawer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ItemDto {
    @JsonAlias({"descricao","descricaoProduto","produto"})
    private String descricao;

    @JsonAlias({"comp","composicao"})
    private String comp;

    @JsonAlias({"unidade","un","unidadeMedida"})
    private String unidade;

    @JsonAlias({"quantidade","qtd"})
    private BigDecimal quantidade;

    @JsonAlias({"custo","valorCusto"})
    private BigDecimal custo;

    @JsonAlias({"precoVenda","preco","valorVenda"})
    private BigDecimal precoVenda;

    @JsonAlias({"limiteDesconto","descontoLimite","limite_desc"})
    private BigDecimal limiteDesconto;

    @JsonAlias({"precoKg","preco_por_kg"})
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
