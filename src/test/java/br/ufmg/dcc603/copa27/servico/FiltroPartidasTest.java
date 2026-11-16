package br.ufmg.dcc603.copa27.servico;

import br.ufmg.dcc603.copa27.Fixtures;
import br.ufmg.dcc603.copa27.dominio.modelo.Fase;
import br.ufmg.dcc603.copa27.dominio.modelo.Grupo;
import br.ufmg.dcc603.copa27.dominio.modelo.Partida;
import br.ufmg.dcc603.copa27.dominio.modelo.Sede;
import br.ufmg.dcc603.copa27.dominio.modelo.Selecao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** RF02: critérios opcionais combinados por conjunção. */
class FiltroPartidasTest {

    private Sede sede;
    private Selecao a;
    private Partida partida;

    @BeforeEach
    void preparar() {
        sede = Fixtures.sede(3, "Salvador");
        Grupo g = Fixtures.grupo('A', 1);
        a = g.getSelecoes().get(0);
        partida = new Partida(1, Fase.GRUPOS, LocalDateTime.of(2027, 6, 25, 16, 0), sede.getEstadio(), g, a,
                g.getSelecoes().get(1));
    }

    @Test
    @DisplayName("Filtro vazio aceita tudo")
    void vazio() {
        assertTrue(FiltroPartidas.nenhum().estaVazio());
        assertTrue(FiltroPartidas.nenhum().aceita(partida));
    }

    @Test
    @DisplayName("Cada critério isolado")
    void criteriosIsolados() {
        assertTrue(FiltroPartidas.porSelecao(a.getId()).aceita(partida));
        assertTrue(FiltroPartidas.porSelecao(2).aceita(partida));
        assertFalse(FiltroPartidas.porSelecao(3).aceita(partida));
        assertTrue(FiltroPartidas.porSede(3).aceita(partida));
        assertFalse(FiltroPartidas.porSede(4).aceita(partida));
        assertTrue(new FiltroPartidas(null, null, Fase.GRUPOS, null, null).aceita(partida));
        assertFalse(new FiltroPartidas(null, null, Fase.FINAL, null, null).aceita(partida));
    }

    @Test
    @DisplayName("Intervalo de datas inclui os extremos")
    void intervalo() {
        LocalDate dia = LocalDate.of(2027, 6, 25);
        assertTrue(new FiltroPartidas(null, null, null, dia, dia).aceita(partida));
        assertFalse(new FiltroPartidas(null, null, null, dia.plusDays(1), null).aceita(partida));
        assertFalse(new FiltroPartidas(null, null, null, null, dia.minusDays(1)).aceita(partida));
    }

    @Test
    @DisplayName("Critérios combinados exigem todos")
    void combinados() {
        FiltroPartidas f = new FiltroPartidas(a.getId(), 3, Fase.GRUPOS, LocalDate.of(2027, 6, 24), null);
        assertTrue(f.aceita(partida));
        assertFalse(f.estaVazio());
        assertEquals(a.getId(), f.getIdSelecao());
        assertEquals(3, f.getIdSede());
        assertEquals(Fase.GRUPOS, f.getFase());
        assertNotNull(f.getDataInicio());
        assertNull(f.getDataFim());
        assertFalse(new FiltroPartidas(a.getId(), 4, Fase.GRUPOS, null, null).aceita(partida));
    }

    @Test
    @DisplayName("Partida de mata-mata sem seleções não atende ao filtro por seleção")
    void mataMataSemSelecoes() {
        Partida p = new Partida(49, Fase.OITAVAS, LocalDateTime.of(2027, 7, 10, 16, 0), sede.getEstadio(), null, null, null);
        assertFalse(FiltroPartidas.porSelecao(1).aceita(p));
    }
}
