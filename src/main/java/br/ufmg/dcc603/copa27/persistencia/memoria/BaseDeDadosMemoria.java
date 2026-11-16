package br.ufmg.dcc603.copa27.persistencia.memoria;

import br.ufmg.dcc603.copa27.dominio.modelo.Administrador;
import br.ufmg.dcc603.copa27.dominio.modelo.Cenario;
import br.ufmg.dcc603.copa27.dominio.modelo.Chaveamento;
import br.ufmg.dcc603.copa27.dominio.modelo.Grupo;
import br.ufmg.dcc603.copa27.dominio.modelo.Partida;
import br.ufmg.dcc603.copa27.dominio.modelo.RegistroAlteracao;
import br.ufmg.dcc603.copa27.dominio.modelo.Sede;
import br.ufmg.dcc603.copa27.dominio.modelo.Selecao;
import br.ufmg.dcc603.copa27.persistencia.GerenciadorTransacoes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

/**
 * Base de dados em memória usada na implementação parcial. Os repositórios
 * leem e gravam nestas estruturas; nenhum outro componente as acessa.
 *
 * <p>As transações são implementadas por instantâneo: antes da operação, o
 * estado é copiado; se a operação falhar, o estado anterior é restaurado,
 * de modo que nada é gravado parcialmente (RNF04).
 */
public class BaseDeDadosMemoria implements GerenciadorTransacoes {

    final Map<Integer, Partida> partidas = new TreeMap<>();
    final Map<Integer, Selecao> selecoes = new TreeMap<>();
    final Map<Integer, Sede> sedes = new TreeMap<>();
    final Map<Integer, Grupo> grupos = new TreeMap<>();
    final Map<String, Administrador> administradores = new HashMap<>();
    final List<RegistroAlteracao> registros = new ArrayList<>();
    Chaveamento chaveamento;

    private boolean emTransacao;

    @Override
    public synchronized <T> T emTransacao(Supplier<T> operacao) {
        if (emTransacao) {
            return operacao.get();
        }
        Instantaneo antes = new Instantaneo();
        emTransacao = true;
        try {
            return operacao.get();
        } catch (RuntimeException e) {
            antes.restaurar();
            throw e;
        } finally {
            emTransacao = false;
        }
    }

    public RepositorioPartidasMemoria repositorioPartidas() {
        return new RepositorioPartidasMemoria(this);
    }

    public RepositorioSelecoesMemoria repositorioSelecoes() {
        return new RepositorioSelecoesMemoria(this);
    }

    public RepositorioSedesMemoria repositorioSedes() {
        return new RepositorioSedesMemoria(this);
    }

    public RepositorioGruposMemoria repositorioGrupos() {
        return new RepositorioGruposMemoria(this);
    }

    public RepositorioChaveamentoMemoria repositorioChaveamento() {
        return new RepositorioChaveamentoMemoria(this);
    }

    public RepositorioAdministradoresMemoria repositorioAdministradores() {
        return new RepositorioAdministradoresMemoria(this);
    }

    public RepositorioRegistrosMemoria repositorioRegistros() {
        return new RepositorioRegistrosMemoria(this);
    }

    static int proximoId(Map<Integer, ?> mapa) {
        return mapa.isEmpty() ? 1 : ((TreeMap<Integer, ?>) mapa).lastKey() + 1;
    }

    /** Cópia do estado da base, restaurável em caso de falha. */
    private final class Instantaneo {

        private final Cenario competicao;
        private final boolean semChaveamento;
        private final Map<Integer, Selecao> selecoesAntes = new HashMap<>();
        private final Map<Integer, Sede> sedesAntes = new HashMap<>();
        private final int totalRegistros;

        Instantaneo() {
            semChaveamento = chaveamento == null;
            Chaveamento ch = semChaveamento ? new Chaveamento(List.of()) : chaveamento;
            competicao = new Cenario(new ArrayList<>(grupos.values()), new ArrayList<>(partidas.values()), ch).copiar();
            selecoes.forEach((id, s) -> selecoesAntes.put(id, s.copiar()));
            sedes.forEach((id, s) -> sedesAntes.put(id, s.copiar()));
            totalRegistros = registros.size();
        }

        void restaurar() {
            partidas.clear();
            competicao.getPartidas().forEach(p -> partidas.put(p.getId(), p));
            grupos.clear();
            competicao.getGrupos().forEach(g -> grupos.put(g.getId(), g));
            chaveamento = semChaveamento ? null : competicao.getChaveamento();
            selecoes.keySet().retainAll(selecoesAntes.keySet());
            selecoesAntes.forEach((id, copia) -> {
                Selecao atual = selecoes.get(id);
                if (atual != null) {
                    atual.atualizarCom(copia);
                } else {
                    selecoes.put(id, copia);
                }
            });
            sedes.keySet().retainAll(sedesAntes.keySet());
            sedesAntes.forEach((id, copia) -> {
                Sede atual = sedes.get(id);
                if (atual != null) {
                    atual.atualizarCom(copia);
                } else {
                    sedes.put(id, copia);
                }
            });
            while (registros.size() > totalRegistros) {
                registros.remove(registros.size() - 1);
            }
        }
    }
}
