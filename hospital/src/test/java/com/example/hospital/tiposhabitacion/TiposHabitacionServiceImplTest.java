package com.example.hospital.tiposhabitacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hospital.habitacion.HabitacionRepository;
import com.example.hospital.shared.exception.DuplicadoException;
import com.example.hospital.shared.exception.RecursoNoEncontradoException;
import com.example.hospital.shared.exception.ReglaNegocioException;
import com.example.hospital.tiposhabitacion.dto.TiposHabitacionDTO;
import com.example.hospital.tiposhabitacion.impl.TiposHabitacionServiceImpl;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TiposHabitacionServiceImplTest {

    @Mock TiposHabitacionRepository tiposHabitacionRepository;
    @Mock HabitacionRepository habitacionRepository;
    @InjectMocks TiposHabitacionServiceImpl service;

    private TiposHabitacion tipo(Long id, String descripcion) {
        TiposHabitacion t = new TiposHabitacion();
        t.setIdTipoHabitacion(id);
        t.setDescripcion(descripcion);
        t.setCostoDia(new BigDecimal("100.00"));
        t.setActivo(true);
        return t;
    }

    @Test
    void crear_valido_guardaActivoYNormalizaNombreBase() {
        when(tiposHabitacionRepository.existsByDescripcionIgnoreCase("UCI")).thenReturn(false);
        when(tiposHabitacionRepository.save(any(TiposHabitacion.class))).thenAnswer(i -> i.getArgument(0));

        TiposHabitacionDTO res = service.crear(new TiposHabitacionDTO(null, "uci", new BigDecimal("350.00"), null));

        assertThat(res.descripcion()).isEqualTo("UCI");
        assertThat(res.activo()).isTrue();
    }

    @Test
    void crear_conCostoCero_lanzaReglaNegocio422_yNoGuarda() {
        assertThrows(ReglaNegocioException.class,
                () -> service.crear(new TiposHabitacionDTO(null, "Suite", BigDecimal.ZERO, null)));

        verify(tiposHabitacionRepository, never()).save(any());
    }

    @Test
    void crear_conDescripcionDuplicada_lanzaDuplicado409_yNoGuarda() {
        when(tiposHabitacionRepository.existsByDescripcionIgnoreCase("Suite")).thenReturn(true);

        assertThrows(DuplicadoException.class,
                () -> service.crear(new TiposHabitacionDTO(null, "Suite", new BigDecimal("500"), null)));

        verify(tiposHabitacionRepository, never()).save(any());
    }

    @Test
    void findById_inexistente_lanzaNoEncontrado404() {
        when(tiposHabitacionRepository.findById(3L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.findById(3L));
    }

    @Test
    void actualizar_renombrandoTipoBase_lanzaReglaNegocio422_yNoGuarda() {
        when(tiposHabitacionRepository.findById(1L)).thenReturn(Optional.of(tipo(1L, "Individual")));

        assertThrows(ReglaNegocioException.class,
                () -> service.actualizar(new TiposHabitacionDTO(null, "Otra", new BigDecimal("90"), null), 1L));

        verify(tiposHabitacionRepository, never()).save(any());
    }

    @Test
    void eliminar_tipoBase_lanzaReglaNegocio422() {
        when(tiposHabitacionRepository.findById(1L)).thenReturn(Optional.of(tipo(1L, "Doble")));

        assertThrows(ReglaNegocioException.class, () -> service.eliminar(1L));

        verify(tiposHabitacionRepository, never()).save(any());
    }

    @Test
    void eliminar_conHabitacionesAsociadas_lanzaConflicto409_yNoGuarda() {
        when(tiposHabitacionRepository.findById(9L)).thenReturn(Optional.of(tipo(9L, "Suite")));
        when(habitacionRepository.existsByIdTipoHabitacion(9L)).thenReturn(true);

        assertThrows(DuplicadoException.class, () -> service.eliminar(9L));

        verify(tiposHabitacionRepository, never()).save(any());
    }

    @Test
    void eliminar_tipoPropioSinHabitaciones_haceBorradoLogico() {
        TiposHabitacion t = tipo(9L, "Suite");
        when(tiposHabitacionRepository.findById(9L)).thenReturn(Optional.of(t));
        when(habitacionRepository.existsByIdTipoHabitacion(9L)).thenReturn(false);

        service.eliminar(9L);

        assertThat(t.getActivo()).isFalse();
        verify(tiposHabitacionRepository).save(t);
    }
}
