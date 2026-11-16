package br.ufmg.dcc603.copa27.dominio.modelo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Raiz do modelo da competição: grupos, partidas e chaveamento. O mesmo tipo
 * representa o cenário real e o simulado; {@link #copiar()} produz uma cópia
 * independente, de modo que simular não afeta o cenário real (RN07).
 */
public class Cenario {

    private final List<Grupo> grupos;
    private final List<Partida> partidas;
    private final Chaveamento chaveamento;

    public Cenario(List<Grupo> grupos, List<Partida> partidas, Chaveamento chaveamento) {
        this.grupos = new ArrayList<>(grupos);
        this.grupos.sort(Comparator.comparing(Grupo::getLetra));
        this.partidas = new ArrayList<>(partidas);
        this.partidas.sort(Comparator.comparingInt(Partida::getId));
        this.chaveamento = Objects.requireNonNull(chaveamento);
    }

    public Cenario copiar() {
        Map<Grupo, Grupo> gruposCopiados = new HashMap<>();
        for (Grupo g : grupos) {
            gruposCopiados.put(g, g.copiar());
        }
        Map<Partida, Partida> partidasCopiadas = new HashMap<>();
        for (Partida p : partidas) {
            Grupo g = p.getGrupo() == null ? null : gruposCopiados.getOrDefault(p.getGrupo(), p.getGrupo());
            partidasCopiadas.put(p, p.copiar(g));
        }
        List<Grupo> novosGrupos = grupos.stream().map(gruposCopiados::get).toList();
        List<Partida> novasPartidas = partidas.stream().map(partidasCopiadas::get).toList();
        return new Cenario(novosGrupos, novasPartidas, chaveamento.copiar(partidasCopiadas));
    }

    /** Partidas ainda não encerradas, que o Visitante pode simular (CU06). */
    public List<Partida> partidasNaoDisputadas() {
        return partidas.stream().filter(p -> !p.estaDisputada()).toList();
    }

    public Partida buscarPartida(int id) {
        return partidas.stream().filter(p -> p.getId() == id).findFirst().orElse(null);
    }

    public List<Partida> partidasDoGrupo(Grupo g) {
        return partidas.stream().filter(p -> p.getGrupo() == g).toList();
    }

    public Selecao buscarSelecao(int id) {
        return grupos.stream().flatMap(g -> g.getSelecoes().stream())
                .filter(s -> s.getId() == id).findFirst().orElse(null);
    }

    public Grupo grupoDa(Selecao s) {
        return grupos.stream().filter(g -> g.contem(s)).findFirst().orElse(null);
    }

    public List<Grupo> getGrupos() {
        return List.copyOf(grupos);
    }

    public List<Partida> getPartidas() {
        return List.copyOf(partidas);
    }

    public Chaveamento getChaveamento() {
        return chaveamento;
    }
}
