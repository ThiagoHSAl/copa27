package br.ufmg.dcc603.copa27.servico;

import br.ufmg.dcc603.copa27.dominio.modelo.Partida;
import br.ufmg.dcc603.copa27.dominio.modelo.Sede;
import br.ufmg.dcc603.copa27.dominio.modelo.Selecao;
import br.ufmg.dcc603.copa27.persistencia.RepositorioPartidas;
import br.ufmg.dcc603.copa27.persistencia.RepositorioSedes;
import br.ufmg.dcc603.copa27.persistencia.RepositorioSelecoes;

import java.util.Comparator;
import java.util.List;

/**
 * Consultas que não envolvem regras de competição: tabela de jogos (CU01 a
 * CU03), sedes (CU10 e CU11) e seleções (CU13 e CU14).
 */
public class ServicoConsulta {

    private final RepositorioPartidas partidas;
    private final RepositorioSelecoes selecoes;
    private final RepositorioSedes sedes;

    public ServicoConsulta(RepositorioPartidas partidas, RepositorioSelecoes selecoes, RepositorioSedes sedes) {
        this.partidas = partidas;
        this.selecoes = selecoes;
        this.sedes = sedes;
    }

    /** Tabela de jogos em ordem cronológica, opcionalmente filtrada (RF01, RF02). */
    public List<Partida> listarPartidas(FiltroPartidas f) {
        return partidas.buscarPorFiltro(f == null ? FiltroPartidas.nenhum() : f);
    }

    public Partida buscarPartida(int id) {
        Partida p = partidas.buscarPorId(id);
        if (p == null) {
            throw new NaoEncontradoException("Partida " + id + " não encontrada.");
        }
        return p;
    }

    /** Sedes em ordem alfabética de cidade (RF14). */
    public List<Sede> listarSedes() {
        return sedes.listar().stream().sorted(Comparator.comparing(Sede::getCidade)).toList();
    }

    public Sede buscarSede(int id) {
        Sede s = sedes.buscarPorId(id);
        if (s == null) {
            throw new NaoEncontradoException("Sede " + id + " não encontrada.");
        }
        return s;
    }

    /** Partidas agendadas para uma sede (RF19). */
    public List<Partida> listarPartidasDaSede(int idSede) {
        buscarSede(idSede);
        return listarPartidas(FiltroPartidas.porSede(idSede));
    }

    /** Seleções em ordem alfabética de país (RF20). */
    public List<Selecao> listarSelecoes() {
        return selecoes.listar().stream().sorted(Comparator.comparing(Selecao::getPais)).toList();
    }

    public Selecao buscarSelecao(int id) {
        Selecao s = selecoes.buscarPorId(id);
        if (s == null) {
            throw new NaoEncontradoException("Seleção " + id + " não encontrada.");
        }
        return s;
    }

    /** Partidas já disputadas e futuras de uma seleção, em ordem cronológica (RF22). */
    public List<Partida> listarPartidasDaSelecao(int id) {
        buscarSelecao(id);
        return listarPartidas(FiltroPartidas.porSelecao(id));
    }
}
