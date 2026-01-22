package com.stongel.pdfdrawer.dto;

public class SectionTextDto {
  private String slug;       // ex.: "apresentacao-da-empresa"
  private String titulo;     // opcional
  private String descricao;  // HTML do WYSIWYG

  public String getSlug() { return slug; }
  public void setSlug(String slug) { this.slug = slug; }

  public String getTitulo() { return titulo; }
  public void setTitulo(String titulo) { this.titulo = titulo; }

  public String getDescricao() { return descricao; }
  public void setDescricao(String descricao) { this.descricao = descricao; }
}
