package br.ufmg.dcc603.copa27.apresentacao;

import br.ufmg.dcc603.copa27.dominio.modelo.Confronto;
import br.ufmg.dcc603.copa27.dominio.modelo.Partida;
import br.ufmg.dcc603.copa27.dominio.modelo.Resultado;
import br.ufmg.dcc603.copa27.dominio.modelo.Selecao;
import br.ufmg.dcc603.copa27.dominio.modelo.SituacaoPartida;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Utilitários de geração de HTML: escape, layout comum e componentes reutilizáveis. */
final class Html {

    /** Muda a cada publicação dos arquivos estáticos, para o navegador baixar a versão nova. */
    static final String VERSAO = "4";
    static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM");
    static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private static final String[] DIAS = {"Segunda-feira", "Terça-feira", "Quarta-feira", "Quinta-feira",
            "Sexta-feira", "Sábado", "Domingo"};
    private static final String[] DIAS_CURTOS = {"SEG", "TER", "QUA", "QUI", "SEX", "SÁB", "DOM"};
    private static final String[] MESES = {"janeiro", "fevereiro", "março", "abril", "maio", "junho", "julho",
            "agosto", "setembro", "outubro", "novembro", "dezembro"};

    private static final String[][] MENU_PUBLICO = {
            {"/partidas", "Jogos"}, {"/classificacao", "Classificação"}, {"/chaveamento", "Chaveamento"},
            {"/simulacao", "Simulador"}, {"/sedes", "Sedes"}, {"/selecoes", "Seleções"}, {"/admin", "Administração"}};
    private static final String[][] MENU_ADMIN = {
            {"/admin", "Partidas e resultados"}, {"/admin/selecoes", "Seleções"}, {"/admin/sedes", "Sedes"},
            {"/admin/log", "Log de alterações"}, {"/", "Área pública"}, {"/admin/sair", "Sair"}};

    private Html() {
    }

