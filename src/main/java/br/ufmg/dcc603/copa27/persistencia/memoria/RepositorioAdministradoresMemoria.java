package br.ufmg.dcc603.copa27.persistencia.memoria;

import br.ufmg.dcc603.copa27.dominio.modelo.Administrador;
import br.ufmg.dcc603.copa27.persistencia.RepositorioAdministradores;

public class RepositorioAdministradoresMemoria implements RepositorioAdministradores {

    private final BaseDeDadosMemoria base;

    RepositorioAdministradoresMemoria(BaseDeDadosMemoria base) {
        this.base = base;
    }

    @Override
    public Administrador buscarPorLogin(String login) {
        return login == null ? null : base.administradores.get(login);
    }

    public void salvar(Administrador a) {
        base.administradores.put(a.getLogin(), a);
    }
}
