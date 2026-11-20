## Grupos turma 2026

- Caio
- Caua
- Diogo
- Gabriel Braga
- Gabriel Paulino
- Raul
- Samuel
- Vitor


---

## Funcionalidade extra (Avaliação P3 – Vitor): Fila de Atendimento com Prioridade

Tela **Fila de Atendimento** (menu do administrador) onde a recepção faz o *check-in* dos pacientes
que têm consulta no dia e o médico chama o próximo paciente.

**Estrutura de dados: Fila (Queue / FIFO)** — `java.util.ArrayDeque`.
Cada médico tem duas filas: **preferencial** (idosos 60+, pessoas com deficiência e prioridades
informadas na recepção, como gestantes) e **normal**. Quem chega primeiro é chamado primeiro dentro
de cada fila. Para a fila normal não ficar parada, a cada **2 preferenciais** é chamado **1 normal**.
As filas de cada médico ficam num `Map<idMedico, FilaMedico>`.

Arquivos principais:
- `backend/.../fila/FilaMedico.java` – as duas filas e a regra de chamada
- `backend/.../fila/FilaAtendimentoService.java` – check-in, chamada, desistência e critérios de prioridade
- `backend/.../fila/FilaAtendimentoController.java` – endpoints `/api/fila`
- `frontend/src/app/fila/` – tela Angular
- `backend/src/test/.../fila/FilaMedicoTest.java` – testes da fila

Endpoints:
| Método | URL | O que faz |
|---|---|---|
| POST | `/api/fila/checkin/{agendamentoId}` | coloca o paciente no fim da fila (corpo opcional `{"prioridade":"Gestante"}`) |
| POST | `/api/fila/medico/{medicoId}/chamar` | tira o próximo da fila (204 se vazia) |
| GET | `/api/fila` | situação de todas as filas |
| GET | `/api/fila/medico/{medicoId}` | situação da fila de um médico |
| DELETE | `/api/fila/{agendamentoId}` | paciente desistiu (sai da fila e o agendamento vira FALTOU) |

Como testar: faça login como admin → **Agendamentos** → crie agendamentos para hoje (um paciente com
60+ anos e outros mais novos) → **Fila de Atendimento** → faça o check-in de todos → clique em
**Chamar próximo** e veja a ordem P, P, N, P, N...
