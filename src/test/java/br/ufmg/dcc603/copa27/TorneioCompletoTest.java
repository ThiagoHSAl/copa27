package br.ufmg.dcc603.copa27;

import br.ufmg.dcc603.copa27.dominio.modelo.Chaveamento;
import br.ufmg.dcc603.copa27.dominio.modelo.Confronto;
import br.ufmg.dcc603.copa27.dominio.modelo.Fase;
import br.ufmg.dcc603.copa27.dominio.modelo.Partida;
import br.ufmg.dcc603.copa27.dominio.modelo.Resultado;
import br.ufmg.dcc603.copa27.dominio.modelo.Selecao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste de integração: disputa as 64 partidas pelo serviço de administração,
 * atravessando todas as camadas, e confere o torneio do início ao fim.
 */
class TorneioCompletoTest {

    @Test
    @DisplayName("As 64 partidas registradas levam a uma campeã, com o chaveamento coerente")
    void torneioCompleto() {
        Ambiente amb = new Ambiente();
        Random aleatorio = new Random(42);
        amb.encerrarFaseDeGrupos(id -> Resultado.placar(aleatorio.nextInt(4), aleatorio.nextInt(4)));

        Chaveamento ch = amb.competicao.obterChaveamento();
        Set<Selecao> classificadas = new HashSet<>();
        for (Confronto c : ch.confrontosDaFase(Fase.OITAVAS)) {
            classificadas.add(c.getPartida().getMandante());
            classificadas.add(c.getPartida().getVisitante());
        }
        assertEquals(16, classificadas.size(), "16 seleções distintas nas oitavas");
        amb.competicao.obterGrupos().forEach(g -> {
            assertTrue(classificadas.contains(g.getClassificacao().primeira()));
            assertTrue(classificadas.contains(g.getClassificacao().segunda()));
        });

        for (Fase f : List.of(Fase.OITAVAS, Fase.QUARTAS, Fase.SEMIFINAL, Fase.TERCEIRO_LUGAR, Fase.FINAL)) {
            for (Confronto c : amb.competicao.obterChaveamento().confrontosDaFase(f)) {
                int g = aleatorio.nextInt(3);
                Resultado r = aleatorio.nextBoolean() ? Resultado.placar(g + 1, g) : Resultado.comPenaltis(g, 4, 5);
                amb.jogar(c.getPartida().getId(), r);
            }
        }

        assertTrue(amb.consulta.listarPartidas(null).stream().allMatch(Partida::estaDisputada));
        Partida fin = amb.competicao.obterChaveamento().buscarConfronto(16).getPartida();
        Selecao campea = fin.vencedora();
        assertNotNull(campea);
        assertTrue(classificadas.contains(campea));
        List<Confronto> caminho = amb.competicao.obterChaveamento().caminhoAteFinal(campea);
        assertEquals(4, caminho.size(), "a campeã joga oitavas, quartas, semifinal e final");
        assertTrue(caminho.stream().allMatch(c -> c.getPartida().vencedora() == campea));
        Partida terceiro = amb.competicao.obterChaveamento().buscarConfronto(15).getPartida();
        assertNotNull(terceiro.vencedora());
        assertFalse(terceiro.envolve(campea));
        // 64 partidas: cada uma gera "iniciar" e "registrar resultado" no log.
        assertEquals(128, amb.auditoria.listar().size());
    }
}
