package br.sp.gov.fatec.ubs.backend.fila;

import br.sp.gov.fatec.ubs.backend.model.Agendamento;
import br.sp.gov.fatec.ubs.backend.model.Paciente;
import br.sp.gov.fatec.ubs.backend.repositories.AgendamentoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controla a fila de atendimento (check-in na recepção -> chamada pelo médico).
 *
 * Guarda um Map<idDoMedico, FilaMedico>: cada médico tem a sua própria fila.
 * Os métodos são synchronized porque várias requisições (recepção e médicos)
 * podem mexer nas filas ao mesmo tempo.
 */
@Service
public class FilaAtendimentoService {

    public static final int IDADE_PREFERENCIAL = 60;

    private final AgendamentoRepository agendamentoRepository;

    private final Map<Long, FilaMedico> filasPorMedico = new LinkedHashMap<>();
    /** agendamentoId -> medicoId: evita check-in duplicado e acha a fila de quem desistiu. */
    private final Map<Long, Long> agendamentosNaFila = new HashMap<>();
    private int contadorPreferencial = 0;
    private int contadorNormal = 0;
    private LocalDate diaDasFilas = LocalDate.now();

    public FilaAtendimentoService(AgendamentoRepository agendamentoRepository) {
        this.agendamentoRepository = agendamentoRepository;
    }

    /** A fila é do dia: se o dia mudou, começa tudo do zero (senhas voltam para 001). */
    private void reiniciarSeMudouODia() {
        LocalDate hoje = LocalDate.now();
        if (!hoje.equals(diaDasFilas)) {
            filasPorMedico.clear();
            agendamentosNaFila.clear();
            contadorPreferencial = 0;
            contadorNormal = 0;
            diaDasFilas = hoje;
        }
    }

    /** Recepção registra a chegada do paciente e ele entra no fim da fila do seu médico. */
    public synchronized ItemFila fazerCheckin(Long agendamentoId, String prioridadeInformada) {
        reiniciarSeMudouODia();
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado"));

        if (agendamentosNaFila.containsKey(agendamentoId)) {
            throw new IllegalStateException("Paciente já está na fila");
        }
        Agendamento.StatusAgendamento status = agendamento.getStatus();
        if (status != Agendamento.StatusAgendamento.AGENDADO && status != Agendamento.StatusAgendamento.CONFIRMADO) {
            throw new IllegalStateException("Agendamento com status " + status + " não pode entrar na fila");
        }

        String motivo = motivoPrioridade(agendamento.getPaciente(), prioridadeInformada, LocalDate.now());
        boolean preferencial = motivo != null;
        String senha = preferencial
                ? String.format("P%03d", ++contadorPreferencial)
                : String.format("N%03d", ++contadorNormal);

        Long medicoId = agendamento.getMedico().getId();
        ItemFila item = new ItemFila(agendamentoId, medicoId,
                agendamento.getPaciente().getNomeCompleto(), agendamento.getMedico().getNomeCompleto(),
                senha, preferencial, motivo, LocalDateTime.now());

        filasPorMedico.computeIfAbsent(medicoId, id -> new FilaMedico()).entrar(item);
        agendamentosNaFila.put(agendamentoId, medicoId);

        agendamento.setStatus(Agendamento.StatusAgendamento.CONFIRMADO);
        agendamentoRepository.save(agendamento);
        return item;
    }

    /** Médico chama o próximo paciente da sua fila. Retorna null se a fila estiver vazia. */
    public synchronized ItemFila chamarProximo(Long medicoId) {
        reiniciarSeMudouODia();
        FilaMedico fila = filasPorMedico.get(medicoId);
        if (fila == null) {
            return null;
        }
        ItemFila proximo = fila.chamarProximo();
        if (proximo != null) {
            agendamentosNaFila.remove(proximo.getAgendamentoId());
        }
        return proximo;
    }

    /** Médico terminou a consulta: libera o médico e o agendamento vira REALIZADO. */
    public synchronized void finalizarAtendimento(Long medicoId, Long agendamentoId) {
        FilaMedico fila = filasPorMedico.get(medicoId);
        if (fila == null || !fila.finalizarAtendimento(agendamentoId)) {
            throw new IllegalArgumentException("Esse paciente não está em atendimento com esse médico");
        }
        agendamentoRepository.findById(agendamentoId).ifPresent(a -> {
            a.setStatus(Agendamento.StatusAgendamento.REALIZADO);
            agendamentoRepository.save(a);
        });
    }

    /** Paciente desistiu: sai da fila e o agendamento é marcado como FALTOU. */
    public synchronized void removerDaFila(Long agendamentoId) {
        Long medicoId = agendamentosNaFila.remove(agendamentoId);
        if (medicoId == null) {
            throw new IllegalArgumentException("Paciente não está na fila");
        }
        filasPorMedico.get(medicoId).remover(agendamentoId);
        agendamentoRepository.findById(agendamentoId).ifPresent(a -> {
            a.setStatus(Agendamento.StatusAgendamento.FALTOU);
            agendamentoRepository.save(a);
        });
    }

    /** Situação da fila de um médico: quem está sendo atendido e a ordem de quem vem depois. */
    public synchronized Map<String, Object> situacao(Long medicoId) {
        FilaMedico fila = filasPorMedico.getOrDefault(medicoId, new FilaMedico());
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("medicoId", medicoId);
        resposta.put("emAtendimento", fila.getEmAtendimento());
        resposta.put("aguardando", fila.ordemDeChamada());
        resposta.put("totalPreferencial", fila.tamanhoPreferencial());
        resposta.put("totalNormal", fila.tamanhoNormal());
        return resposta;
    }

    /** Situação de todas as filas (tela da recepção). */
    public synchronized List<Map<String, Object>> situacaoGeral() {
        reiniciarSeMudouODia();
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Long medicoId : filasPorMedico.keySet()) {
            lista.add(situacao(medicoId));
        }
        return lista;
    }

    /**
     * Decide se o paciente é preferencial. Retorna o motivo ou null se for atendimento normal.
     * Critérios: prioridade informada na recepção (gestante, lactante, criança de colo...),
     * idade de 60 anos ou mais (Estatuto do Idoso) ou deficiência registrada no cadastro.
     */
    static String motivoPrioridade(Paciente paciente, String prioridadeInformada, LocalDate hoje) {
        if (prioridadeInformada != null && !prioridadeInformada.isBlank()) {
            return prioridadeInformada.trim();
        }
        Integer idade = calcularIdade(paciente.getDataNascimento(), hoje);
        if (idade != null && idade >= IDADE_PREFERENCIAL) {
            return "Idoso (" + idade + " anos)";
        }
        String deficiente = paciente.getDeficiente();
        if (deficiente != null && deficiente.trim().toLowerCase().matches("s|sim|true")) {
            return "Pessoa com deficiência";
        }
        return null;
    }

    /** Aceita datas no formato do banco (yyyy-MM-dd) ou brasileiro (dd/MM/yyyy). */
    static Integer calcularIdade(String dataNascimento, LocalDate hoje) {
        if (dataNascimento == null || dataNascimento.isBlank()) {
            return null;
        }
        for (String formato : new String[]{"yyyy-MM-dd", "dd/MM/yyyy"}) {
            try {
                LocalDate nascimento = LocalDate.parse(dataNascimento.trim(), DateTimeFormatter.ofPattern(formato));
                return Period.between(nascimento, hoje).getYears();
            } catch (DateTimeParseException ignorada) {
                // tenta o próximo formato
            }
        }
        return null;
    }
}
