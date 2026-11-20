package br.sp.gov.fatec.ubs.backend.bootstrap;

import br.sp.gov.fatec.ubs.backend.model.Agendamento;
import br.sp.gov.fatec.ubs.backend.model.Medico;
import br.sp.gov.fatec.ubs.backend.model.Paciente;
import br.sp.gov.fatec.ubs.backend.repositories.AgendamentoRepository;
import br.sp.gov.fatec.ubs.backend.repositories.MedicoRepository;
import br.sp.gov.fatec.ubs.backend.repositories.PacienteRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Dados de exemplo para testar a Fila de Atendimento localmente.
 * Só roda com o perfil "local" (banco H2 em memória).
 */
@Component
@Profile("local")
public class LocalDataSeeder implements CommandLineRunner {

    private final MedicoRepository medicoRepository;
    private final PacienteRepository pacienteRepository;
    private final AgendamentoRepository agendamentoRepository;

    private final AdminSeeder adminSeeder;

    public LocalDataSeeder(MedicoRepository medicoRepository, PacienteRepository pacienteRepository,
                           AgendamentoRepository agendamentoRepository, AdminSeeder adminSeeder) {
        this.medicoRepository = medicoRepository;
        this.pacienteRepository = pacienteRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.adminSeeder = adminSeeder;
    }

    @Override
    public void run(String... args) {
        // Com o banco vazio, o AdminSeeder pode rodar antes do RoleSeeder e não criar o admin.
        // Aqui os perfis já existem, então chamamos de novo (ele não duplica o usuário).
        adminSeeder.onApplicationEvent(null);

        if (medicoRepository.count() > 0) {
            return;
        }
        Medico clinico = medicoRepository.save(new Medico("Ana Souza", "Clínica Geral", "123456-SP", "ana.souza@ubs.local"));
        Medico pediatra = medicoRepository.save(new Medico("Carlos Lima", "Pediatria", "654321-SP", "carlos.lima@ubs.local"));

        Paciente joao = paciente("João da Silva", "1950-03-10", "Não");      // idoso -> preferencial
        Paciente maria = paciente("Maria Oliveira", "1995-07-22", "Não");    // normal
        Paciente pedro = paciente("Pedro Santos", "1988-11-02", "Sim");      // deficiência -> preferencial
        Paciente julia = paciente("Júlia Costa", "2001-01-15", "Não");       // normal
        Paciente lucas = paciente("Lucas Pereira", "1992-05-30", "Não");     // normal
        Paciente helena = paciente("Helena Rocha", "1958-09-09", "Não");     // idosa -> preferencial
        Paciente bia = paciente("Beatriz Alves", "2019-04-12", "Não");       // criança, pediatria

        LocalDate hoje = LocalDate.now();
        Paciente[] doClinico = {maria, joao, julia, pedro, lucas, helena};
        for (int i = 0; i < doClinico.length; i++) {
            agendamento(doClinico[i], clinico, hoje, 8 + i, "Consulta Clínica Geral");
        }
        agendamento(bia, pediatra, hoje, 9, "Consulta Pediatria");
    }

    private Paciente paciente(String nome, String nascimento, String deficiente) {
        Paciente p = new Paciente();
        p.setNomeCompleto(nome);
        p.setDataNascimento(nascimento);
        p.setDeficiente(deficiente);
        p.setCpf(String.valueOf(10000000000L + Math.abs(nome.hashCode() % 899999999L)));
        return pacienteRepository.save(p);
    }

    private void agendamento(Paciente p, Medico m, LocalDate dia, int hora, String tipo) {
        agendamentoRepository.save(new Agendamento(p, m, dia.atTime(hora, 0), tipo));
    }
}
