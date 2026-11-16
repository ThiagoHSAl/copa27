package br.ufmg.dcc603.copa27.servico;

import br.ufmg.dcc603.copa27.dominio.modelo.Administrador;
import br.ufmg.dcc603.copa27.dominio.modelo.RegistroAlteracao;
import br.ufmg.dcc603.copa27.persistencia.RepositorioRegistros;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** Registra no log cada alteração feita pelo Administrador (CU19). */
public class ServicoAuditoria {

    private final RepositorioRegistros repositorio;
    private final Clock relogio;

    public ServicoAuditoria(RepositorioRegistros repositorio, Clock relogio) {
        this.repositorio = repositorio;
        this.relogio = relogio;
    }

    public ServicoAuditoria(RepositorioRegistros repositorio) {
        this(repositorio, Clock.systemDefaultZone());
    }

    public void registrar(Administrador autor, String operacao, String dado) {
        repositorio.registrar(new RegistroAlteracao(LocalDateTime.now(relogio), operacao, dado, autor));
    }

    public List<RegistroAlteracao> listar() {
        return repositorio.listar();
    }
}
