package br.ufmg.dcc603.copa27.persistencia.memoria;

import br.ufmg.dcc603.copa27.dominio.modelo.Chaveamento;
import br.ufmg.dcc603.copa27.persistencia.RepositorioChaveamento;

public class RepositorioChaveamentoMemoria implements RepositorioChaveamento {

    private final BaseDeDadosMemoria base;

    RepositorioChaveamentoMemoria(BaseDeDadosMemoria base) {
        this.base = base;
    }

    @Override
    public Chaveamento carregar() {
        return base.chaveamento;
    }

    @Override
    public void salvar(Chaveamento c) {
        base.chaveamento = c;
    }
}
