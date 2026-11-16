package br.ufmg.dcc603.copa27.dominio.modelo;

import java.util.ArrayList;
import java.util.List;

/** Seleção participante do torneio, identificada pelo país e pelo código FIFA. */
public class Selecao {

    private int id;
    private String pais;
    private String codigoFifa;
    private Confederacao confederacao;
    private final List<Jogadora> elenco = new ArrayList<>();
    private final List<MembroComissao> comissao = new ArrayList<>();
    private final List<ParticipacaoCopa> historico = new ArrayList<>();

    public Selecao(int id, String pais, String codigoFifa, Confederacao confederacao) {
        validar(pais, codigoFifa, confederacao);
        this.id = id;
        this.pais = pais.trim();
        this.codigoFifa = codigoFifa;
        this.confederacao = confederacao;
    }

    private static void validar(String pais, String codigoFifa, Confederacao confederacao) {
        if (pais == null || pais.isBlank()) {
            throw new RegraNegocioException("O país da seleção é obrigatório.");
        }
        if (codigoFifa == null || !codigoFifa.matches("[A-Z]{3}")) {
            throw new RegraNegocioException("O código FIFA deve ter três letras maiúsculas.");
        }
        if (confederacao == null) {
            throw new RegraNegocioException("A confederação da seleção é obrigatória.");
        }
    }

    /** Cópia independente, usada pela persistência para desfazer transações. */
    public Selecao copiar() {
        Selecao c = new Selecao(id, pais, codigoFifa, confederacao);
        c.elenco.addAll(elenco);
        c.comissao.addAll(comissao);
        c.historico.addAll(historico);
        return c;
    }

    /** Copia os dados de outra seleção mantendo a identidade deste objeto. */
    public void atualizarCom(Selecao outra) {
        this.pais = outra.pais;
        this.codigoFifa = outra.codigoFifa;
        this.confederacao = outra.confederacao;
        elenco.clear();
        elenco.addAll(outra.elenco);
        comissao.clear();
        comissao.addAll(outra.comissao);
        historico.clear();
        historico.addAll(outra.historico);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPais() {
        return pais;
    }

    public String getCodigoFifa() {
        return codigoFifa;
    }

    public Confederacao getConfederacao() {
        return confederacao;
    }

    public List<Jogadora> getElenco() {
        return elenco;
    }

    public List<MembroComissao> getComissao() {
        return comissao;
    }

    public List<ParticipacaoCopa> getHistorico() {
        return historico;
    }

    @Override
    public String toString() {
        return pais + " (" + codigoFifa + ")";
    }
}
