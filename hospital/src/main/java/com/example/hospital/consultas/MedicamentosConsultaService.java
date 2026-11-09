package com.example.hospital.consultas;

import com.example.hospital.citas.Citas;
import com.example.hospital.citas.CitasRepository;
import com.example.hospital.consultas.dto.MedicamentoConsultaDTO;
import com.example.hospital.consultas.dto.MedicamentosConsultaDTO;
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
import com.example.hospital.shared.exception.RecursoNoEncontradoException;
import com.example.hospital.shared.exception.ReglaNegocioException;
import com.example.hospital.tratamiento.Tratamiento;
import com.example.hospital.tratamiento.TratamientoRepository;
import com.example.hospital.tratamientomedicamento.TratamientoMedicamentoService;
import com.example.hospital.tratamientomedicamento.dto.TratamientoMedicamentoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Medicamentos recetados dentro de una consulta: se asignan al tratamiento y se cobran en la factura de esa consulta.
 * La factura se crea Pendiente la primera vez; cada medicamento es una línea (cantidad x precio del medicamento) y
 * el servidor recalcula subtotal, IVA y total. Solo en consultas editables (24 horas) y con la factura Pendiente.
 */
@Service
public class MedicamentosConsultaService {

    private static final Logger log = LoggerFactory.getLogger(MedicamentosConsultaService.class);

    // La línea de factura identifica medicamento y tratamiento al inicio del concepto (no cambia aunque se renombre el medicamento)
    private static final Pattern CONCEPTO = Pattern.compile("^Medicamento #(\\d+) · tratamiento #(\\d+): ");
    private static final int LARGO_CONCEPTO = 200;

    private final ConsultasRepository consultasRepository;
    private final CitasRepository citasRepository;
    private final TratamientoRepository tratamientoRepository;
    private final DiagnosticoConsultaRepository diagnosticoConsultaRepository;
    private final MedicamentosRepository medicamentosRepository;
    private final MetodosPagoRepository metodosPagoRepository;
    private final FacturasRepository facturasRepository;
    private final DetalleFacturaRepository detalleFacturaRepository;
    private final TratamientoMedicamentoService tratamientoMedicamentoService;
    private final FacturasService facturasService;
    private final DetalleFacturaService detalleFacturaService;
    private final EstadosCatalogo estados;

    public MedicamentosConsultaService(ConsultasRepository consultasRepository,
                                       CitasRepository citasRepository,
                                       TratamientoRepository tratamientoRepository,
                                       DiagnosticoConsultaRepository diagnosticoConsultaRepository,
                                       MedicamentosRepository medicamentosRepository,
                                       MetodosPagoRepository metodosPagoRepository,
                                       FacturasRepository facturasRepository,
                                       DetalleFacturaRepository detalleFacturaRepository,
                                       TratamientoMedicamentoService tratamientoMedicamentoService,
                                       FacturasService facturasService,
                                       DetalleFacturaService detalleFacturaService,
                                       EstadosCatalogo estados) {
        this.consultasRepository = consultasRepository;
        this.citasRepository = citasRepository;
        this.tratamientoRepository = tratamientoRepository;
        this.diagnosticoConsultaRepository = diagnosticoConsultaRepository;
        this.medicamentosRepository = medicamentosRepository;
        this.metodosPagoRepository = metodosPagoRepository;
        this.facturasRepository = facturasRepository;
        this.detalleFacturaRepository = detalleFacturaRepository;
        this.tratamientoMedicamentoService = tratamientoMedicamentoService;
        this.facturasService = facturasService;
        this.detalleFacturaService = detalleFacturaService;
        this.estados = estados;
    }

