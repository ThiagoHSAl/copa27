package br.ufmg.dcc603.copa27.persistencia.memoria;

import br.ufmg.dcc603.copa27.dominio.modelo.Administrador;
import br.ufmg.dcc603.copa27.dominio.modelo.Aeroporto;
import br.ufmg.dcc603.copa27.dominio.modelo.Confederacao;
import br.ufmg.dcc603.copa27.dominio.modelo.Estadio;
import br.ufmg.dcc603.copa27.dominio.modelo.Fase;
import br.ufmg.dcc603.copa27.dominio.modelo.Grupo;
import br.ufmg.dcc603.copa27.dominio.modelo.Partida;
import br.ufmg.dcc603.copa27.dominio.modelo.Regiao;
import br.ufmg.dcc603.copa27.dominio.modelo.Sede;
import br.ufmg.dcc603.copa27.dominio.modelo.Selecao;
import br.ufmg.dcc603.copa27.dominio.regras.GeradorChaveamento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Carga inicial da base. As sedes, os estádios e os aeroportos são os reais
 * (dados aproximados). Como a FIFA ainda não realizou o sorteio nem divulgou
 * a tabela (risco R1), as seleções, os grupos e a tabela são FICTÍCIOS,
 * porém consistentes com o formato oficial: 32 seleções, 8 grupos de 4 e
 * 64 partidas entre 24/06 e 25/07/2027.
 */
public final class CargaInicial {

    /** Seleções por grupo, de A a H: país, código FIFA e confederação. */
    private static final Object[][][] GRUPOS = {
            {{"Brasil", "BRA", Confederacao.CONMEBOL}, {"Noruega", "NOR", Confederacao.UEFA},
                    {"Zâmbia", "ZAM", Confederacao.CAF}, {"Coreia do Sul", "KOR", Confederacao.AFC}},
            {{"Estados Unidos", "USA", Confederacao.CONCACAF}, {"Países Baixos", "NED", Confederacao.UEFA},
                    {"Vietnã", "VIE", Confederacao.AFC}, {"Portugal", "POR", Confederacao.UEFA}},
            {{"Espanha", "ESP", Confederacao.UEFA}, {"Japão", "JPN", Confederacao.AFC},
                    {"Costa Rica", "CRC", Confederacao.CONCACAF}, {"Marrocos", "MAR", Confederacao.CAF}},
            {{"Inglaterra", "ENG", Confederacao.UEFA}, {"China", "CHN", Confederacao.AFC},
                    {"Haiti", "HAI", Confederacao.CONCACAF}, {"Dinamarca", "DEN", Confederacao.UEFA}},
            {{"Alemanha", "GER", Confederacao.UEFA}, {"Colômbia", "COL", Confederacao.CONMEBOL},
                    {"Filipinas", "PHI", Confederacao.AFC}, {"Irlanda", "IRL", Confederacao.UEFA}},
            {{"França", "FRA", Confederacao.UEFA}, {"Jamaica", "JAM", Confederacao.CONCACAF},
                    {"Panamá", "PAN", Confederacao.CONCACAF}, {"Austrália", "AUS", Confederacao.AFC}},
            {{"Suécia", "SWE", Confederacao.UEFA}, {"Argentina", "ARG", Confederacao.CONMEBOL},
                    {"África do Sul", "RSA", Confederacao.CAF}, {"Itália", "ITA", Confederacao.UEFA}},
            {{"Canadá", "CAN", Confederacao.CONCACAF}, {"Nova Zelândia", "NZL", Confederacao.OFC},
                    {"Nigéria", "NGA", Confederacao.CAF}, {"Suíça", "SUI", Confederacao.UEFA}},
    };

    /** Rodadas da fase de grupos: índices (0 a 3) da mandante e da visitante. */
    private static final int[][][] RODADAS = {
            {{0, 1}, {2, 3}},
            {{0, 2}, {3, 1}},
            {{3, 0}, {1, 2}},
    };

