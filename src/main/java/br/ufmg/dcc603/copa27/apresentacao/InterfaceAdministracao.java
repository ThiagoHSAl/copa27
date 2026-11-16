package br.ufmg.dcc603.copa27.apresentacao;

import br.ufmg.dcc603.copa27.dominio.modelo.Administrador;
import br.ufmg.dcc603.copa27.dominio.modelo.Chaveamento;
import br.ufmg.dcc603.copa27.dominio.modelo.Confederacao;
import br.ufmg.dcc603.copa27.dominio.modelo.Confronto;
import br.ufmg.dcc603.copa27.dominio.modelo.Partida;
import br.ufmg.dcc603.copa27.dominio.modelo.RegistroAlteracao;
import br.ufmg.dcc603.copa27.dominio.modelo.RegraNegocioException;
import br.ufmg.dcc603.copa27.dominio.modelo.Resultado;
import br.ufmg.dcc603.copa27.dominio.modelo.Sede;
import br.ufmg.dcc603.copa27.dominio.modelo.Selecao;
import br.ufmg.dcc603.copa27.dominio.modelo.SituacaoPartida;
import br.ufmg.dcc603.copa27.servico.AutenticacaoException;
import br.ufmg.dcc603.copa27.servico.NaoEncontradoException;
import br.ufmg.dcc603.copa27.servico.ServicoAdministracao;
import br.ufmg.dcc603.copa27.servico.ServicoAuditoria;
import br.ufmg.dcc603.copa27.servico.ServicoAutenticacao;
import br.ufmg.dcc603.copa27.servico.ServicoCompeticao;
import br.ufmg.dcc603.copa27.servico.ServicoConsulta;
import br.ufmg.dcc603.copa27.servico.TipoDado;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static br.ufmg.dcc603.copa27.apresentacao.Html.esc;

/**
 * Interface de Administração (CU16 a CU18). Só libera as funções de
 * manutenção depois da autenticação (RNF09). Para listar os dados a manter,
 * usa as consultas, pois o Administrador também é um Visitante.
 */
class InterfaceAdministracao {

    private final ServicoAutenticacao autenticacao;
    private final ServicoAdministracao administracao;
    private final ServicoAuditoria auditoria;
    private final ServicoConsulta consulta;
    private final ServicoCompeticao competicao;
    private final Map<String, Administrador> sessoes = new ConcurrentHashMap<>();

    InterfaceAdministracao(ServicoAutenticacao autenticacao, ServicoAdministracao administracao,
                           ServicoAuditoria auditoria, ServicoConsulta consulta, ServicoCompeticao competicao) {
        this.autenticacao = autenticacao;
        this.administracao = administracao;
        this.auditoria = auditoria;
        this.consulta = consulta;
        this.competicao = competicao;
    }

    /** Administrador da sessão, ou null se ela não estiver autenticada. */
    private Administrador autenticado(Requisicao req) {
        return sessoes.get(req.sessao());
    }

    /** Executa a rota apenas para sessões autenticadas; as demais vão para o login. */
    void restrito(Requisicao req, RotaAdmin rota) throws IOException {
        Administrador a = autenticado(req);
        if (a == null) {
            req.redirecionar("/admin/login");
            return;
        }
        rota.tratar(req, a);
    }

    @FunctionalInterface
    interface RotaAdmin {
        void tratar(Requisicao req, Administrador autor) throws IOException;
    }

    // ---------------------------------------------------------------- CU18

    void login(Requisicao req) throws IOException {
        if (req.isPost()) {
            try {
                Administrador a = autenticacao.autenticar(req.param("login"), req.param("senha"));
                sessoes.put(req.sessao(), a);
                req.redirecionar("/admin");
            } catch (AutenticacaoException e) {
                req.redirecionar(Requisicao.url("/admin/login", "erro", e.getMessage()));
            }
            return;
        }
        if (autenticado(req) != null) {
            req.redirecionar("/admin");
            return;
        }
        String corpo = "<form class=\"cartao login\" method=\"post\" action=\"/admin/login\">"
                + "<h1>Administração</h1><p class=\"muted\" style=\"margin:0\">Área restrita à manutenção dos dados do torneio.</p>"
                + Html.mensagens(req)
                + "<label>Usuário<input name=\"login\" autocomplete=\"username\" required></label>"
                + "<label>Senha<input type=\"password\" name=\"senha\" autocomplete=\"current-password\" required></label>"
                + "<button type=\"submit\">Entrar</button></form>";
        req.html(200, Html.pagina("Login", corpo, false, "/admin", ""));
    }

