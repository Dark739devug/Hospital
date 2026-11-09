package com.example.hospital.consultas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hospital.citas.Citas;
import com.example.hospital.citas.CitasRepository;
import com.example.hospital.consultas.dto.MedicamentoConsultaDTO;
import com.example.hospital.detallefactura.DetalleFactura;
import com.example.hospital.detallefactura.DetalleFacturaRepository;
import com.example.hospital.detallefactura.DetalleFacturaService;
import com.example.hospital.detallefactura.dto.DetalleFacturaDTO;
import com.example.hospital.diagnosticoconsulta.DiagnosticoConsultaRepository;
import com.example.hospital.facturas.Facturas;
import com.example.hospital.facturas.FacturasRepository;
import com.example.hospital.facturas.FacturasService;
import com.example.hospital.facturas.dto.FacturasDTO;
import com.example.hospital.medicamentos.Medicamentos;
import com.example.hospital.medicamentos.MedicamentosRepository;
import com.example.hospital.metodospago.MetodosPago;
import com.example.hospital.metodospago.MetodosPagoRepository;
import com.example.hospital.shared.catalogos.EstadosCatalogo;
import com.example.hospital.shared.exception.ReglaNegocioException;
import com.example.hospital.tratamiento.Tratamiento;
import com.example.hospital.tratamiento.TratamientoRepository;
import com.example.hospital.tratamientomedicamento.TratamientoMedicamentoService;
import com.example.hospital.tratamientomedicamento.dto.TratamientoMedicamentoDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Medicamentos recetados en la consulta y cobrados en la factura de esa consulta. */
@ExtendWith(MockitoExtension.class)
class MedicamentosConsultaServiceTest {

    static final long PENDIENTE = 1L;
    static final long PAGADA = 2L;

    @Mock ConsultasRepository consultasRepository;
    @Mock CitasRepository citasRepository;
    @Mock TratamientoRepository tratamientoRepository;
    @Mock DiagnosticoConsultaRepository diagnosticoConsultaRepository;
    @Mock MedicamentosRepository medicamentosRepository;
    @Mock MetodosPagoRepository metodosPagoRepository;
    @Mock FacturasRepository facturasRepository;
    @Mock DetalleFacturaRepository detalleFacturaRepository;
    @Mock TratamientoMedicamentoService tratamientoMedicamentoService;
    @Mock FacturasService facturasService;
    @Mock DetalleFacturaService detalleFacturaService;
    @Mock EstadosCatalogo estados;
    @InjectMocks MedicamentosConsultaService service;

    private final MedicamentoConsultaDTO pedido = new MedicamentoConsultaDTO(3L, 5L, 2);

    @BeforeEach
    void catalogo() {
        lenient().when(estados.facturaPendiente()).thenReturn(PENDIENTE);
        lenient().when(estados.facturaPagada()).thenReturn(PAGADA);
        lenient().when(estados.facturaCancelada()).thenReturn(3L);
    }

    private Consultas consulta(LocalDateTime fechaHora) {
        Consultas c = new Consultas();
        c.setIdConsulta(10L);
        c.setIdCita(7L);
        c.setFechaHora(fechaHora);
        c.setActivo(true);
        return c;
    }

    private void consultaDeHoyConTratamiento() {
        when(consultasRepository.findById(10L)).thenReturn(Optional.of(consulta(LocalDateTime.now().minusHours(1))));
        Tratamiento t = new Tratamiento();
        t.setIdTratamiento(3L);
        t.setIdDiagnostico(8L);
        when(tratamientoRepository.findById(3L)).thenReturn(Optional.of(t));
        when(diagnosticoConsultaRepository.existsByIdDiagnosticoAndIdConsulta(8L, 10L)).thenReturn(true);
        Medicamentos m = new Medicamentos();
        m.setIdMedicamento(5L);
        m.setNombreComercial("Rocephin");
        m.setPresentacion("Vial 1 g");
        m.setPrecio(new BigDecimal("145.50"));
        when(medicamentosRepository.findById(5L)).thenReturn(Optional.of(m));
    }

    private Facturas factura(long id, long estado) {
        Facturas f = new Facturas();
        f.setIdFactura(id);
        f.setIdEstadoFactura(estado);
        f.setActivo(true);
        f.setIdConsulta(10L);
        return f;
    }

    @Test
    void primeraVez_creaLaFacturaDeLaConsulta_yAgregaLaLineaConElPrecioDelMedicamento() {
        consultaDeHoyConTratamiento();
        when(facturasRepository.findByIdConsultaAndActivoTrue(10L)).thenReturn(List.of());
        Citas cita = new Citas();
        cita.setIdPaciente(4L);
        when(citasRepository.findById(7L)).thenReturn(Optional.of(cita));
        MetodosPago efectivo = new MetodosPago();
        efectivo.setIdMetodoPago(1L);
        when(metodosPagoRepository.findByActivoTrue()).thenReturn(List.of(efectivo));
        when(facturasService.crear(any())).thenAnswer(inv -> {
            FacturasDTO d = inv.getArgument(0);
            return new FacturasDTO(20L, d.idPaciente(), null, null, null, null, null, d.idMetodoPago(), PENDIENTE, true, d.idConsulta());
        });

        service.agregar(10L, pedido);

        verify(tratamientoMedicamentoService).crear(new TratamientoMedicamentoDTO(5L, 3L));
        ArgumentCaptor<FacturasDTO> facturaNueva = ArgumentCaptor.forClass(FacturasDTO.class);
        verify(facturasService).crear(facturaNueva.capture());
        assertThat(facturaNueva.getValue().idPaciente()).isEqualTo(4L);
        assertThat(facturaNueva.getValue().idConsulta()).isEqualTo(10L);
        ArgumentCaptor<DetalleFacturaDTO> linea = ArgumentCaptor.forClass(DetalleFacturaDTO.class);
        verify(detalleFacturaService).crear(linea.capture());
        assertThat(linea.getValue().idFactura()).isEqualTo(20L);
        assertThat(linea.getValue().cantidad()).isEqualTo(2);
        assertThat(linea.getValue().precioUnitario()).isEqualByComparingTo("145.50");
        assertThat(linea.getValue().concepto()).startsWith("Medicamento #5 · tratamiento #3: Rocephin");
    }

