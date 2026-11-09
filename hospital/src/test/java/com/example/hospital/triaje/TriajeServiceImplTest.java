package com.example.hospital.triaje;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hospital.emergencia.EmergenciaRepository;
import com.example.hospital.shared.exception.DuplicadoException;
import com.example.hospital.shared.exception.RecursoNoEncontradoException;
import com.example.hospital.shared.exception.ReglaNegocioException;
import com.example.hospital.triaje.dto.TriajeDTO;
import com.example.hospital.triaje.impl.TriajeServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TriajeServiceImplTest {

    @Mock TriajeRepository triajeRepository;
    @Mock EmergenciaRepository emergenciaRepository;
    @InjectMocks TriajeServiceImpl service;

    private Triaje triaje(Long id) {
        Triaje t = new Triaje();
        t.setIdTriaje(id);
        t.setNivelDeEmergencia("N1");
        t.setActivo(true);
        return t;
    }

    @Test
    void eliminar_conEmergenciasActivasQueLoUsan_lanzaConflicto409_yNoGuarda() {
        when(triajeRepository.findById(1L)).thenReturn(Optional.of(triaje(1L)));
        when(emergenciaRepository.existsByIdTriajeAndActivoTrue(1L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.eliminar(1L));

        verify(triajeRepository, never()).save(any());
    }

    @Test
    void eliminar_sinEmergenciasActivas_haceBorradoLogico() {
        Triaje t = triaje(1L);
        when(triajeRepository.findById(1L)).thenReturn(Optional.of(t));
        when(emergenciaRepository.existsByIdTriajeAndActivoTrue(1L)).thenReturn(false);

        service.eliminar(1L);

        assertThat(t.getActivo()).isFalse();
        verify(triajeRepository).save(t);
    }

    @Test
    void crear_conNivelDuplicado_lanzaDuplicado409() {
        when(triajeRepository.existsByNivelDeEmergenciaIgnoreCase("n1")).thenReturn(true);

        assertThrows(DuplicadoException.class, () -> service.crear(new TriajeDTO(null, "n1", null, null)));
    }

    @Test
    void crear_valido_recortaNivelYNaceActivo() {
        when(triajeRepository.existsByNivelDeEmergenciaIgnoreCase("N2")).thenReturn(false);
        when(triajeRepository.save(any(Triaje.class))).thenAnswer(i -> i.getArgument(0));

        TriajeDTO res = service.crear(new TriajeDTO(null, "  N2  ", "  Urgente ", null));

        assertThat(res.nivelDeEmergencia()).isEqualTo("N2");
        assertThat(res.descripcion()).isEqualTo("Urgente");
        assertThat(res.activo()).isTrue();
    }

    @Test
    void crear_conNivelDuplicado_noGuarda() {
        when(triajeRepository.existsByNivelDeEmergenciaIgnoreCase("N1")).thenReturn(true);

        assertThrows(DuplicadoException.class, () -> service.crear(new TriajeDTO(null, "N1", null, null)));

        verify(triajeRepository, never()).save(any());
    }

    @Test
    void findById_inexistente_lanzaNoEncontrado404() {
        when(triajeRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.findById(9L));
    }

    @Test
    void actualizar_inexistente_lanzaNoEncontrado404_yNoGuarda() {
        when(triajeRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.actualizar(new TriajeDTO(null, "N3", null, null), 9L));

        verify(triajeRepository, never()).save(any());
    }

    @Test
    void actualizar_conNivelDeOtroTriaje_lanzaDuplicado409_yNoGuarda() {
        when(triajeRepository.findById(1L)).thenReturn(Optional.of(triaje(1L)));
        when(triajeRepository.existsByNivelDeEmergenciaIgnoreCaseAndIdTriajeNot("N5", 1L)).thenReturn(true);

        assertThrows(DuplicadoException.class,
                () -> service.actualizar(new TriajeDTO(null, "N5", null, null), 1L));

        verify(triajeRepository, never()).save(any());
    }

    @Test
    void eliminar_conIdNulo_lanzaReglaNegocio422() {
        assertThrows(ReglaNegocioException.class, () -> service.eliminar(null));
    }
}
