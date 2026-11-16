package br.ufmg.dcc603.copa27.persistencia.memoria;

import br.ufmg.dcc603.copa27.dominio.modelo.RegistroAlteracao;
import br.ufmg.dcc603.copa27.persistencia.RepositorioRegistros;

import java.util.List;

public class RepositorioRegistrosMemoria implements RepositorioRegistros {

    private final BaseDeDadosMemoria base;

    RepositorioRegistrosMemoria(BaseDeDadosMemoria base) {
        this.base = base;
    }

    @Override
    public void registrar(RegistroAlteracao r) {
        base.registros.add(r);
    }

    @Override
    public List<RegistroAlteracao> listar() {
        return List.copyOf(base.registros);
    }
}
