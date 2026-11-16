package br.ufmg.dcc603.copa27.persistencia;

import br.ufmg.dcc603.copa27.dominio.modelo.Chaveamento;

public interface RepositorioChaveamento {

    Chaveamento carregar();

    void salvar(Chaveamento c);
}
