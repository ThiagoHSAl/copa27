package br.ufmg.dcc603.copa27.apresentacao;

import br.ufmg.dcc603.copa27.dominio.modelo.Aeroporto;
import br.ufmg.dcc603.copa27.dominio.modelo.Cenario;
import br.ufmg.dcc603.copa27.dominio.modelo.Chaveamento;
import br.ufmg.dcc603.copa27.dominio.modelo.Classificacao;
import br.ufmg.dcc603.copa27.dominio.modelo.Confronto;
import br.ufmg.dcc603.copa27.dominio.modelo.Fase;
import br.ufmg.dcc603.copa27.dominio.modelo.Grupo;
import br.ufmg.dcc603.copa27.dominio.modelo.LinhaClassificacao;
import br.ufmg.dcc603.copa27.dominio.modelo.Partida;
import br.ufmg.dcc603.copa27.dominio.modelo.RegraNegocioException;
import br.ufmg.dcc603.copa27.dominio.modelo.Resultado;
import br.ufmg.dcc603.copa27.dominio.modelo.Sede;
import br.ufmg.dcc603.copa27.dominio.modelo.Selecao;
import br.ufmg.dcc603.copa27.servico.FiltroPartidas;
import br.ufmg.dcc603.copa27.servico.ServicoCompeticao;
import br.ufmg.dcc603.copa27.servico.ServicoConsulta;
import br.ufmg.dcc603.copa27.servico.ServicoSimulacao;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static br.ufmg.dcc603.copa27.apresentacao.Html.esc;

/**
 * Interface Pública: atende o Visitante (CU01 a CU14). Mantém o cenário
 * simulado de cada visitante associado à sessão do navegador, apenas em
 * memória; ele nunca é gravado na base nem compartilhado (RN07).
 */
class InterfacePublica {

    private final ServicoConsulta consulta;
    private final ServicoCompeticao competicao;
    private final ServicoSimulacao simulacao;
    private final Map<String, Cenario> simulacoes = new ConcurrentHashMap<>();

    InterfacePublica(ServicoConsulta consulta, ServicoCompeticao competicao, ServicoSimulacao simulacao) {
        this.consulta = consulta;
        this.competicao = competicao;
        this.simulacao = simulacao;
    }

    // ------------------------------------------------------------ início

    void inicio(Requisicao req) throws IOException {
        String corpo = "<h1>Copa do Mundo Feminina Brasil 2027</h1>"
                + "<p>De 24 de junho a 25 de julho de 2027: 32 seleções, 64 partidas e oito cidades-sede.</p>"
                + "<ul><li><a href=\"/partidas\">Tabela de jogos</a>, com filtros por seleção, sede, fase e datas</li>"
                + "<li><a href=\"/classificacao\">Classificação dos grupos</a></li>"
                + "<li><a href=\"/chaveamento\">Chaveamento do mata-mata</a></li>"
                + "<li><a href=\"/simulacao\">Simular cruzamentos futuros</a> e ver o caminho de uma seleção até a final</li>"
                + "<li><a href=\"/sedes\">Cidades-sede</a>, com estádio, aeroportos e partidas</li>"
                + "<li><a href=\"/selecoes\">Seleções participantes</a></li></ul>";
        req.html(200, Html.pagina("Início", corpo, false));
    }

    // ------------------------------------------------------- CU01 a CU03

    void partidas(Requisicao req) throws IOException {
        StringBuilder sb = new StringBuilder("<h1>Tabela de jogos</h1>");
        FiltroPartidas filtro;
        String erro = null;
        try {
            filtro = new FiltroPartidas(req.paramInt("selecao"), req.paramInt("sede"), fase(req.param("fase")),
                    data(req.param("de")), data(req.param("ate")));
        } catch (RegraNegocioException e) {
            erro = e.getMessage();
            filtro = FiltroPartidas.nenhum();
        }
        sb.append(formularioFiltro(req));
        if (erro != null) {
            sb.append("<p class=\"erro\" role=\"alert\">").append(esc(erro)).append("</p>");
        }
        List<Partida> lista = consulta.listarPartidas(filtro);
        if (lista.isEmpty()) {
            sb.append(filtro.estaVazio() ? "<p>A tabela de jogos ainda não está disponível.</p>"
                    : "<p>Nenhuma partida atende aos filtros escolhidos.</p>");
        } else {
            if (!filtro.estaVazio()) {
                sb.append("<p>").append(lista.size()).append(" partida(s) com os filtros ativos. ")
                        .append("<a href=\"/partidas\">Limpar filtros</a></p>");
            }
            sb.append(tabelaPartidas(lista));
        }
        req.html(200, Html.pagina("Tabela de jogos", sb.toString(), false));
    }

