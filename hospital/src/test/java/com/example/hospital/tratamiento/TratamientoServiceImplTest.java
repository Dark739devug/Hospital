package com.example.hospital.tratamiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hospital.diagnosticos.Diagnosticos;
import com.example.hospital.diagnosticos.DiagnosticosRepository;
import com.example.hospital.shared.exception.RecursoNoEncontradoException;
import com.example.hospital.shared.exception.ReglaNegocioException;
import com.example.hospital.tratamiento.dto.TratamientoDTO;
import com.example.hospital.tratamiento.impl.TratamientoServiceImpl;
import com.example.hospital.tratamientomedicamento.TratamientoMedicamentoRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TratamientoServiceImplTest {

    @Mock TratamientoRepository tratamientoRepository;
    @Mock DiagnosticosRepository diagnosticosRepository;
    @Mock TratamientoMedicamentoRepository tratamientoMedicamentoRepository;
    @InjectMocks TratamientoServiceImpl service;

    private Diagnosticos diagnostico(boolean activo) {
        Diagnosticos d = new Diagnosticos();
        d.setIdDiagnostico(1L);
        d.setActivo(activo);
        return d;
    }

    private Tratamiento tratamiento(Long id) {
        Tratamiento t = new Tratamiento();
        t.setIdTratamiento(id);
        t.setDescripcion("Reposo");
        t.setDuracionDias(5);
        t.setIdDiagnostico(1L);
        t.setActivo(true);
        return t;
    }

    private TratamientoDTO dto(LocalDate inicio, LocalDate fin) {
        return new TratamientoDTO(null, "  Reposo  ", 5, "  beber agua ", inicio, fin, 1L, null);
    }

    @Test
    void crear_valido_guardaActivoConFechaInicioPorDefecto() {
        when(diagnosticosRepository.findById(1L)).thenReturn(Optional.of(diagnostico(true)));
        when(tratamientoRepository.save(any(Tratamiento.class))).thenAnswer(i -> i.getArgument(0));

        TratamientoDTO res = service.crear(dto(null, null));

        assertThat(res.descripcion()).isEqualTo("Reposo");
        assertThat(res.indicaciones()).isEqualTo("beber agua");
        assertThat(res.fechaInicio()).isEqualTo(LocalDate.now());
        assertThat(res.activo()).isTrue();
    }

    @Test
    void crear_conDiagnosticoInexistente_lanzaNoEncontrado_yNoGuarda() {
        when(diagnosticosRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.crear(dto(null, null)));

        verify(tratamientoRepository, never()).save(any());
    }

    @Test
    void crear_conDiagnosticoInactivo_lanzaReglaNegocio422_yNoGuarda() {
        when(diagnosticosRepository.findById(1L)).thenReturn(Optional.of(diagnostico(false)));

        assertThrows(ReglaNegocioException.class, () -> service.crear(dto(null, null)));

        verify(tratamientoRepository, never()).save(any());
    }

    @Test
    void crear_conFechaFinAnteriorAInicio_lanzaReglaNegocio422_yNoGuarda() {
        when(diagnosticosRepository.findById(1L)).thenReturn(Optional.of(diagnostico(true)));

        assertThrows(ReglaNegocioException.class,
                () -> service.crear(dto(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 1))));

        verify(tratamientoRepository, never()).save(any());
    }

    @Test
    void findById_inexistente_lanzaNoEncontrado404() {
        when(tratamientoRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.findById(7L));
    }

    @Test
    void actualizar_inexistente_lanzaNoEncontrado404_yNoGuarda() {
        when(tratamientoRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.actualizar(dto(null, null), 7L));

        verify(tratamientoRepository, never()).save(any());
    }

    @Test
    void actualizar_valido_modificaLosCampos() {
        Tratamiento existente = tratamiento(2L);
        when(tratamientoRepository.findById(2L)).thenReturn(Optional.of(existente));
        when(diagnosticosRepository.findById(1L)).thenReturn(Optional.of(diagnostico(true)));
        when(tratamientoRepository.save(any(Tratamiento.class))).thenAnswer(i -> i.getArgument(0));

        TratamientoDTO res = service.actualizar(dto(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 6)), 2L);

        assertThat(res.fechaFin()).isEqualTo(LocalDate.of(2026, 1, 6));
        assertThat(res.descripcion()).isEqualTo("Reposo");
    }

    @Test
    void eliminar_conMedicamentosAsociados_lanzaReglaNegocio422_yNoGuarda() {
        when(tratamientoRepository.findById(2L)).thenReturn(Optional.of(tratamiento(2L)));
        when(tratamientoMedicamentoRepository.existsByIdTratamiento(2L)).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> service.eliminar(2L));

        verify(tratamientoRepository, never()).save(any());
    }

    @Test
    void eliminar_sinMedicamentos_haceBorradoLogico() {
        Tratamiento t = tratamiento(2L);
        when(tratamientoRepository.findById(2L)).thenReturn(Optional.of(t));
        when(tratamientoMedicamentoRepository.existsByIdTratamiento(2L)).thenReturn(false);

        service.eliminar(2L);

        assertThat(t.getActivo()).isFalse();
        verify(tratamientoRepository).save(t);
    }

    @Test
    void buscar_filtraPorDiagnosticoYSoloActivos() {
        Tratamiento a = tratamiento(1L);
        Tratamiento b = tratamiento(2L);
        b.setActivo(false);
        Tratamiento c = tratamiento(3L);
        c.setIdDiagnostico(8L);
        when(tratamientoRepository.findAll()).thenReturn(List.of(a, b, c));

        List<TratamientoDTO> res = service.buscar(1L, true);

        assertThat(res).extracting(TratamientoDTO::idTratamiento).containsExactly(1L);
    }
}
