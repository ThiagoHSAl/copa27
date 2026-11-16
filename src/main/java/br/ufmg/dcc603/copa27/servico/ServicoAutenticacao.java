package br.ufmg.dcc603.copa27.servico;

import br.ufmg.dcc603.copa27.dominio.modelo.Administrador;
import br.ufmg.dcc603.copa27.persistencia.RepositorioAdministradores;

/** Verifica as credenciais do Administrador (CU18). */
public class ServicoAutenticacao {

    static final String MENSAGEM_FALHA = "Usuário ou senha inválidos.";

    private final RepositorioAdministradores repositorio;

    public ServicoAutenticacao(RepositorioAdministradores repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Devolve o administrador autenticado. A mensagem de falha é a mesma para
     * usuário inexistente e senha errada, sem indicar qual dos dois está errado (CU18, E1).
     */
    public Administrador autenticar(String login, String senha) {
        Administrador a = repositorio.buscarPorLogin(login);
        if (a == null || !HashSenha.verificar(senha, a.getHashSenha())) {
            throw new AutenticacaoException(MENSAGEM_FALHA);
        }
        return a;
    }
}
