import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AgendamentoService } from '../service/agendamento.service';
import { FilaService, ItemFila, SituacaoFila } from '../service/fila.service';
import { Agendamento, StatusAgendamento } from '../model/agendamento.model';

@Component({
  selector: 'app-fila',
  standalone: true,
  imports: [CommonModule, FormsModule, MatCardModule, MatButtonModule, MatIconModule,
    MatSelectModule, MatFormFieldModule, MatSnackBarModule],
  templateUrl: './fila.component.html',
  styleUrls: ['./fila.component.scss']
})
export class FilaComponent implements OnInit, OnDestroy {
  agendamentosDeHoje: Agendamento[] = [];
  filas: SituacaoFila[] = [];
  agendamentoSelecionado: number | null = null;
  prioridadeSelecionada = '';
  prioridades = ['Gestante', 'Lactante', 'Pessoa com criança de colo', 'Pessoa com deficiência', 'Obeso'];
  private atualizacao?: ReturnType<typeof setInterval>;

  constructor(
    private filaService: FilaService,
    private agendamentoService: AgendamentoService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.carregar();
    this.atualizacao = setInterval(() => this.carregarFilas(), 10000); // atualiza a cada 10s
  }

  ngOnDestroy(): void {
    clearInterval(this.atualizacao);
  }

  carregar(): void {
    this.carregarFilas();
    this.agendamentoService.listarTodos().subscribe(lista => {
      const hoje = new Date().toDateString();
      this.agendamentosDeHoje = lista
        .filter(a => new Date(a.dataHoraConsulta).toDateString() === hoje)
        .filter(a => a.status === StatusAgendamento.AGENDADO || a.status === StatusAgendamento.CONFIRMADO)
        .sort((a, b) => a.dataHoraConsulta.localeCompare(b.dataHoraConsulta));
    });
  }

  carregarFilas(): void {
    this.filaService.situacaoGeral().subscribe(f => this.filas = f);
  }

  /** Agendamentos de hoje que ainda não estão em nenhuma fila. */
  get disponiveisParaCheckin(): Agendamento[] {
    const naFila = new Set(this.filas.flatMap(f => f.aguardando.map(i => i.agendamentoId)));
    const atendidos = new Set(this.filas.map(f => f.emAtendimento?.agendamentoId));
    return this.agendamentosDeHoje.filter(a => !naFila.has(a.id) && !atendidos.has(a.id));
  }

  fazerCheckin(): void {
    if (!this.agendamentoSelecionado) return;
    this.filaService.checkin(this.agendamentoSelecionado, this.prioridadeSelecionada).subscribe({
      next: item => {
        this.mensagem(`Senha ${item.senha} – ${item.nomePaciente}` +
          (item.preferencial ? ` (preferencial: ${item.motivoPrioridade})` : ''));
        this.agendamentoSelecionado = null;
        this.prioridadeSelecionada = '';
        this.carregar();
      },
      error: err => this.mensagem(err.error?.erro || 'Erro ao fazer check-in')
    });
  }

  chamar(fila: SituacaoFila): void {
    this.filaService.chamarProximo(fila.medicoId).subscribe({
      next: item => {
        this.mensagem(item ? `Chamando senha ${item.senha}: ${item.nomePaciente}` : 'Fila vazia');
        this.carregarFilas();
      },
      error: () => this.mensagem('Erro ao chamar próximo')
    });
  }

  finalizar(item: ItemFila): void {
    this.agendamentoService.marcarComoRealizado(item.agendamentoId).subscribe({
      next: () => this.mensagem(`Atendimento de ${item.nomePaciente} finalizado`),
      error: () => this.mensagem('Erro ao finalizar atendimento')
    });
  }

  remover(item: ItemFila): void {
    this.filaService.remover(item.agendamentoId).subscribe({
      next: () => { this.mensagem(`${item.nomePaciente} saiu da fila (falta)`); this.carregar(); },
      error: () => this.mensagem('Erro ao remover da fila')
    });
  }

  nomeMedico(fila: SituacaoFila): string {
    return fila.emAtendimento?.nomeMedico || fila.aguardando[0]?.nomeMedico || `Médico ${fila.medicoId}`;
  }

  hora(s: string): string {
    return new Date(s).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
  }

  private mensagem(msg: string): void {
    this.snackBar.open(msg, 'Fechar', { duration: 4000 });
  }
}
