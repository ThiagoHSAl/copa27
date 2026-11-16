package br.ufmg.dcc603.copa27.dominio.modelo;

import java.time.LocalDateTime;

/**
 * Partida do torneio. A situação só muda pelas operações {@link #iniciar()} e
 * {@link #encerrar()}, conforme a máquina de estados do Documento 05 (seção 4).
 */
public class Partida {

    private int id;
    private final Fase fase;
    private LocalDateTime dataHora;
    private SituacaoPartida situacao = SituacaoPartida.AGENDADA;
    private final Grupo grupo;
    private Estadio estadio;
    private Selecao mandante;
    private Selecao visitante;
    private Resultado resultado;
    private boolean resultadoSimulado;

    public Partida(int id, Fase fase, LocalDateTime dataHora, Estadio estadio,
                   Grupo grupo, Selecao mandante, Selecao visitante) {
        if (fase == null) {
            throw new RegraNegocioException("A fase da partida é obrigatória.");
        }
        if (dataHora == null) {
            throw new RegraNegocioException("A data e o horário da partida são obrigatórios.");
        }
        if (estadio == null) {
            throw new RegraNegocioException("O estádio da partida é obrigatório.");
        }
        if ((fase == Fase.GRUPOS) != (grupo != null)) {
            throw new RegraNegocioException("Apenas partidas da fase de grupos pertencem a um grupo.");
        }
        if (fase == Fase.GRUPOS && (mandante == null || visitante == null)) {
            throw new RegraNegocioException("Partidas da fase de grupos precisam das duas seleções.");
        }
        if (mandante != null && mandante == visitante) {
            throw new RegraNegocioException("Uma seleção não pode enfrentar a si mesma.");
        }
        this.id = id;
        this.fase = fase;
        this.dataHora = dataHora;
        this.estadio = estadio;
        this.grupo = grupo;
        this.mandante = mandante;
        this.visitante = visitante;
    }

    /** Cópia independente da partida, ligada ao grupo informado (usada por {@link Cenario#copiar()}). */
    Partida copiar(Grupo grupoDaCopia) {
        Partida c = new Partida(id, fase, dataHora, estadio, grupoDaCopia, mandante, visitante);
        c.situacao = situacao;
        c.resultado = resultado;
        c.resultadoSimulado = resultadoSimulado;
        return c;
    }

    /** Agendada para em andamento, desde que as duas seleções estejam definidas. */
    public void iniciar() {
        if (situacao != SituacaoPartida.AGENDADA) {
            throw new RegraNegocioException("Apenas partidas agendadas podem ser iniciadas.");
        }
        if (mandante == null || visitante == null) {
            throw new RegraNegocioException("A partida só pode começar com as duas seleções definidas.");
        }
        situacao = SituacaoPartida.EM_ANDAMENTO;
    }

    /**
     * Registra o placar real. Partidas agendadas recusam o registro (RN06); uma
     * partida de mata-mata encerrada só aceita correção que mantenha uma vencedora (RN05).
     */
    public void registrarResultado(Resultado r) {
        if (r == null) {
            throw new RegraNegocioException("Informe o resultado.");
        }
        if (situacao == SituacaoPartida.AGENDADA) {
            throw new RegraNegocioException("A partida ainda não começou; o resultado não pode ser registrado.");
        }
        validarPenaltis(r);
        if (situacao == SituacaoPartida.ENCERRADA && fase.isMataMata() && !r.defineVencedora()) {
            throw new RegraNegocioException("Partidas de mata-mata não podem terminar empatadas.");
        }
        resultado = r;
        resultadoSimulado = false;
    }

    /** Em andamento para encerrada; exige resultado e, no mata-mata, uma vencedora (RN05). */
    public void encerrar() {
        if (situacao != SituacaoPartida.EM_ANDAMENTO) {
            throw new RegraNegocioException("Apenas partidas em andamento podem ser encerradas.");
        }
        if (resultado == null) {
            throw new RegraNegocioException("Registre o resultado antes de encerrar a partida.");
        }
        if (fase.isMataMata() && !resultado.defineVencedora()) {
            throw new RegraNegocioException(
                    "Partidas de mata-mata não podem terminar empatadas; informe a prorrogação e os pênaltis.");
        }
        situacao = SituacaoPartida.ENCERRADA;
    }

