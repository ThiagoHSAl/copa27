package br.ufmg.dcc603.copa27.persistencia.memoria;

import br.ufmg.dcc603.copa27.dominio.modelo.Grupo;
import br.ufmg.dcc603.copa27.persistencia.RepositorioGrupos;

import java.util.ArrayList;
import java.util.List;

public class RepositorioGruposMemoria implements RepositorioGrupos {

    private final BaseDeDadosMemoria base;

    RepositorioGruposMemoria(BaseDeDadosMemoria base) {
        this.base = base;
    }

    @Override
    public List<Grupo> listar() {
        return new ArrayList<>(base.grupos.values());
    }

    @Override
    public Grupo buscarPorId(int id) {
        return base.grupos.get(id);
    }

    @Override
    public void salvar(Grupo g) {
        base.grupos.put(g.getId(), g);
    }

    @Override
    public void remover(int id) {
        base.grupos.remove(id);
    }
}
