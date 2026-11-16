package br.ufmg.dcc603.copa27.apresentacao;

import br.ufmg.dcc603.copa27.Ambiente;
import br.ufmg.dcc603.copa27.Fixtures;
import br.ufmg.dcc603.copa27.dominio.modelo.Cenario;
import br.ufmg.dcc603.copa27.dominio.modelo.Confederacao;
import br.ufmg.dcc603.copa27.dominio.modelo.Fase;
import br.ufmg.dcc603.copa27.dominio.modelo.Partida;
import br.ufmg.dcc603.copa27.dominio.modelo.Selecao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Componentes da camada de apresentação que não dependem de HTTP. */
class ApresentacaoTest {

    @Test
    @DisplayName("Cada rodada da fase de grupos tem 16 partidas, duas por grupo, e as fases do mata-mata têm as suas")
    void rodadas() {
        Cenario c = new Ambiente().simulacao.iniciar();
        for (int i = 0; i < 3; i++) {
            List<Partida> l = InterfacePublica.partidasDaRodada(c, InterfacePublica.RODADAS.get(i));
            assertEquals(16, l.size());
            assertTrue(l.stream().allMatch(p -> p.getFase() == Fase.GRUPOS));
            for (char g = 'A'; g <= 'H'; g++) {
                char letra = g;
                assertEquals(2, l.stream().filter(p -> p.getGrupo().getLetra() == letra).count());
            }
        }
        assertEquals(8, InterfacePublica.partidasDaRodada(c, InterfacePublica.RODADAS.get(3)).size());
        assertEquals(4, InterfacePublica.partidasDaRodada(c, InterfacePublica.RODADAS.get(4)).size());
        assertEquals(2, InterfacePublica.partidasDaRodada(c, InterfacePublica.RODADAS.get(5)).size());
        List<Partida> finais = InterfacePublica.partidasDaRodada(c, InterfacePublica.RODADAS.get(6));
        assertEquals(List.of(Fase.TERCEIRO_LUGAR, Fase.FINAL), finais.stream().map(Partida::getFase).toList());
    }

    @Test
    @DisplayName("Vagas abreviadas para a árvore compacta e para a árvore completa")
    void vagas() {
        assertEquals("1º A", InterfacePublica.vagaCurta("1º do Grupo A"));
        assertEquals("V. oit. 3", InterfacePublica.vagaCurta("Vencedora da oitava 3"));
        assertEquals("V. qua. 2", InterfacePublica.vagaCurta("Vencedora da quarta 2"));
        assertEquals("P. semi 1", InterfacePublica.vagaCurta("Perdedora da semifinal 1"));
        assertEquals("A definir", InterfacePublica.vagaCurta(null));
        assertEquals("Venc. semi 2", InterfacePublica.vagaMedia("Vencedora da semifinal 2"));
        assertEquals("Perd. semi 1", InterfacePublica.vagaMedia("Perdedora da semifinal 1"));
        assertEquals("A definir", InterfacePublica.vagaMedia(null));
    }

    @Test
    @DisplayName("Bandeira por código FIFA, com selo para seleções sem bandeira cadastrada")
    void bandeiras() {
        assertTrue(Bandeiras.img(new Selecao(1, "Brasil", "BRA", Confederacao.CONMEBOL)).contains("/static/bandeiras/br.svg"));
        assertTrue(Bandeiras.img(new Selecao(2, "Inglaterra", "ENG", Confederacao.UEFA)).contains("gb-eng.svg"));
        String semBandeira = Bandeiras.img(new Selecao(3, "Chile", "CHI", Confederacao.CONMEBOL));
        assertTrue(semBandeira.contains("sem-imagem") && semBandeira.contains("CHI"));
        assertTrue(Bandeiras.img(null, "p").contains("vazia"));
    }

    @Test
    @DisplayName("Datas por extenso em português e escape de HTML")
    void textos() {
        assertEquals("Quinta-feira, 24 de junho", Html.dataPorExtenso(LocalDate.of(2027, 6, 24)));
        assertEquals("DOM", Html.diaCurto(LocalDate.of(2027, 7, 25)));
        assertEquals("&lt;a href=&quot;x&quot;&gt;&amp;&#39;", Html.esc("<a href=\"x\">&'"));
        assertEquals("", Html.esc(null));
        Partida p = Fixtures.jogoMataMata(49, Fase.OITAVAS, null, null);
        assertTrue(Html.cartaoJogo(p, null).contains("A definir"));
        assertEquals("Oitavas de final", Html.fase(p));
    }
}
