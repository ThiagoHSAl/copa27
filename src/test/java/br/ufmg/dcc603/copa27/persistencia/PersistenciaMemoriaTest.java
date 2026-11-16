package br.ufmg.dcc603.copa27.persistencia;

import br.ufmg.dcc603.copa27.Ambiente;
import br.ufmg.dcc603.copa27.dominio.modelo.Administrador;
import br.ufmg.dcc603.copa27.dominio.modelo.Confederacao;
import br.ufmg.dcc603.copa27.dominio.modelo.Fase;
import br.ufmg.dcc603.copa27.dominio.modelo.Grupo;
import br.ufmg.dcc603.copa27.dominio.modelo.Partida;
import br.ufmg.dcc603.copa27.dominio.modelo.RegistroAlteracao;
import br.ufmg.dcc603.copa27.dominio.modelo.Resultado;
import br.ufmg.dcc603.copa27.dominio.modelo.Sede;
import br.ufmg.dcc603.copa27.dominio.modelo.Selecao;
import br.ufmg.dcc603.copa27.persistencia.memoria.BaseDeDadosMemoria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Repositórios em memória, transações (RNF04) e carga inicial. */
class PersistenciaMemoriaTest {

    private final Ambiente amb = new Ambiente();
    private final BaseDeDadosMemoria base = amb.base;

    @Test
    @DisplayName("Carga inicial: 32 seleções, 8 grupos de 4, 8 sedes e 64 partidas no período do torneio")
    void cargaInicial() {
        assertEquals(32, base.repositorioSelecoes().listar().size());
        List<Grupo> grupos = base.repositorioGrupos().listar();
        assertEquals(8, grupos.size());
        assertTrue(grupos.stream().allMatch(g -> g.getSelecoes().size() == 4));
        Set<Selecao> emGrupos = new HashSet<>();
        grupos.forEach(g -> emGrupos.addAll(g.getSelecoes()));
        assertEquals(32, emGrupos.size(), "RN01: cada seleção em exatamente um grupo");
        assertEquals(8, base.repositorioSedes().listar().size());
        List<Partida> partidas = base.repositorioPartidas().listar();
        assertEquals(64, partidas.size());
        assertEquals(48, partidas.stream().filter(p -> p.getFase() == Fase.GRUPOS).count());
        assertTrue(partidas.stream().allMatch(p -> !p.getDataHora().toLocalDate().isBefore(LocalDate.of(2027, 6, 24))
                && !p.getDataHora().toLocalDate().isAfter(LocalDate.of(2027, 7, 25))));
        for (Selecao s : emGrupos) {
            assertEquals(3, partidas.stream().filter(p -> p.envolve(s)).count(), s.getPais());
        }
        assertEquals(16, base.repositorioChaveamento().carregar().getConfrontos().size());
        assertNotNull(base.repositorioAdministradores().buscarPorLogin("admin"));
        assertNull(base.repositorioAdministradores().buscarPorLogin(null));
        assertNull(base.repositorioAdministradores().buscarPorLogin("ninguem"));
    }

    @Test
    @DisplayName("Na última rodada, os dois jogos de cada grupo são simultâneos")
    void ultimaRodadaSimultanea() {
        for (Grupo g : base.repositorioGrupos().listar()) {
            List<LocalDateTime> horarios = base.repositorioPartidas().buscarPorGrupo(g).stream()
                    .map(Partida::getDataHora).sorted().toList();
            assertEquals(6, horarios.size());
            assertEquals(horarios.get(4), horarios.get(5), "Grupo " + g.getLetra());
        }
    }

    @Test
    @DisplayName("Gravar com identificador 0 cria; gravar existente substitui; remover apaga")
    void crud() {
        RepositorioSelecoes rep = base.repositorioSelecoes();
        Selecao s = new Selecao(0, "Chile", "CHI", Confederacao.CONMEBOL);
        rep.salvar(s);
        assertEquals(33, s.getId());
        assertSame(s, rep.buscarPorId(33));
        rep.remover(33);
        assertNull(rep.buscarPorId(33));

        RepositorioGrupos grupos = base.repositorioGrupos();
        Grupo a = grupos.buscarPorId(1);
        grupos.remover(1);
        assertNull(grupos.buscarPorId(1));
        grupos.salvar(a);
        assertSame(a, grupos.buscarPorId(1));

        RepositorioSedes sedes = base.repositorioSedes();
        Sede bh = sedes.buscarPorId(1);
        sedes.remover(1);
        assertEquals(7, sedes.listar().size());
        sedes.salvar(bh);
        assertEquals(8, sedes.listar().size());
    }

