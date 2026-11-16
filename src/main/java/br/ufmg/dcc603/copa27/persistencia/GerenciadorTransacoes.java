package br.ufmg.dcc603.copa27.persistencia;

import java.util.function.Supplier;

/**
 * Executa uma operação como transação: se ela lançar exceção, todas as
 * escritas feitas por ela são desfeitas (RNF04).
 */
public interface GerenciadorTransacoes {

    <T> T emTransacao(Supplier<T> operacao);

    default void emTransacao(Runnable operacao) {
        emTransacao(() -> {
            operacao.run();
            return null;
        });
    }
}