    private static final LocalTime[] HORARIOS = {LocalTime.of(13, 0), LocalTime.of(16, 0),
            LocalTime.of(19, 0), LocalTime.of(21, 30)};

    private CargaInicial() {
    }

    /** Popula a base com sedes, seleções, grupos, as 64 partidas, o chaveamento e o administrador. */
    public static void popular(BaseDeDadosMemoria base, String loginAdmin, String hashSenhaAdmin) {
        List<Sede> sedes = criarSedes();
        RepositorioSedesMemoria repSedes = base.repositorioSedes();
        sedes.forEach(repSedes::salvar);

        RepositorioSelecoesMemoria repSelecoes = base.repositorioSelecoes();
        RepositorioGruposMemoria repGrupos = base.repositorioGrupos();
        List<Grupo> grupos = new ArrayList<>();
        for (int g = 0; g < GRUPOS.length; g++) {
            Grupo grupo = new Grupo((char) ('A' + g));
            for (Object[] dados : GRUPOS[g]) {
                Selecao s = new Selecao(0, (String) dados[0], (String) dados[1], (Confederacao) dados[2]);
                repSelecoes.salvar(s);
                grupo.adicionarSelecao(s);
            }
            repGrupos.salvar(grupo);
            grupos.add(grupo);
        }

        RepositorioPartidasMemoria repPartidas = base.repositorioPartidas();
        List<Partida> partidas = criarPartidas(grupos, sedes);
        partidas.forEach(repPartidas::salvar);

        List<Partida> mataMata = partidas.stream().filter(p -> p.getFase().isMataMata()).toList();
        base.repositorioChaveamento().salvar(new GeradorChaveamento().montarChaveamento(mataMata));

        base.repositorioAdministradores().salvar(new Administrador(loginAdmin, hashSenhaAdmin));
    }

    static List<Sede> criarSedes() {
        List<Sede> l = new ArrayList<>();
        l.add(new Sede(0, "Belo Horizonte", Regiao.SUDESTE,
                new Estadio("Estádio Mineirão", 61_846, 1965, "Av. Antônio Abrahão Caram, 1001, Pampulha"),
                List.of(new Aeroporto("Aeroporto Internacional de Confins", "CNF", 38.0),
                        new Aeroporto("Aeroporto da Pampulha", "PLU", 4.5))));
        l.add(new Sede(0, "Brasília", Regiao.CENTRO_OESTE,
                new Estadio("Estádio Nacional Mané Garrincha", 72_788, 1974, "SRPN, Asa Norte"),
                List.of(new Aeroporto("Aeroporto Internacional de Brasília", "BSB", 14.0))));
        l.add(new Sede(0, "Fortaleza", Regiao.NORDESTE,
                new Estadio("Arena Castelão", 63_903, 1973, "Av. Alberto Craveiro, 2901, Castelão"),
                List.of(new Aeroporto("Aeroporto Internacional Pinto Martins", "FOR", 9.0))));
        l.add(new Sede(0, "Porto Alegre", Regiao.SUL,
                new Estadio("Estádio Beira-Rio", 50_842, 1969, "Av. Padre Cacique, 891, Praia de Belas"),
                List.of(new Aeroporto("Aeroporto Internacional Salgado Filho", "POA", 13.0))));
        l.add(new Sede(0, "Recife", Regiao.NORDESTE,
                new Estadio("Arena de Pernambuco", 44_300, 2013, "Av. Deus é Fiel, 1, São Lourenço da Mata"),
                List.of(new Aeroporto("Aeroporto Internacional dos Guararapes", "REC", 25.0))));
        l.add(new Sede(0, "Rio de Janeiro", Regiao.SUDESTE,
                new Estadio("Estádio do Maracanã", 78_838, 1950, "Av. Presidente Castelo Branco, Maracanã"),
                List.of(new Aeroporto("Aeroporto Internacional do Galeão", "GIG", 15.0),
                        new Aeroporto("Aeroporto Santos Dumont", "SDU", 10.0))));
        l.add(new Sede(0, "Salvador", Regiao.NORDESTE,
                new Estadio("Arena Fonte Nova", 47_907, 1951, "Ladeira da Fonte das Pedras, Nazaré"),
                List.of(new Aeroporto("Aeroporto Internacional de Salvador", "SSA", 26.0))));
        l.add(new Sede(0, "São Paulo", Regiao.SUDESTE,
                new Estadio("Arena Itaquera", 47_605, 2014, "Av. Miguel Ignácio Curi, 111, Itaquera"),
                List.of(new Aeroporto("Aeroporto Internacional de Guarulhos", "GRU", 17.0),
                        new Aeroporto("Aeroporto de Congonhas", "CGH", 25.0))));
        return l;
    }