    @Transactional(readOnly = true)
    public MedicamentosConsultaDTO listar(Long idConsulta) {
        buscarConsulta(idConsulta);
        Optional<Facturas> factura = facturaDeConsulta(idConsulta);
        if (factura.isEmpty()) {
            return new MedicamentosConsultaDTO(null, null, BigDecimal.ZERO, BigDecimal.ZERO, List.of());
        }
        Facturas f = factura.get();
        List<MedicamentosConsultaDTO.Item> items = detalleFacturaRepository.findByIdFactura(f.getIdFactura()).stream()
                .map(this::aItem)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(MedicamentosConsultaDTO.Item::idDetalle))
                .toList();
        return new MedicamentosConsultaDTO(f.getIdFactura(), nombreEstado(f.getIdEstadoFactura()), f.getSubtotal(), f.getTotal(), items);
    }

    @Transactional
    public MedicamentosConsultaDTO agregar(Long idConsulta, MedicamentoConsultaDTO dto) {
        Consultas consulta = consultaEditable(idConsulta);
        Tratamiento tratamiento = tratamientoRepository.findById(dto.idTratamiento())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el tratamiento con ID: " + dto.idTratamiento()));
        if (!diagnosticoConsultaRepository.existsByIdDiagnosticoAndIdConsulta(tratamiento.getIdDiagnostico(), idConsulta)) {
            throw new ReglaNegocioException("El tratamiento no pertenece a un diagnóstico de esta consulta");
        }
        Medicamentos medicamento = medicamentosRepository.findById(dto.idMedicamento())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el medicamento con ID: " + dto.idMedicamento()));
        if (medicamento.getPrecio() == null) {
            throw new ReglaNegocioException("El medicamento no tiene precio registrado");
        }

        // Valida que el tratamiento y el medicamento estén activos y que no esté ya asignado (409)
        tratamientoMedicamentoService.crear(new TratamientoMedicamentoDTO(dto.idMedicamento(), dto.idTratamiento()));

        Long idFactura = facturaPendienteOCrear(consulta);
        detalleFacturaService.crear(new DetalleFacturaDTO(null, idFactura, concepto(medicamento, dto.idTratamiento()),
                dto.cantidad(), medicamento.getPrecio(), null));
        log.info("Consulta {}: medicamento {} x{} agregado al tratamiento {} y a la factura {}",
                idConsulta, medicamento.getIdMedicamento(), dto.cantidad(), dto.idTratamiento(), idFactura);
        return listar(idConsulta);
    }

    @Transactional
    public MedicamentosConsultaDTO quitar(Long idConsulta, Long idTratamiento, Long idMedicamento) {
        consultaEditable(idConsulta);
        Optional<Facturas> factura = facturaDeConsulta(idConsulta);
        if (factura.isPresent()) {
            Optional<DetalleFactura> linea = detalleFacturaRepository.findByIdFactura(factura.get().getIdFactura()).stream()
                    .filter(d -> aItem(d).filter(i -> Objects.equals(i.idTratamiento(), idTratamiento)
                            && Objects.equals(i.idMedicamento(), idMedicamento)).isPresent())
                    .findFirst();
            // Si la factura ya no está Pendiente, DetalleFacturaService rechaza el cambio (422)
            linea.ifPresent(d -> detalleFacturaService.eliminar(d.getIdDetalle()));
        }
        tratamientoMedicamentoService.eliminar(idMedicamento, idTratamiento);
        log.info("Consulta {}: medicamento {} quitado del tratamiento {}", idConsulta, idMedicamento, idTratamiento);
        return listar(idConsulta);
    }

    private Consultas buscarConsulta(Long idConsulta) {
        return consultasRepository.findById(idConsulta)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la consulta médica con ID: " + idConsulta));
    }

    private Consultas consultaEditable(Long idConsulta) {
        Consultas consulta = buscarConsulta(idConsulta);
        if (Boolean.FALSE.equals(consulta.getActivo())) {
            throw new ReglaNegocioException("La consulta médica se encuentra inactiva");
        }
        VentanaEdicionConsulta.exigirEditable(consulta);
        return consulta;
    }

    /** Factura activa de la consulta: la Pendiente si hay, si no la más reciente. */
    private Optional<Facturas> facturaDeConsulta(Long idConsulta) {
        List<Facturas> facturas = facturasRepository.findByIdConsultaAndActivoTrue(idConsulta);
        return facturas.stream()
                .filter(f -> Objects.equals(f.getIdEstadoFactura(), estados.facturaPendiente()))
                .findFirst()
                .or(() -> facturas.stream().max(Comparator.comparing(Facturas::getIdFactura)));
    }

    private Long facturaPendienteOCrear(Consultas consulta) {
        Optional<Facturas> existente = facturaDeConsulta(consulta.getIdConsulta());
        if (existente.isPresent()) {
            Facturas f = existente.get();
            if (!Objects.equals(f.getIdEstadoFactura(), estados.facturaPendiente())) {
                throw new ReglaNegocioException("La factura #" + f.getIdFactura() + " de esta consulta ya está "
                        + nombreEstado(f.getIdEstadoFactura()).toLowerCase() + " y no admite más cargos");
            }
            return f.getIdFactura();
        }
        Long idPaciente = citasRepository.findById(consulta.getIdCita()).map(Citas::getIdPaciente)
                .orElseThrow(() -> new ReglaNegocioException("La cita de la consulta no existe"));
        // El método de pago es obligatorio al emitir; se usa el primero activo y caja lo cambia al cobrar
        Long idMetodoPago = metodosPagoRepository.findByActivoTrue().stream()
                .min(Comparator.comparing(MetodosPago::getIdMetodoPago))
                .map(MetodosPago::getIdMetodoPago)
                .orElseThrow(() -> new ReglaNegocioException("No hay métodos de pago activos para emitir la factura"));
        FacturasDTO creada = facturasService.crear(new FacturasDTO(null, idPaciente, null, null, null, null, null,
                idMetodoPago, null, true, consulta.getIdConsulta()));
        log.info("Factura {} creada para la consulta {}", creada.idFactura(), consulta.getIdConsulta());
        return creada.idFactura();
    }

    private String concepto(Medicamentos m, Long idTratamiento) {
        String nombre = m.getNombreComercial() + (m.getPresentacion() != null && !m.getPresentacion().isBlank() ? " (" + m.getPresentacion() + ")" : "");
        String texto = "Medicamento #" + m.getIdMedicamento() + " · tratamiento #" + idTratamiento + ": " + nombre;
        return texto.length() > LARGO_CONCEPTO ? texto.substring(0, LARGO_CONCEPTO) : texto;
    }

    private Optional<MedicamentosConsultaDTO.Item> aItem(DetalleFactura d) {
        Matcher m = CONCEPTO.matcher(d.getConcepto() == null ? "" : d.getConcepto());
        if (!m.find()) {
            return Optional.empty();
        }
        return Optional.of(new MedicamentosConsultaDTO.Item(d.getIdDetalle(), Long.valueOf(m.group(2)), Long.valueOf(m.group(1)),
                d.getConcepto().substring(m.end()), d.getCantidad(), d.getPrecioUnitario(), d.getImporte()));
    }

    private String nombreEstado(Long idEstado) {
        if (Objects.equals(idEstado, estados.facturaPendiente())) return "Pendiente";
        if (Objects.equals(idEstado, estados.facturaPagada())) return "Pagada";
        if (Objects.equals(idEstado, estados.facturaCancelada())) return "Cancelada";
        return "#" + idEstado;
    }
}
