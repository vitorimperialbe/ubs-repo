package br.sp.gov.fatec.ubs.backend.fila;

import br.sp.gov.fatec.ubs.backend.model.Paciente;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FilaMedicoTest {

    private ItemFila item(long id, boolean preferencial) {
        return new ItemFila(id, 1L, "Paciente " + id, "Dr. Teste",
                (preferencial ? "P" : "N") + id, preferencial, preferencial ? "Idoso" : null, LocalDateTime.now());
    }

    @Test
    void filaNormalRespeitaOrdemDeChegada() {
        FilaMedico fila = new FilaMedico();
        fila.entrar(item(1, false));
        fila.entrar(item(2, false));
        fila.entrar(item(3, false));

        assertEquals(1L, fila.chamarProximo().getAgendamentoId());
        assertEquals(2L, fila.chamarProximo().getAgendamentoId());
        assertEquals(3L, fila.chamarProximo().getAgendamentoId());
        assertNull(fila.chamarProximo());
    }

    @Test
    void chamaDoisPreferenciaisParaCadaNormal() {
        FilaMedico fila = new FilaMedico();
        fila.entrar(item(1, false));
        fila.entrar(item(2, false));
        fila.entrar(item(10, true));
        fila.entrar(item(11, true));
        fila.entrar(item(12, true));

        // P10, P11, N1, P12, N2
        List<Long> esperado = List.of(10L, 11L, 1L, 12L, 2L);
        assertEquals(esperado, fila.ordemDeChamada().stream().map(ItemFila::getAgendamentoId).toList());
        for (Long id : esperado) {
            assertEquals(id, fila.chamarProximo().getAgendamentoId());
        }
    }

    @Test
    void ordemDeChamadaNaoAlteraAFila() {
        FilaMedico fila = new FilaMedico();
        fila.entrar(item(1, true));
        fila.entrar(item(2, false));
        fila.ordemDeChamada();
        assertEquals(2, fila.total());
    }

    @Test
    void finalizarLiberaOMedico() {
        FilaMedico fila = new FilaMedico();
        fila.entrar(item(1, false));
        fila.chamarProximo();
        assertFalse(fila.finalizarAtendimento(99L));
        assertTrue(fila.finalizarAtendimento(1L));
        assertNull(fila.getEmAtendimento());
    }

    @Test
    void removerPacienteQueDesistiu() {
        FilaMedico fila = new FilaMedico();
        fila.entrar(item(1, false));
        fila.entrar(item(2, false));
        assertTrue(fila.remover(1L));
        assertFalse(fila.remover(99L));
        assertEquals(2L, fila.chamarProximo().getAgendamentoId());
    }

    @Test
    void criteriosDePrioridade() {
        LocalDate hoje = LocalDate.of(2026, 11, 20);
        Paciente idoso = new Paciente();
        idoso.setDataNascimento("1960-01-15");
        Paciente idosoFormatoBr = new Paciente();
        idosoFormatoBr.setDataNascimento("15/01/1950");
        Paciente pcd = new Paciente();
        pcd.setDataNascimento("2000-05-05");
        pcd.setDeficiente("Sim");
        Paciente comum = new Paciente();
        comum.setDataNascimento("2000-05-05");

        assertEquals("Idoso (66 anos)", FilaAtendimentoService.motivoPrioridade(idoso, null, hoje));
        assertEquals("Idoso (76 anos)", FilaAtendimentoService.motivoPrioridade(idosoFormatoBr, null, hoje));
        assertEquals("Pessoa com deficiência", FilaAtendimentoService.motivoPrioridade(pcd, null, hoje));
        assertNull(FilaAtendimentoService.motivoPrioridade(comum, null, hoje));
        assertEquals("Gestante", FilaAtendimentoService.motivoPrioridade(comum, "Gestante", hoje));
    }
}
