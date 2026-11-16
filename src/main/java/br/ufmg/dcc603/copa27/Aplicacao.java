package br.ufmg.dcc603.copa27;

import br.ufmg.dcc603.copa27.apresentacao.ServidorWeb;
import br.ufmg.dcc603.copa27.dominio.modelo.Administrador;
import br.ufmg.dcc603.copa27.dominio.modelo.Fase;
import br.ufmg.dcc603.copa27.dominio.modelo.Partida;
import br.ufmg.dcc603.copa27.dominio.modelo.Resultado;
import br.ufmg.dcc603.copa27.dominio.regras.CalculadoraClassificacao;
import br.ufmg.dcc603.copa27.dominio.regras.GeradorChaveamento;
import br.ufmg.dcc603.copa27.persistencia.memoria.BaseDeDadosMemoria;
import br.ufmg.dcc603.copa27.persistencia.memoria.CargaInicial;
import br.ufmg.dcc603.copa27.servico.HashSenha;
import br.ufmg.dcc603.copa27.servico.ServicoAdministracao;
import br.ufmg.dcc603.copa27.servico.ServicoAuditoria;
import br.ufmg.dcc603.copa27.servico.ServicoAutenticacao;
import br.ufmg.dcc603.copa27.servico.ServicoCompeticao;
import br.ufmg.dcc603.copa27.servico.ServicoConsulta;
import br.ufmg.dcc603.copa27.servico.ServicoSimulacao;

import java.io.IOException;
import java.util.Random;

/**
 * Ponto de entrada: monta as quatro camadas (persistência, domínio, serviços
 * e apresentação) e inicia o servidor web.
 *
 * <p>Argumentos: {@code --porta N} (padrão 8080) e {@code --demo}, que
 * registra resultados das duas primeiras rodadas da fase de grupos para
 * demonstração. A senha do administrador vem da variável de ambiente
 * {@code COPA27_ADMIN_SENHA} (padrão "copa2027").
 */
public final class Aplicacao {

    public static final String LOGIN_ADMIN = "admin";
    public static final String SENHA_PADRAO = "copa2027";

    private final BaseDeDadosMemoria base = new BaseDeDadosMemoria();
    private final ServicoConsulta consulta;
    private final ServicoCompeticao competicao;
    private final ServicoSimulacao simulacao;
    private final ServicoAutenticacao autenticacao;
    private final ServicoAuditoria auditoria;
    private final ServicoAdministracao administracao;

    public Aplicacao(String senhaAdmin) {
        CargaInicial.popular(base, LOGIN_ADMIN, HashSenha.gerar(senhaAdmin));
        CalculadoraClassificacao calculadora = new CalculadoraClassificacao();
        GeradorChaveamento gerador = new GeradorChaveamento();
        consulta = new ServicoConsulta(base.repositorioPartidas(), base.repositorioSelecoes(), base.repositorioSedes());
        competicao = new ServicoCompeticao(base.repositorioGrupos(), base.repositorioChaveamento());
        simulacao = new ServicoSimulacao(base.repositorioPartidas(), base.repositorioGrupos(),
                base.repositorioChaveamento(), calculadora, gerador);
        autenticacao = new ServicoAutenticacao(base.repositorioAdministradores());
        auditoria = new ServicoAuditoria(base.repositorioRegistros());
        administracao = new ServicoAdministracao(base.repositorioPartidas(), base.repositorioSelecoes(),
                base.repositorioSedes(), base.repositorioGrupos(), base.repositorioChaveamento(), auditoria,
                calculadora, gerador, base);
    }

    /** Registra placares pseudoaleatórios (semente fixa) para as rodadas informadas da fase de grupos. */
    public void carregarDemonstracao(String senhaAdmin, int rodadas) {
        Administrador admin = autenticacao.autenticar(LOGIN_ADMIN, senhaAdmin);
        Random aleatorio = new Random(2027);
        for (Partida p : consulta.listarPartidas(null)) {
            if (p.getFase() == Fase.GRUPOS && p.getId() <= rodadas * 16) {
                administracao.iniciarPartida(p.getId(), admin);
                Resultado r = Resultado.placar(aleatorio.nextInt(4), aleatorio.nextInt(3));
                administracao.registrarResultado(p.getId(), r, true, admin);
            }
        }
    }

    public ServidorWeb criarServidor(int porta) throws IOException {
        return new ServidorWeb(porta, consulta, competicao, simulacao, autenticacao, administracao, auditoria);
    }

    public ServicoConsulta getConsulta() {
        return consulta;
    }

    public ServicoCompeticao getCompeticao() {
        return competicao;
    }

    public ServicoSimulacao getSimulacao() {
        return simulacao;
    }

    public ServicoAutenticacao getAutenticacao() {
        return autenticacao;
    }

    public ServicoAdministracao getAdministracao() {
        return administracao;
    }

    public ServicoAuditoria getAuditoria() {
        return auditoria;
    }

    public static void main(String[] args) throws IOException {
        int porta = 8080;
        boolean demo = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--porta") && i + 1 < args.length) {
                porta = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--demo")) {
                demo = true;
            }
        }
        String senha = System.getenv().getOrDefault("COPA27_ADMIN_SENHA", SENHA_PADRAO);
        Aplicacao app = new Aplicacao(senha);
        if (demo) {
            app.carregarDemonstracao(senha, 2);
        }
        ServidorWeb servidor = app.criarServidor(porta);
        servidor.iniciar();
        System.out.println("Copa27 em execução: http://localhost:" + servidor.porta()
                + (demo ? " (modo demonstração: rodadas 1 e 2 da fase de grupos encerradas)" : ""));
        System.out.println("Administração: http://localhost:" + servidor.porta() + "/admin  (usuário: " + LOGIN_ADMIN + ")");
    }
}