    private String formularioFiltro(Requisicao req) {
        StringBuilder sb = new StringBuilder("<form class=\"filtros\" method=\"get\" action=\"/partidas\">");
        sb.append("<label>Seleção<select name=\"selecao\"><option value=\"\">Todas</option>");
        for (Selecao s : consulta.listarSelecoes()) {
            sb.append(opcao(String.valueOf(s.getId()), s.getPais(), req.param("selecao")));
        }
        sb.append("</select></label><label>Sede<select name=\"sede\"><option value=\"\">Todas</option>");
        for (Sede s : consulta.listarSedes()) {
            sb.append(opcao(String.valueOf(s.getId()), s.getCidade(), req.param("sede")));
        }
        sb.append("</select></label><label>Fase<select name=\"fase\"><option value=\"\">Todas</option>");
        for (Fase f : Fase.values()) {
            sb.append(opcao(f.name(), f.getDescricao(), req.param("fase")));
        }
        sb.append("</select></label>")
                .append("<label>De<input type=\"date\" name=\"de\" value=\"").append(esc(req.param("de"))).append("\"></label>")
                .append("<label>Até<input type=\"date\" name=\"ate\" value=\"").append(esc(req.param("ate"))).append("\"></label>")
                .append("<button type=\"submit\">Filtrar</button></form>");
        return sb.toString();
    }

    private static String opcao(String valor, String rotulo, String selecionado) {
        return "<option value=\"" + esc(valor) + "\"" + (valor.equals(selecionado) ? " selected" : "") + ">"
                + esc(rotulo) + "</option>";
    }

    private static Fase fase(String s) {
        if (s == null) {
            return null;
        }
        try {
            return Fase.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw new RegraNegocioException("Fase inválida.");
        }
    }

