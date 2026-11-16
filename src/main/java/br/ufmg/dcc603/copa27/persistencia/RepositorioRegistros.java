package br.ufmg.dcc603.copa27.persistencia;

import br.ufmg.dcc603.copa27.dominio.modelo.RegistroAlteracao;

import java.util.List;

public interface RepositorioRegistros {

    void registrar(RegistroAlteracao r);

    List<RegistroAlteracao> listar();
}
