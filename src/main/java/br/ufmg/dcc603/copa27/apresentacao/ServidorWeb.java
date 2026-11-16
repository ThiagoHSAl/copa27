package br.ufmg.dcc603.copa27.apresentacao;

import br.ufmg.dcc603.copa27.dominio.modelo.RegraNegocioException;
import br.ufmg.dcc603.copa27.servico.NaoEncontradoException;
import br.ufmg.dcc603.copa27.servico.ServicoAdministracao;
import br.ufmg.dcc603.copa27.servico.ServicoAuditoria;
import br.ufmg.dcc603.copa27.servico.ServicoAutenticacao;
import br.ufmg.dcc603.copa27.servico.ServicoCompeticao;
import br.ufmg.dcc603.copa27.servico.ServicoConsulta;
import br.ufmg.dcc603.copa27.servico.ServicoSimulacao;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Servidor HTTP da camada de apresentação, baseado no servidor embutido no
 * JDK. Encaminha cada caminho à Interface Pública ou à Interface de
 * Administração. As requisições são tratadas uma por vez, o que dispensa
 * sincronização nas camadas inferiores.
 */
public class ServidorWeb {

    @FunctionalInterface
    private interface Rota {
        void tratar(Requisicao req) throws IOException;
    }

    private final HttpServer servidor;
    private final Map<String, Rota> getRotas = new HashMap<>();
    private final Map<String, Rota> postRotas = new HashMap<>();

    public ServidorWeb(int porta, ServicoConsulta consulta, ServicoCompeticao competicao, ServicoSimulacao simulacao,
                       ServicoAutenticacao autenticacao, ServicoAdministracao administracao,
                       ServicoAuditoria auditoria) throws IOException {
        InterfacePublica pub = new InterfacePublica(consulta, competicao, simulacao);
        InterfaceAdministracao adm = new InterfaceAdministracao(autenticacao, administracao, auditoria, consulta, competicao);

        getRotas.put("/", pub::inicio);
        getRotas.put("/partidas", pub::partidas);
        getRotas.put("/partida", pub::partida);
        getRotas.put("/classificacao", pub::classificacao);
        getRotas.put("/chaveamento", pub::chaveamento);
        getRotas.put("/simulacao", pub::simulacao);
        postRotas.put("/simulacao/placar", pub::simularPlacar);
        postRotas.put("/simulacao/aleatorio", pub::simularAleatorio);
        postRotas.put("/simulacao/restaurar", pub::restaurar);
        getRotas.put("/sedes", pub::sedes);
        getRotas.put("/sede", pub::sede);
        getRotas.put("/selecoes", pub::selecoes);
        getRotas.put("/selecao", pub::selecao);

        getRotas.put("/admin/login", adm::login);
        postRotas.put("/admin/login", adm::login);
        getRotas.put("/admin/sair", adm::sair);
        getRotas.put("/admin", r -> adm.restrito(r, adm::painel));
        postRotas.put("/admin/iniciar", r -> adm.restrito(r, adm::iniciar));
        postRotas.put("/admin/resultado", r -> adm.restrito(r, adm::registrarResultado));
        getRotas.put("/admin/selecoes", r -> adm.restrito(r, adm::selecoes));
        postRotas.put("/admin/selecoes/salvar", r -> adm.restrito(r, adm::salvarSelecao));
        getRotas.put("/admin/sedes", r -> adm.restrito(r, adm::sedes));
        postRotas.put("/admin/remover", r -> adm.restrito(r, adm::remover));
        getRotas.put("/admin/log", r -> adm.restrito(r, adm::log));

        servidor = HttpServer.create(new InetSocketAddress(porta), 0);
        servidor.createContext("/", this::tratar);
    }

    private void tratar(HttpExchange troca) throws IOException {
        try {
            String caminho = troca.getRequestURI().getPath();
            if (caminho.startsWith("/static/")) {
                servirEstatico(troca, caminho.substring("/static/".length()));
                return;
            }
            Requisicao req = new Requisicao(troca);
            Rota rota = (req.isPost() ? postRotas : getRotas).get(req.caminho());
            if (rota == null) {
                req.html(404, Html.pagina("Não encontrado", "<h1>Página não encontrada</h1>", false));
                return;
            }
            try {
                rota.tratar(req);
            } catch (NaoEncontradoException e) {
                req.html(404, Html.pagina("Não encontrado", "<h1>Não encontrado</h1><p class=\"erro\">"
                        + Html.esc(e.getMessage()) + "</p>", false));
            } catch (RegraNegocioException e) {
                req.html(400, Html.pagina("Requisição inválida", "<h1>Requisição inválida</h1><p class=\"erro\">"
                        + Html.esc(e.getMessage()) + "</p>", false));
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            byte[] msg = "Erro interno".getBytes();
            troca.sendResponseHeaders(500, msg.length);
            troca.getResponseBody().write(msg);
        } finally {
            troca.close();
        }
    }

    /** Serve CSS, JavaScript e SVG empacotados em {@code static/}; nomes fora do padrão são recusados. */
    private static void servirEstatico(HttpExchange troca, String nome) throws IOException {
        String tipo = nome.endsWith(".css") ? "text/css; charset=utf-8"
                : nome.endsWith(".js") ? "text/javascript; charset=utf-8"
                : nome.endsWith(".svg") ? "image/svg+xml" : null;
        InputStream in = tipo != null && nome.matches("[a-z0-9-]+(/[a-z0-9-]+)*\\.[a-z]+")
                ? ServidorWeb.class.getResourceAsStream("/static/" + nome) : null;
        if (in == null) {
            troca.sendResponseHeaders(404, -1);
            return;
        }
        byte[] bytes;
        try (in) {
            bytes = in.readAllBytes();
        }
        troca.getResponseHeaders().set("Content-Type", tipo);
        troca.getResponseHeaders().set("Cache-Control", "no-cache");
        troca.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        troca.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = troca.getResponseBody()) {
            out.write(bytes);
        }
    }

    public void iniciar() {
        servidor.start();
    }

    public void parar() {
        servidor.stop(0);
    }

    public int porta() {
        return servidor.getAddress().getPort();
    }
}