    void sair(Requisicao req) throws IOException {
        sessoes.remove(req.sessao());
        req.redirecionar("/");
    }

    // ---------------------------------------------------------------- CU17

    void painel(Requisicao req, Administrador autor) throws IOException {
        Chaveamento ch = competicao.obterChaveamento();
        StringBuilder sb = new StringBuilder("<h1>Partidas e resultados</h1>")
                .append("<p>Conectado como <strong>").append(esc(autor.getLogin())).append("</strong>. ")
                .append("Inicie a partida para registrar o resultado; marque \"encerrar\" no placar final. ")
                .append("Ao encerrar a última partida da fase de grupos, as oitavas são preenchidas automaticamente.</p>")
                .append(Html.mensagens(req))
                .append("<div class=\"cartao rolagem\"><table class=\"dados\"><thead><tr><th>Nº</th><th>Data</th>")
                .append("<th>Fase</th><th>Partida</th><th>Situação</th><th>Ações</th></tr></thead><tbody>");
        for (Partida p : consulta.listarPartidas(null)) {
            Confronto c = ch == null ? null : ch.confrontoDe(p);
            sb.append("<tr><td>").append(p.getId()).append("</td><td>").append(p.getDataHora().format(Html.DATA_HORA))
                    .append("</td><td>").append(esc(p.getFase().getDescricao()))
                    .append(p.getGrupo() == null ? "" : " " + p.getGrupo().getLetra()).append("</td><td>")
                    .append(Html.time(p.getMandante(), c == null ? null : c.getVagaMandante(), "p")).append("<br>")
                    .append(Html.time(p.getVisitante(), c == null ? null : c.getVagaVisitante(), "p")).append("</td><td>")
                    .append(Html.seloSituacao(p)).append("</td><td>");
            if (p.getSituacao() == SituacaoPartida.AGENDADA) {
                sb.append("<form class=\"inline\" method=\"post\" action=\"/admin/iniciar\">")
                        .append("<input type=\"hidden\" name=\"partida\" value=\"").append(p.getId()).append("\">")
                        .append("<button type=\"submit\">Iniciar</button></form> ");
            }
            sb.append(formularioPlacar(p)).append("</td></tr>");
        }
        sb.append("</tbody></table></div>");
        req.html(200, Html.pagina("Administração", sb.toString(), true, "/admin", ""));
    }

    /** Formulário de placar do administrador, com prorrogação, pênaltis e encerramento. */
    private static String formularioPlacar(Partida p) {
        Resultado r = p.getResultado();
        StringBuilder sb = new StringBuilder("<form class=\"form-placar\" method=\"post\" action=\"/admin/resultado\">")
                .append("<input type=\"hidden\" name=\"partida\" value=\"").append(p.getId()).append("\">")
                .append(caixa("gm", r == null ? null : r.getGolsMandante(), "Gols da mandante", true)).append(" x ")
                .append(caixa("gv", r == null ? null : r.getGolsVisitante(), "Gols da visitante", true));
        if (p.getFase().isMataMata()) {
            sb.append("<label><input type=\"checkbox\" name=\"prorrogacao\"")
                    .append(r != null && r.isHouveProrrogacao() ? " checked" : "").append("> prorrogação</label>")
                    .append("<label>pên.").append(caixa("pm", r == null ? null : r.getPenaltisMandante(), "Pênaltis da mandante", false))
                    .append(" x ").append(caixa("pv", r == null ? null : r.getPenaltisVisitante(), "Pênaltis da visitante", false))
                    .append("</label>");
        }
        if (p.getSituacao() != SituacaoPartida.ENCERRADA) {
            sb.append("<label><input type=\"checkbox\" name=\"encerrar\"> encerrar</label>");
        }
        return sb.append("<button type=\"submit\">Salvar</button></form>").toString();
    }

