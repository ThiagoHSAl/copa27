package br.ufmg.dcc603.copa27.servico;

import br.ufmg.dcc603.copa27.dominio.modelo.Cenario;
import br.ufmg.dcc603.copa27.dominio.modelo.Confronto;
import br.ufmg.dcc603.copa27.dominio.modelo.Grupo;
import br.ufmg.dcc603.copa27.dominio.modelo.Partida;
import br.ufmg.dcc603.copa27.dominio.modelo.Resultado;
import br.ufmg.dcc603.copa27.dominio.modelo.Selecao;
import br.ufmg.dcc603.copa27.dominio.regras.CalculadoraClassificacao;
import br.ufmg.dcc603.copa27.dominio.regras.GeradorChaveamento;
import br.ufmg.dcc603.copa27.persistencia.RepositorioChaveamento;
import br.ufmg.dcc603.copa27.persistencia.RepositorioGrupos;
import br.ufmg.dcc603.copa27.persistencia.RepositorioPartidas;

import java.util.List;

/**
 * Simulação de cruzamentos futuros (CU06 a CU09). Trabalha sobre uma cópia do
 * cenário real e nunca grava na base (RN07); o cenário simulado fica com a
 * camada de apresentação, na sessão do Visitante.
 */
public class ServicoSimulacao {

    private final RepositorioPartidas partidas;
    private final RepositorioGrupos grupos;
    private final RepositorioChaveamento chaveamento;
    private final CalculadoraClassificacao calculadora;
    private final GeradorChaveamento gerador;

    public ServicoSimulacao(RepositorioPartidas partidas, RepositorioGrupos grupos, RepositorioChaveamento chaveamento,
                            CalculadoraClassificacao calculadora, GeradorChaveamento gerador) {
        this.partidas = partidas;
        this.grupos = grupos;
        this.chaveamento = chaveamento;
        this.calculadora = calculadora;
        this.gerador = gerador;
    }

    /** Cria um cenário simulado, cópia independente do cenário real corrente. */
    public Cenario iniciar() {
        Cenario real = new Cenario(grupos.listar(), partidas.listar(), chaveamento.carregar());
        Cenario simulado = real.copiar();
        recalcular(simulado);
        return simulado;
    }

    /** Aplica um placar hipotético e recalcula classificação e chaveamento (CU06, CU07). */
    public void simular(Cenario c, int idPartida, Resultado r) {
        Partida p = c.buscarPartida(idPartida);
        if (p == null) {
            throw new NaoEncontradoException("Partida " + idPartida + " não encontrada.");
        }
        p.simular(r);
        recalcular(c);
    }

    /** Confrontos projetados da seleção até a final no cenário simulado (CU08). */
    public List<Confronto> caminhoAteFinal(Cenario c, int idSelecao) {
        Selecao s = c.buscarSelecao(idSelecao);
        if (s == null) {
            throw new NaoEncontradoException("Seleção " + idSelecao + " não encontrada.");
        }
        return c.getChaveamento().caminhoAteFinal(s);
    }

    /** Descarta a simulação e volta ao cenário real corrente (CU09). */
    public Cenario restaurar() {
        return iniciar();
    }

    private void recalcular(Cenario c) {
        for (Grupo g : c.getGrupos()) {
            g.setClassificacao(calculadora.calcular(g, c.partidasDoGrupo(g)));
        }
        gerador.projetar(c.getChaveamento(), c.getGrupos());
    }
}