    /**
     * Aplica um placar hipotético, usado apenas em um cenário simulado (CU06).
     * Partidas encerradas não podem ser simuladas, e no mata-mata o placar
     * precisa apontar uma vencedora.
     */
    public void simular(Resultado r) {
        if (r == null) {
            throw new RegraNegocioException("Informe o resultado.");
        }
        if (situacao == SituacaoPartida.ENCERRADA) {
            throw new RegraNegocioException("Apenas partidas não disputadas podem ser simuladas.");
        }
        if (mandante == null || visitante == null) {
            throw new RegraNegocioException("As seleções desta partida ainda não estão definidas.");
        }
        validarPenaltis(r);
        if (fase.isMataMata() && !r.defineVencedora()) {
            throw new RegraNegocioException(
                    "O mata-mata não admite empate; informe o vencedor na prorrogação ou nos pênaltis.");
        }
        resultado = r;
        resultadoSimulado = true;
    }

    private void validarPenaltis(Resultado r) {
        if (!fase.isMataMata() && (r.temPenaltis() || r.isHouveProrrogacao())) {
            throw new RegraNegocioException("A fase de grupos não tem prorrogação nem pênaltis.");
        }
    }

    public boolean estaDisputada() {
        return situacao == SituacaoPartida.ENCERRADA;
    }

    /** Resultado que não muda mais no cenário: real e encerrado, ou simulado. */
    public boolean temResultadoDefinitivo() {
        return resultado != null && (estaDisputada() || resultadoSimulado);
    }

    public Selecao vencedora() {
        if (resultado == null || !resultado.defineVencedora()) {
            return null;
        }
        return resultado.mandanteVenceu() ? mandante : visitante;
    }

    public Selecao perdedora() {
        Selecao v = vencedora();
        if (v == null) {
            return null;
        }
        return v == mandante ? visitante : mandante;
    }

    public boolean envolve(Selecao s) {
        return s != null && (s == mandante || s == visitante);
    }

    /**
     * Define a mandante de uma partida de mata-mata. Se a seleção mudar, um
     * placar simulado anterior perde o sentido e é descartado.
     */
    public void definirMandante(Selecao s) {
        if (s == mandante) {
            return;
        }
        verificarParticipantesEditaveis(s, visitante);
        mandante = s;
        descartarSimulacao();
    }

    public void definirVisitante(Selecao s) {
        if (s == visitante) {
            return;
        }
        verificarParticipantesEditaveis(mandante, s);
        visitante = s;
        descartarSimulacao();
    }

    private void verificarParticipantesEditaveis(Selecao m, Selecao v) {
        if (situacao != SituacaoPartida.AGENDADA) {
            throw new RegraNegocioException("As seleções só podem mudar enquanto a partida está agendada.");
        }
        if (m != null && m == v) {
            throw new RegraNegocioException("Uma seleção não pode enfrentar a si mesma.");
        }
    }

    private void descartarSimulacao() {
        if (resultadoSimulado) {
            resultado = null;
            resultadoSimulado = false;
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Fase getFase() {
        return fase;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        if (dataHora == null) {
            throw new RegraNegocioException("A data e o horário da partida são obrigatórios.");
        }
        this.dataHora = dataHora;
    }

    public SituacaoPartida getSituacao() {
        return situacao;
    }

    public Grupo getGrupo() {
        return grupo;
    }

    public Estadio getEstadio() {
        return estadio;
    }

    public void setEstadio(Estadio estadio) {
        if (estadio == null) {
            throw new RegraNegocioException("O estádio da partida é obrigatório.");
        }
        this.estadio = estadio;
    }

    public Selecao getMandante() {
        return mandante;
    }

    public Selecao getVisitante() {
        return visitante;
    }

    public Resultado getResultado() {
        return resultado;
    }

    public boolean isResultadoSimulado() {
        return resultadoSimulado;
    }

    @Override
    public String toString() {
        String m = mandante == null ? "a definir" : mandante.getPais();
        String v = visitante == null ? "a definir" : visitante.getPais();
        return "Partida " + id + " (" + m + " x " + v + ")";
    }
}
