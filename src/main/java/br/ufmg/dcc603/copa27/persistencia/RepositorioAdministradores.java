package br.ufmg.dcc603.copa27.persistencia;

import br.ufmg.dcc603.copa27.dominio.modelo.Administrador;

public interface RepositorioAdministradores {

    /** Devolve o administrador com o login informado, ou null se não houver. */
    Administrador buscarPorLogin(String login);
}
