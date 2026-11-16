package br.ufmg.dcc603.copa27.dominio.modelo;

/** Responsável pela manutenção dos dados. Guarda apenas o hash da senha (RNF07). */
public class Administrador {

    private final String login;
    private final String hashSenha;

    public Administrador(String login, String hashSenha) {
        if (login == null || login.isBlank()) {
            throw new RegraNegocioException("O login é obrigatório.");
        }
        if (hashSenha == null || hashSenha.isBlank()) {
            throw new RegraNegocioException("O hash da senha é obrigatório.");
        }
        this.login = login;
        this.hashSenha = hashSenha;
    }

    public String getLogin() {
        return login;
    }

    public String getHashSenha() {
        return hashSenha;
    }
}
