package br.ufmg.dcc603.copa27.dominio.regras;

import br.ufmg.dcc603.copa27.Fixtures;
import br.ufmg.dcc603.copa27.dominio.modelo.Chaveamento;
import br.ufmg.dcc603.copa27.dominio.modelo.Classificacao;
import br.ufmg.dcc603.copa27.dominio.modelo.Confronto;
import br.ufmg.dcc603.copa27.dominio.modelo.Fase;
import br.ufmg.dcc603.copa27.dominio.modelo.Grupo;
import br.ufmg.dcc603.copa27.dominio.modelo.LinhaClassificacao;
import br.ufmg.dcc603.copa27.dominio.modelo.Partida;
import br.ufmg.dcc603.copa27.dominio.modelo.RegraNegocioException;
import br.ufmg.dcc603.copa27.dominio.modelo.Resultado;
import br.ufmg.dcc603.copa27.dominio.modelo.Selecao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** RN04 e CU21: montagem da árvore, preenchimento das oitavas e avanço das vencedoras. */
class GeradorChaveamentoTest {

    private final GeradorChaveamento gerador = new GeradorChaveamento();
    private Chaveamento ch;
    private List<Grupo> grupos;

    @BeforeEach
    void preparar() {
        ch = gerador.montarChaveamento(Fixtures.partidasMataMata());
        grupos = new ArrayList<>();
        for (int g = 0; g < 8; g++) {
            Grupo grupo = Fixtures.grupo((char) ('A' + g), g * 4 + 1);
            definirClassificacao(grupo, true);
            grupos.add(grupo);
        }
    }

    /** Classificação na ordem em que as seleções foram incluídas no grupo. */
    private static void definirClassificacao(Grupo g, boolean definitiva) {
        g.setClassificacao(new Classificacao(g.getSelecoes().stream().map(LinhaClassificacao::new).toList(), definitiva));
    }

    private Grupo grupo(char letra) {
        return grupos.get(letra - 'A');
    }

    private Partida partidaDoConfronto(int numero) {
        return ch.buscarConfronto(numero).getPartida();
    }

    @Test
    @DisplayName("Monta 16 confrontos com as vagas descritas")
    void montagem() {
        assertEquals(16, ch.getConfrontos().size());
        assertEquals("1º do Grupo A", ch.buscarConfronto(1).getVagaMandante());
        assertEquals("2º do Grupo C", ch.buscarConfronto(1).getVagaVisitante());
        assertEquals("1º do Grupo H", ch.buscarConfronto(8).getVagaMandante());
        assertEquals("Vencedora da oitava 1", ch.buscarConfronto(9).getVagaMandante());
        assertEquals("Vencedora da oitava 3", ch.buscarConfronto(9).getVagaVisitante());
        assertEquals("Vencedora da quarta 3", ch.buscarConfronto(14).getVagaMandante());
        assertEquals("Perdedora da semifinal 1", ch.buscarConfronto(15).getVagaMandante());
        assertEquals("Vencedora da semifinal 2", ch.buscarConfronto(16).getVagaVisitante());
    }

    @Test
    @DisplayName("Liga cada confronto ao seguinte, na posição correta")
    void ligacoes() {
        assertSame(ch.buscarConfronto(9), ch.buscarConfronto(1).getProximo());
        assertTrue(ch.buscarConfronto(1).isVencedoraEntraComoMandante());
        assertSame(ch.buscarConfronto(9), ch.buscarConfronto(3).getProximo());
        assertFalse(ch.buscarConfronto(3).isVencedoraEntraComoMandante());
        assertSame(ch.buscarConfronto(12), ch.buscarConfronto(8).getProximo());
        assertSame(ch.buscarConfronto(13), ch.buscarConfronto(10).getProximo());
        assertSame(ch.buscarConfronto(16), ch.buscarConfronto(14).getProximo());
        assertNull(ch.buscarConfronto(16).getProximo());
        assertNull(ch.buscarConfronto(15).getProximo());
    }

