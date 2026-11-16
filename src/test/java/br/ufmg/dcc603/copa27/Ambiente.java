package br.ufmg.dcc603.copa27;

import br.ufmg.dcc603.copa27.dominio.modelo.Administrador;
import br.ufmg.dcc603.copa27.dominio.modelo.Fase;
import br.ufmg.dcc603.copa27.dominio.modelo.Partida;
import br.ufmg.dcc603.copa27.dominio.modelo.Resultado;
import br.ufmg.dcc603.copa27.dominio.regras.CalculadoraClassificacao;
import br.ufmg.dcc603.copa27.dominio.regras.GeradorChaveamento;
import br.ufmg.dcc603.copa27.persistencia.RepositorioRegistros;
import br.ufmg.dcc603.copa27.persistencia.memoria.BaseDeDadosMemoria;
import br.ufmg.dcc603.copa27.persistencia.memoria.CargaInicial;
import br.ufmg.dcc603.copa27.servico.HashSenha;
import br.ufmg.dcc603.copa27.servico.ServicoAdministracao;
import br.ufmg.dcc603.copa27.servico.ServicoAuditoria;
import br.ufmg.dcc603.copa27.servico.ServicoAutenticacao;
import br.ufmg.dcc603.copa27.servico.ServicoCompeticao;
import br.ufmg.dcc603.copa27.servico.ServicoConsulta;
import br.ufmg.dcc603.copa27.servico.ServicoSimulacao;

import java.util.function.IntFunction;

/**
 * Sistema completo montado sobre a carga inicial, para testes de serviço e
 * de integração. Cada instância tem sua própria base em memória.
 */
public final class Ambiente {

    public static final String SENHA = "senha-de-teste";
    private static final String HASH = HashSenha.gerar(SENHA);

    public final BaseDeDadosMemoria base = new BaseDeDadosMemoria();
    public final ServicoConsulta consulta;
    public final ServicoCompeticao competicao;
    public final ServicoSimulacao simulacao;
    public final ServicoAutenticacao autenticacao;
    public final ServicoAuditoria auditoria;
    public final ServicoAdministracao administracao;
    public final Administrador admin;

    public Ambiente() {
        this(null);
    }

    /** Permite substituir o repositório do log, por exemplo para simular falha de gravação. */
    public Ambiente(RepositorioRegistros registros) {
        CargaInicial.popular(base, "admin", HASH);
        CalculadoraClassificacao calc = new CalculadoraClassificacao();
        GeradorChaveamento gerador = new GeradorChaveamento();
        consulta = new ServicoConsulta(base.repositorioPartidas(), base.repositorioSelecoes(), base.repositorioSedes());
        competicao = new ServicoCompeticao(base.repositorioGrupos(), base.repositorioChaveamento());
        simulacao = new ServicoSimulacao(base.repositorioPartidas(), base.repositorioGrupos(),
                base.repositorioChaveamento(), calc, gerador);
        autenticacao = new ServicoAutenticacao(base.repositorioAdministradores());
        auditoria = new ServicoAuditoria(registros == null ? base.repositorioRegistros() : registros);
        administracao = new ServicoAdministracao(base.repositorioPartidas(), base.repositorioSelecoes(),
                base.repositorioSedes(), base.repositorioGrupos(), base.repositorioChaveamento(), auditoria,
                calc, gerador, base);
        admin = autenticacao.autenticar("admin", SENHA);
    }

    /** Inicia, registra o placar e encerra a partida pelo serviço de administração. */
    public void jogar(int idPartida, Resultado r) {
        administracao.iniciarPartida(idPartida, admin);
        administracao.registrarResultado(idPartida, r, true, admin);
    }

    /**
     * Encerra as partidas da fase de grupos com o placar dado pelo identificador
     * da partida; as partidas cujo placar for null não são jogadas.
     */
    public void encerrarFaseDeGrupos(IntFunction<Resultado> placar) {
        for (Partida p : consulta.listarPartidas(null)) {
            Resultado r = placar.apply(p.getId());
            if (p.getFase() == Fase.GRUPOS && !p.estaDisputada() && r != null) {
                jogar(p.getId(), r);
            }
        }
    }

    /** A mandante vence por 1 x 0 todas as partidas da fase de grupos. */
    public void encerrarFaseDeGrupos() {
        encerrarFaseDeGrupos(id -> Resultado.placar(1, 0));
    }
}
