package br.ufmg.dcc603.copa27.persistencia.memoria;

import br.ufmg.dcc603.copa27.dominio.modelo.Selecao;
import br.ufmg.dcc603.copa27.persistencia.RepositorioSelecoes;

import java.util.ArrayList;
import java.util.List;

public class RepositorioSelecoesMemoria implements RepositorioSelecoes {

    private final BaseDeDadosMemoria base;

    RepositorioSelecoesMemoria(BaseDeDadosMemoria base) {
        this.base = base;
    }

    @Override
    public List<Selecao> listar() {
        return new ArrayList<>(base.selecoes.values());
    }

    @Override
    public Selecao buscarPorId(int id) {
        return base.selecoes.get(id);
    }

    /** Ao editar, preserva o objeto já gravado, ao qual grupos e partidas fazem referência. */
    @Override
    public void salvar(Selecao s) {
        if (s.getId() == 0) {
            s.setId(BaseDeDadosMemoria.proximoId(base.selecoes));
        }
        Selecao existente = base.selecoes.get(s.getId());
        if (existente != null && existente != s) {
            existente.atualizarCom(s);
        } else {
            base.selecoes.put(s.getId(), s);
        }
    }

    @Override
    public void remover(int id) {
        base.selecoes.remove(id);
    }
}