    @Test
    void conFacturaPendiente_sumaLaLineaALaMismaFactura() {
        consultaDeHoyConTratamiento();
        when(facturasRepository.findByIdConsultaAndActivoTrue(10L)).thenReturn(List.of(factura(20L, PENDIENTE)));

        service.agregar(10L, pedido);

        verify(facturasService, never()).crear(any());
        ArgumentCaptor<DetalleFacturaDTO> linea = ArgumentCaptor.forClass(DetalleFacturaDTO.class);
        verify(detalleFacturaService).crear(linea.capture());
        assertThat(linea.getValue().idFactura()).isEqualTo(20L);
    }

    @Test
    void facturaYaPagada_rechazaElCargo() {
        consultaDeHoyConTratamiento();
        when(facturasRepository.findByIdConsultaAndActivoTrue(10L)).thenReturn(List.of(factura(20L, PAGADA)));

        assertThrows(ReglaNegocioException.class, () -> service.agregar(10L, pedido));
        verify(detalleFacturaService, never()).crear(any());
    }

    @Test
    void tratamientoDeOtraConsulta_seRechaza_sinAsignarNiFacturar() {
        when(consultasRepository.findById(10L)).thenReturn(Optional.of(consulta(LocalDateTime.now())));
        Tratamiento t = new Tratamiento();
        t.setIdTratamiento(3L);
        t.setIdDiagnostico(99L);
        when(tratamientoRepository.findById(3L)).thenReturn(Optional.of(t));
        when(diagnosticoConsultaRepository.existsByIdDiagnosticoAndIdConsulta(99L, 10L)).thenReturn(false);

        assertThrows(ReglaNegocioException.class, () -> service.agregar(10L, pedido));
        verify(tratamientoMedicamentoService, never()).crear(any());
        verify(detalleFacturaService, never()).crear(any());
    }

    @Test
    void consultaDeHaceDosDias_noAdmiteMedicamentos() {
        when(consultasRepository.findById(10L)).thenReturn(Optional.of(consulta(LocalDateTime.now().minusDays(2))));

        assertThrows(ReglaNegocioException.class, () -> service.agregar(10L, pedido));
        verify(tratamientoMedicamentoService, never()).crear(any());
    }

    @Test
    void quitar_eliminaLaLineaDeLaFactura_yLaAsignacion() {
        when(consultasRepository.findById(10L)).thenReturn(Optional.of(consulta(LocalDateTime.now())));
        when(facturasRepository.findByIdConsultaAndActivoTrue(10L)).thenReturn(List.of(factura(20L, PENDIENTE)));
        DetalleFactura otra = new DetalleFactura();
        otra.setIdDetalle(30L);
        otra.setConcepto("Consulta general");
        DetalleFactura delMedicamento = new DetalleFactura();
        delMedicamento.setIdDetalle(31L);
        delMedicamento.setConcepto("Medicamento #5 · tratamiento #3: Rocephin");
        when(detalleFacturaRepository.findByIdFactura(20L)).thenReturn(List.of(otra, delMedicamento));

        service.quitar(10L, 3L, 5L);

        verify(detalleFacturaService).eliminar(31L);
        verify(detalleFacturaService, never()).eliminar(30L);
        verify(tratamientoMedicamentoService).eliminar(5L, 3L);
    }

    @Test
    void listar_devuelveSoloLasLineasDeMedicamentos_conSusIds() {
        when(consultasRepository.findById(10L)).thenReturn(Optional.of(consulta(LocalDateTime.now())));
        Facturas f = factura(20L, PENDIENTE);
        f.setTotal(new BigDecimal("325.92"));
        when(facturasRepository.findByIdConsultaAndActivoTrue(10L)).thenReturn(List.of(f));
        DetalleFactura otra = new DetalleFactura();
        otra.setIdDetalle(30L);
        otra.setConcepto("Consulta general");
        DetalleFactura linea = new DetalleFactura();
        linea.setIdDetalle(31L);
        linea.setConcepto("Medicamento #5 · tratamiento #3: Rocephin (Vial 1 g)");
        linea.setCantidad(2);
        when(detalleFacturaRepository.findByIdFactura(20L)).thenReturn(List.of(otra, linea));

        var r = service.listar(10L);

        assertThat(r.idFactura()).isEqualTo(20L);
        assertThat(r.estadoFactura()).isEqualTo("Pendiente");
        assertThat(r.medicamentos()).hasSize(1);
        assertThat(r.medicamentos().get(0).idMedicamento()).isEqualTo(5L);
        assertThat(r.medicamentos().get(0).idTratamiento()).isEqualTo(3L);
        assertThat(r.medicamentos().get(0).concepto()).isEqualTo("Rocephin (Vial 1 g)");
    }
}
