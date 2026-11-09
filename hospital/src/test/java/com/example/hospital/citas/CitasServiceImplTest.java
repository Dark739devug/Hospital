package com.example.hospital.citas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hospital.citas.dto.CitasDTO;
import com.example.hospital.citas.impl.CitasServiceImpl;
import com.example.hospital.consultas.ConsultasRepository;
import com.example.hospital.estadocita.EstadoCita;
import com.example.hospital.estadocita.EstadoCitaRepository;
import com.example.hospital.estadocita.EstadoCitaService;
import com.example.hospital.medico.Medico;
import com.example.hospital.medico.MedicoRepository;
import com.example.hospital.paciente.Paciente;
import com.example.hospital.paciente.PacienteRepository;
import com.example.hospital.shared.exception.DuplicadoException;
import com.example.hospital.shared.exception.RecursoNoEncontradoException;
import com.example.hospital.shared.exception.ReglaNegocioException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CitasServiceImplTest {

    @Mock CitasRepository citasRepository;
    @Mock PacienteRepository pacienteRepository;
    @Mock MedicoRepository medicoRepository;
    @Mock EstadoCitaRepository estadoCitaRepository;
    @Mock EstadoCitaService estadoCitaService;
    @Mock ConsultasRepository consultasRepository;
    @InjectMocks CitasServiceImpl service;

    private Citas cita(Long id, Long idEstado) {
        Citas c = new Citas();
        c.setIdCita(id);
        c.setIdEstado(idEstado);
        c.setActivo(true);
        return c;
    }

    private EstadoCita estado(Long id, String descripcion) {
        EstadoCita e = new EstadoCita();
        e.setIdEstado(id);
        e.setDescripcion(descripcion);
        e.setActivo(true);
        return e;
    }

    @Test
    void crear_asignaFechaCreacionYActivo_yGuarda() {
        LocalDate manana = LocalDate.now().plusDays(1);
        LocalTime hora = LocalTime.of(9, 0);
        CitasDTO dto = new CitasDTO(null, manana, hora, "  Control  ", null, 1L, 2L, 3L, null);
        when(pacienteRepository.findById(3L)).thenReturn(Optional.of(new Paciente()));
        when(medicoRepository.findByIdParaReservar(2L)).thenReturn(Optional.of(new Medico()));
        when(estadoCitaRepository.findById(1L)).thenReturn(Optional.of(estado(1L, "Programada")));
        when(citasRepository.existsByIdMedicoAndFechaAndHoraAndActivoTrueAndIdEstadoNotIn(eq(2L), eq(manana), eq(hora), anyCollection())).thenReturn(false);
        when(citasRepository.save(any(Citas.class))).thenAnswer(inv -> {
            Citas c = inv.getArgument(0);
            c.setIdCita(10L);
            return c;
        });

        CitasDTO r = service.crear(dto);

        assertThat(r.idCita()).isEqualTo(10L);
        assertThat(r.activo()).isTrue();
        assertThat(r.fechaCreacion()).isNotNull();
        assertThat(r.motivo()).isEqualTo("Control");
    }

    @Test
    void findById_lanzaNoEncontrado_siLaCitaNoExiste() {
        when(citasRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.findById(99L));
    }

    @Test
    void actualizar_lanzaNoEncontrado_siLaCitaNoExiste_yNoGuarda() {
        when(citasRepository.findById(99L)).thenReturn(Optional.empty());
        CitasDTO dto = new CitasDTO(null, LocalDate.now(), LocalTime.NOON, "x", null, 1L, 2L, 3L, true);

        assertThrows(RecursoNoEncontradoException.class, () -> service.actualizar(dto, 99L));

        verify(citasRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_lanzaNoEncontrado_siLaCitaNoExiste_yNoGuarda() {
        when(citasRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.cambiarEstado(99L, 2L));

        verify(citasRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_validaLaTransicionConEstadoCitaService_yGuardaElNuevoEstado() {
        Citas existente = cita(1L, 1L);
        when(citasRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(estadoCitaRepository.findById(1L)).thenReturn(Optional.of(estado(1L, "Programada")));
        when(estadoCitaRepository.findById(2L)).thenReturn(Optional.of(estado(2L, "Cancelada")));
        when(citasRepository.save(existente)).thenReturn(existente);

        CitasDTO r = service.cambiarEstado(1L, 2L);

        verify(estadoCitaService).validarTransicion("Programada", "Cancelada");
        assertThat(r.idEstado()).isEqualTo(2L);
    }

    @Test
    void eliminar_lanzaNoEncontrado_siLaCitaNoExiste_yNoGuarda() {
        when(citasRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminar(99L));

        verify(citasRepository, never()).save(any());
    }

    @Test
    void eliminar_haceBorradoLogico_noBorraLaFila() {
        Citas existente = cita(1L, 1L);
        when(citasRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(consultasRepository.existsByIdCita(1L)).thenReturn(false);

        service.eliminar(1L);

        assertThat(existente.getActivo()).isFalse();
        verify(citasRepository).save(existente);
        verify(citasRepository, never()).deleteById(any());
    }

    @Test
    void agendaMedico_lanzaNoEncontrado_siElMedicoNoExiste() {
        when(medicoRepository.existsById(99L)).thenReturn(false);

        assertThrows(RecursoNoEncontradoException.class, () -> service.agendaMedico(99L, null));
    }

    @Test
    void agendaMedico_sinFecha_consultaLaAgendaDeHoy() {
        when(medicoRepository.existsById(2L)).thenReturn(true);
        when(citasRepository.findByIdMedicoAndFechaAndActivoTrueOrderByHoraAsc(2L, LocalDate.now()))
                .thenReturn(List.of(cita(1L, 1L)));

        assertThat(service.agendaMedico(2L, null)).hasSize(1);
    }

    private void mockCatalogos(Long idEstado, String descripcionEstado) {
        when(pacienteRepository.findById(3L)).thenReturn(Optional.of(new Paciente()));
        when(medicoRepository.findByIdParaReservar(2L)).thenReturn(Optional.of(new Medico()));
        when(estadoCitaRepository.findById(idEstado)).thenReturn(Optional.of(estado(idEstado, descripcionEstado)));
    }

    @Test
    void crear_rechazaUnEstadoInicialDistintoDeProgramadaOConfirmada_con422() {
        mockCatalogos(4L, "Atendida");
        CitasDTO dto = new CitasDTO(null, LocalDate.now().plusDays(1), LocalTime.of(9, 0), null, null, 4L, 2L, 3L, null);

        assertThrows(ReglaNegocioException.class, () -> service.crear(dto));

        verify(citasRepository, never()).save(any());
    }

    @Test
    void crear_aceptaElEstadoInicialConfirmada() {
        mockCatalogos(2L, "Confirmada");
        CitasDTO dto = new CitasDTO(null, LocalDate.now().plusDays(1), LocalTime.of(9, 0), null, null, 2L, 2L, 3L, null);
        when(citasRepository.save(any(Citas.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.crear(dto).idEstado()).isEqualTo(2L);
    }

    @Test
    void crear_rechazaUnaFechaPasada_con422() {
        CitasDTO dto = new CitasDTO(null, LocalDate.now().minusDays(1), LocalTime.of(9, 0), null, null, 1L, 2L, 3L, null);

        assertThrows(ReglaNegocioException.class, () -> service.crear(dto));
    }

    @Test
    void crear_lasCitasCanceladasONoAsistioNoOcupanElHorario() {
        mockCatalogos(1L, "Programada");
        when(estadoCitaRepository.findByDescripcionIgnoreCase("Cancelada")).thenReturn(Optional.of(estado(5L, "Cancelada")));
        when(estadoCitaRepository.findByDescripcionIgnoreCase("No asistió")).thenReturn(Optional.of(estado(6L, "No asistió")));
        LocalDate manana = LocalDate.now().plusDays(1);
        LocalTime hora = LocalTime.of(9, 0);
        // El chequeo de choque excluye las citas en estado Cancelada (5) y No asistió (6)
        when(citasRepository.existsByIdMedicoAndFechaAndHoraAndActivoTrueAndIdEstadoNotIn(eq(2L), eq(manana), eq(hora),
                argThat(ids -> ids.size() == 2 && ids.containsAll(List.of(5L, 6L))))).thenReturn(false);
        when(citasRepository.save(any(Citas.class))).thenAnswer(inv -> inv.getArgument(0));

        service.crear(new CitasDTO(null, manana, hora, null, null, 1L, 2L, 3L, null));

        verify(citasRepository).save(any(Citas.class));
    }

    @Test
    void crear_lanzaDuplicado409_siElMedicoYaTieneCitaEnEsaHora() {
        mockCatalogos(1L, "Programada");
        LocalDate manana = LocalDate.now().plusDays(1);
        when(citasRepository.existsByIdMedicoAndFechaAndHoraAndActivoTrueAndIdEstadoNotIn(eq(2L), eq(manana), eq(LocalTime.of(9, 0)), anyCollection())).thenReturn(true);
        CitasDTO dto = new CitasDTO(null, manana, LocalTime.of(9, 0), null, null, 1L, 2L, 3L, null);

        assertThrows(DuplicadoException.class, () -> service.crear(dto));
    }

    @Test
    void actualizar_permiteEditarElMotivoDeUnaCitaPasada_siNoCambianFechaNiHora() {
        LocalDate ayer = LocalDate.now().minusDays(1);
        Citas existente = cita(1L, 1L);
        existente.setFecha(ayer);
        existente.setHora(LocalTime.of(9, 0));
        when(citasRepository.findById(1L)).thenReturn(Optional.of(existente));
        mockCatalogos(1L, "Programada");
        when(citasRepository.save(existente)).thenReturn(existente);
        CitasDTO dto = new CitasDTO(null, ayer, LocalTime.of(9, 0), "Nuevo motivo", null, 1L, 2L, 3L, null);

        assertThat(service.actualizar(dto, 1L).motivo()).isEqualTo("Nuevo motivo");
    }

    @Test
    void actualizar_rechazaUnaFechaPasada_siSeCambiaLaFecha() {
        Citas existente = cita(1L, 1L);
        existente.setFecha(LocalDate.now().plusDays(2));
        existente.setHora(LocalTime.of(9, 0));
        when(citasRepository.findById(1L)).thenReturn(Optional.of(existente));
        CitasDTO dto = new CitasDTO(null, LocalDate.now().minusDays(1), LocalTime.of(9, 0), null, null, 1L, 2L, 3L, null);

        assertThrows(ReglaNegocioException.class, () -> service.actualizar(dto, 1L));

        verify(citasRepository, never()).save(any());
    }

    @Test
    void buscar_combinaTodosLosFiltros() {
        LocalDate hoy = LocalDate.now();
        Citas a = cita(1L, 1L);
        a.setIdPaciente(3L);
        a.setIdMedico(2L);
        a.setFecha(hoy);
        Citas b = cita(2L, 1L);
        b.setIdPaciente(3L);
        b.setIdMedico(9L);
        b.setFecha(hoy);
        Citas c = cita(3L, 1L);
        c.setIdPaciente(3L);
        c.setIdMedico(2L);
        c.setFecha(hoy.plusDays(1));
        when(citasRepository.findAll()).thenReturn(List.of(a, b, c));

        assertThat(service.buscar(3L, 2L, hoy, false)).extracting(CitasDTO::idCita).containsExactly(1L);
        assertThat(service.buscar(3L, null, null, false)).hasSize(3);
    }
}
