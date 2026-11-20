package br.sp.gov.fatec.ubs.backend.fila;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Fila de atendimento de UM médico.
 *
 * Usa duas FILAS (FIFO - o primeiro que chega é o primeiro a ser chamado):
 *  - preferencial: idosos (60+), pessoas com deficiência e prioridades informadas na recepção
 *  - normal: demais pacientes
 *
 * Regra de chamada: a fila preferencial tem prioridade, mas depois de
 * {@value #MAX_PREFERENCIAIS_SEGUIDOS} chamadas preferenciais seguidas, se houver alguém
 * na fila normal, ele é chamado. Assim a fila normal nunca fica parada para sempre.
 *
 * ArrayDeque é a implementação de fila do Java: offer() coloca no fim e
 * poll() tira do começo, as duas em O(1).
 */
public class FilaMedico {

    public static final int MAX_PREFERENCIAIS_SEGUIDOS = 2;

    private final Queue<ItemFila> preferencial = new ArrayDeque<>();
    private final Queue<ItemFila> normal = new ArrayDeque<>();
    private int preferenciaisSeguidos = 0;
    private ItemFila emAtendimento;

    /** Coloca o paciente no FIM da fila correspondente (enqueue). */
    public void entrar(ItemFila item) {
        if (item.isPreferencial()) {
            preferencial.offer(item);
        } else {
            normal.offer(item);
        }
    }

    /**
     * Tira o próximo paciente do COMEÇO da fila (dequeue), seguindo a regra 2 preferenciais : 1 normal.
     * Retorna null se não houver ninguém esperando.
     */
    public ItemFila chamarProximo() {
        boolean podeChamarPreferencial = !preferencial.isEmpty()
                && (preferenciaisSeguidos < MAX_PREFERENCIAIS_SEGUIDOS || normal.isEmpty());

        ItemFila proximo;
        if (podeChamarPreferencial) {
            proximo = preferencial.poll();
            preferenciaisSeguidos++;
        } else {
            proximo = normal.poll();
            preferenciaisSeguidos = 0;
        }
        emAtendimento = proximo;
        return proximo;
    }

    /** Remove um paciente que desistiu de esperar. */
    public boolean remover(Long agendamentoId) {
        return preferencial.removeIf(i -> i.getAgendamentoId().equals(agendamentoId))
                || normal.removeIf(i -> i.getAgendamentoId().equals(agendamentoId));
    }

    /**
     * Retorna a ordem em que os pacientes SERÃO chamados, simulando a regra
     * em cópias das filas (as filas reais não são alteradas).
     */
    public List<ItemFila> ordemDeChamada() {
        Queue<ItemFila> pref = new ArrayDeque<>(preferencial);
        Queue<ItemFila> norm = new ArrayDeque<>(normal);
        int seguidos = preferenciaisSeguidos;
        List<ItemFila> ordem = new ArrayList<>();

        while (!pref.isEmpty() || !norm.isEmpty()) {
            if (!pref.isEmpty() && (seguidos < MAX_PREFERENCIAIS_SEGUIDOS || norm.isEmpty())) {
                ordem.add(pref.poll());
                seguidos++;
            } else {
                ordem.add(norm.poll());
                seguidos = 0;
            }
        }
        return ordem;
    }

    public ItemFila getEmAtendimento() { return emAtendimento; }
    public int tamanhoPreferencial() { return preferencial.size(); }
    public int tamanhoNormal() { return normal.size(); }
    public int total() { return preferencial.size() + normal.size(); }
}
