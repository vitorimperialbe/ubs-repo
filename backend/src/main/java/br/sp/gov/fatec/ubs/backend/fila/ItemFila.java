package br.sp.gov.fatec.ubs.backend.fila;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Representa um paciente esperando na fila de atendimento de um médico.
 * Não é uma entidade do banco: a fila fica só na memória do servidor
 * enquanto a UBS está funcionando no dia.
 */
public class ItemFila {

    private final Long agendamentoId;
    private final Long medicoId;
    private final String nomePaciente;
    private final String nomeMedico;
    private final String senha;          // ex.: P001 (preferencial) ou N001 (normal)
    private final boolean preferencial;
    private final String motivoPrioridade;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime horaChegada;

    public ItemFila(Long agendamentoId, Long medicoId, String nomePaciente, String nomeMedico,
                    String senha, boolean preferencial, String motivoPrioridade, LocalDateTime horaChegada) {
        this.agendamentoId = agendamentoId;
        this.medicoId = medicoId;
        this.nomePaciente = nomePaciente;
        this.nomeMedico = nomeMedico;
        this.senha = senha;
        this.preferencial = preferencial;
        this.motivoPrioridade = motivoPrioridade;
        this.horaChegada = horaChegada;
    }

    public Long getAgendamentoId() { return agendamentoId; }
    public Long getMedicoId() { return medicoId; }
    public String getNomePaciente() { return nomePaciente; }
    public String getNomeMedico() { return nomeMedico; }
    public String getSenha() { return senha; }
    public boolean isPreferencial() { return preferencial; }
    public String getMotivoPrioridade() { return motivoPrioridade; }
    public LocalDateTime getHoraChegada() { return horaChegada; }
}
