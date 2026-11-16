package br.ufmg.dcc603.copa27.persistencia;

import br.ufmg.dcc603.copa27.dominio.modelo.Grupo;
import br.ufmg.dcc603.copa27.dominio.modelo.Partida;
import br.ufmg.dcc603.copa27.servico.FiltroPartidas;

import java.util.List;

public interface RepositorioPartidas extends Repositorio<Partida> {

    List<Partida> buscarPorFiltro(FiltroPartidas f);

    List<Partida> buscarPorGrupo(Grupo g);
}