    private static String caixa(String nome, Integer valor, String rotulo, boolean obrigatoria) {
        return "<input class=\"caixa" + (valor != null ? " preenchida" : "") + "\" type=\"number\" min=\"0\" name=\""
                + nome + "\" aria-label=\"" + rotulo + "\" value=\"" + (valor == null ? "" : valor) + "\""
                + (obrigatoria ? " required" : "") + ">";
    }

    void iniciar(Requisicao req, Administrador autor) throws IOException {
        executar(req, "/admin", () -> {
            int id = InterfacePublica.obrigatorio(req.paramInt("partida"));
            administracao.iniciarPartida(id, autor);
            return "Partida " + id + " iniciada.";
        });
    }

    void registrarResultado(Requisicao req, Administrador autor) throws IOException {
        executar(req, "/admin", () -> {
            int id = InterfacePublica.obrigatorio(req.paramInt("partida"));
            boolean encerrar = req.param("encerrar") != null;
            administracao.registrarResultado(id, InterfacePublica.lerResultado(req), encerrar, autor);
            return "Resultado da partida " + id + " registrado" + (encerrar ? " e partida encerrada." : ".");
        });
    }

    // ---------------------------------------------------------------- CU16

    void selecoes(Requisicao req, Administrador autor) throws IOException {
        StringBuilder sb = new StringBuilder("<h1>Seleções</h1>").append(Html.mensagens(req))
                .append("<div class=\"cartao rolagem\"><table class=\"dados\"><thead><tr><th>Seleção</th><th>Editar</th>")
                .append("<th>Remover</th></tr></thead><tbody>");
        for (Selecao s : consulta.listarSelecoes()) {
            sb.append("<tr><td>").append(Html.time(s, null, "p")).append("</td><td>").append(formularioSelecao(s))
                    .append("</td><td>").append(botaoRemover(TipoDado.SELECAO, s.getId(), "/admin/selecoes"))
                    .append("</td></tr>");
        }
        sb.append("</tbody></table></div><h2 class=\"titulo-secao\">Cadastrar seleção</h2><div class=\"cartao\" style=\"padding:14px\">")
                .append(formularioSelecao(null)).append("</div>");
        req.html(200, Html.pagina("Seleções", sb.toString(), true, "/admin/selecoes", ""));
    }

    private static String formularioSelecao(Selecao s) {
        StringBuilder sb = new StringBuilder("<form class=\"form-placar\" method=\"post\" action=\"/admin/selecoes/salvar\">")
                .append("<input type=\"hidden\" name=\"id\" value=\"").append(s == null ? 0 : s.getId()).append("\">")
                .append("<input name=\"pais\" required aria-label=\"País\" placeholder=\"País\" value=\"")
                .append(s == null ? "" : esc(s.getPais())).append("\"> ")
                .append("<input name=\"codigo\" required maxlength=\"3\" size=\"4\" aria-label=\"Código FIFA\" ")
                .append("placeholder=\"ABC\" value=\"").append(s == null ? "" : esc(s.getCodigoFifa())).append("\"> ")
                .append("<select name=\"confederacao\" aria-label=\"Confederação\">");
        for (Confederacao c : Confederacao.values()) {
            sb.append("<option").append(s != null && s.getConfederacao() == c ? " selected" : "").append(">")
                    .append(c).append("</option>");
        }
        return sb.append("</select> <button type=\"submit\">Salvar</button></form>").toString();
    }

    void salvarSelecao(Requisicao req, Administrador autor) throws IOException {
        executar(req, "/admin/selecoes", () -> {
            int id = req.paramInt("id") == null ? 0 : req.paramInt("id");
            Confederacao conf;
            try {
                conf = Confederacao.valueOf(req.param("confederacao"));
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new RegraNegocioException("Confederação inválida.");
            }
            String codigo = req.param("codigo") == null ? null : req.param("codigo").toUpperCase();
            Selecao s = new Selecao(id, req.param("pais"), codigo, conf);
            if (id != 0) {
                Selecao atual = consulta.buscarSelecao(id);
                s.getElenco().addAll(atual.getElenco());
                s.getComissao().addAll(atual.getComissao());
                s.getHistorico().addAll(atual.getHistorico());
            }
            administracao.salvar(s, autor);
            return "Seleção " + s.getPais() + " salva.";
        });
    }

