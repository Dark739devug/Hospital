package com.example.hospital.estadosfactura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hospital.estadosfactura.dto.EstadosFacturaDTO;
import com.example.hospital.estadosfactura.impl.EstadosFacturaServiceImpl;
import com.example.hospital.facturas.FacturasRepository;
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
class EstadosFacturaServiceImplTest {

    @Mock EstadosFacturaRepository repository;
    @Mock FacturasRepository facturasRepository;
    @InjectMocks EstadosFacturaServiceImpl service;

    private EstadosFactura estado(Long id, String descripcion) {
        EstadosFactura e = new EstadosFactura();
        e.setIdEstadoFactura(id);
        e.setDescripcion(descripcion);
        e.setActivo(true);
        return e;
    }

    @Test
    void crear_valido_guardaActivoPorDefecto() {
        when(repository.existsByDescripcionIgnoreCase("Anulada")).thenReturn(false);
        when(repository.save(any(EstadosFactura.class))).thenAnswer(i -> i.getArgument(0));

        EstadosFacturaDTO res = service.crear(new EstadosFacturaDTO(null, " Anulada ", null));

        assertThat(res.descripcion()).isEqualTo("Anulada");
        assertThat(res.activo()).isTrue();
    }

    @Test
    void crear_conDescripcionDuplicada_lanzaDuplicado409_yNoGuarda() {
        when(repository.existsByDescripcionIgnoreCase("Pagada")).thenReturn(true);

        assertThrows(DuplicadoException.class, () -> service.crear(new EstadosFacturaDTO(null, "Pagada", null)));

        verify(repository, never()).save(any());
    }

    @Test
    void findById_inexistente_lanzaNoEncontrado404() {
        when(repository.findById(8L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.findById(8L));
    }

    @Test
    void actualizar_renombrandoEstadoBase_lanzaReglaNegocio422_yNoGuarda() {
        when(repository.findById(1L)).thenReturn(Optional.of(estado(1L, "Pendiente")));
        when(repository.existsByDescripcionIgnoreCaseAndIdEstadoFacturaNot("Otro", 1L)).thenReturn(false);

        assertThrows(ReglaNegocioException.class,
                () -> service.actualizar(new EstadosFacturaDTO(null, "Otro", true), 1L));

        verify(repository, never()).save(any());
    }

    @Test
    void actualizar_desactivandoEstadoBase_lanzaReglaNegocio422() {
        when(repository.findById(2L)).thenReturn(Optional.of(estado(2L, "Pagada")));
        when(repository.existsByDescripcionIgnoreCaseAndIdEstadoFacturaNot("Pagada", 2L)).thenReturn(false);

        assertThrows(ReglaNegocioException.class,
                () -> service.actualizar(new EstadosFacturaDTO(null, "Pagada", false), 2L));

        verify(repository, never()).save(any());
    }

    @Test
    void eliminar_estadoBase_lanzaReglaNegocio422() {
        when(repository.findById(3L)).thenReturn(Optional.of(estado(3L, "Cancelada")));

        assertThrows(ReglaNegocioException.class, () -> service.eliminar(3L));

        verify(repository, never()).save(any());
    }

    @Test
    void eliminar_conFacturasConEseEstado_lanzaReglaNegocio422_yNoGuarda() {
        when(repository.findById(9L)).thenReturn(Optional.of(estado(9L, "Anulada")));
        when(facturasRepository.existsByIdEstadoFactura(9L)).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> service.eliminar(9L));

        verify(repository, never()).save(any());
    }

    @Test
    void eliminar_estadoPropioSinFacturas_haceBorradoLogico() {
        EstadosFactura e = estado(9L, "Anulada");
        when(repository.findById(9L)).thenReturn(Optional.of(e));
        when(facturasRepository.existsByIdEstadoFactura(9L)).thenReturn(false);

        service.eliminar(9L);

        assertThat(e.getActivo()).isFalse();
        verify(repository).save(e);
    }
}
