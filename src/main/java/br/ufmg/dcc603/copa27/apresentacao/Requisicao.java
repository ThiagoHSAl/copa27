package br.ufmg.dcc603.copa27.apresentacao;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

/** Envolve uma troca HTTP: parâmetros, cookie de sessão e respostas. */
final class Requisicao {

    static final String COOKIE_SESSAO = "copa27_sessao";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final HttpExchange troca;
    private final Map<String, String> params = new HashMap<>();
    private String sessao;

    Requisicao(HttpExchange troca) throws IOException {
        this.troca = troca;
        lerParametros(troca.getRequestURI().getRawQuery());
        if ("POST".equalsIgnoreCase(troca.getRequestMethod())) {
            try (InputStream in = troca.getRequestBody()) {
                lerParametros(new String(in.readNBytes(1_000_000), StandardCharsets.UTF_8));
            }
        }
        String cookies = troca.getRequestHeaders().getFirst("Cookie");
        if (cookies != null) {
            for (String c : cookies.split(";")) {
                String[] kv = c.trim().split("=", 2);
                if (kv.length == 2 && kv[0].equals(COOKIE_SESSAO) && kv[1].matches("[0-9a-f]{32}")) {
                    sessao = kv[1];
                }
            }
        }
    }

    private void lerParametros(String bruto) {
        if (bruto == null || bruto.isEmpty()) {
            return;
        }
        for (String par : bruto.split("&")) {
            String[] kv = par.split("=", 2);
            String chave = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String valor = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            params.put(chave, valor);
        }
    }

    String caminho() {
        return troca.getRequestURI().getPath();
    }

    boolean isPost() {
        return "POST".equalsIgnoreCase(troca.getRequestMethod());
    }

    /** Parâmetro não vazio, ou null. */
    String param(String nome) {
        String v = params.get(nome);
        return v == null || v.isBlank() ? null : v.trim();
    }

    Integer paramInt(String nome) {
        String v = param(nome);
        if (v == null) {
            return null;
        }
        try {
            return Integer.valueOf(v);
        } catch (NumberFormatException e) {
            throw new br.ufmg.dcc603.copa27.dominio.modelo.RegraNegocioException("Valor numérico inválido: " + v);
        }
    }

    /** Identificador da sessão do navegador, criado na primeira visita. */
    String sessao() {
        if (sessao == null) {
            byte[] b = new byte[16];
            ALEATORIO.nextBytes(b);
            sessao = HexFormat.of().formatHex(b);
            troca.getResponseHeaders().add("Set-Cookie",
                    COOKIE_SESSAO + "=" + sessao + "; Path=/; HttpOnly; SameSite=Lax");
        }
        return sessao;
    }

    void html(int status, String conteudo) throws IOException {
        byte[] bytes = conteudo.getBytes(StandardCharsets.UTF_8);
        troca.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        troca.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        troca.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = troca.getResponseBody()) {
            out.write(bytes);
        }
    }

    void redirecionar(String destino) throws IOException {
        troca.getResponseHeaders().set("Location", destino);
        troca.sendResponseHeaders(303, -1);
        troca.close();
    }

    static String url(String caminho, String chave, String valor) {
        String sep = caminho.contains("?") ? "&" : "?";
        return caminho + sep + chave + "=" + URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }
}
