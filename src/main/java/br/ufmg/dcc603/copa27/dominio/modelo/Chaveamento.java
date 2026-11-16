package br.ufmg.dcc603.copa27.dominio.modelo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Árvore do mata-mata, com os 16 confrontos das oitavas até a final. */
public class Chaveamento {

    private final List<Confronto> confrontos;

    public Chaveamento(List<Confronto> confrontos) {
        this.confrontos = new ArrayList<>(confrontos);
        this.confrontos.sort(Comparator.comparingInt(Confronto::getNumero));
    }

    public List<Confronto> getConfrontos() {
        return List.copyOf(confrontos);
    }

    public List<Confronto> confrontosDaFase(Fase f) {
        return confrontos.stream().filter(c -> c.getFase() == f).toList();
    }

    public Confronto confrontoDe(Partida p) {
        return confrontos.stream().filter(c -> c.getPartida() == p).findFirst().orElse(null);
    }

    public Confronto buscarConfronto(int numero) {
        return confrontos.stream().filter(c -> c.getNumero() == numero).findFirst().orElse(null);
    }

    /**
     * Caminho da seleção até a final (CU08). Começa no confronto das oitavas em
     * que ela está e segue a árvore enquanto ela não for eliminada; confrontos
     * ainda não disputados entram como projeção. A lista fica vazia se a seleção
     * não estiver no mata-mata.
     */
    public List<Confronto> caminhoAteFinal(Selecao s) {
        List<Confronto> caminho = new ArrayList<>();
        Confronto atual = confrontosDaFase(Fase.OITAVAS).stream()
                .filter(c -> c.getPartida().envolve(s)).findFirst().orElse(null);
        while (atual != null) {
            caminho.add(atual);
            Selecao vencedora = atual.getPartida().vencedora();
            if (vencedora != null && vencedora != s) {
                break;
            }
            atual = atual.getProximo();
        }
        return caminho;
    }

    /** Cópia da árvore cujos confrontos apontam para as partidas copiadas. */
    Chaveamento copiar(Map<Partida, Partida> partidasCopiadas) {
        Map<Confronto, Confronto> mapa = new HashMap<>();
        for (Confronto c : confrontos) {
            Partida p = partidasCopiadas.getOrDefault(c.getPartida(), c.getPartida());
            mapa.put(c, new Confronto(c.getNumero(), p, c.getVagaMandante(), c.getVagaVisitante()));
        }
        for (Confronto c : confrontos) {
            if (c.getProximo() != null) {
                mapa.get(c).ligarAo(mapa.get(c.getProximo()), c.isVencedoraEntraComoMandante());
            }
        }
        return new Chaveamento(new ArrayList<>(mapa.values()));
    }
}
