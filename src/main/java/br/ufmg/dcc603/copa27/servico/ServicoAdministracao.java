package br.ufmg.dcc603.copa27.servico;

import br.ufmg.dcc603.copa27.dominio.modelo.Administrador;
import br.ufmg.dcc603.copa27.dominio.modelo.Chaveamento;
import br.ufmg.dcc603.copa27.dominio.modelo.Fase;
import br.ufmg.dcc603.copa27.dominio.modelo.Grupo;
import br.ufmg.dcc603.copa27.dominio.modelo.Partida;
import br.ufmg.dcc603.copa27.dominio.modelo.RegraNegocioException;
import br.ufmg.dcc603.copa27.dominio.modelo.Resultado;
import br.ufmg.dcc603.copa27.dominio.modelo.Sede;
import br.ufmg.dcc603.copa27.dominio.modelo.Selecao;
import br.ufmg.dcc603.copa27.dominio.modelo.SituacaoPartida;
import br.ufmg.dcc603.copa27.dominio.regras.CalculadoraClassificacao;
import br.ufmg.dcc603.copa27.dominio.regras.GeradorChaveamento;
import br.ufmg.dcc603.copa27.persistencia.GerenciadorTransacoes;
import br.ufmg.dcc603.copa27.persistencia.RepositorioChaveamento;
import br.ufmg.dcc603.copa27.persistencia.RepositorioGrupos;
import br.ufmg.dcc603.copa27.persistencia.RepositorioPartidas;
import br.ufmg.dcc603.copa27.persistencia.RepositorioSedes;
import br.ufmg.dcc603.copa27.persistencia.RepositorioSelecoes;

import java.util.List;

/**
 * Manutenção dos dados do torneio (CU16) e registro de resultados (CU17). É o
 * único serviço que grava dados; cada operação exige um administrador
 * autenticado (RNF09) e executa em uma única transação, que inclui o
 * registro no log (RNF04).
 */
public class ServicoAdministracao {

    private final RepositorioPartidas partidas;
    private final RepositorioSelecoes selecoes;
    private final RepositorioSedes sedes;
    private final RepositorioGrupos grupos;
    private final RepositorioChaveamento chaveamento;
    private final ServicoAuditoria auditoria;
    private final CalculadoraClassificacao calculadora;
    private final GeradorChaveamento gerador;
    private final GerenciadorTransacoes transacoes;

    public ServicoAdministracao(RepositorioPartidas partidas, RepositorioSelecoes selecoes, RepositorioSedes sedes,
                                RepositorioGrupos grupos, RepositorioChaveamento chaveamento,
                                ServicoAuditoria auditoria, CalculadoraClassificacao calculadora,
                                GeradorChaveamento gerador, GerenciadorTransacoes transacoes) {
        this.partidas = partidas;
        this.selecoes = selecoes;
        this.sedes = sedes;
        this.grupos = grupos;
        this.chaveamento = chaveamento;
        this.auditoria = auditoria;
        this.calculadora = calculadora;
        this.gerador = gerador;
        this.transacoes = transacoes;
    }

    // ---------------------------------------------------------------- CU16

    /** Cadastra (id 0) ou edita uma seleção. O código FIFA é único. */
    public void salvar(Selecao s, Administrador autor) {
        exigirAutor(autor);
        if (s == null) {
            throw new RegraNegocioException("Informe a seleção.");
        }
        boolean nova = s.getId() == 0;
        if (!nova && selecoes.buscarPorId(s.getId()) == null) {
            throw new NaoEncontradoException("Seleção " + s.getId() + " não encontrada.");
        }
        boolean codigoRepetido = selecoes.listar().stream()
                .anyMatch(o -> o.getId() != s.getId() && o.getCodigoFifa().equals(s.getCodigoFifa()));
        if (codigoRepetido) {
            throw new RegraNegocioException("Já existe uma seleção com o código " + s.getCodigoFifa() + ".");
        }
        transacoes.emTransacao(() -> {
            selecoes.salvar(s);
            auditoria.registrar(autor, nova ? "cadastrar seleção" : "editar seleção", s.toString());
        });
    }

    /** Cadastra (id 0) ou edita uma sede, com seu estádio. A cidade é única. */
    public void salvar(Sede s, Administrador autor) {
        exigirAutor(autor);
        if (s == null) {
            throw new RegraNegocioException("Informe a sede.");
        }
        boolean nova = s.getId() == 0;
        if (!nova && sedes.buscarPorId(s.getId()) == null) {
            throw new NaoEncontradoException("Sede " + s.getId() + " não encontrada.");
        }
        boolean cidadeRepetida = sedes.listar().stream()
                .anyMatch(o -> o.getId() != s.getId() && o.getCidade().equalsIgnoreCase(s.getCidade()));
        if (cidadeRepetida) {
            throw new RegraNegocioException("Já existe uma sede em " + s.getCidade() + ".");
        }
        transacoes.emTransacao(() -> {
            sedes.salvar(s);
            auditoria.registrar(autor, nova ? "cadastrar sede" : "editar sede", s.getCidade());
        });
    }

