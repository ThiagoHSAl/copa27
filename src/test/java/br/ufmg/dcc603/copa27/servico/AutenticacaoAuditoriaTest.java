package br.ufmg.dcc603.copa27.servico;

import br.ufmg.dcc603.copa27.Ambiente;
import br.ufmg.dcc603.copa27.dominio.modelo.Administrador;
import br.ufmg.dcc603.copa27.dominio.modelo.RegistroAlteracao;
import br.ufmg.dcc603.copa27.persistencia.memoria.BaseDeDadosMemoria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

/** CU18 (RNF07 e RNF09) e CU19. */
class AutenticacaoAuditoriaTest {

    @Test
    @DisplayName("RNF07: o hash usa sal, nunca é a senha em claro e é verificável")
    void hash() {
        String h1 = HashSenha.gerar("segredo");
        String h2 = HashSenha.gerar("segredo");
        assertNotEquals(h1, h2, "sal aleatório gera hashes diferentes para a mesma senha");
        assertFalse(h1.contains("segredo"));
        assertTrue(h1.startsWith("pbkdf2$"));
        assertTrue(HashSenha.verificar("segredo", h1));
        assertTrue(HashSenha.verificar("segredo", h2));
        assertFalse(HashSenha.verificar("Segredo", h1));
        assertFalse(HashSenha.verificar(null, h1));
        assertFalse(HashSenha.verificar("segredo", null));
        assertFalse(HashSenha.verificar("segredo", "texto-claro"));
        assertFalse(HashSenha.verificar("segredo", "pbkdf2$x$y$z"));
        assertThrows(IllegalArgumentException.class, () -> HashSenha.gerar(""));
    }

    @Test
    @DisplayName("CU18: credenciais corretas autenticam o administrador")
    void autenticar() {
        Ambiente amb = new Ambiente();
        Administrador a = amb.autenticacao.autenticar("admin", Ambiente.SENHA);
        assertEquals("admin", a.getLogin());
        assertNotEquals(Ambiente.SENHA, a.getHashSenha());
    }

    @Test
    @DisplayName("CU18, E1: usuário ou senha errados geram a mesma mensagem")
    void falha() {
        Ambiente amb = new Ambiente();
        AutenticacaoException senhaErrada = assertThrows(AutenticacaoException.class,
                () -> amb.autenticacao.autenticar("admin", "errada"));
        AutenticacaoException usuarioErrado = assertThrows(AutenticacaoException.class,
                () -> amb.autenticacao.autenticar("outro", Ambiente.SENHA));
        assertEquals(senhaErrada.getMessage(), usuarioErrado.getMessage());
        assertThrows(AutenticacaoException.class, () -> amb.autenticacao.autenticar(null, null));
    }

    @Test
    @DisplayName("CU19: o registro guarda data, hora, autor, operação e dado")
    void auditoria() {
        BaseDeDadosMemoria base = new BaseDeDadosMemoria();
        Clock relogio = Clock.fixed(Instant.parse("2027-06-24T19:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        ServicoAuditoria aud = new ServicoAuditoria(base.repositorioRegistros(), relogio);
        Administrador a = new Administrador("admin", "hash");
        aud.registrar(a, "registrar resultado", "Partida 1");
        assertEquals(1, aud.listar().size());
        RegistroAlteracao r = aud.listar().get(0);
        assertEquals(LocalDateTime.of(2027, 6, 24, 16, 0), r.getDataHora());
        assertSame(a, r.getAutor());
        assertEquals("registrar resultado", r.getOperacao());
        assertEquals("Partida 1", r.getDado());
    }
}