    private static LocalDate data(String s) {
        if (s == null) {
            return null;
        }
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            throw new RegraNegocioException("Data inválida: " + s);
        }
    }

    private String tabelaPartidas(List<Partida> lista) {
        Chaveamento ch = competicao.obterChaveamento();
        StringBuilder sb = new StringBuilder("<table><thead><tr><th>Nº</th><th>Data e hora</th><th>Fase</th>"
                + "<th>Grupo</th><th>Partida</th><th>Placar</th><th>Sede</th></tr></thead><tbody>");
        for (Partida p : lista) {
            Confronto c = ch == null ? null : ch.confrontoDe(p);
            sb.append("<tr><td>").append(p.getId()).append("</td><td>").append(p.getDataHora().format(Html.DATA_HORA))
                    .append("</td><td>").append(esc(p.getFase().getDescricao())).append("</td><td>")
                    .append(p.getGrupo() == null ? "" : p.getGrupo().getLetra()).append("</td><td>")
                    .append("<a href=\"/partida?id=").append(p.getId()).append("\">")
                    .append(Html.nomeMandante(p, c == null ? null : c.getVagaMandante())).append(" x ")
                    .append(Html.nomeVisitante(p, c == null ? null : c.getVagaVisitante())).append("</a></td><td>")
                    .append(Html.placar(p)).append("</td><td>").append(esc(cidade(p))).append("</td></tr>");
        }
        return sb.append("</tbody></table>").toString();
    }

    private static String cidade(Partida p) {
        return p.getEstadio().getSede() == null ? "" : p.getEstadio().getSede().getCidade();
    }

    void partida(Requisicao req) throws IOException {
        Partida p = consulta.buscarPartida(obrigatorio(req.paramInt("id")));
        Chaveamento ch = competicao.obterChaveamento();
        Confronto c = ch == null ? null : ch.confrontoDe(p);
        StringBuilder sb = new StringBuilder("<h1>Partida ").append(p.getId()).append("</h1><table><tbody>");
        linha(sb, "Confronto", Html.nomeMandante(p, c == null ? null : c.getVagaMandante()) + " x "
                + Html.nomeVisitante(p, c == null ? null : c.getVagaVisitante()));
        linha(sb, "Fase", esc(p.getFase().getDescricao()) + (p.getGrupo() == null ? "" : ", Grupo " + p.getGrupo().getLetra()));
        linha(sb, "Data e hora", p.getDataHora().format(Html.DATA_HORA) + " (horário de Brasília)");
        linha(sb, "Estádio", esc(p.getEstadio().getNome()));
        linha(sb, "Cidade", esc(cidade(p)));
        linha(sb, "Situação", esc(p.getSituacao().getDescricao()));
        Resultado r = p.getResultado();
        if (r != null) {
            linha(sb, p.estaDisputada() ? "Placar final" : "Placar parcial",
                    r.getGolsMandante() + " x " + r.getGolsVisitante());
            if (r.isHouveProrrogacao()) {
                linha(sb, "Prorrogação", "Sim; o placar inclui os gols da prorrogação");
            }
            if (r.temPenaltis()) {
                linha(sb, "Pênaltis", r.getPenaltisMandante() + " x " + r.getPenaltisVisitante());
            }
        }
        sb.append("</tbody></table><p><a href=\"/partidas\">Voltar à tabela de jogos</a></p>");
        req.html(200, Html.pagina("Partida " + p.getId(), sb.toString(), false));
    }

    private static void linha(StringBuilder sb, String rotulo, String valorHtml) {
        sb.append("<tr><th scope=\"row\">").append(esc(rotulo)).append("</th><td>").append(valorHtml).append("</td></tr>");
    }

    // ------------------------------------------------------- CU04 e CU05

    void classificacao(Requisicao req) throws IOException {
        String corpo = "<h1>Classificação dos grupos</h1>"
                + "<p>Critérios: pontos, saldo de gols, gols pró e confronto direto. "
                + "As duas primeiras de cada grupo, em destaque, avançam às oitavas de final.</p>"
                + tabelasClassificacao(competicao.obterGrupos());
        req.html(200, Html.pagina("Classificação", corpo, false));
    }

    static String tabelasClassificacao(List<Grupo> grupos) {
        StringBuilder sb = new StringBuilder("<div class=\"grade\">");
        for (Grupo g : grupos) {
            Classificacao c = g.getClassificacao();
            sb.append("<section><h2>Grupo ").append(g.getLetra()).append("</h2><table><thead><tr>")
                    .append("<th>#</th><th>Seleção</th><th title=\"Pontos\">P</th><th title=\"Jogos\">J</th>")
                    .append("<th title=\"Vitórias\">V</th><th title=\"Empates\">E</th><th title=\"Derrotas\">D</th>")
                    .append("<th title=\"Gols pró\">GP</th><th title=\"Gols contra\">GC</th><th title=\"Saldo\">SG</th>")
                    .append("</tr></thead><tbody>");
            int pos = 1;
            for (LinhaClassificacao l : c.getLinhas()) {
                sb.append(pos <= 2 ? "<tr class=\"classifica\">" : "<tr>")
                        .append("<td>").append(pos++).append("</td><td>").append(esc(l.getSelecao().getPais()))
                        .append("</td><td><strong>").append(l.pontos()).append("</strong></td><td>").append(l.getJogos())
                        .append("</td><td>").append(l.getVitorias()).append("</td><td>").append(l.getEmpates())
                        .append("</td><td>").append(l.getDerrotas()).append("</td><td>").append(l.getGolsPro())
                        .append("</td><td>").append(l.getGolsContra()).append("</td><td>").append(l.saldo())
                        .append("</td></tr>");
            }
            sb.append("</tbody></table>");
            if (c.isDefinitiva()) {
                sb.append("<p><small>Classificação definitiva.</small></p>");
            }
            sb.append("</section>");
        }
        return sb.append("</div>").toString();
    }

    void chaveamento(Requisicao req) throws IOException {
        Chaveamento ch = competicao.obterChaveamento();
        String corpo = "<h1>Chaveamento do mata-mata</h1>"
                + "<p>Enquanto as vagas não são definidas, cada confronto mostra a origem da vaga.</p>"
                + arvore(ch);
        req.html(200, Html.pagina("Chaveamento", corpo, false));
    }

    static String arvore(Chaveamento ch) {
        if (ch == null) {
            return "<p>O chaveamento ainda não está disponível.</p>";
        }
        StringBuilder sb = new StringBuilder("<div class=\"chave\">");
        coluna(sb, "Oitavas de final", ch.confrontosDaFase(Fase.OITAVAS));
        coluna(sb, "Quartas de final", ch.confrontosDaFase(Fase.QUARTAS));
        coluna(sb, "Semifinais", ch.confrontosDaFase(Fase.SEMIFINAL));
        coluna(sb, "Final", ch.confrontosDaFase(Fase.FINAL));
        coluna(sb, "Terceiro lugar", ch.confrontosDaFase(Fase.TERCEIRO_LUGAR));
        return sb.append("</div>").toString();
    }

    private static void coluna(StringBuilder sb, String titulo, List<Confronto> confrontos) {
        sb.append("<div class=\"coluna\"><h3>").append(esc(titulo)).append("</h3>");
        for (Confronto c : confrontos) {
            sb.append(cartao(c));
        }
        sb.append("</div>");
    }

    static String cartao(Confronto c) {
        Partida p = c.getPartida();
        Selecao venc = p.vencedora();
        String m = Html.nomeMandante(p, c.getVagaMandante());
        String v = Html.nomeVisitante(p, c.getVagaVisitante());
        if (venc != null && venc == p.getMandante()) {
            m = "<span class=\"venc\">" + m + "</span>";
        } else if (venc != null) {
            v = "<span class=\"venc\">" + v + "</span>";
        }
        return "<div class=\"confronto\"><small>Jogo " + p.getId() + ", " + p.getDataHora().format(Html.DATA)
                + ", " + esc(cidade(p)) + "</small><br>" + m + "<br>" + v
                + (p.getResultado() == null ? "" : "<br><small>" + Html.placar(p) + "</small>") + "</div>";
    }

    // ------------------------------------------------------- CU06 a CU09

    private Cenario cenarioDaSessao(Requisicao req) {
        return simulacoes.computeIfAbsent(req.sessao(), k -> simulacao.iniciar());
    }

    void simulacao(Requisicao req) throws IOException {
        Cenario c = cenarioDaSessao(req);
        StringBuilder sb = new StringBuilder("<h1>Simular cruzamentos futuros</h1>")
                .append("<p>Informe placares hipotéticos para as partidas ainda não disputadas. A simulação vale só ")
                .append("para você, nesta sessão do navegador, e não altera os dados reais do torneio.</p>")
                .append(Html.mensagens(req))
                .append("<p><form class=\"inline\" method=\"post\" action=\"/simulacao/restaurar\">")
                .append("<button type=\"submit\" class=\"secundario\">Restaurar cenário real</button></form> ")
                .append("<form class=\"inline\" method=\"post\" action=\"/simulacao/aleatorio\">")
                .append("<button type=\"submit\" class=\"secundario\">Preencher fase de grupos com placares aleatórios</button>")
                .append("</form></p>");
        sb.append(secaoCaminho(req, c));
        sb.append("<h2>Chaveamento simulado</h2>").append(arvore(c.getChaveamento()));
        sb.append("<h2>Classificação simulada</h2>").append(tabelasClassificacao(c.getGrupos()));
        sb.append("<h2>Partidas não disputadas</h2>").append(formulariosSimulacao(c));
        req.html(200, Html.pagina("Simulação", sb.toString(), false));
    }

    private String secaoCaminho(Requisicao req, Cenario c) {
        StringBuilder sb = new StringBuilder("<h2>Caminho até a final</h2>")
                .append("<form class=\"filtros\" method=\"get\" action=\"/simulacao\"><label>Seleção")
                .append("<select name=\"caminho\">");
        Integer escolhida = req.paramInt("caminho");
        for (Grupo g : c.getGrupos()) {
            for (Selecao s : g.getSelecoes()) {
                sb.append(opcao(String.valueOf(s.getId()), s.getPais(), escolhida == null ? null : escolhida.toString()));
            }
        }
        sb.append("</select></label><button type=\"submit\">Ver caminho</button></form>");
        if (escolhida == null) {
            return sb.toString();
        }
        Selecao s = c.buscarSelecao(escolhida);
        List<Confronto> caminho = simulacao.caminhoAteFinal(c, escolhida);
        if (caminho.isEmpty()) {
            Grupo g = c.grupoDa(s);
            boolean definitiva = g != null && g.getClassificacao().isDefinitiva();
            sb.append("<p>").append(esc(s.getPais())).append(definitiva
                    ? " não se classifica neste cenário: é eliminada na fase de grupos."
                    : " ainda não tem vaga definida no mata-mata; simule as partidas restantes do grupo.").append("</p>");
            return sb.toString();
        }
        sb.append("<ol>");
        for (Confronto cf : caminho) {
            Partida p = cf.getPartida();
            String adversaria;
            if (p.getMandante() == s) {
                adversaria = Html.nomeVisitante(p, cf.getVagaVisitante());
            } else if (p.getVisitante() == s) {
                adversaria = Html.nomeMandante(p, cf.getVagaMandante());
            } else {
                adversaria = "<span class=\"vaga\">a definir: " + Html.nomeMandante(p, cf.getVagaMandante())
                        + " ou " + Html.nomeVisitante(p, cf.getVagaVisitante()) + "</span>";
            }
            sb.append("<li><strong>").append(esc(cf.getFase().getDescricao())).append("</strong>, jogo ")
                    .append(p.getId()).append(", ").append(p.getDataHora().format(Html.DATA_HORA)).append(", ")
                    .append(esc(cidade(p))).append(": contra ").append(adversaria);
            if (p.getResultado() != null) {
                sb.append(" (").append(Html.placar(p)).append(")");
            }
            Selecao venc = p.vencedora();
            if (venc != null && venc != s) {
                sb.append(". <strong>Eliminada nesta fase.</strong>");
            }
            sb.append("</li>");
        }
        sb.append("</ol>");
        Confronto ultimo = caminho.get(caminho.size() - 1);
        if (ultimo.getFase() == Fase.FINAL && ultimo.getPartida().vencedora() == s) {
            sb.append("<p><strong>").append(esc(s.getPais())).append(" é campeã neste cenário.</strong></p>");
        }
        return sb.toString();
    }

    private String formulariosSimulacao(Cenario c) {
        List<Partida> abertas = c.partidasNaoDisputadas();
        if (abertas.isEmpty()) {
            return "<p>Todas as partidas já foram disputadas.</p>";
        }
        StringBuilder sb = new StringBuilder("<table><thead><tr><th>Nº</th><th>Fase</th><th>Partida</th>"
                + "<th>Placar simulado</th></tr></thead><tbody>");
        for (Partida p : abertas) {
            Confronto cf = c.getChaveamento().confrontoDe(p);
            sb.append("<tr><td>").append(p.getId()).append("</td><td>").append(esc(p.getFase().getDescricao()))
                    .append(p.getGrupo() == null ? "" : " " + p.getGrupo().getLetra()).append("</td><td>")
                    .append(Html.nomeMandante(p, cf == null ? null : cf.getVagaMandante())).append(" x ")
                    .append(Html.nomeVisitante(p, cf == null ? null : cf.getVagaVisitante())).append("</td><td>");
            if (p.getMandante() == null || p.getVisitante() == null) {
                sb.append("<span class=\"vaga\">Aguardando a definição das seleções</span>");
            } else {
                sb.append(formularioPlacar("/simulacao/placar", p, false));
                if (p.getResultado() != null) {
                    sb.append(" ").append(Html.placar(p));
                }
            }
            sb.append("</td></tr>");
        }
        return sb.append("</tbody></table>").toString();
    }

    /** Formulário de placar compartilhado pela simulação e pelo registro de resultados. */
    static String formularioPlacar(String acao, Partida p, boolean comEncerrar) {
        Resultado r = p.getResultado();
        StringBuilder sb = new StringBuilder("<form class=\"inline\" method=\"post\" action=\"").append(acao).append("\">")
                .append("<input type=\"hidden\" name=\"partida\" value=\"").append(p.getId()).append("\">")
                .append("<input type=\"number\" min=\"0\" name=\"gm\" required aria-label=\"Gols da mandante\" value=\"")
                .append(r == null ? "" : r.getGolsMandante()).append("\"> x ")
                .append("<input type=\"number\" min=\"0\" name=\"gv\" required aria-label=\"Gols da visitante\" value=\"")
                .append(r == null ? "" : r.getGolsVisitante()).append("\"> ");
        if (p.getFase().isMataMata()) {
            sb.append("<label><input type=\"checkbox\" name=\"prorrogacao\"")
                    .append(r != null && r.isHouveProrrogacao() ? " checked" : "").append("> prorrogação</label> ")
                    .append("pênaltis <input type=\"number\" min=\"0\" name=\"pm\" aria-label=\"Pênaltis da mandante\" value=\"")
                    .append(r == null || r.getPenaltisMandante() == null ? "" : r.getPenaltisMandante()).append("\"> x ")
                    .append("<input type=\"number\" min=\"0\" name=\"pv\" aria-label=\"Pênaltis da visitante\" value=\"")
                    .append(r == null || r.getPenaltisVisitante() == null ? "" : r.getPenaltisVisitante()).append("\"> ");
        }
        if (comEncerrar) {
            sb.append("<label><input type=\"checkbox\" name=\"encerrar\"> encerrar</label> ");
        }
        return sb.append("<button type=\"submit\">Salvar</button></form>").toString();
    }

    /** Lê o placar do formulário; valores ausentes ou negativos são rejeitados (CU06, E1). */
    static Resultado lerResultado(Requisicao req) {
        Integer gm = req.paramInt("gm");
        Integer gv = req.paramInt("gv");
        if (gm == null || gv == null) {
            throw new RegraNegocioException("Informe os gols das duas seleções.");
        }
        Integer pm = req.paramInt("pm");
        Integer pv = req.paramInt("pv");
        boolean prorrogacao = req.param("prorrogacao") != null || pm != null || pv != null;
        return new Resultado(gm, gv, prorrogacao, pm, pv);
    }

    void simularPlacar(Requisicao req) throws IOException {
        Cenario c = cenarioDaSessao(req);
        try {
            int id = obrigatorio(req.paramInt("partida"));
            simulacao.simular(c, id, lerResultado(req));
            req.redirecionar(Requisicao.url("/simulacao", "msg", "Placar da partida " + id + " simulado."));
        } catch (RegraNegocioException e) {
            req.redirecionar(Requisicao.url("/simulacao", "erro", e.getMessage()));
        }
    }

    void simularAleatorio(Requisicao req) throws IOException {
        Cenario c = cenarioDaSessao(req);
        Random aleatorio = new Random();
        for (Partida p : c.partidasNaoDisputadas()) {
            if (p.getFase() == Fase.GRUPOS) {
                simulacao.simular(c, p.getId(), Resultado.placar(aleatorio.nextInt(4), aleatorio.nextInt(4)));
            }
        }
        req.redirecionar(Requisicao.url("/simulacao", "msg", "Fase de grupos preenchida com placares aleatórios."));
    }

    void restaurar(Requisicao req) throws IOException {
        simulacoes.put(req.sessao(), simulacao.restaurar());
        req.redirecionar(Requisicao.url("/simulacao", "msg", "Simulação descartada; cenário real restaurado."));
    }

    // ------------------------------------------------------ CU10 e CU11

    void sedes(Requisicao req) throws IOException {
        StringBuilder sb = new StringBuilder("<h1>Cidades-sede</h1><table><thead><tr><th>Cidade</th><th>Região</th>"
                + "<th>Estádio</th></tr></thead><tbody>");
        for (Sede s : consulta.listarSedes()) {
            sb.append("<tr><td><a href=\"/sede?id=").append(s.getId()).append("\">").append(esc(s.getCidade()))
                    .append("</a></td><td>").append(esc(s.getRegiao().getDescricao())).append("</td><td>")
                    .append(esc(s.getEstadio().getNome())).append("</td></tr>");
        }
        sb.append("</tbody></table>");
        req.html(200, Html.pagina("Sedes", sb.toString(), false));
    }

    void sede(Requisicao req) throws IOException {
        int id = obrigatorio(req.paramInt("id"));
        Sede s = consulta.buscarSede(id);
        StringBuilder sb = new StringBuilder("<h1>").append(esc(s.getCidade())).append("</h1>")
                .append("<h2>Estádio</h2><table><tbody>");
        linha(sb, "Nome", esc(s.getEstadio().getNome()));
        linha(sb, "Capacidade", String.format("%,d lugares", s.getEstadio().getCapacidade()).replace(',', '.'));
        linha(sb, "Inauguração", String.valueOf(s.getEstadio().getAnoInauguracao()));
        linha(sb, "Endereço", esc(s.getEstadio().getEndereco()));
        sb.append("</tbody></table><h2>Aeroportos próximos</h2>")
                .append("<p>Ordenados pela distância até o estádio.</p><table><thead><tr><th>Aeroporto</th>")
                .append("<th>IATA</th><th>Distância</th></tr></thead><tbody>");
        for (Aeroporto a : s.getAeroportos()) {
            sb.append("<tr><td>").append(esc(a.nome())).append("</td><td>").append(esc(a.codigoIata()))
                    .append("</td><td>").append(String.format("%.1f km", a.distanciaKm()).replace('.', ','))
                    .append("</td></tr>");
        }
        sb.append("</tbody></table><h2>Partidas na sede</h2>");
        List<Partida> jogos = consulta.listarPartidasDaSede(id);
        sb.append(jogos.isEmpty() ? "<p>Nenhuma partida agendada nesta sede.</p>" : tabelaPartidas(jogos));
        req.html(200, Html.pagina(s.getCidade(), sb.toString(), false));
    }

    // ------------------------------------------------------ CU13 e CU14

    void selecoes(Requisicao req) throws IOException {
        StringBuilder sb = new StringBuilder("<h1>Seleções participantes</h1><table><thead><tr><th>País</th>"
                + "<th>Código FIFA</th><th>Confederação</th><th>Grupo</th></tr></thead><tbody>");
        List<Grupo> grupos = competicao.obterGrupos();
        for (Selecao s : consulta.listarSelecoes()) {
            String grupo = grupos.stream().filter(g -> g.contem(s)).map(g -> String.valueOf(g.getLetra()))
                    .findFirst().orElse("");
            sb.append("<tr><td><a href=\"/selecao?id=").append(s.getId()).append("\">").append(esc(s.getPais()))
                    .append("</a></td><td>").append(esc(s.getCodigoFifa())).append("</td><td>")
                    .append(s.getConfederacao()).append("</td><td>").append(grupo).append("</td></tr>");
        }
        sb.append("</tbody></table>");
        req.html(200, Html.pagina("Seleções", sb.toString(), false));
    }

    void selecao(Requisicao req) throws IOException {
        int id = obrigatorio(req.paramInt("id"));
        Selecao s = consulta.buscarSelecao(id);
        List<Partida> jogos = consulta.listarPartidasDaSelecao(id);
        List<Partida> disputadas = jogos.stream().filter(Partida::estaDisputada).toList();
        List<Partida> futuras = jogos.stream().filter(p -> !p.estaDisputada()).toList();
        StringBuilder sb = new StringBuilder("<h1>").append(esc(s.getPais())).append("</h1><p>Código FIFA ")
                .append(esc(s.getCodigoFifa())).append(", ").append(s.getConfederacao()).append(".</p>")
                .append("<h2>Partidas disputadas</h2>")
                .append(disputadas.isEmpty() ? "<p>Nenhuma partida disputada até agora.</p>" : tabelaPartidas(disputadas))
                .append("<h2>Próximas partidas</h2>")
                .append(futuras.isEmpty() ? "<p>Nenhuma partida futura definida.</p>" : tabelaPartidas(futuras));
        req.html(200, Html.pagina(s.getPais(), sb.toString(), false));
    }

    static int obrigatorio(Integer id) {
        if (id == null) {
            throw new RegraNegocioException("Identificador não informado.");
        }
        return id;
    }
}