    void sedes(Requisicao req, Administrador autor) throws IOException {
        StringBuilder sb = new StringBuilder("<h1>Sedes</h1>").append(Html.mensagens(req))
                .append("<p>Uma sede com partidas não pode ser removida.</p>")
                .append("<div class=\"cartao rolagem\"><table class=\"dados\"><thead><tr><th>Cidade</th><th>Estádio</th>")
                .append("<th>Capacidade</th><th>Remover</th></tr></thead><tbody>");
        for (Sede s : consulta.listarSedes()) {
            sb.append("<tr><td>").append(esc(s.getCidade())).append("</td><td>").append(esc(s.getEstadio().getNome()))
                    .append("</td><td>").append(s.getEstadio().getCapacidade()).append("</td><td>")
                    .append(botaoRemover(TipoDado.SEDE, s.getId(), "/admin/sedes")).append("</td></tr>");
        }
        sb.append("</tbody></table></div>");
        req.html(200, Html.pagina("Sedes", sb.toString(), true, "/admin/sedes", ""));
    }

    private static String botaoRemover(TipoDado tipo, int id, String voltar) {
        return "<form class=\"form-placar\" method=\"post\" action=\"/admin/remover\">"
                + "<input type=\"hidden\" name=\"tipo\" value=\"" + tipo + "\">"
                + "<input type=\"hidden\" name=\"id\" value=\"" + id + "\">"
                + "<input type=\"hidden\" name=\"voltar\" value=\"" + esc(voltar) + "\">"
                + "<label><input type=\"checkbox\" name=\"confirmar\" required> confirmo</label> "
                + "<button type=\"submit\" class=\"secundario\">Remover</button></form>";
    }

    void remover(Requisicao req, Administrador autor) throws IOException {
        String voltar = req.param("voltar");
        if (voltar == null || !voltar.startsWith("/admin")) {
            voltar = "/admin";
        }
        executar(req, voltar, () -> {
            if (req.param("confirmar") == null) {
                throw new RegraNegocioException("Confirme a remoção.");
            }
            TipoDado tipo;
            try {
                tipo = TipoDado.valueOf(req.param("tipo"));
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new RegraNegocioException("Tipo de dado inválido.");
            }
            administracao.remover(tipo, InterfacePublica.obrigatorio(req.paramInt("id")), autor);
            return "Remoção concluída.";
        });
    }

    void log(Requisicao req, Administrador autor) throws IOException {
        List<RegistroAlteracao> registros = auditoria.listar();
        StringBuilder sb = new StringBuilder("<h1>Log de alterações</h1>");
        if (registros.isEmpty()) {
            sb.append("<p>Nenhuma alteração registrada.</p>");
        } else {
            sb.append("<div class=\"cartao rolagem\"><table class=\"dados\"><thead><tr><th>Data e hora</th><th>Autor</th>")
                    .append("<th>Operação</th><th>Dado</th></tr></thead><tbody>");
            for (int i = registros.size() - 1; i >= 0; i--) {
                RegistroAlteracao r = registros.get(i);
                sb.append("<tr><td>").append(r.getDataHora().format(Html.DATA_HORA)).append("</td><td>")
                        .append(esc(r.getAutor().getLogin())).append("</td><td>").append(esc(r.getOperacao()))
                        .append("</td><td>").append(esc(r.getDado())).append("</td></tr>");
            }
            sb.append("</tbody></table></div>");
        }
        req.html(200, Html.pagina("Log", sb.toString(), true, "/admin/log", ""));
    }

    @FunctionalInterface
    private interface Operacao {
        String executar();
    }

    /** Executa a operação e volta à página com a mensagem de sucesso ou de erro. */
    private static void executar(Requisicao req, String voltar, Operacao op) throws IOException {
        String destino;
        try {
            destino = Requisicao.url(voltar, "msg", op.executar());
        } catch (RegraNegocioException | NaoEncontradoException e) {
            destino = Requisicao.url(voltar, "erro", e.getMessage());
        }
        req.redirecionar(destino);
    }
}
