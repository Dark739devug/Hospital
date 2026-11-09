package com.example.hospital.estadoshospitalizacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hospital.estadoshospitalizacion.dto.EstadosHospitalizacionDTO;
import com.example.hospital.estadoshospitalizacion.impl.EstadosHospitalizacionServiceImpl;
import com.example.hospital.hospitalizaciones.HospitalizacionesRepository;
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
class EstadosHospitalizacionServiceImplTest {

    @Mock EstadosHospitalizacionRepository repository;
    @Mock HospitalizacionesRepository hospitalizacionesRepository;
    @InjectMocks EstadosHospitalizacionServiceImpl service;

    private EstadosHospitalizacion estado(Long id, String descripcion) {
        EstadosHospitalizacion e = new EstadosHospitalizacion();
        e.setIdEstadoHosp(id);
        e.setDescripcion(descripcion);
        e.setActivo(true);
        return e;
    }

    @Test
    void crear_valido_guardaActivoPorDefecto() {
        when(repository.existsByDescripcionIgnoreCase("Observacion")).thenReturn(false);
        when(repository.save(any(EstadosHospitalizacion.class))).thenAnswer(i -> i.getArgument(0));

        EstadosHospitalizacionDTO res = service.crear(new EstadosHospitalizacionDTO(null, "Observacion", null));

        assertThat(res.descripcion()).isEqualTo("Observacion");
        assertThat(res.activo()).isTrue();
    }

    @Test
    void crear_conDescripcionDuplicada_lanzaDuplicado409_yNoGuarda() {
        when(repository.existsByDescripcionIgnoreCase("Alta")).thenReturn(true);

        assertThrows(DuplicadoException.class,
                () -> service.crear(new EstadosHospitalizacionDTO(null, "Alta", null)));

        verify(repository, never()).save(any());
    }

    @Test
    void findById_inexistente_lanzaNoEncontrado404() {
        when(repository.findById(8L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.findById(8L));
    }

    @Test
    void actualizar_renombrandoEstadoBase_lanzaReglaNegocio422_yNoGuarda() {
        when(repository.findById(1L)).thenReturn(Optional.of(estado(1L, "Activa")));
        when(repository.existsByDescripcionIgnoreCaseAndIdEstadoHospNot("Otro", 1L)).thenReturn(false);

        assertThrows(ReglaNegocioException.class,
                () -> service.actualizar(new EstadosHospitalizacionDTO(null, "Otro", true), 1L));

        verify(repository, never()).save(any());
    }

    @Test
    void eliminar_estadoBase_lanzaReglaNegocio422() {
        when(repository.findById(2L)).thenReturn(Optional.of(estado(2L, "Alta")));

        assertThrows(ReglaNegocioException.class, () -> service.eliminar(2L));

        verify(repository, never()).save(any());
    }

    @Test
    void eliminar_conHospitalizacionesConEseEstado_lanzaConflicto409_yNoGuarda() {
        when(repository.findById(9L)).thenReturn(Optional.of(estado(9L, "Observacion")));
        when(hospitalizacionesRepository.existsByIdEstadoHosp(9L)).thenReturn(true);

        assertThrows(DuplicadoException.class, () -> service.eliminar(9L));

        verify(repository, never()).save(any());
    }

    @Test
    void eliminar_estadoPropioSinUso_haceBorradoLogico() {
        EstadosHospitalizacion e = estado(9L, "Observacion");
        when(repository.findById(9L)).thenReturn(Optional.of(e));
        when(hospitalizacionesRepository.existsByIdEstadoHosp(9L)).thenReturn(false);

        service.eliminar(9L);

        assertThat(e.getActivo()).isFalse();
        verify(repository).save(e);
    }
}
