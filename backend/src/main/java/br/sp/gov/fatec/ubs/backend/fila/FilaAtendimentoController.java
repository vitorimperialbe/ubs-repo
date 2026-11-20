package br.sp.gov.fatec.ubs.backend.fila;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fila")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'USER', 'MEDICO')")
public class FilaAtendimentoController {

    private final FilaAtendimentoService filaService;

    public FilaAtendimentoController(FilaAtendimentoService filaService) {
        this.filaService = filaService;
    }

    /** Check-in do paciente. Corpo opcional: { "prioridade": "Gestante" } */
    @PostMapping("/checkin/{agendamentoId}")
    public ResponseEntity<?> checkin(@PathVariable Long agendamentoId,
                                     @RequestBody(required = false) Map<String, String> body) {
        String prioridade = body != null ? body.get("prioridade") : null;
        try {
            return ResponseEntity.ok(filaService.fazerCheckin(agendamentoId, prioridade));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("erro", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("erro", e.getMessage()));
        }
    }

    /** Médico chama o próximo. 204 = fila vazia. */
    @PostMapping("/medico/{medicoId}/chamar")
    public ResponseEntity<ItemFila> chamarProximo(@PathVariable Long medicoId) {
        ItemFila proximo = filaService.chamarProximo(medicoId);
        return proximo == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(proximo);
    }

    /** Médico terminou a consulta do paciente que está em atendimento. */
    @PostMapping("/medico/{medicoId}/finalizar/{agendamentoId}")
    public ResponseEntity<?> finalizar(@PathVariable Long medicoId, @PathVariable Long agendamentoId) {
        try {
            filaService.finalizarAtendimento(medicoId, agendamentoId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/medico/{medicoId}")
    public ResponseEntity<Map<String, Object>> situacao(@PathVariable Long medicoId) {
        return ResponseEntity.ok(filaService.situacao(medicoId));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> situacaoGeral() {
        return ResponseEntity.ok(filaService.situacaoGeral());
    }

    /** Paciente desistiu de esperar. */
    @DeleteMapping("/{agendamentoId}")
    public ResponseEntity<?> remover(@PathVariable Long agendamentoId) {
        try {
            filaService.removerDaFila(agendamentoId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("erro", e.getMessage()));
        }
    }
}
