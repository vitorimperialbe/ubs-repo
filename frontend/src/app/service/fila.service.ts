import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ItemFila {
  agendamentoId: number;
  medicoId: number;
  nomePaciente: string;
  nomeMedico: string;
  senha: string;
  preferencial: boolean;
  motivoPrioridade: string | null;
  horaChegada: string;
}

export interface SituacaoFila {
  medicoId: number;
  emAtendimento: ItemFila | null;
  aguardando: ItemFila[];
  totalPreferencial: number;
  totalNormal: number;
}

@Injectable({ providedIn: 'root' })
export class FilaService {
  private apiUrl = 'http://localhost:8080/api/fila';

  constructor(private http: HttpClient) {}

  checkin(agendamentoId: number, prioridade?: string): Observable<ItemFila> {
    return this.http.post<ItemFila>(`${this.apiUrl}/checkin/${agendamentoId}`, { prioridade: prioridade || null });
  }

  chamarProximo(medicoId: number): Observable<ItemFila | null> {
    return this.http.post<ItemFila | null>(`${this.apiUrl}/medico/${medicoId}/chamar`, {});
  }

  finalizar(medicoId: number, agendamentoId: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/medico/${medicoId}/finalizar/${agendamentoId}`, {});
  }

  situacaoGeral(): Observable<SituacaoFila[]> {
    return this.http.get<SituacaoFila[]>(this.apiUrl);
  }

  remover(agendamentoId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${agendamentoId}`);
  }
}
