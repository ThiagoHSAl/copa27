package br.ufmg.dcc603.copa27.servico;

import br.ufmg.dcc603.copa27.Ambiente;
import br.ufmg.dcc603.copa27.dominio.modelo.Aeroporto;
import br.ufmg.dcc603.copa27.dominio.modelo.Chaveamento;
import br.ufmg.dcc603.copa27.dominio.modelo.Confederacao;
import br.ufmg.dcc603.copa27.dominio.modelo.Confronto;
import br.ufmg.dcc603.copa27.dominio.modelo.Estadio;
import br.ufmg.dcc603.copa27.dominio.modelo.Fase;
import br.ufmg.dcc603.copa27.dominio.modelo.Grupo;
import br.ufmg.dcc603.copa27.dominio.modelo.Partida;
import br.ufmg.dcc603.copa27.dominio.modelo.RegistroAlteracao;
import br.ufmg.dcc603.copa27.dominio.modelo.RegraNegocioException;
import br.ufmg.dcc603.copa27.dominio.modelo.Regiao;
import br.ufmg.dcc603.copa27.dominio.modelo.Resultado;
import br.ufmg.dcc603.copa27.dominio.modelo.Sede;
import br.ufmg.dcc603.copa27.dominio.modelo.Selecao;
import br.ufmg.dcc603.copa27.dominio.modelo.SituacaoPartida;
import br.ufmg.dcc603.copa27.persistencia.RepositorioRegistros;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServicoAdministracaoTest {

    private Ambiente amb;
    private ServicoAdministracao adm;

    @BeforeEach
    void preparar() {
        amb = new Ambiente();
        adm = amb.administracao;
    }

    private Partida partida(int id) {
        return amb.consulta.buscarPartida(id);
    }

    private Chaveamento chaveamento() {
        return amb.competicao.obterChaveamento();
    }

    @Test
    @DisplayName("RNF09: toda operação exige administrador autenticado")
    void exigeAutor() {
        assertThrows(AutenticacaoException.class, () -> adm.iniciarPartida(1, null));
        assertThrows(AutenticacaoException.class, () -> adm.registrarResultado(1, Resultado.placar(1, 0), true, null));
        assertThrows(AutenticacaoException.class, () -> adm.remover(TipoDado.SEDE, 1, null));
        assertThrows(AutenticacaoException.class,
                () -> adm.salvar(new Selecao(0, "X", "XXX", Confederacao.OFC), null));
        assertEquals(SituacaoPartida.AGENDADA, partida(1).getSituacao());
    }

    @Nested
    @DisplayName("CU17, Registrar resultado de partida")
    class RegistrarResultado {

        @Test
        @DisplayName("RF25 e RN06: partida agendada recusa o resultado e nada é gravado")
        void agendadaRecusa() {
            assertThrows(RegraNegocioException.class, () -> adm.registrarResultado(1, Resultado.placar(1, 0), false, amb.admin));
            assertNull(partida(1).getResultado());
            assertTrue(amb.auditoria.listar().isEmpty());
        }

        @Test
        @DisplayName("Partida inexistente é informada")
        void inexistente() {
            assertThrows(NaoEncontradoException.class, () -> adm.registrarResultado(99, Resultado.placar(1, 0), false, amb.admin));
            assertThrows(NaoEncontradoException.class, () -> adm.iniciarPartida(99, amb.admin));
        }

        @Test
        @DisplayName("RF24 e RF08: registrar e encerrar atualiza a classificação e grava o log")
        void fluxoPrincipal() {
            adm.iniciarPartida(1, amb.admin);
            assertEquals(SituacaoPartida.EM_ANDAMENTO, partida(1).getSituacao());
            adm.registrarResultado(1, Resultado.placar(1, 1), false, amb.admin);
            assertEquals(SituacaoPartida.EM_ANDAMENTO, partida(1).getSituacao());
            adm.registrarResultado(1, Resultado.placar(2, 1), true, amb.admin);
            Partida p = partida(1);
            assertEquals(SituacaoPartida.ENCERRADA, p.getSituacao());
            Grupo a = amb.competicao.obterGrupos().get(0);
            assertSame(p.getMandante(), a.getClassificacao().primeira());
            assertEquals(3, a.getClassificacao().getLinhas().get(0).pontos());
            List<RegistroAlteracao> log = amb.auditoria.listar();
            assertEquals(3, log.size());
            assertEquals("iniciar partida", log.get(0).getOperacao());
            assertEquals("registrar resultado", log.get(2).getOperacao());
            assertSame(amb.admin, log.get(2).getAutor());
        }

        @Test
        @DisplayName("Resultado corrigido após o encerramento recalcula a classificação")
        void correcao() {
            amb.jogar(1, Resultado.placar(2, 0));
            adm.registrarResultado(1, Resultado.placar(0, 2), false, amb.admin);
            Grupo a = amb.competicao.obterGrupos().get(0);
            assertSame(partida(1).getVisitante(), a.getClassificacao().primeira());
        }

        @Test
        @DisplayName("RF09 e CU21: a última partida da fase de grupos preenche as oitavas")
        void preencheOitavas() {
            amb.encerrarFaseDeGrupos(id -> id == 48 ? null : Resultado.placar(1, 0));
            assertNull(chaveamento().buscarConfronto(1).getPartida().getMandante());
        }

        @Test
        @DisplayName("RF09 e CU21: com a fase de grupos encerrada, as 16 vagas são definidas")
        void oitavasDefinidas() {
            amb.encerrarFaseDeGrupos();
            for (Confronto c : chaveamento().confrontosDaFase(Fase.OITAVAS)) {
                assertNotNull(c.getPartida().getMandante());
                assertNotNull(c.getPartida().getVisitante());
            }
            Grupo a = amb.competicao.obterGrupos().get(0);
            Grupo c = amb.competicao.obterGrupos().get(2);
            Partida oitava1 = chaveamento().buscarConfronto(1).getPartida();
            assertSame(a.getClassificacao().primeira(), oitava1.getMandante());
            assertSame(c.getClassificacao().segunda(), oitava1.getVisitante());
            assertTrue(a.getClassificacao().isDefinitiva());
        }

        @Test
        @DisplayName("RN05 e RNF04: mata-mata empatado não encerra e a transação é desfeita")
        void mataMataEmpatado() {
            amb.encerrarFaseDeGrupos();
            int id = chaveamento().buscarConfronto(1).getPartida().getId();
            adm.iniciarPartida(id, amb.admin);
            int registrosAntes = amb.auditoria.listar().size();
            assertThrows(RegraNegocioException.class, () -> adm.registrarResultado(id, Resultado.placar(1, 1), true, amb.admin));
            Partida p = partida(id);
            assertNull(p.getResultado(), "o placar registrado antes da falha foi desfeito");
            assertEquals(SituacaoPartida.EM_ANDAMENTO, p.getSituacao());
            assertEquals(registrosAntes, amb.auditoria.listar().size());
        }

        @Test
        @DisplayName("CU17, A1 e A2: decisão nos pênaltis leva a vencedora às quartas")
        void avancaVencedora() {
            amb.encerrarFaseDeGrupos();
            Partida oitava1 = chaveamento().buscarConfronto(1).getPartida();
            amb.jogar(oitava1.getId(), Resultado.comPenaltis(0, 3, 5));
            Partida quarta1 = chaveamento().buscarConfronto(9).getPartida();
            assertSame(partida(oitava1.getId()).getVisitante(), quarta1.getMandante());
            assertNull(quarta1.getVisitante());
        }

        @Test
        @DisplayName("Partida de mata-mata sem seleções não pode ser iniciada")
        void mataMataSemSelecoes() {
            assertThrows(RegraNegocioException.class, () -> adm.iniciarPartida(49, amb.admin));
            assertEquals(SituacaoPartida.AGENDADA, partida(49).getSituacao());
        }

        @Test
        @DisplayName("RNF04: falha na gravação do log desfaz o resultado, a classificação e o chaveamento")
        void falhaNoLogDesfazTudo() {
            RepositorioRegistros falha = new RepositorioRegistros() {
                @Override
                public void registrar(RegistroAlteracao r) {
                    if (r.getOperacao().equals("registrar resultado") && r.getDado().startsWith("Partida 48")) {
                        throw new IllegalStateException("disco cheio");
                    }
                }

                @Override
                public List<RegistroAlteracao> listar() {
                    return List.of();
                }
            };
            Ambiente comFalha = new Ambiente(falha);
            comFalha.encerrarFaseDeGrupos(id -> id == 48 ? null : Resultado.placar(1, 0));
            comFalha.administracao.iniciarPartida(48, comFalha.admin);
            assertThrows(IllegalStateException.class,
                    () -> comFalha.administracao.registrarResultado(48, Resultado.placar(3, 0), true, comFalha.admin));
            Partida p48 = comFalha.consulta.buscarPartida(48);
            assertNull(p48.getResultado());
            assertEquals(SituacaoPartida.EM_ANDAMENTO, p48.getSituacao());
            Grupo h = comFalha.competicao.obterGrupos().get(7);
            assertFalse(h.getClassificacao().isDefinitiva());
            assertNull(comFalha.competicao.obterChaveamento().buscarConfronto(1).getPartida().getMandante());
        }
    }

    @Nested
    @DisplayName("CU16, Manter dados do torneio")
    class ManterDados {

        @Test
        @DisplayName("Cadastra uma seleção nova, que recebe identificador")
        void cadastrarSelecao() {
            Selecao s = new Selecao(0, "Chile", "CHI", Confederacao.CONMEBOL);
            adm.salvar(s, amb.admin);
            assertEquals(33, s.getId());
            assertEquals("Chile", amb.consulta.buscarSelecao(33).getPais());
            assertEquals("cadastrar seleção", amb.auditoria.listar().get(0).getOperacao());
        }

        @Test
        @DisplayName("Editar uma seleção mantém o objeto referenciado por grupos e partidas")
        void editarSelecao() {
            Selecao brasil = amb.consulta.listarSelecoes().stream()
                    .filter(s -> s.getCodigoFifa().equals("BRA")).findFirst().orElseThrow();
            adm.salvar(new Selecao(brasil.getId(), "Brasil (sede)", "BRA", Confederacao.CONMEBOL), amb.admin);
            assertEquals("Brasil (sede)", partida(1).getMandante().getPais());
            assertSame(brasil, amb.consulta.buscarSelecao(brasil.getId()));
            assertEquals("editar seleção", amb.auditoria.listar().get(0).getOperacao());
        }

        @Test
        @DisplayName("CU16, E1: código FIFA repetido ou seleção inexistente são recusados")
        void selecaoInvalida() {
            assertThrows(RegraNegocioException.class,
                    () -> adm.salvar(new Selecao(0, "Outro Brasil", "BRA", Confederacao.CONMEBOL), amb.admin));
            assertThrows(NaoEncontradoException.class,
                    () -> adm.salvar(new Selecao(500, "Fantasma", "FAN", Confederacao.CAF), amb.admin));
            assertThrows(RegraNegocioException.class, () -> adm.salvar((Selecao) null, amb.admin));
            assertEquals(32, amb.consulta.listarSelecoes().size());
        }

        @Test
        @DisplayName("CU16, E3: seleção em grupo ou partida não pode ser removida; seleção livre pode")
        void removerSelecao() {
            assertThrows(RegraNegocioException.class, () -> adm.remover(TipoDado.SELECAO, 1, amb.admin));
            Selecao s = new Selecao(0, "Chile", "CHI", Confederacao.CONMEBOL);
            adm.salvar(s, amb.admin);
            adm.remover(TipoDado.SELECAO, s.getId(), amb.admin);
            assertThrows(NaoEncontradoException.class, () -> amb.consulta.buscarSelecao(s.getId()));
            assertThrows(NaoEncontradoException.class, () -> adm.remover(TipoDado.SELECAO, s.getId(), amb.admin));
            assertEquals("remover selecao", amb.auditoria.listar().get(1).getOperacao());
        }

        @Test
        @DisplayName("CU16, E3: sede com partidas não pode ser removida; sede nova pode")
        void sedes() {
            Sede bh = amb.consulta.listarSedes().get(0);
            assertThrows(RegraNegocioException.class, () -> adm.remover(TipoDado.SEDE, bh.getId(), amb.admin));
            Sede manaus = new Sede(0, "Manaus", Regiao.NORTE, new Estadio("Arena da Amazônia", 44_000, 2014, "Av. Constantino Nery"),
                    List.of(new Aeroporto("Aeroporto Eduardo Gomes", "MAO", 16)));
            adm.salvar(manaus, amb.admin);
            assertEquals(9, amb.consulta.listarSedes().size());
            assertThrows(RegraNegocioException.class, () -> adm.salvar(new Sede(0, "manaus", Regiao.NORTE,
                    new Estadio("Outro", 1, 2000, "R"), List.of(new Aeroporto("A", "AAA", 1))), amb.admin));
            adm.remover(TipoDado.SEDE, manaus.getId(), amb.admin);
            assertEquals(8, amb.consulta.listarSedes().size());
            assertThrows(NaoEncontradoException.class, () -> adm.remover(TipoDado.SEDE, 99, amb.admin));
            assertThrows(NaoEncontradoException.class, () -> adm.salvar(new Sede(99, "X", Regiao.SUL,
                    new Estadio("E", 1, 2000, "R"), List.of(new Aeroporto("A", "AAA", 1))), amb.admin));
        }

        @Test
        @DisplayName("Editar a sede atualiza o estádio visto pelas partidas")
        void editarSede() {
            Sede bh = amb.consulta.listarSedes().get(0);
            Partida jogoEmBh = amb.consulta.listarPartidasDaSede(bh.getId()).get(0);
            adm.salvar(new Sede(bh.getId(), "Belo Horizonte", Regiao.SUDESTE,
                    new Estadio("Mineirão", 62_000, 1965, "Av. Antônio Abrahão Caram, 1001"),
                    List.of(new Aeroporto("Confins", "CNF", 38))), amb.admin);
            assertEquals("Mineirão", partida(jogoEmBh.getId()).getEstadio().getNome());
            assertEquals(1, amb.consulta.buscarSede(bh.getId()).getAeroportos().size());
        }

        @Test
        @DisplayName("Cadastrar partida exige estádio de sede cadastrada e seleções do grupo")
        void cadastrarPartida() {
            Grupo a = amb.competicao.obterGrupos().get(0);
            Grupo b = amb.competicao.obterGrupos().get(1);
            Estadio estadio = amb.consulta.listarSedes().get(0).getEstadio();
            Partida extra = new Partida(0, Fase.GRUPOS, partida(1).getDataHora(), estadio, a,
                    a.getSelecoes().get(0), a.getSelecoes().get(1));
            adm.salvar(extra, amb.admin);
            assertEquals(65, extra.getId());
            Partida misturada = new Partida(0, Fase.GRUPOS, partida(1).getDataHora(), estadio, a,
                    a.getSelecoes().get(0), b.getSelecoes().get(0));
            assertThrows(RegraNegocioException.class, () -> adm.salvar(misturada, amb.admin));
            Partida estadioAvulso = new Partida(0, Fase.GRUPOS, partida(1).getDataHora(),
                    new Estadio("Sem sede", 1, 2000, "R"), a, a.getSelecoes().get(0), a.getSelecoes().get(1));
            assertThrows(RegraNegocioException.class, () -> adm.salvar(estadioAvulso, amb.admin));
            adm.remover(TipoDado.PARTIDA, 65, amb.admin);
            assertEquals(64, amb.consulta.listarPartidas(null).size());
        }

        @Test
        @DisplayName("Editar partida existente e recusar partida inexistente")
        void editarPartida() {
            Partida p = partida(2);
            p.setDataHora(p.getDataHora().plusHours(1));
            adm.salvar(p, amb.admin);
            assertEquals("editar partida", amb.auditoria.listar().get(0).getOperacao());
            Grupo a = amb.competicao.obterGrupos().get(0);
            Partida fantasma = new Partida(300, Fase.GRUPOS, p.getDataHora(), p.getEstadio(), a,
                    a.getSelecoes().get(0), a.getSelecoes().get(1));
            assertThrows(NaoEncontradoException.class, () -> adm.salvar(fantasma, amb.admin));
            assertThrows(RegraNegocioException.class, () -> adm.salvar((Partida) null, amb.admin));
        }

        @Test
        @DisplayName("Partida do chaveamento ou já iniciada não pode ser removida")
        void removerPartida() {
            assertThrows(RegraNegocioException.class, () -> adm.remover(TipoDado.PARTIDA, 64, amb.admin));
            adm.iniciarPartida(1, amb.admin);
            assertThrows(RegraNegocioException.class, () -> adm.remover(TipoDado.PARTIDA, 1, amb.admin));
            assertThrows(NaoEncontradoException.class, () -> adm.remover(TipoDado.PARTIDA, 99, amb.admin));
            assertThrows(RegraNegocioException.class, () -> adm.remover(null, 1, amb.admin));
            assertThrows(RegraNegocioException.class, () -> adm.salvar((Sede) null, amb.admin));
        }
    }
}