    @Test
    @DisplayName("RNF04: falha dentro da transação restaura partidas, grupos, seleções, sedes e log")
    void rollback() {
        Selecao brasil = base.repositorioSelecoes().buscarPorId(1);
        String paisAntes = brasil.getPais();
        String cidadeAntes = base.repositorioSedes().buscarPorId(1).getCidade();
        Administrador admin = amb.admin;
        assertThrows(IllegalStateException.class, () -> base.emTransacao(() -> {
            Partida p = base.repositorioPartidas().buscarPorId(1);
            p.iniciar();
            p.registrarResultado(Resultado.placar(9, 0));
            base.repositorioSelecoes().salvar(new Selecao(1, "Alterado", "ALT", Confederacao.CAF));
            base.repositorioSelecoes().salvar(new Selecao(0, "Nova", "NOV", Confederacao.CAF));
            base.repositorioSelecoes().remover(2);
            Sede bh = base.repositorioSedes().buscarPorId(1);
            base.repositorioSedes().salvar(new Sede(1, "Outra", bh.getRegiao(), bh.getEstadio(), bh.getAeroportos()));
            base.repositorioSedes().remover(2);
            base.repositorioGrupos().buscarPorId(1).setClassificacao(null);
            base.repositorioChaveamento().carregar().buscarConfronto(1).getPartida()
                    .definirMandante(brasil);
            base.repositorioRegistros().registrar(new RegistroAlteracao(LocalDateTime.now(), "op", "dado", admin));
            throw new IllegalStateException("falha simulada");
        }));
        Partida p1 = base.repositorioPartidas().buscarPorId(1);
        assertNull(p1.getResultado());
        assertEquals(br.ufmg.dcc603.copa27.dominio.modelo.SituacaoPartida.AGENDADA, p1.getSituacao());
        assertSame(brasil, base.repositorioSelecoes().buscarPorId(1));
        assertEquals(paisAntes, brasil.getPais());
        assertNotNull(base.repositorioSelecoes().buscarPorId(2));
        assertEquals(32, base.repositorioSelecoes().listar().size());
        assertEquals(cidadeAntes, base.repositorioSedes().buscarPorId(1).getCidade());
        assertNotNull(base.repositorioSedes().buscarPorId(2));
        assertNull(base.repositorioChaveamento().carregar().buscarConfronto(1).getPartida().getMandante());
        assertTrue(base.repositorioRegistros().listar().isEmpty());
        assertSame(base.repositorioGrupos().buscarPorId(1), p1.getGrupo());
    }

    @Test
    @DisplayName("Transação bem-sucedida mantém as alterações; transações aninhadas usam a externa")
    void commitEAninhamento() {
        Integer r = base.emTransacao(() -> base.emTransacao(() -> {
            base.repositorioSelecoes().salvar(new Selecao(0, "Chile", "CHI", Confederacao.CONMEBOL));
            return 42;
        }));
        assertEquals(42, r);
        assertEquals(33, base.repositorioSelecoes().listar().size());
    }

    @Test
    @DisplayName("Rollback funciona também em base sem chaveamento")
    void rollbackSemChaveamento() {
        BaseDeDadosMemoria vazia = new BaseDeDadosMemoria();
        assertThrows(RuntimeException.class, () -> vazia.emTransacao(() -> {
            vazia.repositorioSelecoes().salvar(new Selecao(0, "X", "XXX", Confederacao.OFC));
            throw new RuntimeException();
        }));
        assertTrue(vazia.repositorioSelecoes().listar().isEmpty());
        assertNull(vazia.repositorioChaveamento().carregar());
    }
}
