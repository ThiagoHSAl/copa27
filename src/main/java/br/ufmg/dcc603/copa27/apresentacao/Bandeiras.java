package br.ufmg.dcc603.copa27.apresentacao;

import br.ufmg.dcc603.copa27.dominio.modelo.Selecao;

import java.util.Map;

import static br.ufmg.dcc603.copa27.apresentacao.Html.esc;

/**
 * Bandeiras das seleções, indexadas pelo código FIFA. Os arquivos SVG vêm do
 * projeto flag-icons (licença MIT) e são servidos pela própria aplicação.
 * Seleções sem bandeira cadastrada exibem um selo com o código FIFA.
 */
final class Bandeiras {

    private static final Map<String, String> ISO = Map.ofEntries(
            Map.entry("BRA", "br"), Map.entry("NOR", "no"), Map.entry("ZAM", "zm"), Map.entry("KOR", "kr"),
            Map.entry("USA", "us"), Map.entry("NED", "nl"), Map.entry("VIE", "vn"), Map.entry("POR", "pt"),
            Map.entry("ESP", "es"), Map.entry("JPN", "jp"), Map.entry("CRC", "cr"), Map.entry("MAR", "ma"),
            Map.entry("ENG", "gb-eng"), Map.entry("CHN", "cn"), Map.entry("HAI", "ht"), Map.entry("DEN", "dk"),
            Map.entry("GER", "de"), Map.entry("COL", "co"), Map.entry("PHI", "ph"), Map.entry("IRL", "ie"),
            Map.entry("FRA", "fr"), Map.entry("JAM", "jm"), Map.entry("PAN", "pa"), Map.entry("AUS", "au"),
            Map.entry("SWE", "se"), Map.entry("ARG", "ar"), Map.entry("RSA", "za"), Map.entry("ITA", "it"),
            Map.entry("CAN", "ca"), Map.entry("NZL", "nz"), Map.entry("NGA", "ng"), Map.entry("SUI", "ch"));

    private Bandeiras() {
    }

    /** Imagem da bandeira (decorativa: o nome da seleção sempre aparece ao lado). */
    static String img(Selecao s, String classe) {
        if (s == null) {
            return "<span class=\"bandeira " + classe + " vazia\" aria-hidden=\"true\"></span>";
        }
        String iso = ISO.get(s.getCodigoFifa());
        if (iso == null) {
            return "<span class=\"bandeira " + classe + " sem-imagem\" aria-hidden=\"true\">"
                    + esc(s.getCodigoFifa()) + "</span>";
        }
        return "<img class=\"bandeira " + classe + "\" src=\"/static/bandeiras/" + iso + ".svg\" alt=\"\" "
                + "loading=\"lazy\" width=\"28\" height=\"21\">";
    }

    static String img(Selecao s) {
        return img(s, "");
    }
}
