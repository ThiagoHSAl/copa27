package br.ufmg.dcc603.copa27.dominio.regras;

import br.ufmg.dcc603.copa27.dominio.modelo.Classificacao;
import br.ufmg.dcc603.copa27.dominio.modelo.Grupo;
import br.ufmg.dcc603.copa27.dominio.modelo.LinhaClassificacao;
import br.ufmg.dcc603.copa27.dominio.modelo.Partida;
import br.ufmg.dcc603.copa27.dominio.modelo.Resultado;
import br.ufmg.dcc603.copa27.dominio.modelo.Selecao;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcula a classificação de um grupo a partir dos resultados registrados
 * (CU20), aplicando a pontuação (RN02) e os critérios de desempate (RN03).
 * O mesmo cálculo serve ao cenário real e ao simulado.
 */
public class CalculadoraClassificacao {

    public Classificacao calcular(Grupo g, List<Partida> partidas) {
        Map<Selecao, LinhaClassificacao> linhas = new LinkedHashMap<>();
        for (Selecao s : g.getSelecoes()) {
            linhas.put(s, new LinhaClassificacao(s));
        }
        List<Partida> doGrupo = partidas.stream().filter(p -> p.getGrupo() == g).toList();
        for (Partida p : doGrupo) {
            Resultado r = p.getResultado();
            if (r == null) {
                continue;
            }
            linhas.get(p.getMandante()).contabilizar(r.getGolsMandante(), r.getGolsVisitante());
            linhas.get(p.getVisitante()).contabilizar(r.getGolsVisitante(), r.getGolsMandante());
        }
        List<LinhaClassificacao> ordenadas = new ArrayList<>(linhas.values());
        ordenadas.sort(new ComparadorDesempate(doGrupo));
        boolean definitiva = !doGrupo.isEmpty() && doGrupo.stream().allMatch(Partida::temResultadoDefinitivo);
        return new Classificacao(ordenadas, definitiva);
    }
}
