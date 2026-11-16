package br.ufmg.dcc603.copa27.servico;

import br.ufmg.dcc603.copa27.Ambiente;
import br.ufmg.dcc603.copa27.dominio.modelo.Fase;
import br.ufmg.dcc603.copa27.dominio.modelo.Partida;
import br.ufmg.dcc603.copa27.dominio.modelo.RegraNegocioException;
import br.ufmg.dcc603.copa27.dominio.modelo.Sede;
import br.ufmg.dcc603.copa27.dominio.modelo.Selecao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** CU01 a CU03, CU10, CU11, CU13 e CU14. */
class ServicoConsultaTest {

    private Ambiente amb;
    private ServicoConsulta consulta;

    @BeforeEach
    void preparar() {
        amb = new Ambiente();
        consulta = amb.consulta;
    }

    @Test
    @DisplayName("RF01: a tabela tem as 64 partidas em ordem cronológica")
    void tabelaCompleta() {
        List<Partida> l = consulta.listarPartidas(null);
        assertEquals(64, l.size());
        for (int i = 1; i < l.size(); i++) {
            assertFalse(l.get(i).getDataHora().isBefore(l.get(i - 1).getDataHora()));
        }
        assertEquals(64, consulta.listarPartidas(FiltroPartidas.nenhum()).size());
    }

    @Test
    @DisplayName("RF02: filtros por fase e por intervalo de datas se combinam")
    void filtrosCombinados() {
        assertEquals(8, consulta.listarPartidas(new FiltroPartidas(null, null, Fase.OITAVAS, null, null)).size());
        List<Partida> l = consulta.listarPartidas(new FiltroPartidas(null, null, Fase.GRUPOS,
                LocalDate.of(2027, 6, 24), LocalDate.of(2027, 6, 24)));
        assertFalse(l.isEmpty());
        assertTrue(l.stream().allMatch(p -> p.getDataHora().toLocalDate().equals(LocalDate.of(2027, 6, 24))));
        assertTrue(consulta.listarPartidas(new FiltroPartidas(null, null, Fase.FINAL,
                LocalDate.of(2027, 6, 24), LocalDate.of(2027, 6, 30))).isEmpty());
    }

    @Test
    @DisplayName("RF03: busca de partida existente e inexistente")
    void buscarPartida() {
        Partida p = consulta.buscarPartida(64);
        assertEquals(Fase.FINAL, p.getFase());
        assertEquals("Rio de Janeiro", p.getEstadio().getSede().getCidade());
        assertThrows(NaoEncontradoException.class, () -> consulta.buscarPartida(65));
    }

    @Test
    @DisplayName("RF14: as oito sedes em ordem alfabética, com estádio")
    void sedes() {
        List<Sede> sedes = consulta.listarSedes();
        assertEquals(8, sedes.size());
        assertEquals("Belo Horizonte", sedes.get(0).getCidade());
        assertEquals("São Paulo", sedes.get(7).getCidade());
        assertThrows(NaoEncontradoException.class, () -> consulta.buscarSede(99));
    }

    @Test
    @DisplayName("RF15, RF16 e RF19: detalhes da sede e suas partidas")
    void detalhesDaSede() {
        Sede rio = consulta.listarSedes().stream().filter(s -> s.getCidade().equals("Rio de Janeiro")).findFirst().orElseThrow();
        Sede buscada = consulta.buscarSede(rio.getId());
        assertEquals("Estádio do Maracanã", buscada.getEstadio().getNome());
        assertEquals("SDU", buscada.getAeroportos().get(0).codigoIata());
        List<Partida> jogos = consulta.listarPartidasDaSede(rio.getId());
        assertFalse(jogos.isEmpty());
        assertTrue(jogos.stream().allMatch(p -> p.getEstadio() == rio.getEstadio()));
        assertTrue(jogos.stream().anyMatch(p -> p.getFase() == Fase.FINAL));
        int total = consulta.listarSedes().stream().mapToInt(s -> consulta.listarPartidasDaSede(s.getId()).size()).sum();
        assertEquals(64, total);
        assertThrows(NaoEncontradoException.class, () -> consulta.listarPartidasDaSede(99));
    }

    @Test
    @DisplayName("RF20: as 32 seleções em ordem alfabética")
    void selecoes() {
        List<Selecao> l = consulta.listarSelecoes();
        assertEquals(32, l.size());
        assertEquals(l.stream().sorted(Comparator.comparing(Selecao::getPais)).toList(), l);
        assertThrows(NaoEncontradoException.class, () -> consulta.buscarSelecao(99));
    }

    @Test
    @DisplayName("RF22: partidas de uma seleção, antes e depois de disputadas")
    void partidasDaSelecao() {
        Selecao brasil = consulta.listarSelecoes().stream().filter(s -> s.getCodigoFifa().equals("BRA")).findFirst().orElseThrow();
        assertSame(brasil, consulta.buscarSelecao(brasil.getId()));
        List<Partida> jogos = consulta.listarPartidasDaSelecao(brasil.getId());
        assertEquals(3, jogos.size());
        assertTrue(jogos.stream().allMatch(p -> p.envolve(brasil)));
        amb.jogar(jogos.get(0).getId(), br.ufmg.dcc603.copa27.dominio.modelo.Resultado.placar(2, 0));
        assertEquals(1, consulta.listarPartidasDaSelecao(brasil.getId()).stream().filter(Partida::estaDisputada).count());
        assertThrows(NaoEncontradoException.class, () -> consulta.listarPartidasDaSelecao(99));
    }

    @Test
    @DisplayName("CU02, E2: intervalo com data inicial posterior à final é rejeitado")
    void intervaloInvalido() {
        assertThrows(RegraNegocioException.class, () -> new FiltroPartidas(null, null, null,
                LocalDate.of(2027, 7, 2), LocalDate.of(2027, 7, 1)));
    }
}