    @Test
    @DisplayName("Recusa montar a árvore com número errado de partidas")
    void partidasInsuficientes() {
        List<Partida> l = new ArrayList<>(Fixtures.partidasMataMata());
        l.remove(0);
        assertThrows(RegraNegocioException.class, () -> gerador.montarChaveamento(l));
    }

    @Test
    @DisplayName("A fase de grupos só termina quando todas as suas partidas estão encerradas")
    void faseDeGruposEncerrada() {
        Grupo g = grupo('A');
        List<Partida> jogos = Fixtures.jogosDoGrupo(g, 1);
        assertFalse(gerador.faseDeGruposEncerrada(jogos));
        for (int i = 0; i < 5; i++) {
            Fixtures.encerrar(jogos.get(i), Resultado.placar(1, 0));
        }
        assertFalse(gerador.faseDeGruposEncerrada(jogos));
        Fixtures.encerrar(jogos.get(5), Resultado.placar(0, 0));
        List<Partida> comMataMata = new ArrayList<>(jogos);
        comMataMata.addAll(Fixtures.partidasMataMata());
        assertTrue(gerador.faseDeGruposEncerrada(comMataMata));
        assertFalse(gerador.faseDeGruposEncerrada(Fixtures.partidasMataMata()));
    }

    @Test
    @DisplayName("RN04: as duas primeiras de cada grupo preenchem as oitavas conforme o cruzamento")
    void preencherOitavas() {
        gerador.preencherOitavas(ch, grupos);
        assertSame(grupo('A').getClassificacao().primeira(), partidaDoConfronto(1).getMandante());
        assertSame(grupo('C').getClassificacao().segunda(), partidaDoConfronto(1).getVisitante());
        assertSame(grupo('C').getClassificacao().primeira(), partidaDoConfronto(2).getMandante());
        assertSame(grupo('A').getClassificacao().segunda(), partidaDoConfronto(2).getVisitante());
        assertSame(grupo('H').getClassificacao().primeira(), partidaDoConfronto(8).getMandante());
        assertSame(grupo('F').getClassificacao().segunda(), partidaDoConfronto(8).getVisitante());
        long preenchidas = ch.confrontosDaFase(Fase.OITAVAS).stream()
                .filter(c -> c.getPartida().getMandante() != null && c.getPartida().getVisitante() != null).count();
        assertEquals(8, preenchidas);
    }

    @Test
    @DisplayName("CU07, A1: grupo sem classificação definitiva deixa suas vagas indefinidas")
    void grupoIndefinido() {
        definirClassificacao(grupo('A'), false);
        gerador.preencherOitavas(ch, grupos);
        assertNull(partidaDoConfronto(1).getMandante());
        assertNull(partidaDoConfronto(2).getVisitante());
        assertNotNull(partidaDoConfronto(1).getVisitante());
    }

    @Test
    @DisplayName("Recusa preencher se faltar um grupo")
    void grupoAusente() {
        grupos.remove(7);
        assertThrows(RegraNegocioException.class, () -> gerador.preencherOitavas(ch, grupos));
    }

    @Test
    @DisplayName("A vencedora avança ao confronto seguinte na posição correta")
    void avancarVencedora() {
        gerador.preencherOitavas(ch, grupos);
        Partida o1 = partidaDoConfronto(1);
        Partida o3 = partidaDoConfronto(3);
        Fixtures.encerrar(o1, Resultado.placar(2, 0));
        Fixtures.encerrar(o3, Resultado.comPenaltis(1, 3, 4));
        gerador.avancarVencedora(ch, o1);
        gerador.avancarVencedora(ch, o3);
        Partida q1 = partidaDoConfronto(9);
        assertSame(o1.getMandante(), q1.getMandante());
        assertSame(o3.getVisitante(), q1.getVisitante());
    }

