package br.ufmg.dcc603.copa27.servico;

import br.ufmg.dcc603.copa27.dominio.modelo.Fase;
import br.ufmg.dcc603.copa27.dominio.modelo.Partida;
import br.ufmg.dcc603.copa27.dominio.modelo.RegraNegocioException;

import java.time.LocalDate;

/**
 * Critérios de filtragem da tabela de jogos (RF02). Todos são opcionais e
 * combinados por conjunção.
 */
public class FiltroPartidas {

    private final Integer idSelecao;
    private final Integer idSede;
    private final Fase fase;
    private final LocalDate dataInicio;
    private final LocalDate dataFim;

    public FiltroPartidas(Integer idSelecao, Integer idSede, Fase fase, LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio != null && dataFim != null && dataInicio.isAfter(dataFim)) {
            throw new RegraNegocioException("A data inicial não pode ser posterior à data final.");
        }
        this.idSelecao = idSelecao;
        this.idSede = idSede;
        this.fase = fase;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
    }

    public static FiltroPartidas nenhum() {
        return new FiltroPartidas(null, null, null, null, null);
    }

    public static FiltroPartidas porSelecao(int idSelecao) {
        return new FiltroPartidas(idSelecao, null, null, null, null);
    }

    public static FiltroPartidas porSede(int idSede) {
        return new FiltroPartidas(null, idSede, null, null, null);
    }

    public boolean aceita(Partida p) {
        if (idSelecao != null && !temSelecao(p, idSelecao)) {
            return false;
        }
        if (idSede != null && (p.getEstadio().getSede() == null || p.getEstadio().getSede().getId() != idSede)) {
            return false;
        }
        if (fase != null && p.getFase() != fase) {
            return false;
        }
        LocalDate dia = p.getDataHora().toLocalDate();
        if (dataInicio != null && dia.isBefore(dataInicio)) {
            return false;
        }
        return dataFim == null || !dia.isAfter(dataFim);
    }

    private static boolean temSelecao(Partida p, int id) {
        return (p.getMandante() != null && p.getMandante().getId() == id)
                || (p.getVisitante() != null && p.getVisitante().getId() == id);
    }

    public boolean estaVazio() {
        return idSelecao == null && idSede == null && fase == null && dataInicio == null && dataFim == null;
    }

    public Integer getIdSelecao() {
        return idSelecao;
    }

    public Integer getIdSede() {
        return idSede;
    }

    public Fase getFase() {
        return fase;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }
}
