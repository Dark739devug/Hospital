package com.example.hospital.tiposdiagnostico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hospital.diagnosticos.DiagnosticosRepository;
import com.example.hospital.shared.exception.DuplicadoException;
import com.example.hospital.shared.exception.RecursoNoEncontradoException;
import com.example.hospital.shared.exception.ReglaNegocioException;
import com.example.hospital.tiposdiagnostico.dto.TiposDiagnosticoDTO;
import com.example.hospital.tiposdiagnostico.impl.TiposDiagnosticoServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TiposDiagnosticoServiceImplTest {

    @Mock TiposDiagnosticoRepository tiposDiagnosticoRepository;
    @Mock DiagnosticosRepository diagnosticosRepository;
    @InjectMocks TiposDiagnosticoServiceImpl service;

    private TiposDiagnostico tipo(Long id, String descripcion) {
        TiposDiagnostico t = new TiposDiagnostico();
        t.setIdTipoDiagnostico(id);
        t.setDescripcion(descripcion);
        t.setActivo(true);
        return t;
    }

    @Test
    void crear_conNombreBaseEnMinusculas_seNormalizaAlCanonico() {
        when(tiposDiagnosticoRepository.existsByDescripcionIgnoreCase("Definitivo")).thenReturn(false);
        when(tiposDiagnosticoRepository.save(any(TiposDiagnostico.class))).thenAnswer(i -> i.getArgument(0));

        TiposDiagnosticoDTO res = service.crear(new TiposDiagnosticoDTO(null, "definitivo", null));

        assertThat(res.descripcion()).isEqualTo("Definitivo");
        assertThat(res.activo()).isTrue();
    }

    @Test
    void crear_conDescripcionDuplicada_lanzaDuplicado409_yNoGuarda() {
        when(tiposDiagnosticoRepository.existsByDescripcionIgnoreCase("Otro")).thenReturn(true);

        assertThrows(DuplicadoException.class,
                () -> service.crear(new TiposDiagnosticoDTO(null, "Otro", null)));

        verify(tiposDiagnosticoRepository, never()).save(any());
    }

    @Test
    void findById_inexistente_lanzaNoEncontrado404() {
        when(tiposDiagnosticoRepository.findById(4L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.findById(4L));
    }

    @Test
    void actualizar_renombrandoUnTipoBase_lanzaReglaNegocio422_yNoGuarda() {
        when(tiposDiagnosticoRepository.findById(1L)).thenReturn(Optional.of(tipo(1L, "Presuntivo")));

        assertThrows(ReglaNegocioException.class,
                () -> service.actualizar(new TiposDiagnosticoDTO(null, "Nuevo nombre", null), 1L));

        verify(tiposDiagnosticoRepository, never()).save(any());
    }

    @Test
    void eliminar_tipoBase_lanzaReglaNegocio422_yNoGuarda() {
        when(tiposDiagnosticoRepository.findById(1L)).thenReturn(Optional.of(tipo(1L, "Presuntivo")));

        assertThrows(ReglaNegocioException.class, () -> service.eliminar(1L));

        verify(tiposDiagnosticoRepository, never()).save(any());
    }

    @Test
    void eliminar_tipoAsignadoADiagnosticos_lanzaConflicto409_yNoGuarda() {
        when(tiposDiagnosticoRepository.findById(7L)).thenReturn(Optional.of(tipo(7L, "Clinico")));
        when(diagnosticosRepository.existsByIdTipoDiagnostico(7L)).thenReturn(true);

        assertThrows(DuplicadoException.class, () -> service.eliminar(7L));

        verify(tiposDiagnosticoRepository, never()).save(any());
    }

    @Test
    void eliminar_tipoPropioSinUso_haceBorradoLogico() {
        TiposDiagnostico t = tipo(7L, "Clinico");
        when(tiposDiagnosticoRepository.findById(7L)).thenReturn(Optional.of(t));
        when(diagnosticosRepository.existsByIdTipoDiagnostico(7L)).thenReturn(false);

        service.eliminar(7L);

        assertThat(t.getActivo()).isFalse();
        verify(tiposDiagnosticoRepository).save(t);
    }
}
