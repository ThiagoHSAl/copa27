package br.ufmg.dcc603.copa27.dominio.modelo;

import br.ufmg.dcc603.copa27.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Ciclo de vida da partida (Documento 05, seção 4) e regras RN05 e RN06. */
class PartidaTest {

    private Grupo grupo;
    private Selecao a;
    private Selecao b;
    private Partida jogoGrupo;
    private Partida jogoMataMata;

    @BeforeEach
    void preparar() {
        grupo = Fixtures.grupo('A', 1);
        a = grupo.getSelecoes().get(0);
        b = grupo.getSelecoes().get(1);
        jogoGrupo = Fixtures.jogoGrupo(1, grupo, a, b);
        jogoMataMata = Fixtures.jogoMataMata(49, Fase.OITAVAS, a, b);
    }

    @Nested
    @DisplayName("Criação")
    class Criacao {

        @Test
        @DisplayName("Toda partida nasce agendada e sem resultado")
        void nasceAgendada() {
            assertEquals(SituacaoPartida.AGENDADA, jogoGrupo.getSituacao());
            assertNull(jogoGrupo.getResultado());
            assertFalse(jogoGrupo.estaDisputada());
            assertNull(jogoGrupo.vencedora());
            assertNull(jogoGrupo.perdedora());
        }

        @Test
        @DisplayName("Dados obrigatórios e coerência entre fase e grupo")
        void validacoes() {
            Estadio e = Fixtures.estadio();
            assertThrows(RegraNegocioException.class, () -> new Partida(1, null, Fixtures.DATA, e, grupo, a, b));
            assertThrows(RegraNegocioException.class, () -> new Partida(1, Fase.GRUPOS, null, e, grupo, a, b));
            assertThrows(RegraNegocioException.class, () -> new Partida(1, Fase.GRUPOS, Fixtures.DATA, null, grupo, a, b));
            assertThrows(RegraNegocioException.class, () -> new Partida(1, Fase.GRUPOS, Fixtures.DATA, e, null, a, b));
            assertThrows(RegraNegocioException.class, () -> new Partida(1, Fase.OITAVAS, Fixtures.DATA, e, grupo, a, b));
            assertThrows(RegraNegocioException.class, () -> new Partida(1, Fase.GRUPOS, Fixtures.DATA, e, grupo, a, null));
            assertThrows(RegraNegocioException.class, () -> new Partida(1, Fase.GRUPOS, Fixtures.DATA, e, grupo, a, a));
        }

        @Test
        @DisplayName("Partida de mata-mata pode ser cadastrada sem as seleções")
        void mataMataSemSelecoes() {
            Partida p = Fixtures.jogoMataMata(50, Fase.QUARTAS, null, null);
            assertNull(p.getMandante());
            assertEquals("Partida 50 (a definir x a definir)", p.toString());
        }
    }

    @Nested
    @DisplayName("Transições de estado")
    class Transicoes {

        @Test
        @DisplayName("RN06: partida agendada recusa o registro de resultado")
        void agendadaRecusaResultado() {
            RegraNegocioException e = assertThrows(RegraNegocioException.class,
                    () -> jogoGrupo.registrarResultado(Resultado.placar(1, 0)));
            assertTrue(e.getMessage().contains("não começou"));
            assertNull(jogoGrupo.getResultado());
            assertEquals(SituacaoPartida.AGENDADA, jogoGrupo.getSituacao());
        }

        @Test
        @DisplayName("iniciar() leva de agendada para em andamento")
        void iniciar() {
            jogoGrupo.iniciar();
            assertEquals(SituacaoPartida.EM_ANDAMENTO, jogoGrupo.getSituacao());
            assertThrows(RegraNegocioException.class, jogoGrupo::iniciar);
        }

        @Test
        @DisplayName("iniciar() exige as duas seleções definidas")
        void iniciarSemSelecoes() {
            Partida p = Fixtures.jogoMataMata(57, Fase.QUARTAS, a, null);
            assertThrows(RegraNegocioException.class, p::iniciar);
            assertEquals(SituacaoPartida.AGENDADA, p.getSituacao());
        }

        @Test
        @DisplayName("Em andamento, o resultado pode ser registrado e atualizado")
        void atualizarResultado() {
            jogoGrupo.iniciar();
            jogoGrupo.registrarResultado(Resultado.placar(0, 0));
            jogoGrupo.registrarResultado(Resultado.placar(1, 0));
            assertEquals(Resultado.placar(1, 0), jogoGrupo.getResultado());
            assertFalse(jogoGrupo.temResultadoDefinitivo());
        }

        @Test
        @DisplayName("encerrar() exige resultado registrado")
        void encerrarSemResultado() {
            jogoGrupo.iniciar();
            assertThrows(RegraNegocioException.class, jogoGrupo::encerrar);
        }

        @Test
        @DisplayName("encerrar() só vale para partida em andamento")
        void encerrarAgendada() {
            assertThrows(RegraNegocioException.class, jogoGrupo::encerrar);
        }

        @Test
        @DisplayName("Fase de grupos encerra também com empate")
        void grupoEncerraEmpatado() {
            Fixtures.encerrar(jogoGrupo, Resultado.placar(2, 2));
            assertEquals(SituacaoPartida.ENCERRADA, jogoGrupo.getSituacao());
            assertTrue(jogoGrupo.estaDisputada());
            assertTrue(jogoGrupo.temResultadoDefinitivo());
            assertNull(jogoGrupo.vencedora());
        }

        @Test
        @DisplayName("Partida encerrada informa vencedora e perdedora")
        void vencedoraEPerdedora() {
            Fixtures.encerrar(jogoGrupo, Resultado.placar(0, 3));
            assertSame(b, jogoGrupo.vencedora());
            assertSame(a, jogoGrupo.perdedora());
        }

