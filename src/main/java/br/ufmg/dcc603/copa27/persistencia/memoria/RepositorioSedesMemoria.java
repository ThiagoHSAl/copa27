package br.ufmg.dcc603.copa27.persistencia.memoria;

import br.ufmg.dcc603.copa27.dominio.modelo.Sede;
import br.ufmg.dcc603.copa27.persistencia.RepositorioSedes;

import java.util.ArrayList;
import java.util.List;

public class RepositorioSedesMemoria implements RepositorioSedes {

    private final BaseDeDadosMemoria base;

    RepositorioSedesMemoria(BaseDeDadosMemoria base) {
        this.base = base;
    }

    @Override
    public List<Sede> listar() {
        return new ArrayList<>(base.sedes.values());
    }

    @Override
    public Sede buscarPorId(int id) {
        return base.sedes.get(id);
    }

    /** Ao editar, preserva o objeto e o estádio já gravados, aos quais as partidas fazem referência. */
    @Override
    public void salvar(Sede s) {
        if (s.getId() == 0) {
            s.setId(BaseDeDadosMemoria.proximoId(base.sedes));
        }
        Sede existente = base.sedes.get(s.getId());
        if (existente != null && existente != s) {
            existente.atualizarCom(s);
        } else {
            base.sedes.put(s.getId(), s);
        }
    }

    @Override
    public void remover(int id) {
        base.sedes.remove(id);
    }
}
