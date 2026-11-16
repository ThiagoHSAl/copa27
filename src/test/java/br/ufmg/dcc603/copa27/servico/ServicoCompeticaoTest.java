package br.ufmg.dcc603.copa27.servico;

import br.ufmg.dcc603.copa27.Ambiente;
import br.ufmg.dcc603.copa27.dominio.modelo.Classificacao;
import br.ufmg.dcc603.copa27.dominio.modelo.Grupo;
import br.ufmg.dcc603.copa27.dominio.modelo.Partida;
import br.ufmg.dcc603.copa27.dominio.modelo.Resultado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** CU04 e CU05 no cenário real. */
class ServicoCompeticaoTest {

    private final Ambiente amb = new Ambiente();

    @Test
    @DisplayName("Oito grupos de A a H, com classificações zeradas antes dos jogos")
    void inicial() {
        List<Grupo> grupos = amb.competicao.obterGrupos();
        assertEquals(8, grupos.size());
        assertEquals('A', grupos.get(0).getLetra());
        assertEquals('H', grupos.get(7).getLetra());
        List<Classificacao> cs = amb.competicao.obterClassificacoes();
        assertEquals(8, cs.size());
        assertTrue(cs.stream().allMatch(c -> c.getLinhas().stream().allMatch(l -> l.getJogos() == 0)));
    }

    @Test
    @DisplayName("RF08: a classificação reflete o resultado registrado")
    void reflete() {
        Partida p = amb.consulta.buscarPartida(1);
        amb.jogar(1, Resultado.placar(3, 1));
        Classificacao a = amb.competicao.obterClassificacoes().get(0);
        assertSame(p.getMandante(), a.primeira());
        assertEquals(3, a.getLinhas().get(0).pontos());
        assertEquals(2, a.getLinhas().get(0).saldo());
    }

    @Test
    @DisplayName("RF07: o chaveamento real tem 16 confrontos, ainda sem seleções")
    void chaveamento() {
        assertEquals(16, amb.competicao.obterChaveamento().getConfrontos().size());
        assertNull(amb.competicao.obterChaveamento().buscarConfronto(1).getPartida().getMandante());
    }
}
