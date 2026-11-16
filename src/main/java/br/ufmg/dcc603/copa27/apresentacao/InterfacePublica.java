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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
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

    /** Rodadas do simulador: as três da fase de grupos e as fases do mata-mata. */
    record Rodada(String chave, String titulo, String fase, String rotuloCurto) {
    }

    static final List<Rodada> RODADAS = List.of(
            new Rodada("1", "Rodada 1", "Fase de grupos", "R1"),
            new Rodada("2", "Rodada 2", "Fase de grupos", "R2"),
            new Rodada("3", "Rodada 3", "Fase de grupos", "R3"),
            new Rodada("OITAVAS", "Oitavas de final", "Mata-mata", "Oitavas"),
            new Rodada("QUARTAS", "Quartas de final", "Mata-mata", "Quartas"),
            new Rodada("SEMIFINAL", "Semifinais", "Mata-mata", "Semis"),
            new Rodada("FINAL", "Final e 3º lugar", "Mata-mata", "Final"));

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
        List<Partida> todas = consulta.listarPartidas(null);
        Chaveamento ch = competicao.obterChaveamento();
        List<Partida> proximas = todas.stream().filter(p -> !p.estaDisputada()).limit(6).toList();
        List<Partida> disputadas = todas.stream().filter(Partida::estaDisputada).toList();
        StringBuilder sb = new StringBuilder("<section class=\"hero\"><h1>Copa do Mundo Feminina <span>Brasil 2027</span></h1>")
                .append("<p>De 24 de junho a 25 de julho de 2027, pela primeira vez na América do Sul. ")
                .append("Acompanhe os jogos, a classificação e simule os cruzamentos até a final.</p>")
                .append("<div class=\"numeros\"><div class=\"numero\"><strong>32</strong>seleções</div>")
                .append("<div class=\"numero\"><strong>64</strong>partidas</div>")
                .append("<div class=\"numero\"><strong>8</strong>cidades-sede</div>")
                .append("<div class=\"numero\"><strong>").append(disputadas.size()).append("</strong>jogos disputados</div></div>")
                .append("<p class=\"acoes\" style=\"margin-top:22px\"><a class=\"botao destaque\" href=\"/simulacao\">Abrir o simulador</a>")
                .append("<a class=\"botao secundario\" href=\"/partidas\">Ver a tabela de jogos</a></p></section>");
        sb.append("<div class=\"grade\">")
                .append(atalho("/partidas", "Tabela de jogos", "Filtre por seleção, sede, fase e datas."))
                .append(atalho("/classificacao", "Classificação", "Os oito grupos, com os critérios de desempate."))
                .append(atalho("/chaveamento", "Chaveamento", "A árvore do mata-mata, das oitavas à final."))
                .append(atalho("/simulacao", "Simulador", "Simule placares e veja o caminho até a final."))
                .append(atalho("/sedes", "Sedes", "Estádios, aeroportos e jogos de cada cidade."))
                .append(atalho("/selecoes", "Seleções", "As 32 participantes e suas partidas."))
                .append("</div>");
        if (!disputadas.isEmpty()) {
            sb.append("<h2 class=\"titulo-secao\">Últimos resultados</h2>").append(grade(
                    disputadas.subList(Math.max(0, disputadas.size() - 3), disputadas.size()), ch));
        }
        if (!proximas.isEmpty()) {
            sb.append("<h2 class=\"titulo-secao\">Próximos jogos</h2>").append(grade(proximas, ch));
        }
        req.html(200, Html.pagina("Início", sb.toString(), false, "/", ""));
    }

    private static String atalho(String href, String titulo, String texto) {
        return "<a class=\"cartao atalho\" href=\"" + href + "\"><strong>" + esc(titulo) + "</strong><span>"
                + esc(texto) + "</span></a>";
    }

    private static String grade(List<Partida> partidas, Chaveamento ch) {
        StringBuilder sb = new StringBuilder("<div class=\"grade\">");
        for (Partida p : partidas) {
            sb.append(Html.cartaoJogo(p, ch == null ? null : ch.confrontoDe(p)));
        }
        return sb.append("</div>").toString();
    }

    // ------------------------------------------------------- CU01 a CU03

    void partidas(Requisicao req) throws IOException {
        StringBuilder sb = new StringBuilder("<h1>Tabela de jogos</h1>")
                .append("<p class=\"subtitulo\">As 64 partidas, em ordem cronológica. Horários de Brasília.</p>");
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
            sb.append(listaPorDia(lista));
        }
        req.html(200, Html.pagina("Tabela de jogos", sb.toString(), false, "/partidas", ""));
    }

    /** Partidas agrupadas por dia, em cartões. */
    private String listaPorDia(List<Partida> lista) {
        Chaveamento ch = competicao.obterChaveamento();
        Map<LocalDate, List<Partida>> porDia = new LinkedHashMap<>();
        for (Partida p : lista) {
            porDia.computeIfAbsent(p.getDataHora().toLocalDate(), d -> new ArrayList<>()).add(p);
        }
        StringBuilder sb = new StringBuilder();
        porDia.forEach((dia, jogos) -> sb.append("<h2 class=\"dia\">").append(esc(Html.dataPorExtenso(dia)))
                .append("</h2>").append(grade(jogos, ch)));
        return sb.toString();
    }

    private String formularioFiltro(Requisicao req) {
        StringBuilder sb = new StringBuilder("<form class=\"cartao filtros\" method=\"get\" action=\"/partidas\">");
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

    void partida(Requisicao req) throws IOException {
        Partida p = consulta.buscarPartida(obrigatorio(req.paramInt("id")));
        Chaveamento ch = competicao.obterChaveamento();
        Confronto c = ch == null ? null : ch.confrontoDe(p);
        Resultado r = p.getResultado();
        String placar;
        if (r == null) {
            placar = "<span class=\"gols-grandes\">x<small>" + p.getDataHora().format(Html.HORA) + "</small></span>";
        } else {
            String detalhe = p.estaDisputada() ? "Placar final" : "Placar parcial";
            if (r.isHouveProrrogacao()) {
                detalhe += ", com prorrogação";
            }
            if (r.temPenaltis()) {
                detalhe += "; pênaltis " + r.getPenaltisMandante() + " x " + r.getPenaltisVisitante();
            }
            placar = "<span class=\"gols-grandes\">" + r.getGolsMandante() + " x " + r.getGolsVisitante()
                    + "<small>" + esc(detalhe) + "</small></span>";
        }
        StringBuilder sb = new StringBuilder("<section class=\"cartao placar-hero\"><div class=\"contexto\">")
                .append(esc(p.getFase().getDescricao()))
                .append(p.getGrupo() == null ? "" : ", Grupo " + p.getGrupo().getLetra())
                .append(" &middot; Jogo ").append(p.getId()).append("</div>")
                .append("<h1 class=\"sr-only\">Partida ").append(p.getId()).append("</h1>")
                .append("<div class=\"confronto-grande\">")
                .append(lado(p.getMandante(), c == null ? null : c.getVagaMandante())).append(placar)
                .append(lado(p.getVisitante(), c == null ? null : c.getVagaVisitante())).append("</div>")
                .append(Html.seloSituacao(p)).append("</section>");
        sb.append("<dl class=\"ficha\">")
                .append(item("Data e hora", p.getDataHora().format(Html.DATA_HORA) + " (horário de Brasília)"))
                .append(item("Estádio", p.getEstadio().getNome()))
                .append(item("Cidade", Html.cidade(p)))
                .append(item("Situação", p.getSituacao().getDescricao()))
                .append("</dl><p><a href=\"/partidas\">Voltar à tabela de jogos</a></p>");
        req.html(200, Html.pagina("Partida " + p.getId(), sb.toString(), false, "/partidas", ""));
    }

    private static String lado(Selecao s, String vaga) {
        if (s == null) {
            return "<div class=\"lado\">" + Bandeiras.img(null, "g") + "<span class=\"vaga\">"
                    + esc(vaga == null ? "A definir" : vaga) + "</span></div>";
        }
        return "<div class=\"lado\">" + Bandeiras.img(s, "g") + "<span>" + esc(s.getPais()) + "</span></div>";
    }

    private static String item(String rotulo, String valor) {
        return "<div class=\"cartao\"><dt>" + esc(rotulo) + "</dt><dd>" + esc(valor) + "</dd></div>";
    }

    // ------------------------------------------------------- CU04 e CU05

    void classificacao(Requisicao req) throws IOException {
        String corpo = "<h1>Classificação</h1><p class=\"subtitulo\">Critérios de desempate: pontos, saldo de gols, "
                + "gols pró e confronto direto.</p>" + tabelasClassificacao(competicao.obterGrupos())
                + "<div class=\"legenda\"><span>Classificadas às oitavas de final</span></div>";
        req.html(200, Html.pagina("Classificação", corpo, false, "/classificacao", ""));
    }

    static String tabelasClassificacao(List<Grupo> grupos) {
        StringBuilder sb = new StringBuilder("<div class=\"grade-larga\">");
        for (Grupo g : grupos) {
            sb.append(tabelaGrupo(g));
        }
        return sb.append("</div>").toString();
    }

    static String tabelaGrupo(Grupo g) {
        Classificacao c = g.getClassificacao();
        StringBuilder sb = new StringBuilder("<section class=\"cartao tabela-grupo\"><header><h3><span class=\"letra\">")
                .append(g.getLetra()).append("</span>Grupo ").append(g.getLetra()).append("</h3>")
                .append(c.isDefinitiva() ? "<span class=\"selo encerrada\">Definida</span>" : "")
                .append("</header><table class=\"classif\"><thead><tr><th>#</th><th class=\"nome\">Seleção</th>")
                .append("<th><abbr title=\"Pontos\">P</abbr></th><th><abbr title=\"Jogos\">J</abbr></th>")
                .append("<th><abbr title=\"Vitórias\">V</abbr></th><th><abbr title=\"Empates\">E</abbr></th>")
                .append("<th><abbr title=\"Derrotas\">D</abbr></th><th><abbr title=\"Saldo de gols\">SG</abbr></th>")
                .append("<th><abbr title=\"Gols pró\">GP</abbr></th></tr></thead><tbody>");
        int pos = 1;
        for (LinhaClassificacao l : c.getLinhas()) {
            sb.append(pos <= 2 ? "<tr class=\"classifica\">" : "<tr>")
                    .append("<td class=\"pos\">").append(pos++).append("</td><td class=\"nome\">")
                    .append(Html.time(l.getSelecao(), null, "p")).append("</td><td class=\"pts\">").append(l.pontos())
                    .append("</td><td>").append(l.getJogos()).append("</td><td>").append(l.getVitorias())
                    .append("</td><td>").append(l.getEmpates()).append("</td><td>").append(l.getDerrotas())
                    .append("</td><td>").append(l.saldo()).append("</td><td>").append(l.getGolsPro())
                    .append("</td></tr>");
        }
        return sb.append("</tbody></table></section>").toString();
    }

    void chaveamento(Requisicao req) throws IOException {
        String corpo = "<h1>Chaveamento</h1><p class=\"subtitulo\">Do mata-mata à final. Enquanto as vagas não são "
                + "definidas, cada confronto mostra a origem da vaga.</p>" + arvore(competicao.obterChaveamento());
        req.html(200, Html.pagina("Chaveamento", corpo, false, "/chaveamento", ""));
    }

    /** Árvore simétrica: metade esquerda, final ao centro e metade direita. */
    static String arvore(Chaveamento ch) {
        return arvore(ch, false);
    }

    /**
     * Árvore simétrica. A versão compacta, usada ao lado do painel do simulador,
     * mostra o código FIFA no lugar do nome e abrevia as vagas.
     */
    static String arvore(Chaveamento ch, boolean compacta) {
        if (ch == null || ch.getConfrontos().isEmpty()) {
            return "<p>O chaveamento ainda não está disponível.</p>";
        }
        StringBuilder sb = new StringBuilder("<div class=\"chave-rolagem\"><div class=\"chave")
                .append(compacta ? " compacta" : "").append("\">");
        coluna(sb, ch, "Oitavas", "esq", compacta, 1, 3, 2, 4);
        coluna(sb, ch, "Quartas", "esq", compacta, 9, 10);
        coluna(sb, ch, "Semi", "esq", compacta, 13);
        sb.append("<div class=\"coluna centro\"><h3>Final</h3><div class=\"coluna-jogos\">");
        Confronto fin = ch.buscarConfronto(16);
        Selecao campea = fin == null ? null : fin.getPartida().vencedora();
        if (campea != null) {
            sb.append("<div class=\"campea\">").append(Bandeiras.img(campea, "g")).append("<strong>")
                    .append(esc(campea.getPais())).append("</strong>campeã</div>");
        }
        if (fin != null) {
            sb.append(cartaoChave(fin, "Final", compacta));
        }
        Confronto terceiro = ch.buscarConfronto(15);
        if (terceiro != null) {
            sb.append(cartaoChave(terceiro, compacta ? "3º lugar" : "Terceiro lugar", compacta));
        }
        sb.append("</div></div>");
        coluna(sb, ch, "Semi", "dir", compacta, 14);
        coluna(sb, ch, "Quartas", "dir", compacta, 11, 12);
        coluna(sb, ch, "Oitavas", "dir", compacta, 5, 7, 6, 8);
        return sb.append("</div></div>").toString();
    }

    private static void coluna(StringBuilder sb, Chaveamento ch, String titulo, String lado, boolean compacta,
                               int... numeros) {
        sb.append("<div class=\"coluna ").append(lado).append("\"><h3>").append(esc(titulo))
                .append("</h3><div class=\"coluna-jogos\">");
        for (int n : numeros) {
            Confronto c = ch.buscarConfronto(n);
            if (c != null) {
                sb.append(cartaoChave(c, null, compacta));
            }
        }
        sb.append("</div></div>");
    }

    static String cartaoChave(Confronto c, String tituloEspecial, boolean compacta) {
        Partida p = c.getPartida();
        Selecao venc = p.vencedora();
        Resultado r = p.getResultado();
        StringBuilder sb = new StringBuilder("<div class=\"cartao chave-jogo")
                .append("Final".equals(tituloEspecial) ? " final" : "")
                .append(p.isResultadoSimulado() ? " simulado" : "").append("\">");
        if (tituloEspecial != null) {
            sb.append("<div class=\"titulo-final\">").append(esc(tituloEspecial)).append("</div>");
        }
        String info = compacta ? p.getDataHora().format(Html.DATA)
                : "Jogo " + p.getId() + " &middot; " + p.getDataHora().format(Html.DATA) + " &middot; " + esc(Html.cidade(p));
        sb.append("<div class=\"info\" title=\"Jogo ").append(p.getId()).append(", ").append(esc(Html.cidade(p)))
                .append("\">").append(info).append("</div>");
        sb.append(linhaChave(p.getMandante(), c.getVagaMandante(), venc,
                r == null ? null : r.getGolsMandante(), r == null ? null : r.getPenaltisMandante(), compacta));
        sb.append(linhaChave(p.getVisitante(), c.getVagaVisitante(), venc,
                r == null ? null : r.getGolsVisitante(), r == null ? null : r.getPenaltisVisitante(), compacta));
        if (p.isResultadoSimulado()) {
            sb.append("<span class=\"selo simulado\">Simulado</span>");
        }
        return sb.append("</div>").toString();
    }

    private static String linhaChave(Selecao s, String vaga, Selecao venc, Integer gols, Integer penaltis,
                                     boolean compacta) {
        String g = gols == null ? "" : gols + (penaltis == null ? "" : " <small class=\"muted\">(" + penaltis + ")</small>");
        String time;
        if (!compacta) {
            time = s != null ? Html.time(s, vaga, "p")
                    : "<span class=\"time\">" + Bandeiras.img(null, "p") + "<span class=\"time-nome vaga\" title=\""
                    + esc(vaga) + "\">" + esc(vagaMedia(vaga)) + "</span></span>";
        } else if (s == null) {
            time = "<span class=\"time\"><span class=\"time-nome vaga\" title=\"" + esc(vaga) + "\">"
                    + esc(vagaCurta(vaga)) + "</span></span>";
        } else {
            time = "<span class=\"time\">" + Bandeiras.img(s, "p") + "<abbr class=\"time-nome\" title=\""
                    + esc(s.getPais()) + "\">" + esc(s.getCodigoFifa()) + "</abbr></span>";
        }
        return "<div class=\"chave-linha" + (s != null && s == venc ? " venceu" : "") + "\">"
                + time + "<span class=\"g\">" + g + "</span></div>";
    }

    /** "Vencedora da oitava 1" vira "Venc. oitava 1"; "Perdedora da semifinal 2", "Perd. semi 2". */
    static String vagaMedia(String vaga) {
        return vaga == null ? "A definir" : vaga.replace("Vencedora da ", "Venc. ").replace("Perdedora da ", "Perd. ")
                .replace("semifinal", "semi");
    }

    /** "1º do Grupo A" vira "1º A"; "Vencedora da oitava 3" vira "V. oit. 3", e assim por diante. */
    static String vagaCurta(String vaga) {
        if (vaga == null) {
            return "A definir";
        }
        return vaga.replace("º do Grupo ", "º ").replace("Vencedora da ", "V. ").replace("Perdedora da ", "P. ")
                .replace("oitava", "oit.").replace("quarta", "qua.").replace("semifinal", "semi");
    }

    // ------------------------------------------------------- CU06 a CU09

    private Cenario cenarioDaSessao(Requisicao req) {
        return simulacoes.computeIfAbsent(req.sessao(), k -> simulacao.iniciar());
    }

    private static Rodada rodada(String chave) {
        return RODADAS.stream().filter(r -> r.chave().equals(chave)).findFirst().orElse(null);
    }

    /** Partidas de uma rodada: a n-ésima dupla de jogos de cada grupo, ou as partidas da fase. */
    static List<Partida> partidasDaRodada(Cenario c, Rodada r) {
        List<Partida> l = new ArrayList<>();
        if (Character.isDigit(r.chave().charAt(0))) {
            int n = Integer.parseInt(r.chave());
            for (Grupo g : c.getGrupos()) {
                List<Partida> jogos = c.partidasDoGrupo(g).stream()
                        .sorted(Comparator.comparing(Partida::getDataHora).thenComparingInt(Partida::getId)).toList();
                for (int i = (n - 1) * 2; i < Math.min(n * 2, jogos.size()); i++) {
                    l.add(jogos.get(i));
                }
            }
        } else {
            Fase f = Fase.valueOf(r.chave());
            for (Partida p : c.getPartidas()) {
                if (p.getFase() == f || (f == Fase.FINAL && p.getFase() == Fase.TERCEIRO_LUGAR)) {
                    l.add(p);
                }
            }
        }
        l.sort(Comparator.comparing(Partida::getDataHora).thenComparingInt(Partida::getId));
        return l;
    }

    /** Primeira rodada com partida aberta para simulação e ainda sem placar. */
    private static Rodada rodadaInicial(Cenario c) {
        for (Rodada r : RODADAS) {
            boolean aberta = partidasDaRodada(c, r).stream().anyMatch(p -> !p.estaDisputada()
                    && p.getResultado() == null && p.getMandante() != null && p.getVisitante() != null);
            if (aberta) {
                return r;
            }
        }
        return RODADAS.get(0);
    }

    void simulacao(Requisicao req) throws IOException {
        Cenario c = cenarioDaSessao(req);
        Rodada r = rodada(req.param("rodada"));
        if (r == null) {
            r = rodadaInicial(c);
        }
        Integer foco = req.paramInt("foco");
        boolean deGrupos = Character.isDigit(r.chave().charAt(0));

        StringBuilder sb = new StringBuilder("<h1>Simulador</h1><p class=\"subtitulo\">Simule os placares das partidas ")
                .append("ainda não disputadas e veja a classificação e os cruzamentos. A simulação vale só para você, ")
                .append("nesta sessão do navegador, e não altera os dados reais.</p>")
                .append(Html.mensagens(req))
                .append("<div class=\"simulador\"><div>");
        if (deGrupos) {
            sb.append("<h2 class=\"titulo-secao\" style=\"margin-top:0\">Classificação simulada</h2>")
                    .append(tabelasClassificacao(c.getGrupos()))
                    .append("<div class=\"legenda\"><span>Classificadas às oitavas de final</span></div>");
        } else {
            sb.append("<h2 class=\"titulo-secao\" style=\"margin-top:0\">Chaveamento simulado</h2>")
                    .append(arvore(c.getChaveamento(), true));
        }
        sb.append(secaoCaminho(req, c, r)).append("</div>");
        sb.append(painelRodada(c, r, foco)).append("</div>");
        req.html(200, Html.pagina("Simulador", sb.toString(), false, "/simulacao",
                "<script src=\"/static/simulador.js?v=" + Html.VERSAO + "\" defer></script>"));
    }

    private String painelRodada(Cenario c, Rodada r, Integer foco) {
        int i = RODADAS.indexOf(r);
        Rodada anterior = i > 0 ? RODADAS.get(i - 1) : null;
        Rodada proxima = i < RODADAS.size() - 1 ? RODADAS.get(i + 1) : null;
        StringBuilder sb = new StringBuilder("<aside class=\"cartao painel-rodada\" aria-label=\"Placares da rodada\">")
                .append("<div class=\"rodada-nav\">")
                .append(seta(anterior, "&lsaquo;", "Rodada anterior"))
                .append("<div class=\"rodada-titulo\"><strong>").append(esc(r.titulo())).append("</strong><span>")
                .append(esc(r.fase())).append("</span></div>")
                .append(seta(proxima, "&rsaquo;", "Próxima rodada")).append("</div>")
                .append("<nav class=\"rodadas-atalho\" aria-label=\"Rodadas\">");
        for (Rodada x : RODADAS) {
            sb.append("<a href=\"/simulacao?rodada=").append(x.chave()).append("\"")
                    .append(x == r ? " class=\"ativo\" aria-current=\"true\"" : "").append(">")
                    .append(esc(x.rotuloCurto())).append("</a>");
        }
        sb.append("</nav><ul class=\"lista-jogos\">");
        List<Partida> jogos = partidasDaRodada(c, r);
        for (int k = 0; k < jogos.size(); k++) {
            Partida p = jogos.get(k);
            Integer seguinte = null;
            for (int j = k + 1; j < jogos.size() && seguinte == null; j++) {
                Partida q = jogos.get(j);
                if (!q.estaDisputada() && q.getMandante() != null && q.getVisitante() != null) {
                    seguinte = q.getId();
                }
            }
            sb.append(itemSimulacao(c, p, r, seguinte, foco != null && foco == p.getId()));
        }
        sb.append("</ul><div class=\"painel-acoes\">")
                .append("<form class=\"inline\" method=\"post\" action=\"/simulacao/aleatorio\">")
                .append("<input type=\"hidden\" name=\"rodada\" value=\"").append(r.chave()).append("\">")
                .append("<button type=\"submit\" class=\"destaque\">Preencher rodada aleatoriamente</button></form>")
                .append("<form class=\"inline\" method=\"post\" action=\"/simulacao/restaurar\">")
                .append("<button type=\"submit\" class=\"secundario\">Restaurar cenário real</button></form>")
                .append("</div></aside>");
        return sb.toString();
    }

    private static String seta(Rodada destino, String simbolo, String rotulo) {
        if (destino == null) {
            return "<a class=\"desabilitado\" aria-hidden=\"true\" tabindex=\"-1\">" + simbolo + "</a>";
        }
        return "<a href=\"/simulacao?rodada=" + destino.chave() + "\" aria-label=\"" + rotulo + ": "
                + esc(destino.titulo()) + "\">" + simbolo + "</a>";
    }

    private static String itemSimulacao(Cenario c, Partida p, Rodada r, Integer seguinte, boolean emFoco) {
        Confronto cf = c.getChaveamento().confrontoDe(p);
        String local = p.getGrupo() != null ? "Grupo " + p.getGrupo().getLetra()
                : (p.getFase() == Fase.TERCEIRO_LUGAR ? "3º lugar" : Html.cidade(p));
        StringBuilder sb = new StringBuilder("<li id=\"jogo-").append(p.getId()).append("\"")
                .append(emFoco ? " class=\"foco\"" : "").append(">")
                .append("<div class=\"sim-topo\"><span>").append(Html.diaCurto(p.getDataHora().toLocalDate()))
                .append(" ").append(p.getDataHora().format(Html.DATA)).append("</span><span>").append(esc(local))
                .append("</span><span>").append(p.getDataHora().format(Html.HORA)).append("</span></div>");
        String mandante = timeSimulacao(p.getMandante(), cf == null ? null : cf.getVagaMandante(), true);
        String visitante = timeSimulacao(p.getVisitante(), cf == null ? null : cf.getVagaVisitante(), false);
        Resultado res = p.getResultado();
        if (p.estaDisputada()) {
            sb.append("<div class=\"sim-linha\">").append(mandante)
                    .append("<span class=\"placar-real\">").append(res.getGolsMandante()).append("</span><span class=\"x\">x</span>")
                    .append("<span class=\"placar-real\">").append(res.getGolsVisitante()).append("</span>")
                    .append(visitante).append("</div>");
            if (res.temPenaltis()) {
                sb.append("<div class=\"penaltis\">Pênaltis ").append(res.getPenaltisMandante()).append(" x ")
                        .append(res.getPenaltisVisitante()).append("</div>");
            }
            return sb.append("</li>").toString();
        }
        boolean definida = p.getMandante() != null && p.getVisitante() != null;
        boolean mataMata = p.getFase().isMataMata();
        String rotulo = definida ? p.getMandante().getPais() + " x " + p.getVisitante().getPais() : "partida " + p.getId();
        sb.append("<form class=\"sim-form\" method=\"post\" action=\"/simulacao/placar\" data-mata-mata=\"")
                .append(mataMata ? "1" : "0").append("\">")
                .append("<input type=\"hidden\" name=\"partida\" value=\"").append(p.getId()).append("\">")
                .append("<input type=\"hidden\" name=\"rodada\" value=\"").append(r.chave()).append("\">");
        if (seguinte != null) {
            sb.append("<input type=\"hidden\" name=\"foco\" value=\"").append(seguinte).append("\">");
        }
        sb.append("<div class=\"sim-linha\">").append(mandante)
                .append(caixa("gm", res == null ? null : res.getGolsMandante(), definida, "Gols de " + rotulo + ", mandante"))
                .append("<span class=\"x\">x</span>")
                .append(caixa("gv", res == null ? null : res.getGolsVisitante(), definida, "Gols de " + rotulo + ", visitante"))
                .append(visitante).append("</div>");
        if (mataMata && definida) {
            boolean empate = res != null && res.empateNoJogo();
            sb.append("<div class=\"penaltis").append(empate ? "" : " oculto").append("\">Pênaltis ")
                    .append(caixa("pm", res == null ? null : res.getPenaltisMandante(), true, "Pênaltis da mandante"))
                    .append(" x ")
                    .append(caixa("pv", res == null ? null : res.getPenaltisVisitante(), true, "Pênaltis da visitante"))
                    .append("</div>");
        }
        if (definida) {
            sb.append("<noscript><div class=\"penaltis\"><button type=\"submit\" class=\"enviar\">ok</button></div></noscript>");
        }
        return sb.append("</form></li>").toString();
    }

    private static String caixa(String nome, Integer valor, boolean habilitada, String rotulo) {
        return "<input class=\"caixa" + (valor != null ? " preenchida" : "") + "\" type=\"number\" inputmode=\"numeric\" "
                + "min=\"0\" max=\"99\" name=\"" + nome + "\" aria-label=\"" + esc(rotulo) + "\" value=\""
                + (valor == null ? "" : valor) + "\"" + (habilitada ? "" : " disabled") + ">";
    }

    private static String timeSimulacao(Selecao s, String vaga, boolean mandante) {
        String classe = "sim-time" + (mandante ? " mandante" : "");
        if (s == null) {
            return "<span class=\"" + classe + "\"><span class=\"vaga\">" + esc(vaga == null ? "A definir" : vaga)
                    + "</span></span>";
        }
        String codigo = "<abbr title=\"" + esc(s.getPais()) + "\">" + esc(s.getCodigoFifa()) + "</abbr>";
        String bandeira = Bandeiras.img(s, "p");
        return "<span class=\"" + classe + "\">" + (mandante ? codigo + bandeira : bandeira + codigo) + "</span>";
    }

    private String secaoCaminho(Requisicao req, Cenario c, Rodada r) {
        StringBuilder sb = new StringBuilder("<section class=\"cartao caminho\"><h2 class=\"titulo-secao\" style=\"margin:0\">")
                .append("Caminho até a final</h2><form method=\"get\" action=\"/simulacao\" style=\"margin-top:12px\">")
                .append("<input type=\"hidden\" name=\"rodada\" value=\"").append(r.chave()).append("\">")
                .append("<label class=\"sr-only\" for=\"caminho\">Seleção</label><select id=\"caminho\" name=\"caminho\">");
        Integer escolhida = req.paramInt("caminho");
        List<Selecao> todas = new ArrayList<>();
        c.getGrupos().forEach(g -> todas.addAll(g.getSelecoes()));
        todas.sort(Comparator.comparing(Selecao::getPais));
        for (Selecao s : todas) {
            sb.append(opcao(String.valueOf(s.getId()), s.getPais(), escolhida == null ? null : escolhida.toString()));
        }
        sb.append("</select><button type=\"submit\">Ver caminho</button></form>");
        if (escolhida == null) {
            return sb.append("</section>").toString();
        }
        Selecao s = c.buscarSelecao(escolhida);
        List<Confronto> caminho = simulacao.caminhoAteFinal(c, escolhida);
        sb.append("<p style=\"margin:14px 0 0\">").append(Html.time(s, null, "")).append("</p>");
        if (caminho.isEmpty()) {
            Grupo g = c.grupoDa(s);
            boolean definitiva = g != null && g.getClassificacao().isDefinitiva();
            sb.append("<p>").append(esc(s.getPais())).append(definitiva
                    ? " não se classifica neste cenário: é eliminada na fase de grupos."
                    : " ainda não tem vaga definida no mata-mata; simule as partidas restantes do grupo.").append("</p>");
            return sb.append("</section>").toString();
        }
        sb.append("<ol>");
        for (Confronto cf : caminho) {
            Partida p = cf.getPartida();
            Selecao venc = p.vencedora();
            String adversaria;
            if (p.getMandante() == s) {
                adversaria = Html.time(p.getVisitante(), cf.getVagaVisitante(), "p");
            } else if (p.getVisitante() == s) {
                adversaria = Html.time(p.getMandante(), cf.getVagaMandante(), "p");
            } else {
                adversaria = "<span class=\"vaga\">a definir: " + Html.nomeMandante(p, cf.getVagaMandante())
                        + " ou " + Html.nomeVisitante(p, cf.getVagaVisitante()) + "</span>";
            }
            String classe = venc == null ? "" : (venc == s ? " class=\"vencida\"" : " class=\"eliminada\"");
            sb.append("<li").append(classe).append("><span class=\"fase\">").append(esc(cf.getFase().getDescricao()))
                    .append("</span><span>Contra ").append(adversaria).append(" <span class=\"muted\">&middot; ")
                    .append(p.getDataHora().format(Html.DATA)).append(", ").append(esc(Html.cidade(p))).append("</span>");
            if (p.getResultado() != null) {
                sb.append(" &middot; ").append(Html.placar(p));
            }
            if (venc != null && venc != s) {
                sb.append(" <strong>Eliminada nesta fase.</strong>");
            }
            sb.append("</span></li>");
        }
        sb.append("</ol>");
        Confronto ultimo = caminho.get(caminho.size() - 1);
        if (ultimo.getFase() == Fase.FINAL && ultimo.getPartida().vencedora() == s) {
            sb.append("<p><strong>").append(esc(s.getPais())).append(" é campeã neste cenário.</strong></p>");
        }
        return sb.append("</section>").toString();
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

    private static String voltarSimulacao(Requisicao req) {
        Rodada r = rodada(req.param("rodada"));
        return r == null ? "/simulacao" : "/simulacao?rodada=" + r.chave();
    }

    void simularPlacar(Requisicao req) throws IOException {
        Cenario c = cenarioDaSessao(req);
        String voltar = voltarSimulacao(req);
        try {
            int id = obrigatorio(req.paramInt("partida"));
            simulacao.simular(c, id, lerResultado(req));
            Integer foco = req.paramInt("foco");
            req.redirecionar(foco == null ? voltar : Requisicao.url(voltar, "foco", foco.toString()));
        } catch (RegraNegocioException e) {
            req.redirecionar(Requisicao.url(voltar, "erro", e.getMessage()));
        }
    }

    /** Preenche com placares aleatórios as partidas abertas da rodada (ou da fase de grupos, sem rodada). */
    void simularAleatorio(Requisicao req) throws IOException {
        Cenario c = cenarioDaSessao(req);
        Random aleatorio = new Random();
        Rodada r = rodada(req.param("rodada"));
        List<Partida> alvo = new ArrayList<>();
        if (r == null) {
            c.partidasNaoDisputadas().stream().filter(p -> p.getFase() == Fase.GRUPOS).forEach(alvo::add);
        } else {
            alvo.addAll(partidasDaRodada(c, r));
        }
        for (Partida p : alvo) {
            if (p.estaDisputada() || p.getMandante() == null || p.getVisitante() == null) {
                continue;
            }
            int gm = aleatorio.nextInt(4);
            int gv = aleatorio.nextInt(4);
            Resultado res = p.getFase().isMataMata() && gm == gv
                    ? (aleatorio.nextBoolean() ? Resultado.comPenaltis(gm, 5, 4) : Resultado.comPenaltis(gm, 3, 4))
                    : Resultado.placar(gm, gv);
            simulacao.simular(c, p.getId(), res);
        }
        req.redirecionar(Requisicao.url(voltarSimulacao(req), "msg",
                (r == null ? "Fase de grupos" : r.titulo()) + " preenchida com placares aleatórios."));
    }

    void restaurar(Requisicao req) throws IOException {
        simulacoes.put(req.sessao(), simulacao.restaurar());
        req.redirecionar(Requisicao.url("/simulacao", "msg", "Simulação descartada; cenário real restaurado."));
    }

    // ------------------------------------------------------ CU10 e CU11

    void sedes(Requisicao req) throws IOException {
        StringBuilder sb = new StringBuilder("<h1>Cidades-sede</h1><p class=\"subtitulo\">Oito cidades em cinco regiões ")
                .append("do país, com deslocamentos de até cerca de 4.000 km entre elas.</p><div class=\"grade\">");
        for (Sede s : consulta.listarSedes()) {
            sb.append("<a class=\"cartao sede\" href=\"/sede?id=").append(s.getId()).append("\">")
                    .append("<div class=\"sede-faixa\" aria-hidden=\"true\"></div><div class=\"sede-corpo\">")
                    .append("<span class=\"selo grupo\" style=\"align-self:flex-start\">").append(esc(s.getRegiao().getDescricao()))
                    .append("</span><strong>").append(esc(s.getCidade())).append("</strong><span>")
                    .append(esc(s.getEstadio().getNome())).append("</span><span class=\"muted\">")
                    .append(milhar(s.getEstadio().getCapacidade())).append(" lugares</span><span class=\"aeroportos\">");
            for (Aeroporto a : s.getAeroportos()) {
                sb.append("<span class=\"selo\">").append(esc(a.codigoIata())).append("</span>");
            }
            sb.append("</span></div></a>");
        }
        sb.append("</div>");
        req.html(200, Html.pagina("Sedes", sb.toString(), false, "/sedes", ""));
    }

    private static String milhar(int n) {
        return String.format("%,d", n).replace(',', '.');
    }

    void sede(Requisicao req) throws IOException {
        int id = obrigatorio(req.paramInt("id"));
        Sede s = consulta.buscarSede(id);
        List<Partida> jogos = consulta.listarPartidasDaSede(id);
        StringBuilder sb = new StringBuilder("<section class=\"cartao sede\" style=\"margin-bottom:8px\">")
                .append("<div class=\"sede-faixa\" aria-hidden=\"true\"></div><div class=\"sede-corpo\">")
                .append("<span class=\"selo grupo\" style=\"align-self:flex-start\">").append(esc(s.getRegiao().getDescricao()))
                .append("</span><h1>").append(esc(s.getCidade())).append("</h1><span>").append(esc(s.getEstadio().getNome()))
                .append(", ").append(esc(s.getEstadio().getEndereco())).append("</span></div></section>")
                .append("<div class=\"metricas\">")
                .append(metrica("Capacidade", milhar(s.getEstadio().getCapacidade())))
                .append(metrica("Inauguração", String.valueOf(s.getEstadio().getAnoInauguracao())))
                .append(metrica("Partidas", String.valueOf(jogos.size())))
                .append(metrica("Aeroportos", String.valueOf(s.getAeroportos().size())))
                .append("</div><h2 class=\"titulo-secao\">Aeroportos próximos</h2>")
                .append("<p class=\"subtitulo\">Ordenados pela distância até o estádio.</p><ul class=\"cartao lista-aeroportos\">");
        for (Aeroporto a : s.getAeroportos()) {
            sb.append("<li><span class=\"iata\">").append(esc(a.codigoIata())).append("</span><span>")
                    .append(esc(a.nome())).append("</span><strong>")
                    .append(String.format("%.1f km", a.distanciaKm()).replace('.', ',')).append("</strong></li>");
        }
        sb.append("</ul><h2 class=\"titulo-secao\">Partidas na sede</h2>");
        sb.append(jogos.isEmpty() ? "<p>Nenhuma partida agendada nesta sede.</p>" : grade(jogos, competicao.obterChaveamento()));
        req.html(200, Html.pagina(s.getCidade(), sb.toString(), false, "/sedes", ""));
    }

    private static String metrica(String rotulo, String valor) {
        return "<div class=\"cartao metrica\"><span>" + esc(rotulo) + "</span><strong>" + esc(valor) + "</strong></div>";
    }

    // ------------------------------------------------------ CU13 e CU14

    void selecoes(Requisicao req) throws IOException {
        List<Grupo> grupos = competicao.obterGrupos();
        StringBuilder sb = new StringBuilder("<h1>Seleções</h1><p class=\"subtitulo\">As 32 participantes, por grupo.</p>")
                .append("<div class=\"grade\">");
        for (Grupo g : grupos) {
            sb.append("<section class=\"cartao grupo-selecoes\"><header><h3><span class=\"letra\">").append(g.getLetra())
                    .append("</span> Grupo ").append(g.getLetra()).append("</h3></header><ul>");
            for (Selecao s : g.getSelecoes().stream().sorted(Comparator.comparing(Selecao::getPais)).toList()) {
                sb.append(itemSelecao(s));
            }
            sb.append("</ul></section>");
        }
        List<Selecao> semGrupo = consulta.listarSelecoes().stream()
                .filter(s -> grupos.stream().noneMatch(g -> g.contem(s))).toList();
        if (!semGrupo.isEmpty()) {
            sb.append("<section class=\"cartao grupo-selecoes\"><header><h3>Sem grupo</h3></header><ul>");
            semGrupo.forEach(s -> sb.append(itemSelecao(s)));
            sb.append("</ul></section>");
        }
        sb.append("</div>");
        req.html(200, Html.pagina("Seleções", sb.toString(), false, "/selecoes", ""));
    }

    private static String itemSelecao(Selecao s) {
        return "<li><a href=\"/selecao?id=" + s.getId() + "\">" + Html.time(s, null, "") + "<span class=\"selo\">"
                + s.getConfederacao() + "</span></a></li>";
    }

    void selecao(Requisicao req) throws IOException {
        int id = obrigatorio(req.paramInt("id"));
        Selecao s = consulta.buscarSelecao(id);
        List<Partida> jogos = consulta.listarPartidasDaSelecao(id);
        List<Partida> disputadas = jogos.stream().filter(Partida::estaDisputada).toList();
        List<Partida> futuras = jogos.stream().filter(p -> !p.estaDisputada()).toList();
        Grupo grupo = competicao.obterGrupos().stream().filter(g -> g.contem(s)).findFirst().orElse(null);
        Chaveamento ch = competicao.obterChaveamento();
        StringBuilder sb = new StringBuilder("<section class=\"cartao perfil\">").append(Bandeiras.img(s, "g"))
                .append("<div><h1>").append(esc(s.getPais())).append("</h1><span class=\"muted\">")
                .append(grupo == null ? "Sem grupo" : "Grupo " + grupo.getLetra()).append(" &middot; ")
                .append(s.getConfederacao()).append(" &middot; Código FIFA ").append(esc(s.getCodigoFifa()))
                .append("</span></div></section>");
        if (grupo != null) {
            sb.append("<h2 class=\"titulo-secao\">Grupo ").append(grupo.getLetra()).append("</h2>")
                    .append("<div style=\"max-width:560px\">").append(tabelaGrupo(grupo)).append("</div>");
        }
        sb.append("<h2 class=\"titulo-secao\">Próximas partidas</h2>")
                .append(futuras.isEmpty() ? "<p>Nenhuma partida futura definida.</p>" : grade(futuras, ch))
                .append("<h2 class=\"titulo-secao\">Partidas disputadas</h2>")
                .append(disputadas.isEmpty() ? "<p>Nenhuma partida disputada até agora.</p>" : grade(disputadas, ch));
        req.html(200, Html.pagina(s.getPais(), sb.toString(), false, "/selecoes", ""));
    }

    static int obrigatorio(Integer id) {
        if (id == null) {
            throw new RegraNegocioException("Identificador não informado.");
        }
        return id;
    }
}
