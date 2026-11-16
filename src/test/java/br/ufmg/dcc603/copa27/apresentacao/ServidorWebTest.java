package br.ufmg.dcc603.copa27.apresentacao;

import br.ufmg.dcc603.copa27.Aplicacao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** Teste de ponta a ponta da camada de apresentação, por HTTP. */
class ServidorWebTest {

    private static final String SENHA = "senha-web";
    private ServidorWeb servidor;
    private HttpClient cliente;
    private String raiz;

    @BeforeEach
    void subir() throws IOException {
        Aplicacao app = new Aplicacao(SENHA);
        servidor = app.criarServidor(0);
        servidor.iniciar();
        raiz = "http://localhost:" + servidor.porta();
        cliente = HttpClient.newBuilder().cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.NORMAL).build();
    }

    @AfterEach
    void parar() {
        servidor.parar();
    }

    private HttpResponse<String> get(String caminho) throws Exception {
        return cliente.send(HttpRequest.newBuilder(URI.create(raiz + caminho)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String caminho, String... pares) throws Exception {
        StringBuilder corpo = new StringBuilder();
        for (int i = 0; i < pares.length; i += 2) {
            if (corpo.length() > 0) {
                corpo.append('&');
            }
            corpo.append(pares[i]).append('=').append(URLEncoder.encode(pares[i + 1], StandardCharsets.UTF_8));
        }
        return cliente.send(HttpRequest.newBuilder(URI.create(raiz + caminho))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(corpo.toString())).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("Páginas públicas respondem e trazem o aviso acadêmico (RNF16)")
    void paginasPublicas() throws Exception {
        for (String p : new String[]{"/", "/partidas", "/partidas?fase=OITAVAS", "/partida?id=1", "/classificacao",
                "/chaveamento", "/simulacao", "/sedes", "/sede?id=1", "/selecoes", "/selecao?id=1"}) {
            HttpResponse<String> r = get(p);
            assertEquals(200, r.statusCode(), p);
            assertTrue(r.body().contains("sem vínculo com a FIFA"), p);
        }
        assertTrue(get("/chaveamento").body().contains("1º do Grupo A"));
        assertTrue(get("/sede?id=1").body().contains("CNF"));
    }

    @Test
    @DisplayName("Erros: página inexistente, partida inexistente, identificador inválido e datas invertidas")
    void erros() throws Exception {
        assertEquals(404, get("/nao-existe").statusCode());
        assertEquals(404, get("/partida?id=999").statusCode());
        assertEquals(400, get("/partida?id=abc").statusCode());
        assertEquals(400, get("/partida").statusCode());
        assertTrue(get("/partidas?de=2027-07-10&ate=2027-07-01").body()
                .contains("A data inicial não pode ser posterior à data final."));
    }

    @Test
    @DisplayName("O conteúdo é escapado contra injeção de HTML")
    void escape() throws Exception {
        String corpo = get("/partidas?de=%3Cscript%3Ealert(1)%3C/script%3E").body();
        assertFalse(corpo.contains("<script>alert(1)"));
        assertTrue(corpo.contains("Data inválida: &lt;script&gt;alert(1)&lt;/script&gt;"));
    }

    @Test
    @DisplayName("RNF09: a administração exige login; credenciais erradas são recusadas")
    void areaRestrita() throws Exception {
        HttpResponse<String> r = get("/admin");
        assertTrue(r.uri().getPath().endsWith("/admin/login"));
        r = post("/admin/resultado", "partida", "1", "gm", "1", "gv", "0", "encerrar", "on");
        assertTrue(r.uri().getPath().endsWith("/admin/login"));
        r = post("/admin/login", "login", "admin", "senha", "errada");
        assertTrue(r.body().contains("Usuário ou senha inválidos."));
        assertNull(get("/partida?id=1").body().contains("1 x 0") ? "registrado sem login" : null);
    }

    @Test
    @DisplayName("Fluxo do administrador: login, RN06, iniciar, registrar, encerrar e log")
    void fluxoAdministrador() throws Exception {
        HttpResponse<String> r = post("/admin/login", "login", Aplicacao.LOGIN_ADMIN, "senha", SENHA);
        assertTrue(r.body().contains("Partidas e resultados"));
        r = post("/admin/resultado", "partida", "1", "gm", "2", "gv", "0");
        assertTrue(r.body().contains("não começou"), "RN06 na interface");
        r = post("/admin/iniciar", "partida", "1");
        assertTrue(r.body().contains("Partida 1 iniciada."));
        r = post("/admin/resultado", "partida", "1", "gm", "2", "gv", "0", "encerrar", "on");
        assertTrue(r.body().contains("registrado e partida encerrada"));
        assertTrue(get("/partida?id=1").body().contains("Placar final"));
        assertTrue(get("/admin/log").body().contains("registrar resultado"));
        r = post("/admin/resultado", "partida", "1", "gm", "-1", "gv", "0");
        assertTrue(r.body().contains("negativos"));
        assertEquals(200, get("/admin/sedes").statusCode());
        r = post("/admin/remover", "tipo", "SEDE", "id", "1", "voltar", "/admin/sedes", "confirmar", "on");
        assertTrue(r.body().contains("não pode ser removida"));
        r = post("/admin/remover", "tipo", "SEDE", "id", "1", "voltar", "/admin/sedes");
        assertTrue(r.body().contains("Confirme a remoção."));
        r = post("/admin/selecoes/salvar", "id", "0", "pais", "Chile", "codigo", "chi", "confederacao", "CONMEBOL");
        assertTrue(r.body().contains("Seleção Chile salva."));
        assertTrue(get("/selecoes").body().contains("Chile"));
        r = post("/admin/selecoes/salvar", "id", "1", "pais", "Brasil", "codigo", "BRA", "confederacao", "XYZ");
        assertTrue(r.body().contains("Confederação inválida."));
        get("/admin/sair");
        assertTrue(get("/admin").uri().getPath().endsWith("/admin/login"));
    }

    @Test
    @DisplayName("RN07: a simulação de um visitante não afeta os dados reais nem outro visitante")
    void simulacaoIsolada() throws Exception {
        HttpResponse<String> r = post("/simulacao/placar", "partida", "1", "gm", "7", "gv", "0", "rodada", "1", "foco", "2");
        assertTrue(r.uri().getQuery().contains("rodada=1"));
        assertTrue(r.body().contains("<li id=\"jogo-2\" class=\"foco\">"), "o foco passa à partida seguinte");
        assertTrue(r.body().contains("name=\"gm\" aria-label=\"Gols de Brasil x Noruega, mandante\" value=\"7\""));
        assertTrue(get("/partida?id=1").body().contains("Agendada"), "o dado real não muda");

        HttpClient outro = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        String outroCorpo = outro.send(HttpRequest.newBuilder(URI.create(raiz + "/simulacao")).build(),
                HttpResponse.BodyHandlers.ofString()).body();
        assertTrue(outroCorpo.contains("name=\"gm\" aria-label=\"Gols de Brasil x Noruega, mandante\" value=\"\""));

        r = post("/simulacao/placar", "partida", "1", "gm", "x", "gv", "0");
        assertTrue(r.body().contains("Valor numérico inválido"));
        r = post("/simulacao/aleatorio");
        assertTrue(r.body().contains("Fase de grupos preenchida com placares aleatórios."));
        r = post("/simulacao/aleatorio", "rodada", "OITAVAS");
        assertTrue(r.body().contains("Oitavas de final preenchida com placares aleatórios."));
        assertTrue(r.body().contains("chave compacta"), "no mata-mata, o simulador mostra a árvore compacta");
        r = get("/simulacao?rodada=OITAVAS&caminho=1");
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("Caminho até a final"));
        for (String rodada : new String[]{"1", "2", "3", "OITAVAS", "QUARTAS", "SEMIFINAL", "FINAL", "inexistente"}) {
            assertEquals(200, get("/simulacao?rodada=" + rodada).statusCode(), rodada);
        }
        r = post("/simulacao/restaurar");
        assertTrue(r.body().contains("cenário real restaurado"));
        assertFalse(r.body().contains("caixa preenchida"));
    }

    @Test
    @DisplayName("Arquivos estáticos: CSS, JavaScript e bandeiras; caminhos inválidos são recusados")
    void estaticos() throws Exception {
        HttpResponse<String> css = get("/static/estilo.css");
        assertEquals(200, css.statusCode());
        assertTrue(css.headers().firstValue("Content-Type").orElse("").startsWith("text/css"));
        assertEquals(200, get("/static/simulador.js").statusCode());
        HttpResponse<String> svg = get("/static/bandeiras/br.svg");
        assertEquals("image/svg+xml", svg.headers().firstValue("Content-Type").orElse(""));
        assertEquals(404, get("/static/bandeiras/xx.svg").statusCode());
        assertEquals(404, get("/static/LICENSE").statusCode());
        assertEquals(404, get("/static/bandeiras/..%2F..%2Festilo.css").statusCode());
        assertTrue(get("/selecoes").body().contains("/static/bandeiras/br.svg"));
    }

    @Test
    @DisplayName("Modo demonstração encerra as duas primeiras rodadas")
    void demonstracao() {
        Aplicacao app = new Aplicacao(SENHA);
        app.carregarDemonstracao(SENHA, 2);
        assertEquals(32, app.getConsulta().listarPartidas(null).stream().filter(p -> p.estaDisputada()).count());
        assertEquals(64, app.getAuditoria().listar().size());
        assertNotNull(app.getSimulacao().iniciar());
        assertNotNull(app.getAdministracao());
        assertEquals(8, app.getCompeticao().obterGrupos().size());
    }
}
