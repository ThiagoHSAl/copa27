package br.ufmg.dcc603.copa27.servico;

import br.ufmg.dcc603.copa27.dominio.modelo.Chaveamento;
import br.ufmg.dcc603.copa27.dominio.modelo.Classificacao;
import br.ufmg.dcc603.copa27.dominio.modelo.Grupo;
import br.ufmg.dcc603.copa27.persistencia.RepositorioChaveamento;
import br.ufmg.dcc603.copa27.persistencia.RepositorioGrupos;

import java.util.Comparator;
import java.util.List;

/**
 * Classificação dos grupos (CU04) e chaveamento do mata-mata (CU05) do
 * cenário real. Apenas lê dados já calculados no registro dos resultados.
 */
public class ServicoCompeticao {

    private final RepositorioGrupos grupos;
    private final RepositorioChaveamento chaveamento;

    public ServicoCompeticao(RepositorioGrupos grupos, RepositorioChaveamento chaveamento) {
        this.grupos = grupos;
        this.chaveamento = chaveamento;
    }

    /** Grupos de A a H, cada um com sua classificação. */
    public List<Grupo> obterGrupos() {
        return grupos.listar().stream().sorted(Comparator.comparing(Grupo::getLetra)).toList();
    }

    public List<Classificacao> obterClassificacoes() {
        return obterGrupos().stream().map(Grupo::getClassificacao).toList();
    }

    public Chaveamento obterChaveamento() {
        return chaveamento.carregar();
    }
}
