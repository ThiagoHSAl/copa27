package br.ufmg.dcc603.copa27.dominio.modelo;

import java.time.LocalDateTime;
import java.util.Objects;

/** Entrada do log de alterações feitas pelo Administrador (CU19). */
public class RegistroAlteracao {

    private final LocalDateTime dataHora;
    private final String operacao;
    private final String dado;
    private final Administrador autor;

    public RegistroAlteracao(LocalDateTime dataHora, String operacao, String dado, Administrador autor) {
        this.dataHora = Objects.requireNonNull(dataHora);
        this.operacao = Objects.requireNonNull(operacao);
        this.dado = Objects.requireNonNull(dado);
        this.autor = Objects.requireNonNull(autor);
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getOperacao() {
        return operacao;
    }

    public String getDado() {
        return dado;
    }

    public Administrador getAutor() {
        return autor;
    }
}
