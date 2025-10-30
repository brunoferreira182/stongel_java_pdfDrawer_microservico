package com.stongel.pdfdrawer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EmpresaDto {
    @JsonAlias({"razaoSocial","razao_social","nomeEmpresa","nome"})
    private String razaoSocial;

    @JsonAlias({"cnpj","CNPJ"})
    private String cnpj;

    @JsonAlias({"telefone","fone","tel"})
    private String telefone;

    @JsonAlias({"email","eMail"})
    private String email;

    public String getRazaoSocial() { return razaoSocial; }
    public void setRazaoSocial(String razaoSocial) { this.razaoSocial = razaoSocial; }

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
