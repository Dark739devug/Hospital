package com.example.hospital.diagnosticoconsulta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hospital.consultas.Consultas;
import com.example.hospital.consultas.ConsultasRepository;
import com.example.hospital.diagnosticoconsulta.dto.DiagnosticoConsultaDTO;
import com.example.hospital.diagnosticoconsulta.impl.DiagnosticoConsultaServiceImpl;
import com.example.hospital.diagnosticos.Diagnosticos;
import com.example.hospital.diagnosticos.DiagnosticosRepository;
import com.example.hospital.shared.exception.DuplicadoException;
import com.example.hospital.shared.exception.RecursoNoEncontradoException;
import com.example.hospital.shared.exception.ReglaNegocioException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiagnosticoConsultaServiceImplTest {

    @Mock DiagnosticoConsultaRepository diagnosticoConsultaRepository;
    @Mock DiagnosticosRepository diagnosticosRepository;
    @Mock ConsultasRepository consultasRepository;
    @InjectMocks DiagnosticoConsultaServiceImpl service;

    private Consultas consulta(boolean activo) {
        Consultas c = new Consultas();
        c.setIdConsulta(10L);
        c.setActivo(activo);
        return c;
    }

    private Diagnosticos diagnostico(boolean activo) {
        Diagnosticos d = new Diagnosticos();
        d.setIdDiagnostico(1L);
        d.setActivo(activo);
        return d;
    }

    private final DiagnosticoConsultaDTO dto = new DiagnosticoConsultaDTO(1L, 10L);

    @Test
    void crear_valido_guardaLaAsociacion() {
        when(consultasRepository.findById(10L)).thenReturn(Optional.of(consulta(true)));
        when(diagnosticosRepository.findById(1L)).thenReturn(Optional.of(diagnostico(true)));
        when(diagnosticoConsultaRepository.existsByIdDiagnosticoAndIdConsulta(1L, 10L)).thenReturn(false);
        when(diagnosticoConsultaRepository.save(any(DiagnosticoConsulta.class))).thenAnswer(i -> i.getArgument(0));

        DiagnosticoConsultaDTO res = service.crear(dto);

        assertThat(res).isEqualTo(dto);
        verify(diagnosticoConsultaRepository).save(any(DiagnosticoConsulta.class));
    }

    @Test
    void crear_conConsultaInexistente_lanzaReglaNegocio422_yNoGuarda() {
        when(consultasRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ReglaNegocioException.class, () -> service.crear(dto));

        verify(diagnosticoConsultaRepository, never()).save(any());
    }

    @Test
    void crear_conConsultaInactiva_lanzaReglaNegocio422_yNoGuarda() {
        when(consultasRepository.findById(10L)).thenReturn(Optional.of(consulta(false)));

        assertThrows(ReglaNegocioException.class, () -> service.crear(dto));

        verify(diagnosticoConsultaRepository, never()).save(any());
    }

    @Test
    void crear_conDiagnosticoInactivo_lanzaReglaNegocio422_yNoGuarda() {
        when(consultasRepository.findById(10L)).thenReturn(Optional.of(consulta(true)));
        when(diagnosticosRepository.findById(1L)).thenReturn(Optional.of(diagnostico(false)));

        assertThrows(ReglaNegocioException.class, () -> service.crear(dto));

        verify(diagnosticoConsultaRepository, never()).save(any());
    }

    @Test
    void crear_conAsociacionRepetida_lanzaDuplicado409_yNoGuarda() {
        when(consultasRepository.findById(10L)).thenReturn(Optional.of(consulta(true)));
        when(diagnosticosRepository.findById(1L)).thenReturn(Optional.of(diagnostico(true)));
        when(diagnosticoConsultaRepository.existsByIdDiagnosticoAndIdConsulta(1L, 10L)).thenReturn(true);

        assertThrows(DuplicadoException.class, () -> service.crear(dto));

        verify(diagnosticoConsultaRepository, never()).save(any());
    }

    @Test
    void crear_conIdsNulos_lanzaReglaNegocio422() {
        assertThrows(ReglaNegocioException.class,
                () -> service.crear(new DiagnosticoConsultaDTO(null, 10L)));

        verify(diagnosticoConsultaRepository, never()).save(any());
    }

    @Test
    void findById_inexistente_lanzaNoEncontrado404() {
        when(diagnosticoConsultaRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.findById(1L, 10L));
    }

    @Test
    void eliminar_inexistente_lanzaNoEncontrado404_yNoBorra() {
        when(diagnosticoConsultaRepository.existsById(any())).thenReturn(false);

        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminar(1L, 10L));

        verify(diagnosticoConsultaRepository, never()).deleteById(any());
    }

    @Test
    void eliminar_existente_borraElVinculo() {
        when(diagnosticoConsultaRepository.existsById(any())).thenReturn(true);

        service.eliminar(1L, 10L);

        verify(diagnosticoConsultaRepository).deleteById(any());
    }
}
