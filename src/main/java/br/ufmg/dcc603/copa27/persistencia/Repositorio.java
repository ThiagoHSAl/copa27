package br.ufmg.dcc603.copa27.persistencia;

import java.util.List;

/** Operações comuns de acesso a dados (interface Acesso a Dados do Documento 04). */
public interface Repositorio<T> {

    List<T> listar();

    /** Devolve a entidade com o identificador informado, ou null se não houver. */
    T buscarPorId(int id);

    /** Grava a entidade; identificador 0 indica uma entidade nova, que recebe um identificador. */
    void salvar(T e);

    void remover(int id);
}