    @Test
    @DisplayName("Sem vencedora, ou partida fora do chaveamento, nada avança")
    void semVencedora() {
        gerador.preencherOitavas(ch, grupos);
        Partida o1 = partidaDoConfronto(1);
        gerador.avancarVencedora(ch, o1);
        assertNull(partidaDoConfronto(9).getMandante());
        Partida avulsa = Fixtures.jogoMataMata(99, Fase.OITAVAS, Fixtures.selecao(1, "X"), Fixtures.selecao(2, "Y"));
        avulsa.simular(Resultado.placar(1, 0));
        assertDoesNotThrow(() -> gerador.avancarVencedora(ch, avulsa));
    }

    @Test
    @DisplayName("Na semifinal, a vencedora vai à final e a perdedora à disputa de terceiro lugar")
    void semifinal() {
        Selecao a = Fixtures.selecao(1, "A");
        Selecao b = Fixtures.selecao(2, "B");
        Selecao c = Fixtures.selecao(3, "C");
        Selecao d = Fixtures.selecao(4, "D");
        Partida s1 = partidaDoConfronto(13);
        Partida s2 = partidaDoConfronto(14);
        s1.definirMandante(a);
        s1.definirVisitante(b);
        s2.definirMandante(c);
        s2.definirVisitante(d);
        Fixtures.encerrar(s1, Resultado.placar(0, 1));
        Fixtures.encerrar(s2, Resultado.placar(3, 2));
        gerador.avancarVencedora(ch, s1);
        gerador.avancarVencedora(ch, s2);
        Partida fin = partidaDoConfronto(16);
        Partida ter = partidaDoConfronto(15);
        assertSame(b, fin.getMandante());
        assertSame(c, fin.getVisitante());
        assertSame(a, ter.getMandante());
        assertSame(d, ter.getVisitante());
    }

    @Test
    @DisplayName("CU07: a projeção propaga as vencedoras simuladas até a final")
    void projetarAteFinal() {
        gerador.preencherOitavas(ch, grupos);
        for (Fase f : List.of(Fase.OITAVAS, Fase.QUARTAS, Fase.SEMIFINAL, Fase.FINAL)) {
            for (Confronto c : ch.confrontosDaFase(f)) {
                c.getPartida().simular(Resultado.placar(1, 0));
            }
            gerador.projetar(ch, grupos);
        }
        Selecao campea = partidaDoConfronto(16).vencedora();
        assertSame(grupo('A').getClassificacao().primeira(), campea);
        assertNotNull(partidaDoConfronto(15).getMandante());
        assertNotNull(partidaDoConfronto(15).getVisitante());
    }

    @Test
    @DisplayName("Alterar um placar simulado libera as vagas seguintes e descarta projeções dependentes")
    void alteracaoLiberaVagas() {
        gerador.preencherOitavas(ch, grupos);
        for (Fase f : List.of(Fase.OITAVAS, Fase.QUARTAS, Fase.SEMIFINAL)) {
            for (Confronto c : ch.confrontosDaFase(f)) {
                c.getPartida().simular(Resultado.placar(1, 0));
            }
            gerador.projetar(ch, grupos);
        }
        Selecao finalistaAntes = partidaDoConfronto(16).getMandante();
        assertNotNull(finalistaAntes);
        // A oitava 1 passa a ser vencida pela visitante: a quarta 1 muda de participante,
        // seu placar simulado é descartado e a vaga na semifinal e na final volta a ficar em aberto.
        partidaDoConfronto(1).simular(Resultado.placar(0, 1));
        gerador.projetar(ch, grupos);
        assertSame(partidaDoConfronto(1).getVisitante(), partidaDoConfronto(9).getMandante());
        assertNull(partidaDoConfronto(9).getResultado());
        assertNull(partidaDoConfronto(13).getMandante());
        assertNull(partidaDoConfronto(16).getMandante());
        assertNull(partidaDoConfronto(15).getMandante());
    }
}