    static String esc(Object o) {
        if (o == null) {
            return "";
        }
        String s = o.toString();
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '&' -> sb.append("&amp;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    static String pagina(String titulo, String corpo, boolean areaAdministrativa) {
        return pagina(titulo, corpo, areaAdministrativa, "", "");
    }

    /**
     * Página completa. {@code ativo} é o caminho do item de menu destacado e
     * {@code extraCabecalho} permite incluir scripts específicos da página.
     */
    static String pagina(String titulo, String corpo, boolean areaAdministrativa, String ativo, String extraCabecalho) {
        StringBuilder nav = new StringBuilder();
        for (String[] item : areaAdministrativa ? MENU_ADMIN : MENU_PUBLICO) {
            boolean marcado = item[0].equals(ativo);
            nav.append("<a href=\"").append(item[0]).append("\"").append(marcado ? " class=\"ativo\" aria-current=\"page\"" : "")
                    .append(">").append(esc(item[1])).append("</a>");
        }
        return "<!doctype html><html lang=\"pt-BR\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>" + esc(titulo) + " | Copa27</title>"
                + "<link rel=\"icon\" href=\"/static/marca.svg\" type=\"image/svg+xml\">"
                + "<link rel=\"preconnect\" href=\"https://fonts.googleapis.com\">"
                + "<link rel=\"preconnect\" href=\"https://fonts.gstatic.com\" crossorigin>"
                + "<link rel=\"stylesheet\" href=\"https://fonts.googleapis.com/css2?family=Barlow+Condensed:wght@600;700"
                + "&family=Barlow:wght@400;500;600;700&display=swap\">"
                + "<link rel=\"stylesheet\" href=\"/static/estilo.css?v=" + VERSAO + "\">"
                + "<script>document.documentElement.className+=' js';</script>" + extraCabecalho + "</head><body>"
                + "<header class=\"topo" + (areaAdministrativa ? " admin" : "") + "\"><div class=\"topo-interno\">"
                + "<a class=\"marca\" href=\"/\"><img src=\"/static/marca.svg\" alt=\"\" width=\"40\" height=\"40\">"
                + "<span class=\"marca-texto\"><span class=\"marca-nome\">COPA<span>27</span></span>"
                + "<span class=\"marca-sub\">Copa do Mundo Feminina Brasil 2027"
                + (areaAdministrativa ? ", administração" : "") + "</span></span></a>"
                + "<nav class=\"navegacao\" aria-label=\"Principal\">" + nav + "</nav></div></header>"
                + "<main>" + corpo + "</main>"
                + "<footer class=\"rodape\"><div>Projeto acadêmico da disciplina DCC603 Engenharia de Software I (UFMG), "
                + "sem vínculo com a FIFA. Não reproduz marcas nem material oficial. As seleções, os grupos e a tabela são "
                + "fictícios, pois o sorteio e a tabela oficial ainda não foram divulgados. Horários no fuso de Brasília. "
                + "Bandeiras: projeto flag-icons (licença MIT).</div></footer>"
                + "</body></html>";
    }

    static String mensagens(Requisicao req) {
        StringBuilder sb = new StringBuilder();
        String msg = req.param("msg");
        String erro = req.param("erro");
        if (msg != null) {
            sb.append("<p class=\"msg\" role=\"status\">").append(esc(msg)).append("</p>");
        }
        if (erro != null) {
            sb.append("<p class=\"erro\" role=\"alert\">").append(esc(erro)).append("</p>");
        }
        return sb.toString();
    }

    /** "Quinta-feira, 24 de junho". */
    static String dataPorExtenso(LocalDate d) {
        return DIAS[d.getDayOfWeek().getValue() - 1] + ", " + d.getDayOfMonth() + " de " + MESES[d.getMonthValue() - 1];
    }

    static String diaCurto(LocalDate d) {
        return DIAS_CURTOS[d.getDayOfWeek().getValue() - 1];
    }

    /** Bandeira e nome da seleção, ou a descrição da vaga enquanto ela não é conhecida. */
    static String time(Selecao s, String vaga, String classeBandeira) {
        if (s == null) {
            return "<span class=\"time\">" + Bandeiras.img(null, classeBandeira) + "<span class=\"time-nome vaga\">"
                    + esc(vaga == null ? "A definir" : vaga) + "</span></span>";
        }
        return "<span class=\"time\">" + Bandeiras.img(s, classeBandeira) + "<span class=\"time-nome\">"
                + esc(s.getPais()) + "</span></span>";
    }

    static String nomeMandante(Partida p, String vaga) {
        return p.getMandante() != null ? esc(p.getMandante().getPais())
                : "<span class=\"vaga\">" + esc(vaga == null ? "A definir" : vaga) + "</span>";
    }

    static String nomeVisitante(Partida p, String vaga) {
        return p.getVisitante() != null ? esc(p.getVisitante().getPais())
                : "<span class=\"vaga\">" + esc(vaga == null ? "A definir" : vaga) + "</span>";
    }

    static String placar(Partida p) {
        Resultado r = p.getResultado();
        if (r == null) {
            return "";
        }
        String s = esc(r.toString());
        return p.isResultadoSimulado() ? "<span class=\"simulado\">" + s + " (simulado)</span>" : s;
    }

    static String cidade(Partida p) {
        return p.getEstadio().getSede() == null ? "" : p.getEstadio().getSede().getCidade();
    }

    /** Fase curta para os cartões: "Grupo A" ou o nome da fase do mata-mata. */
    static String fase(Partida p) {
        return p.getGrupo() != null ? "Grupo " + p.getGrupo().getLetra() : p.getFase().getDescricao();
    }

    static String seloSituacao(Partida p) {
        if (p.isResultadoSimulado()) {
            return "<span class=\"selo simulado\">Simulado</span>";
        }
        return switch (p.getSituacao()) {
            case EM_ANDAMENTO -> "<span class=\"selo ao-vivo\">Em andamento</span>";
            case ENCERRADA -> "<span class=\"selo encerrada\">Encerrada</span>";
            case AGENDADA -> "<span class=\"selo\">" + p.getDataHora().format(HORA) + "</span>";
        };
    }

    /** Cartão de partida usado nas listas de jogos (tabela, sede, seleção, início). */
    static String cartaoJogo(Partida p, Confronto c) {
        Resultado r = p.getResultado();
        Selecao venc = p.getSituacao() == SituacaoPartida.ENCERRADA ? p.vencedora() : null;
        String gm = r == null ? "" : String.valueOf(r.getGolsMandante());
        String gv = r == null ? "" : String.valueOf(r.getGolsVisitante());
        if (r != null && r.temPenaltis()) {
            gm += " <small class=\"muted\">(" + r.getPenaltisMandante() + ")</small>";
            gv += " <small class=\"muted\">(" + r.getPenaltisVisitante() + ")</small>";
        }
        return "<a class=\"cartao jogo\" href=\"/partida?id=" + p.getId() + "\">"
                + "<div class=\"jogo-topo\"><span>" + esc(fase(p)) + " &middot; " + p.getDataHora().format(DATA)
                + " &middot; " + p.getDataHora().format(HORA) + "</span>" + seloSituacao(p) + "</div>"
                + "<div class=\"jogo-linha" + (venc != null && venc == p.getMandante() ? " venceu" : "") + "\">"
                + time(p.getMandante(), c == null ? null : c.getVagaMandante(), "") + "<span class=\"gols\">" + gm + "</span></div>"
                + "<div class=\"jogo-linha" + (venc != null && venc == p.getVisitante() ? " venceu" : "") + "\">"
                + time(p.getVisitante(), c == null ? null : c.getVagaVisitante(), "") + "<span class=\"gols\">" + gv + "</span></div>"
                + "<div class=\"jogo-rodape\">" + esc(p.getEstadio().getNome()) + ", " + esc(cidade(p)) + "</div></a>";
    }
}
