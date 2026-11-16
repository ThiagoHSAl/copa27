package br.ufmg.dcc603.copa27.dominio.modelo;

import java.util.Objects;

/**
 * Nó da árvore do mata-mata: liga uma partida ao confronto para onde avança
 * a vencedora. Enquanto as seleções não são conhecidas, exibe a descrição
 * da vaga, como "1º do Grupo A".
 */
public class Confronto {

    private final int numero;
    private final Partida partida;
    private final String vagaMandante;
    private final String vagaVisitante;
    private Confronto proximo;
    private boolean vencedoraEntraComoMandante;

    public Confronto(int numero, Partida partida, String vagaMandante, String vagaVisitante) {
        if (partida == null || !partida.getFase().isMataMata()) {
            throw new RegraNegocioException("Um confronto exige uma partida de mata-mata.");
        }
        this.numero = numero;
        this.partida = partida;
        this.vagaMandante = Objects.requireNonNull(vagaMandante);
        this.vagaVisitante = Objects.requireNonNull(vagaVisitante);
    }

    /** Define o confronto seguinte e a posição que a vencedora ocupará nele. */
    public void ligarAo(Confronto proximo, boolean comoMandante) {
        this.proximo = proximo;
        this.vencedoraEntraComoMandante = comoMandante;
    }

    public int getNumero() {
        return numero;
    }

    public Partida getPartida() {
        return partida;
    }

    public Fase getFase() {
        return partida.getFase();
    }

    public String getVagaMandante() {
        return vagaMandante;
    }

    public String getVagaVisitante() {
        return vagaVisitante;
    }

    public Confronto getProximo() {
        return proximo;
    }

    public boolean isVencedoraEntraComoMandante() {
        return vencedoraEntraComoMandante;
    }

    /** Nome da mandante, ou a descrição da vaga enquanto ela não é conhecida. */
    public String descricaoMandante() {
        return partida.getMandante() != null ? partida.getMandante().getPais() : vagaMandante;
    }

    public String descricaoVisitante() {
        return partida.getVisitante() != null ? partida.getVisitante().getPais() : vagaVisitante;
    }
}
