package br.ufmg.dcc603.copa27.dominio.modelo;

import java.util.ArrayList;
import java.util.List;

/** Grupo da primeira fase, com quatro seleções (RN01) e sua classificação. */
public class Grupo {

    public static final int TAMANHO = 4;

    private final char letra;
    private final List<Selecao> selecoes = new ArrayList<>();
    private Classificacao classificacao;

    public Grupo(char letra) {
        if (letra < 'A' || letra > 'H') {
            throw new RegraNegocioException("Os grupos vão de A a H.");
        }
        this.letra = letra;
    }

    public Grupo(char letra, List<Selecao> selecoes) {
        this(letra);
        selecoes.forEach(this::adicionarSelecao);
    }

    /** Inclui uma seleção, respeitando o limite de quatro por grupo (RN01). */
    public void adicionarSelecao(Selecao s) {
        if (s == null) {
            throw new RegraNegocioException("Informe a seleção.");
        }
        if (selecoes.contains(s)) {
            throw new RegraNegocioException(s.getPais() + " já integra o Grupo " + letra + ".");
        }
        if (selecoes.size() >= TAMANHO) {
            throw new RegraNegocioException("O Grupo " + letra + " já tem quatro seleções.");
        }
        selecoes.add(s);
        classificacao = null;
    }

    public boolean contem(Selecao s) {
        return selecoes.contains(s);
    }

    Grupo copiar() {
        Grupo c = new Grupo(letra);
        c.selecoes.addAll(selecoes);
        c.classificacao = classificacao == null ? null : classificacao.copiar();
        return c;
    }

    /** Identificador numérico usado pela persistência: A = 1, ..., H = 8. */
    public int getId() {
        return letra - 'A' + 1;
    }

    public char getLetra() {
        return letra;
    }

    public List<Selecao> getSelecoes() {
        return List.copyOf(selecoes);
    }

    public Classificacao getClassificacao() {
        return classificacao != null ? classificacao : Classificacao.inicial(selecoes);
    }

    public void setClassificacao(Classificacao classificacao) {
        this.classificacao = classificacao;
    }

    @Override
    public String toString() {
        return "Grupo " + letra;
    }
}
