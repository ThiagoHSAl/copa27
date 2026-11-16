package br.ufmg.dcc603.copa27.dominio.modelo;

import java.util.Objects;

/**
 * Placar de uma partida. Os gols incluem os eventuais gols da prorrogação; os
 * pênaltis só são preenchidos quando uma partida de mata-mata termina
 * empatada após a prorrogação (RN05). Objeto imutável.
 */
public final class Resultado {

    private final int golsMandante;
    private final int golsVisitante;
    private final boolean houveProrrogacao;
    private final Integer penaltisMandante;
    private final Integer penaltisVisitante;

    public Resultado(int golsMandante, int golsVisitante, boolean houveProrrogacao,
                     Integer penaltisMandante, Integer penaltisVisitante) {
        if (golsMandante < 0 || golsVisitante < 0) {
            throw new RegraNegocioException("O placar não pode ter valores negativos.");
        }
        if ((penaltisMandante == null) != (penaltisVisitante == null)) {
            throw new RegraNegocioException("Informe os pênaltis das duas seleções.");
        }
        if (penaltisMandante != null) {
            if (golsMandante != golsVisitante) {
                throw new RegraNegocioException("Só há pênaltis quando o jogo termina empatado.");
            }
            if (!houveProrrogacao) {
                throw new RegraNegocioException("Os pênaltis só ocorrem após a prorrogação.");
            }
            if (penaltisMandante < 0 || penaltisVisitante < 0) {
                throw new RegraNegocioException("O placar dos pênaltis não pode ser negativo.");
            }
            if (penaltisMandante.equals(penaltisVisitante)) {
                throw new RegraNegocioException("A disputa de pênaltis precisa ter uma vencedora.");
            }
        }
        this.golsMandante = golsMandante;
        this.golsVisitante = golsVisitante;
        this.houveProrrogacao = houveProrrogacao;
        this.penaltisMandante = penaltisMandante;
        this.penaltisVisitante = penaltisVisitante;
    }

    /** Placar no tempo normal, sem prorrogação. */
    public static Resultado placar(int golsMandante, int golsVisitante) {
        return new Resultado(golsMandante, golsVisitante, false, null, null);
    }

    /** Placar decidido na prorrogação; os gols incluem os da prorrogação. */
    public static Resultado comProrrogacao(int golsMandante, int golsVisitante) {
        return new Resultado(golsMandante, golsVisitante, true, null, null);
    }

    /** Placar empatado após a prorrogação e decidido nos pênaltis. */
    public static Resultado comPenaltis(int gols, int penaltisMandante, int penaltisVisitante) {
        return new Resultado(gols, gols, true, penaltisMandante, penaltisVisitante);
    }

    /** Indica se o jogo, incluída a prorrogação, terminou empatado. */
    public boolean empateNoJogo() {
        return golsMandante == golsVisitante;
    }

    public boolean temPenaltis() {
        return penaltisMandante != null;
    }

    /** Indica se o resultado aponta uma vencedora, condição exigida no mata-mata (RN05). */
    public boolean defineVencedora() {
        return !empateNoJogo() || temPenaltis();
    }

    /** Indica se a mandante venceu; só faz sentido quando {@link #defineVencedora()}. */
    public boolean mandanteVenceu() {
        if (!empateNoJogo()) {
            return golsMandante > golsVisitante;
        }
        return temPenaltis() && penaltisMandante > penaltisVisitante;
    }

    public int getGolsMandante() {
        return golsMandante;
    }

    public int getGolsVisitante() {
        return golsVisitante;
    }

    public boolean isHouveProrrogacao() {
        return houveProrrogacao;
    }

    public Integer getPenaltisMandante() {
        return penaltisMandante;
    }

    public Integer getPenaltisVisitante() {
        return penaltisVisitante;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Resultado r)) {
            return false;
        }
        return golsMandante == r.golsMandante && golsVisitante == r.golsVisitante
                && houveProrrogacao == r.houveProrrogacao
                && Objects.equals(penaltisMandante, r.penaltisMandante)
                && Objects.equals(penaltisVisitante, r.penaltisVisitante);
    }

    @Override
    public int hashCode() {
        return Objects.hash(golsMandante, golsVisitante, houveProrrogacao, penaltisMandante, penaltisVisitante);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder().append(golsMandante).append(" x ").append(golsVisitante);
        if (houveProrrogacao) {
            sb.append(" (prorrogação)");
        }
        if (temPenaltis()) {
            sb.append(", pênaltis ").append(penaltisMandante).append(" x ").append(penaltisVisitante);
        }
        return sb.toString();
    }
}
