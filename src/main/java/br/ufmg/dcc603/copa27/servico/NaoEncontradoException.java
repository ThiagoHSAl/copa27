package br.ufmg.dcc603.copa27.servico;

/** O dado solicitado não existe na base. */
public class NaoEncontradoException extends RuntimeException {

    public NaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
