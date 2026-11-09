package com.example.hospital.diagnosticos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hospital.diagnosticoconsulta.DiagnosticoConsulta;
import com.example.hospital.diagnosticoconsulta.DiagnosticoConsultaRepository;
import com.example.hospital.diagnosticos.dto.DiagnosticosDTO;
import com.example.hospital.diagnosticos.impl.DiagnosticosServiceImpl;
import com.example.hospital.shared.exception.RecursoNoEncontradoException;
import com.example.hospital.shared.exception.ReglaNegocioException;
import com.example.hospital.tiposdiagnostico.TiposDiagnostico;
import com.example.hospital.tiposdiagnostico.TiposDiagnosticoRepository;
import com.example.hospital.tratamiento.TratamientoRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiagnosticosServiceImplTest {

    @Mock DiagnosticosRepository diagnosticosRepository;
    @Mock TiposDiagnosticoRepository tiposDiagnosticoRepository;
    @Mock DiagnosticoConsultaRepository diagnosticoConsultaRepository;
    @Mock TratamientoRepository tratamientoRepository;
    @InjectMocks DiagnosticosServiceImpl service;

    private TiposDiagnostico tipo(Long id, boolean activo) {
        TiposDiagnostico t = new TiposDiagnostico();
        t.setIdTipoDiagnostico(id);
        t.setDescripcion("Definitivo");
        t.setActivo(activo);
        return t;
    }

    private Diagnosticos diagnostico(Long id) {
        Diagnosticos d = new Diagnosticos();
        d.setIdDiagnostico(id);
        d.setIdTipoDiagnostico(1L);
        d.setDescripcion("Gripe");
        d.setActivo(true);
        return d;
    }

    private DiagnosticosDTO dto(Long idTipo) {
        return new DiagnosticosDTO(null, " obs ", idTipo, "  Gripe estacional  ", null, null);
    }

    @Test
    void crear_conTipoActivo_guardaConDefectosYRecortaTextos() {
        when(tiposDiagnosticoRepository.findById(1L)).thenReturn(Optional.of(tipo(1L, true)));
        when(diagnosticosRepository.save(any(Diagnosticos.class))).thenAnswer(i -> i.getArgument(0));

        DiagnosticosDTO res = service.crear(dto(1L));

        ArgumentCaptor<Diagnosticos> cap = ArgumentCaptor.forClass(Diagnosticos.class);
        verify(diagnosticosRepository).save(cap.capture());
        assertThat(cap.getValue().getActivo()).isTrue();
        assertThat(cap.getValue().getFecha()).isEqualTo(LocalDate.now());
        assertThat(cap.getValue().getDescripcion()).isEqualTo("Gripe estacional");
        assertThat(res.observaciones()).isEqualTo("obs");
    }

    @Test
    void crear_conTipoInexistente_lanzaReglaNegocio422_yNoGuarda() {
        when(tiposDiagnosticoRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ReglaNegocioException.class, () -> service.crear(dto(9L)));

        verify(diagnosticosRepository, never()).save(any());
    }

    @Test
    void crear_conTipoInactivo_lanzaReglaNegocio422_yNoGuarda() {
        when(tiposDiagnosticoRepository.findById(2L)).thenReturn(Optional.of(tipo(2L, false)));

        assertThrows(ReglaNegocioException.class, () -> service.crear(dto(2L)));

        verify(diagnosticosRepository, never()).save(any());
    }

    @Test
    void findById_inexistente_lanzaNoEncontrado404() {
        when(diagnosticosRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.findById(5L));
    }

    @Test
    void actualizar_inexistente_lanzaNoEncontrado404_yNoGuarda() {
        when(diagnosticosRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.actualizar(dto(1L), 5L));

        verify(diagnosticosRepository, never()).save(any());
    }

    @Test
    void actualizar_existente_modificaCamposYConservaFechaSiNoSeEnvia() {
        Diagnosticos existente = diagnostico(3L);
        existente.setFecha(LocalDate.of(2025, 1, 10));
        when(diagnosticosRepository.findById(3L)).thenReturn(Optional.of(existente));
        when(tiposDiagnosticoRepository.findById(1L)).thenReturn(Optional.of(tipo(1L, true)));
        when(diagnosticosRepository.save(any(Diagnosticos.class))).thenAnswer(i -> i.getArgument(0));

        DiagnosticosDTO res = service.actualizar(dto(1L), 3L);

        assertThat(res.descripcion()).isEqualTo("Gripe estacional");
        assertThat(res.fecha()).isEqualTo(LocalDate.of(2025, 1, 10));
    }

    @Test
    void eliminar_vinculadoAConsulta_lanzaConflicto409_yNoGuarda() {
        when(diagnosticosRepository.findById(1L)).thenReturn(Optional.of(diagnostico(1L)));
        when(diagnosticoConsultaRepository.findByIdDiagnostico(1L))
                .thenReturn(List.of(new DiagnosticoConsulta(1L, 10L)));

        assertThrows(IllegalStateException.class, () -> service.eliminar(1L));

        verify(diagnosticosRepository, never()).save(any());
    }

    @Test
    void eliminar_conTratamientos_lanzaConflicto409_yNoGuarda() {
        when(diagnosticosRepository.findById(1L)).thenReturn(Optional.of(diagnostico(1L)));
        when(diagnosticoConsultaRepository.findByIdDiagnostico(1L)).thenReturn(List.of());
        when(tratamientoRepository.existsByIdDiagnostico(1L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.eliminar(1L));

        verify(diagnosticosRepository, never()).save(any());
    }

    @Test
    void eliminar_sinDependencias_haceBorradoLogico() {
        Diagnosticos d = diagnostico(1L);
        when(diagnosticosRepository.findById(1L)).thenReturn(Optional.of(d));
        when(diagnosticoConsultaRepository.findByIdDiagnostico(1L)).thenReturn(List.of());
        when(tratamientoRepository.existsByIdDiagnostico(1L)).thenReturn(false);

        service.eliminar(1L);

        assertThat(d.getActivo()).isFalse();
        verify(diagnosticosRepository).save(d);
    }

    @Test
    void buscar_filtraPorTipoYSoloActivos() {
        Diagnosticos a = diagnostico(1L);
        Diagnosticos b = diagnostico(2L);
        b.setIdTipoDiagnostico(2L);
        Diagnosticos c = diagnostico(3L);
        c.setActivo(false);
        when(diagnosticosRepository.findAll()).thenReturn(List.of(a, b, c));

        List<DiagnosticosDTO> res = service.buscar(1L, null, true);

        assertThat(res).extracting(DiagnosticosDTO::idDiagnostico).containsExactly(1L);
    }
}
