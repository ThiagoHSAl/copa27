package br.ufmg.dcc603.copa27.dominio.modelo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Cidade-sede, composta por um estádio, aeroportos e hotéis (RF14 a RF17). */
public class Sede {

    private int id;
    private String cidade;
    private Regiao regiao;
    private final Estadio estadio;
    private final List<Aeroporto> aeroportos = new ArrayList<>();
    private final List<Hotel> hoteis = new ArrayList<>();

    public Sede(int id, String cidade, Regiao regiao, Estadio estadio, List<Aeroporto> aeroportos) {
        if (cidade == null || cidade.isBlank()) {
            throw new RegraNegocioException("A cidade da sede é obrigatória.");
        }
        if (regiao == null) {
            throw new RegraNegocioException("A região da sede é obrigatória.");
        }
        if (estadio == null) {
            throw new RegraNegocioException("Toda sede deve ter um estádio.");
        }
        if (aeroportos == null || aeroportos.isEmpty()) {
            throw new RegraNegocioException("Toda sede deve ter ao menos um aeroporto.");
        }
        this.id = id;
        this.cidade = cidade.trim();
        this.regiao = regiao;
        this.estadio = estadio;
        this.aeroportos.addAll(aeroportos);
        estadio.setSede(this);
    }

    /** Cópia independente, usada pela persistência para desfazer transações. */
    public Sede copiar() {
        Sede c = new Sede(id, cidade, regiao, estadio.copiar(), aeroportos);
        c.hoteis.addAll(hoteis);
        return c;
    }

    /**
     * Copia os dados de outra sede mantendo a identidade deste objeto e do seu
     * estádio, ao qual as partidas fazem referência.
     */
    public void atualizarCom(Sede outra) {
        this.cidade = outra.cidade;
        this.regiao = outra.regiao;
        this.estadio.atualizarCom(outra.estadio);
        aeroportos.clear();
        aeroportos.addAll(outra.aeroportos);
        hoteis.clear();
        hoteis.addAll(outra.hoteis);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCidade() {
        return cidade;
    }

    public Regiao getRegiao() {
        return regiao;
    }

    public Estadio getEstadio() {
        return estadio;
    }

    /** Aeroportos ordenados pela distância até o estádio, critério objetivo (RNF17). */
    public List<Aeroporto> getAeroportos() {
        return aeroportos.stream().sorted(Comparator.comparingDouble(Aeroporto::distanciaKm)).toList();
    }

    /** Hotéis ordenados pela distância até o estádio, critério objetivo (RNF17). */
    public List<Hotel> getHoteis() {
        return hoteis.stream().sorted(Comparator.comparingDouble(Hotel::distanciaKm)).toList();
    }

    public void adicionarHotel(Hotel hotel) {
        hoteis.add(hotel);
    }

    @Override
    public String toString() {
        return cidade;
    }
}