        @Test
        @DisplayName("RN05: mata-mata empatado não pode ser encerrado e permanece em andamento")
        void mataMataEmpatadoNaoEncerra() {
            jogoMataMata.iniciar();
            jogoMataMata.registrarResultado(Resultado.placar(1, 1));
            assertThrows(RegraNegocioException.class, jogoMataMata::encerrar);
            assertEquals(SituacaoPartida.EM_ANDAMENTO, jogoMataMata.getSituacao());
        }

        @Test
        @DisplayName("RN05: mata-mata decidido nos pênaltis é encerrado com vencedora")
        void mataMataPenaltis() {
            Fixtures.encerrar(jogoMataMata, Resultado.comPenaltis(2, 3, 5));
            assertEquals(SituacaoPartida.ENCERRADA, jogoMataMata.getSituacao());
            assertSame(b, jogoMataMata.vencedora());
            assertSame(a, jogoMataMata.perdedora());
        }

        @Test
        @DisplayName("Encerrada, a correção do mata-mata precisa manter uma vencedora")
        void correcaoMataMata() {
            Fixtures.encerrar(jogoMataMata, Resultado.placar(2, 0));
            assertThrows(RegraNegocioException.class, () -> jogoMataMata.registrarResultado(Resultado.placar(1, 1)));
            jogoMataMata.registrarResultado(Resultado.placar(3, 0));
            assertEquals(Resultado.placar(3, 0), jogoMataMata.getResultado());
        }

        @Test
        @DisplayName("Fase de grupos não admite prorrogação nem pênaltis")
        void grupoSemProrrogacao() {
            jogoGrupo.iniciar();
            assertThrows(RegraNegocioException.class, () -> jogoGrupo.registrarResultado(Resultado.comProrrogacao(2, 1)));
            assertThrows(RegraNegocioException.class, () -> jogoGrupo.registrarResultado(null));
        }
    }

    @Nested
    @DisplayName("Simulação")
    class Simulacao {

        @Test
        @DisplayName("Partida agendada aceita placar simulado")
        void simularAgendada() {
            jogoGrupo.simular(Resultado.placar(2, 1));
            assertTrue(jogoGrupo.isResultadoSimulado());
            assertTrue(jogoGrupo.temResultadoDefinitivo());
            assertEquals(SituacaoPartida.AGENDADA, jogoGrupo.getSituacao());
            assertSame(a, jogoGrupo.vencedora());
        }

        @Test
        @DisplayName("Partida encerrada não pode ser simulada (CU06, E2)")
        void simularEncerrada() {
            Fixtures.encerrar(jogoGrupo, Resultado.placar(1, 0));
            assertThrows(RegraNegocioException.class, () -> jogoGrupo.simular(Resultado.placar(0, 1)));
        }

        @Test
        @DisplayName("Mata-mata simulado exige vencedora (CU06, A2)")
        void simularEmpateMataMata() {
            assertThrows(RegraNegocioException.class, () -> jogoMataMata.simular(Resultado.placar(0, 0)));
            jogoMataMata.simular(Resultado.comPenaltis(0, 4, 2));
            assertSame(a, jogoMataMata.vencedora());
        }

        @Test
        @DisplayName("Não se simula partida sem as seleções definidas")
        void simularSemSelecoes() {
            Partida p = Fixtures.jogoMataMata(57, Fase.QUARTAS, null, null);
            assertThrows(RegraNegocioException.class, () -> p.simular(Resultado.placar(1, 0)));
            assertThrows(RegraNegocioException.class, () -> p.simular(null));
        }

        @Test
        @DisplayName("Trocar uma seleção descarta o placar simulado, que perde o sentido")
        void trocaDescartaSimulacao() {
            jogoMataMata.simular(Resultado.placar(1, 0));
            Selecao c = grupo.getSelecoes().get(2);
            jogoMataMata.definirMandante(c);
            assertSame(c, jogoMataMata.getMandante());
            assertNull(jogoMataMata.getResultado());
            assertFalse(jogoMataMata.isResultadoSimulado());
        }

        @Test
        @DisplayName("Definir a mesma seleção não altera o placar simulado")
        void mesmaSelecaoMantemSimulacao() {
            jogoMataMata.simular(Resultado.placar(1, 0));
            jogoMataMata.definirMandante(a);
            jogoMataMata.definirVisitante(b);
            assertNotNull(jogoMataMata.getResultado());
        }

        @Test
        @DisplayName("Seleções não mudam depois que a partida começa")
        void selecoesFixasAposInicio() {
            jogoMataMata.iniciar();
            Selecao c = grupo.getSelecoes().get(2);
            assertThrows(RegraNegocioException.class, () -> jogoMataMata.definirVisitante(c));
        }

        @Test
        @DisplayName("Uma seleção não pode enfrentar a si mesma")
        void mesmaSelecaoNosDoisLados() {
            assertThrows(RegraNegocioException.class, () -> jogoMataMata.definirVisitante(a));
        }
    }

    @Test
    @DisplayName("Data e estádio podem ser editados, mas não anulados")
    void edicao() {
        Estadio novo = Fixtures.estadio();
        jogoGrupo.setEstadio(novo);
        jogoGrupo.setDataHora(Fixtures.DATA.plusHours(3));
        assertSame(novo, jogoGrupo.getEstadio());
        assertEquals(Fixtures.DATA.plusHours(3), jogoGrupo.getDataHora());
        assertThrows(RegraNegocioException.class, () -> jogoGrupo.setEstadio(null));
        assertThrows(RegraNegocioException.class, () -> jogoGrupo.setDataHora(null));
        assertTrue(jogoGrupo.envolve(a));
        assertFalse(jogoGrupo.envolve(null));
    }
}
