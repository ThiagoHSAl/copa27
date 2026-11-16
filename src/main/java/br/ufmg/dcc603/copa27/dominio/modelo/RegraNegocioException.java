package br.ufmg.dcc603.copa27.dominio.modelo;

/**
 * Sinaliza a violação de uma regra de negócio ou de uma validação de dados do
 * torneio. A mensagem é sempre apresentável ao usuário.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