    /**
     * Cadastra (id 0) ou edita uma partida. O estádio deve pertencer a uma sede
     * cadastrada e, na fase de grupos, as duas seleções devem integrar o grupo.
     */
    public void salvar(Partida p, Administrador autor) {
        exigirAutor(autor);
        if (p == null) {
            throw new RegraNegocioException("Informe a partida.");
        }
        boolean nova = p.getId() == 0;
        if (!nova && partidas.buscarPorId(p.getId()) == null) {
            throw new NaoEncontradoException("Partida " + p.getId() + " não encontrada.");
        }
        boolean estadioCadastrado = sedes.listar().stream().anyMatch(s -> s.getEstadio() == p.getEstadio());
        if (!estadioCadastrado) {
            throw new RegraNegocioException("O estádio da partida não pertence a nenhuma sede cadastrada.");
        }
        if (p.getFase() == Fase.GRUPOS) {
            Grupo g = grupos.buscarPorId(p.getGrupo().getId());
            if (g == null || !g.contem(p.getMandante()) || !g.contem(p.getVisitante())) {
                throw new RegraNegocioException("As duas seleções devem integrar o grupo da partida.");
            }
        }
        transacoes.emTransacao(() -> {
            partidas.salvar(p);
            auditoria.registrar(autor, nova ? "cadastrar partida" : "editar partida", p.toString());
        });
    }

    /** Remove um dado, desde que nenhum outro faça referência a ele (CU16, E3). */
    public void remover(TipoDado tipo, int id, Administrador autor) {
        exigirAutor(autor);
        if (tipo == null) {
            throw new RegraNegocioException("Informe o tipo de dado.");
        }
        transacoes.emTransacao(() -> {
            String descricao = switch (tipo) {
                case SELECAO -> removerSelecao(id);
                case SEDE -> removerSede(id);
                case PARTIDA -> removerPartida(id);
            };
            auditoria.registrar(autor, "remover " + tipo.name().toLowerCase(), descricao);
        });
    }

    private String removerSelecao(int id) {
        Selecao s = selecoes.buscarPorId(id);
        if (s == null) {
            throw new NaoEncontradoException("Seleção " + id + " não encontrada.");
        }
        if (grupos.listar().stream().anyMatch(g -> g.contem(s))) {
            throw new RegraNegocioException(s.getPais() + " integra um grupo e não pode ser removida.");
        }
        long jogos = partidas.listar().stream().filter(p -> p.envolve(s)).count();
        if (jogos > 0) {
            throw new RegraNegocioException(s.getPais() + " está em " + jogos + " partida(s) e não pode ser removida.");
        }
        selecoes.remover(id);
        return s.toString();
    }

    private String removerSede(int id) {
        Sede s = sedes.buscarPorId(id);
        if (s == null) {
            throw new NaoEncontradoException("Sede " + id + " não encontrada.");
        }
        long jogos = partidas.listar().stream().filter(p -> p.getEstadio() == s.getEstadio()).count();
        if (jogos > 0) {
            throw new RegraNegocioException(s.getCidade() + " tem " + jogos + " partida(s) e não pode ser removida.");
        }
        sedes.remover(id);
        return s.getCidade();
    }

    private String removerPartida(int id) {
        Partida p = partidas.buscarPorId(id);
        if (p == null) {
            throw new NaoEncontradoException("Partida " + id + " não encontrada.");
        }
        Chaveamento ch = chaveamento.carregar();
        if (ch != null && ch.confrontoDe(p) != null) {
            throw new RegraNegocioException("A partida integra o chaveamento e não pode ser removida.");
        }
        if (p.getSituacao() != SituacaoPartida.AGENDADA) {
            throw new RegraNegocioException("Apenas partidas agendadas podem ser removidas.");
        }
        partidas.remover(id);
        return p.toString();
    }

    /** Muda a partida de agendada para em andamento, o que permite registrar o resultado. */
    public void iniciarPartida(int idPartida, Administrador autor) {
        exigirAutor(autor);
        transacoes.emTransacao(() -> {
            Partida p = buscarPartida(idPartida);
            p.iniciar();
            partidas.salvar(p);
            auditoria.registrar(autor, "iniciar partida", p.toString());
        });
    }

    // ---------------------------------------------------------------- CU17

    /**
     * Registra o placar de uma partida em andamento ou encerrada e, se pedido,
     * a encerra. Na fase de grupos, atualiza a classificação (CU20) e, quando a
     * fase termina, preenche as oitavas (CU21); no mata-mata, uma partida
     * encerrada faz a vencedora avançar. Tudo ocorre em uma única transação.
     */
    public void registrarResultado(int idPartida, Resultado r, boolean encerrar, Administrador autor) {
        exigirAutor(autor);
        transacoes.emTransacao(() -> {
            Partida p = buscarPartida(idPartida);
            p.registrarResultado(r);
            if (encerrar) {
                p.encerrar();
            }
            partidas.salvar(p);
            if (p.getFase() == Fase.GRUPOS) {
                Grupo g = p.getGrupo();
                List<Partida> partidasDoGrupo = partidas.buscarPorGrupo(g);
                g.setClassificacao(calculadora.calcular(g, partidasDoGrupo));
                grupos.salvar(g);
                if (gerador.faseDeGruposEncerrada(partidas.listar())) {
                    Chaveamento ch = chaveamento.carregar();
                    gerador.preencherOitavas(ch, grupos.listar());
                    chaveamento.salvar(ch);
                }
            } else if (p.getSituacao() == SituacaoPartida.ENCERRADA) {
                Chaveamento ch = chaveamento.carregar();
                gerador.avancarVencedora(ch, p);
                chaveamento.salvar(ch);
            }
            auditoria.registrar(autor, "registrar resultado", p + ": " + r);
        });
    }

    private Partida buscarPartida(int id) {
        Partida p = partidas.buscarPorId(id);
        if (p == null) {
            throw new NaoEncontradoException("Partida " + id + " não encontrada.");
        }
        return p;
    }

    private static void exigirAutor(Administrador autor) {
        if (autor == null) {
            throw new AutenticacaoException("Operação restrita ao administrador autenticado.");
        }
    }
}
