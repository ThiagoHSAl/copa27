package br.ufmg.dcc603.copa27.persistencia.memoria;

import br.ufmg.dcc603.copa27.dominio.modelo.Grupo;
import br.ufmg.dcc603.copa27.dominio.modelo.Partida;
import br.ufmg.dcc603.copa27.persistencia.RepositorioPartidas;
import br.ufmg.dcc603.copa27.servico.FiltroPartidas;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RepositorioPartidasMemoria implements RepositorioPartidas {

    private final BaseDeDadosMemoria base;

    RepositorioPartidasMemoria(BaseDeDadosMemoria base) {
        this.base = base;
    }

    @Override
    public List<Partida> listar() {
        return new ArrayList<>(base.partidas.values());
    }

    @Override
    public Partida buscarPorId(int id) {
        return base.partidas.get(id);
    }

    @Override
    public void salvar(Partida p) {
        if (p.getId() == 0) {
            p.setId(BaseDeDadosMemoria.proximoId(base.partidas));
        }
        base.partidas.put(p.getId(), p);
    }

    @Override
    public void remover(int id) {
        base.partidas.remove(id);
    }

    @Override
    public List<Partida> buscarPorFiltro(FiltroPartidas f) {
        return base.partidas.values().stream().filter(f::aceita)
                .sorted(Comparator.comparing(Partida::getDataHora).thenComparingInt(Partida::getId)).toList();
    }

    @Override
    public List<Partida> buscarPorGrupo(Grupo g) {
        return base.partidas.values().stream().filter(p -> p.getGrupo() != null && p.getGrupo().getLetra() == g.getLetra())
                .toList();
    }
}
