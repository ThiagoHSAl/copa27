package br.ufmg.dcc603.copa27.dominio.modelo;

import br.ufmg.dcc603.copa27.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Seleção, sede, estádio, aeroporto e demais classes de consulta. */
class SelecaoSedeTest {

    @Test
    @DisplayName("Seleção exige país, código FIFA de três letras e confederação")
    void validacaoSelecao() {
        assertThrows(RegraNegocioException.class, () -> new Selecao(0, " ", "BRA", Confederacao.CONMEBOL));
        assertThrows(RegraNegocioException.class, () -> new Selecao(0, "Brasil", "BR", Confederacao.CONMEBOL));
        assertThrows(RegraNegocioException.class, () -> new Selecao(0, "Brasil", "bra", Confederacao.CONMEBOL));
        assertThrows(RegraNegocioException.class, () -> new Selecao(0, "Brasil", "BRA", null));
        Selecao s = new Selecao(7, " Brasil ", "BRA", Confederacao.CONMEBOL);
        assertEquals("Brasil", s.getPais());
        assertEquals("Brasil (BRA)", s.toString());
    }

    @Test
    @DisplayName("atualizarCom copia os dados mantendo a identidade; copiar é independente")
    void copiaEAtualizacao() {
        Selecao s = new Selecao(1, "Brasil", "BRA", Confederacao.CONMEBOL);
        s.getElenco().add(new Jogadora("Marta", 10, "Atacante", "Orlando Pride"));
        s.getComissao().add(new MembroComissao("Técnico Exemplo", "Treinador"));
        s.getHistorico().add(new ParticipacaoCopa(2007, "Vice-campeã"));
        Selecao copia = s.copiar();
        assertNotSame(s, copia);
        assertEquals(1, copia.getElenco().size());
        Selecao nova = new Selecao(1, "Brasil", "BRZ", Confederacao.CONMEBOL);
        s.atualizarCom(nova);
        assertEquals("BRZ", s.getCodigoFifa());
        assertTrue(s.getElenco().isEmpty());
        assertEquals("BRA", copia.getCodigoFifa());
    }

    @Test
    @DisplayName("RNF17: aeroportos e hotéis são ordenados pela distância até o estádio")
    void ordenacaoPorDistancia() {
        Sede s = new Sede(1, "Rio de Janeiro", Regiao.SUDESTE, Fixtures.estadio(),
                List.of(new Aeroporto("Galeão", "GIG", 15), new Aeroporto("Santos Dumont", "SDU", 10)));
        assertEquals("SDU", s.getAeroportos().get(0).codigoIata());
        s.adicionarHotel(new Hotel("Longe", 4, FaixaPreco.ALTA, 8));
        s.adicionarHotel(new Hotel("Perto", 3, FaixaPreco.ECONOMICA, 1));
        assertEquals("Perto", s.getHoteis().get(0).nome());
    }

    @Test
    @DisplayName("Sede exige cidade, região, estádio e ao menos um aeroporto")
    void validacaoSede() {
        Estadio e = Fixtures.estadio();
        List<Aeroporto> aer = List.of(new Aeroporto("A", "AAA", 1));
        assertThrows(RegraNegocioException.class, () -> new Sede(0, "", Regiao.SUL, e, aer));
        assertThrows(RegraNegocioException.class, () -> new Sede(0, "X", null, e, aer));
        assertThrows(RegraNegocioException.class, () -> new Sede(0, "X", Regiao.SUL, null, aer));
        assertThrows(RegraNegocioException.class, () -> new Sede(0, "X", Regiao.SUL, e, List.of()));
        Sede s = new Sede(0, "X", Regiao.SUL, e, aer);
        assertSame(s, e.getSede());
        assertEquals("X", s.toString());
    }

    @Test
    @DisplayName("Editar a sede preserva o objeto do estádio, referenciado pelas partidas")
    void atualizarSede() {
        Sede s = Fixtures.sede(1, "Recife");
        Estadio original = s.getEstadio();
        Sede editada = new Sede(1, "Recife", Regiao.NORDESTE,
                new Estadio("Arena Nova", 46_000, 2013, "Av. Nova, 1"), List.of(new Aeroporto("Guararapes", "REC", 25)));
        s.atualizarCom(editada);
        assertSame(original, s.getEstadio());
        assertEquals("Arena Nova", s.getEstadio().getNome());
        assertEquals(46_000, s.getEstadio().getCapacidade());
        assertEquals(Regiao.NORDESTE, s.getRegiao());
        Sede copia = s.copiar();
        assertNotSame(s.getEstadio(), copia.getEstadio());
        assertEquals("Arena Nova", copia.getEstadio().toString());
    }

    @Test
    @DisplayName("Estádio e aeroporto validam seus dados")
    void validacaoEstadioAeroporto() {
        assertThrows(RegraNegocioException.class, () -> new Estadio("", 1, 2000, "R"));
        assertThrows(RegraNegocioException.class, () -> new Estadio("E", 0, 2000, "R"));
        assertThrows(RegraNegocioException.class, () -> new Estadio("E", 1, 1700, "R"));
        assertThrows(RegraNegocioException.class, () -> new Estadio("E", 1, 2000, " "));
        assertThrows(RegraNegocioException.class, () -> new Aeroporto(" ", "AAA", 1));
        assertThrows(RegraNegocioException.class, () -> new Aeroporto("A", "aa", 1));
        assertThrows(RegraNegocioException.class, () -> new Aeroporto("A", "AAA", -1));
    }

    @Test
    @DisplayName("Administrador e registro de alteração guardam seus dados")
    void administradorERegistro() {
        assertThrows(RegraNegocioException.class, () -> new Administrador("", "h"));
        assertThrows(RegraNegocioException.class, () -> new Administrador("admin", null));
        Administrador a = new Administrador("admin", "hash");
        RegistroAlteracao r = new RegistroAlteracao(LocalDateTime.of(2027, 6, 24, 10, 0), "op", "dado", a);
        assertSame(a, r.getAutor());
        assertEquals("op", r.getOperacao());
        assertEquals("dado", r.getDado());
        assertEquals(2027, r.getDataHora().getYear());
    }

    @Test
    @DisplayName("Enumerações têm descrições para a interface")
    void descricoes() {
        assertEquals("Oitavas de final", Fase.OITAVAS.getDescricao());
        assertTrue(Fase.FINAL.isMataMata());
        assertFalse(Fase.GRUPOS.isMataMata());
        assertEquals("Em andamento", SituacaoPartida.EM_ANDAMENTO.getDescricao());
        assertEquals("Centro-Oeste", Regiao.CENTRO_OESTE.getDescricao());
    }
}