    /**
     * Partidas 1 a 48 (grupos, três rodadas), 49 a 56 (oitavas), 57 a 60
     * (quartas), 61 e 62 (semifinais), 63 (terceiro lugar) e 64 (final).
     */
    static List<Partida> criarPartidas(List<Grupo> grupos, List<Sede> sedes) {
        List<Partida> l = new ArrayList<>();
        int id = 1;
        LocalDate[] inicioRodada = {LocalDate.of(2027, 6, 24), LocalDate.of(2027, 6, 28), LocalDate.of(2027, 7, 3)};
        for (int r = 0; r < RODADAS.length; r++) {
            for (int g = 0; g < grupos.size(); g++) {
                Grupo grupo = grupos.get(g);
                LocalDate dia = inicioRodada[r].plusDays(g / 2);
                for (int j = 0; j < 2; j++) {
                    Selecao m = grupo.getSelecoes().get(RODADAS[r][j][0]);
                    Selecao v = grupo.getSelecoes().get(RODADAS[r][j][1]);
                    // Na última rodada os dois jogos do grupo são simultâneos.
                    LocalTime hora = r == 2 ? HORARIOS[(g % 2) * 2 + 1] : HORARIOS[(g % 2) * 2 + j];
                    Sede sede = sedes.get((g * 2 + j + r * 3) % sedes.size());
                    l.add(new Partida(id++, Fase.GRUPOS, LocalDateTime.of(dia, hora), sede.getEstadio(),
                            grupo, m, v));
                }
            }
        }
        Sede rio = sedes.stream().filter(s -> s.getCidade().equals("Rio de Janeiro")).findFirst().orElseThrow();
        Sede sp = sedes.stream().filter(s -> s.getCidade().equals("São Paulo")).findFirst().orElseThrow();
        for (int i = 0; i < 8; i++) {
            LocalDate dia = LocalDate.of(2027, 7, 10 + i / 2);
            l.add(mataMata(id++, Fase.OITAVAS, dia, HORARIOS[1 + (i % 2) * 2], sedes.get(i).getEstadio()));
        }
        for (int i = 0; i < 4; i++) {
            LocalDate dia = LocalDate.of(2027, 7, 16 + i / 2);
            l.add(mataMata(id++, Fase.QUARTAS, dia, HORARIOS[1 + (i % 2) * 2], sedes.get(i * 2 + 1).getEstadio()));
        }
        l.add(mataMata(id++, Fase.SEMIFINAL, LocalDate.of(2027, 7, 20), HORARIOS[3], sp.getEstadio()));
        l.add(mataMata(id++, Fase.SEMIFINAL, LocalDate.of(2027, 7, 21), HORARIOS[3], sedes.get(0).getEstadio()));
        l.add(mataMata(id++, Fase.TERCEIRO_LUGAR, LocalDate.of(2027, 7, 24), HORARIOS[1],
                sedes.get(1).getEstadio()));
        l.add(mataMata(id, Fase.FINAL, LocalDate.of(2027, 7, 25), HORARIOS[2], rio.getEstadio()));
        return l;
    }

    private static Partida mataMata(int id, Fase fase, LocalDate dia, LocalTime hora, Estadio estadio) {
        return new Partida(id, fase, LocalDateTime.of(dia, hora), estadio, null, null, null);
    }
}
