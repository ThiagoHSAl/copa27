package br.ufmg.dcc603.copa27.apresentacao;

import br.ufmg.dcc603.copa27.dominio.modelo.Partida;
import br.ufmg.dcc603.copa27.dominio.modelo.Resultado;

import java.time.format.DateTimeFormatter;

/** Utilitários de geração de HTML: escape, layout comum e formatação. */
final class Html {

    static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM");

    private static final String CSS = """
            :root{--azul:#14304f;--azul-claro:#e8edf2;--borda:#97a5b4;--texto:#1a1a1a;--destaque:#dff0e3;--erro:#8a1c1c;}
            *{box-sizing:border-box}
            body{margin:0;font-family:system-ui,-apple-system,"Segoe UI",Roboto,sans-serif;color:var(--texto);background:#fff;line-height:1.45}
            header{background:var(--azul);color:#fff;padding:12px 16px}
            header a{color:#fff;text-decoration:none;font-weight:600}
            nav{display:flex;flex-wrap:wrap;gap:6px 18px;margin-top:6px}
            nav a{font-weight:500}
            nav a:focus,a:focus,button:focus,input:focus,select:focus{outline:3px solid #f2b705;outline-offset:2px}
            main{max-width:1180px;margin:0 auto;padding:16px}
            h1{color:var(--azul);font-size:1.6rem;margin:.4em 0}
            h2{color:var(--azul);font-size:1.2rem;margin:1.2em 0 .4em}
            a{color:#0b4a8b}
            table{border-collapse:collapse;width:100%;margin:.4em 0 1em;font-size:.95rem}
            th,td{border:1px solid var(--borda);padding:5px 8px;text-align:left;vertical-align:top}
            th{background:var(--azul-claro)}
            tr.classifica td{background:var(--destaque)}
            .grade{display:grid;grid-template-columns:repeat(auto-fill,minmax(330px,1fr));gap:12px}
            .chave{display:grid;grid-template-columns:repeat(5,minmax(170px,1fr));gap:10px;overflow-x:auto}
            .coluna h3{font-size:1rem;color:var(--azul);margin:.2em 0 .4em}
            .confronto{border:1px solid var(--borda);border-radius:6px;padding:6px 8px;margin-bottom:8px;background:#fafbfc;font-size:.9rem}
            .confronto .vaga{color:#555;font-style:italic}
            .confronto .venc{font-weight:700}
            .msg{background:var(--destaque);border-left:5px solid #2e7d32;padding:8px 12px;margin:10px 0}
            .erro{background:#fbe9e9;border-left:5px solid var(--erro);color:var(--erro);padding:8px 12px;margin:10px 0}
            form.inline{display:inline}
            input[type=number]{width:4.2em}
            input,select,button{font:inherit;padding:3px 6px}
            button{background:var(--azul);color:#fff;border:0;border-radius:4px;padding:5px 10px;cursor:pointer}
            button.secundario{background:#5b6b7b}
            .filtros{display:flex;flex-wrap:wrap;gap:8px 14px;align-items:end;background:var(--azul-claro);padding:10px;border-radius:6px}
            .filtros label{display:flex;flex-direction:column;font-size:.85rem}
            .simulado{color:#0b4a8b;font-style:italic}
            footer{max-width:1180px;margin:20px auto;padding:12px 16px;border-top:1px solid var(--borda);font-size:.85rem;color:#444}
            @media (max-width:700px){.chave{grid-template-columns:repeat(5,170px)}}
            """;

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
        String nav = areaAdministrativa
                ? "<a href=\"/admin\">Partidas e resultados</a><a href=\"/admin/selecoes\">Seleções</a>"
                + "<a href=\"/admin/sedes\">Sedes</a><a href=\"/admin/log\">Log de alterações</a>"
                + "<a href=\"/\">Área pública</a><a href=\"/admin/sair\">Sair</a>"
                : "<a href=\"/partidas\">Tabela de jogos</a><a href=\"/classificacao\">Classificação</a>"
                + "<a href=\"/chaveamento\">Chaveamento</a><a href=\"/simulacao\">Simular cruzamentos</a>"
                + "<a href=\"/sedes\">Sedes</a><a href=\"/selecoes\">Seleções</a><a href=\"/admin\">Administração</a>";
        return "<!doctype html><html lang=\"pt-BR\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>" + esc(titulo) + " | Copa27</title><style>" + CSS + "</style></head><body>"
                + "<header><a href=\"/\">Copa27</a> <span>Copa do Mundo Feminina Brasil 2027"
                + (areaAdministrativa ? ", administração" : "") + "</span><nav>" + nav + "</nav></header>"
                + "<main>" + corpo + "</main>"
                + "<footer>Projeto acadêmico da disciplina DCC603 Engenharia de Software I (UFMG), sem vínculo com a FIFA. "
                + "Não reproduz marcas nem material oficial. As seleções, os grupos e a tabela são fictícios, pois o "
                + "sorteio e a tabela oficial ainda não foram divulgados. Horários no fuso de Brasília.</footer>"
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
}
