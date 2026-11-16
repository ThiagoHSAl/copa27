package br.ufmg.dcc603.copa27.dominio.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultadoTest {

    @Test
    @DisplayName("Placar com vitória da mandante define a vencedora")
    void vitoriaMandante() {
        Resultado r = Resultado.placar(2, 1);
        assertFalse(r.empateNoJogo());
        assertTrue(r.defineVencedora());
        assertTrue(r.mandanteVenceu());
        assertFalse(r.isHouveProrrogacao());
        assertFalse(r.temPenaltis());
    }

    @Test
    @DisplayName("Empate sem pênaltis não define vencedora")
    void empateSemPenaltis() {
        Resultado r = Resultado.placar(1, 1);
        assertTrue(r.empateNoJogo());
        assertFalse(r.defineVencedora());
        assertFalse(r.mandanteVenceu());
    }

    @Test
    @DisplayName("Vitória na prorrogação marca a prorrogação e define a vencedora")
    void prorrogacao() {
        Resultado r = Resultado.comProrrogacao(1, 2);
        assertTrue(r.isHouveProrrogacao());
        assertTrue(r.defineVencedora());
        assertFalse(r.mandanteVenceu());
    }

    @Test
    @DisplayName("Empate decidido nos pênaltis define a vencedora (RN05)")
    void penaltis() {
        Resultado r = Resultado.comPenaltis(1, 3, 4);
        assertTrue(r.empateNoJogo());
        assertTrue(r.temPenaltis());
        assertTrue(r.defineVencedora());
        assertFalse(r.mandanteVenceu());
        assertEquals(3, r.getPenaltisMandante());
        assertEquals(4, r.getPenaltisVisitante());
        assertTrue(Resultado.comPenaltis(0, 5, 4).mandanteVenceu());
    }

    @Test
    @DisplayName("Placar negativo é rejeitado")
    void negativo() {
        assertThrows(RegraNegocioException.class, () -> Resultado.placar(-1, 0));
        assertThrows(RegraNegocioException.class, () -> Resultado.placar(0, -2));
    }

    @Test
    @DisplayName("Pênaltis exigem empate, prorrogação, os dois lados e uma vencedora")
    void penaltisInvalidos() {
        assertThrows(RegraNegocioException.class, () -> new Resultado(2, 1, true, 4, 3));
        assertThrows(RegraNegocioException.class, () -> new Resultado(1, 1, false, 4, 3));
        assertThrows(RegraNegocioException.class, () -> new Resultado(1, 1, true, 4, null));
        assertThrows(RegraNegocioException.class, () -> new Resultado(1, 1, true, 4, 4));
        assertThrows(RegraNegocioException.class, () -> new Resultado(1, 1, true, -1, 3));
    }

    @Test
    @DisplayName("Resultados com os mesmos valores são iguais e têm descrição legível")
    void igualdadeEDescricao() {
        assertEquals(Resultado.placar(2, 0), Resultado.placar(2, 0));
        assertEquals(Resultado.placar(2, 0).hashCode(), Resultado.placar(2, 0).hashCode());
        assertNotEquals(Resultado.placar(2, 0), Resultado.comProrrogacao(2, 0));
        assertNotEquals(Resultado.placar(2, 0), "2 x 0");
        assertEquals("2 x 0", Resultado.placar(2, 0).toString());
        assertEquals("1 x 1 (prorrogação), pênaltis 5 x 4", Resultado.comPenaltis(1, 5, 4).toString());
    }
}
