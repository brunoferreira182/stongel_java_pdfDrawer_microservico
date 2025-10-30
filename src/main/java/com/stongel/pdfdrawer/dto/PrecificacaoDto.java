package com.stongel.pdfdrawer.dto;

public class PrecificacaoDto {
    private Number mobilizacao;
    private TotaisDto totais = new TotaisDto();

    public Number getMobilizacao() { return mobilizacao; }
    public void setMobilizacao(Number mobilizacao) { this.mobilizacao = mobilizacao; }

    public TotaisDto getTotais() { return totais; }
    public void setTotais(TotaisDto totais) { this.totais = totais; }
}
