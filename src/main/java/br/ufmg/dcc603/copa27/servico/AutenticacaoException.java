package br.ufmg.dcc603.copa27.servico;

/** Falha de autenticação ou operação administrativa sem administrador autenticado (RNF09). */
public class AutenticacaoException extends RuntimeException {

    public AutenticacaoException(String mensagem) {
        super(mensagem);
    }
}
