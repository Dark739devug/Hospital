package com.example.hospital.facturas.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.hospital.estadosfactura.EstadosFactura;
import com.example.hospital.estadosfactura.EstadosFacturaRepository;
import com.example.hospital.facturas.CalculoImportes;
import com.example.hospital.facturas.Facturas;
import com.example.hospital.facturas.FacturasRepository;
import com.example.hospital.facturas.FacturasService;
import com.example.hospital.facturas.dto.FacturasDTO;
import com.example.hospital.facturas.dto.SaldoFacturaDTO;
import com.example.hospital.hospitalizaciones.Hospitalizaciones;
import com.example.hospital.hospitalizaciones.HospitalizacionesRepository;
import com.example.hospital.metodospago.MetodosPago;
import com.example.hospital.metodospago.MetodosPagoRepository;
import com.example.hospital.paciente.Paciente;
import com.example.hospital.paciente.PacienteRepository;
import com.example.hospital.pagos.Pagos;
import com.example.hospital.pagos.PagosRepository;
import com.example.hospital.shared.catalogos.EstadosCatalogo;
import com.example.hospital.shared.exception.DuplicadoException;
import com.example.hospital.shared.exception.RecursoNoEncontradoException;
import com.example.hospital.shared.exception.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class FacturasServiceImpl implements FacturasService {

    private static final Logger log = LoggerFactory.getLogger(FacturasServiceImpl.class);

    private final FacturasRepository facturasRepository;
    private final PacienteRepository pacienteRepository;
    private final HospitalizacionesRepository hospitalizacionesRepository;
    private final MetodosPagoRepository metodosPagoRepository;
    private final EstadosFacturaRepository estadosFacturaRepository;
    private final PagosRepository pagosRepository;
    // Los estados (Pendiente, Pagada, Cancelada) se resuelven por nombre, no por id fijo
    private final EstadosCatalogo estados;
    private final CalculoImportes calculo;

    public FacturasServiceImpl(FacturasRepository facturasRepository,
                               PacienteRepository pacienteRepository,
                               HospitalizacionesRepository hospitalizacionesRepository,
                               MetodosPagoRepository metodosPagoRepository,
                               EstadosFacturaRepository estadosFacturaRepository,
                               PagosRepository pagosRepository,
                               EstadosCatalogo estados,
                               CalculoImportes calculo) {
        this.facturasRepository = facturasRepository;
        this.pacienteRepository = pacienteRepository;
        this.hospitalizacionesRepository = hospitalizacionesRepository;
        this.metodosPagoRepository = metodosPagoRepository;
        this.estadosFacturaRepository = estadosFacturaRepository;
        this.pagosRepository = pagosRepository;
        this.estados = estados;
        this.calculo = calculo;
    }

    private FacturasDTO mapToDTO(Facturas f) {
        return new FacturasDTO(
                f.getIdFactura(),
                f.getIdPaciente(),
                f.getIdHospitalizacion(),
                f.getFechaEmision(),
                f.getSubtotal(),
                f.getIva(),
                f.getTotal(),
                f.getIdMetodoPago(),
                f.getIdEstadoFactura(),
                f.getActivo()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<FacturasDTO> findAll() {
        return facturasRepository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FacturasDTO> findActivas() {
        return facturasRepository.findByActivoTrue().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FacturasDTO findById(Long id) {
        return mapToDTO(buscarEntidad(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FacturasDTO> findByPaciente(Long idPaciente) {
        if (idPaciente == null) {
            throw new ReglaNegocioException("El ID del paciente es requerido");
        }
        return facturasRepository.findByIdPacienteAndActivoTrue(idPaciente)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FacturasDTO> findByHospitalizacion(Long idHospitalizacion) {
        if (idHospitalizacion == null) {
            throw new ReglaNegocioException("El ID de hospitalización es requerido");
        }
        return facturasRepository.findByIdHospitalizacionAndActivoTrue(idHospitalizacion)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FacturasDTO> buscar(Long idPaciente, Long idHospitalizacion, Long idEstadoFactura,
                                    LocalDate desde, LocalDate hasta, boolean soloActivas) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ReglaNegocioException("La fecha 'desde' no puede ser posterior a la fecha 'hasta'.");
        }
        return facturasRepository.findAll().stream()
                .filter(f -> !soloActivas || Boolean.TRUE.equals(f.getActivo()))
                .filter(f -> idPaciente == null || idPaciente.equals(f.getIdPaciente()))
                .filter(f -> idHospitalizacion == null || idHospitalizacion.equals(f.getIdHospitalizacion()))
                .filter(f -> idEstadoFactura == null || idEstadoFactura.equals(f.getIdEstadoFactura()))
                .filter(f -> desde == null || (f.getFechaEmision() != null && !f.getFechaEmision().toLocalDate().isBefore(desde)))
                .filter(f -> hasta == null || (f.getFechaEmision() != null && !f.getFechaEmision().toLocalDate().isAfter(hasta)))
                .sorted(Comparator.comparing(Facturas::getIdFactura))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FacturasDTO crear(FacturasDTO dto) {
        if (dto == null) {
            throw new ReglaNegocioException("Los datos de la factura son requeridos");
        }
        validarPaciente(dto.idPaciente());
        validarHospitalizacion(dto.idHospitalizacion(), dto.idPaciente());
        validarMetodoPago(dto.idMetodoPago());

        // El cliente no fija subtotal, IVA, total, estado ni fecha: la factura nace vacía, Pendiente y con la hora actual.
        // El subtotal y el IVA se calculan después, a partir de las líneas (detalle-factura).
        Facturas entity = new Facturas();
        entity.setIdPaciente(dto.idPaciente());
        entity.setIdHospitalizacion(dto.idHospitalizacion());
        entity.setFechaEmision(LocalDateTime.now());
        entity.setSubtotal(BigDecimal.ZERO.setScale(2));
        entity.setIva(BigDecimal.ZERO.setScale(2));
        entity.setTotal(BigDecimal.ZERO.setScale(2));
        entity.setIdMetodoPago(dto.idMetodoPago());
        entity.setIdEstadoFactura(estados.facturaPendiente());
        entity.setActivo(true);
        Facturas guardada = facturasRepository.save(entity);
        log.info("Factura {} emitida: paciente {}, hospitalización {}", guardada.getIdFactura(), guardada.getIdPaciente(), guardada.getIdHospitalizacion());
        return mapToDTO(guardada);
    }

    @Override
    @Transactional
    public FacturasDTO actualizar(FacturasDTO dto, Long id) {
        if (id == null) {
            throw new ReglaNegocioException("El ID de la factura es obligatorio para la actualización");
        }
        Facturas existente = buscarEntidad(id);
        if (dto == null) {
            throw new ReglaNegocioException("Los datos de la factura son requeridos");
        }
        if (!Boolean.TRUE.equals(existente.getActivo())) {
            throw new ReglaNegocioException("La factura está anulada y no se puede modificar.");
        }

        boolean pendiente = Objects.equals(existente.getIdEstadoFactura(), estados.facturaPendiente());
        boolean pacienteCambia = !Objects.equals(existente.getIdPaciente(), dto.idPaciente());
        boolean hospCambia = !Objects.equals(existente.getIdHospitalizacion(), dto.idHospitalizacion());

        // RN-139: en una factura pagada o cancelada solo se puede corregir el método de pago
        if (!pendiente && (pacienteCambia || hospCambia)) {
            throw new ReglaNegocioException("RN-139: No se permite modificar el paciente ni la hospitalización de una factura pagada o cancelada.");
        }
        if (pacienteCambia) {
            if (!pagosRepository.findByIdFactura(id).isEmpty()) {
                throw new ReglaNegocioException("No se puede cambiar el paciente de una factura que ya tiene pagos.");
            }
            validarPaciente(dto.idPaciente());
        }
        // La hospitalización siempre debe ser del paciente de la factura (RN-133)
        validarHospitalizacion(dto.idHospitalizacion(), dto.idPaciente());
        validarMetodoPago(dto.idMetodoPago());

        // Subtotal, IVA y total los calcula el servidor (líneas); el estado cambia por PATCH o automáticamente;
        // la fecha de emisión y `activo` no son editables: se ignoran si vienen en el cuerpo.
        existente.setIdPaciente(dto.idPaciente());
        existente.setIdHospitalizacion(dto.idHospitalizacion());
        existente.setIdMetodoPago(dto.idMetodoPago());
        return mapToDTO(facturasRepository.save(existente));
    }

    @Override
    @Transactional
    public FacturasDTO cambiarEstado(Long id, Long nuevoEstadoId) {
        if (id == null || nuevoEstadoId == null) {
            throw new ReglaNegocioException("El ID de factura y el nuevo estado son requeridos");
        }
        Facturas existente = buscarEntidad(id);
        if (!Boolean.TRUE.equals(existente.getActivo())) {
            throw new ReglaNegocioException("La factura está anulada y no se puede cambiar su estado.");
        }
        EstadosFactura destino = estadosFacturaRepository.findById(nuevoEstadoId)
                .orElseThrow(() -> new ReglaNegocioException("El estado de factura " + nuevoEstadoId + " no existe"));
        if (!Boolean.TRUE.equals(destino.getActivo())) {
            throw new ReglaNegocioException("El estado de factura seleccionado se encuentra inactivo");
        }
        if (nuevoEstadoId.equals(existente.getIdEstadoFactura())) {
            throw new ReglaNegocioException("La factura ya se encuentra en ese estado.");
        }

        Long pendiente = estados.facturaPendiente();
        Long pagada = estados.facturaPagada();
        Long cancelada = estados.facturaCancelada();

        // Pagada y Cancelada son terminales (la única vuelta, Pagada -> Pendiente, es automática al anular un pago)
        if (!Objects.equals(existente.getIdEstadoFactura(), pendiente)) {
            throw new ReglaNegocioException("Una factura Pagada o Cancelada no puede cambiar de estado.");
        }

        List<Pagos> pagos = pagosRepository.findByIdFactura(id);
        BigDecimal pagado = calculo.pagado(pagos);
        if (nuevoEstadoId.equals(pagada)) {
            BigDecimal total = existente.getTotal() != null ? existente.getTotal() : BigDecimal.ZERO;
            if (total.signum() <= 0) {
                throw new ReglaNegocioException("No se puede marcar como Pagada una factura con total cero.");
            }
            if (calculo.pendiente(total, pagado).signum() > 0) {
                throw new ReglaNegocioException("No se puede marcar como Pagada: la factura tiene saldo pendiente.");
            }
        } else if (nuevoEstadoId.equals(cancelada)) {
            if (pagado.signum() > 0) {
                throw new ReglaNegocioException("No se puede cancelar una factura con pagos activos: anule primero los pagos.");
            }
        } else {
            throw new ReglaNegocioException("Transición de estado no permitida.");
        }

        existente.setIdEstadoFactura(nuevoEstadoId);
        log.info("Factura {} cambió al estado {}", id, nuevoEstadoId);
        return mapToDTO(facturasRepository.save(existente));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (id == null) {
            throw new ReglaNegocioException("El ID de la factura no puede ser nulo");
        }
        Facturas existente = buscarEntidad(id);
        if (!Boolean.TRUE.equals(existente.getActivo())) {
            throw new ReglaNegocioException("La factura ya se encuentra anulada.");
        }
        // Protección contable: no anular facturas pagadas
        if (Objects.equals(existente.getIdEstadoFactura(), estados.facturaPagada())) {
            throw new ReglaNegocioException("No se puede eliminar una factura que ya ha sido Pagada.");
        }
        // Conflicto con dependencias: la factura tiene pagos aplicados
        if (calculo.pagado(pagosRepository.findByIdFactura(id)).signum() > 0) {
            throw new DuplicadoException("No se puede anular la factura porque tiene pagos activos: anule primero los pagos.");
        }
        existente.setActivo(false);
        facturasRepository.save(existente);
        log.info("Factura {} anulada", id);
    }

    @Override
    @Transactional(readOnly = true)
    public SaldoFacturaDTO obtenerSaldo(Long idFactura) {
        Facturas factura = buscarEntidad(idFactura);
        BigDecimal total = factura.getTotal() != null ? factura.getTotal() : BigDecimal.ZERO;
        List<Pagos> activos = pagosRepository.findByIdFactura(idFactura).stream()
                .filter(p -> !Boolean.FALSE.equals(p.getActivo()))
                .collect(Collectors.toList());
        BigDecimal pagado = calculo.pagado(activos);
        BigDecimal pendiente = calculo.pendiente(total, pagado);
        return new SaldoFacturaDTO(
                factura.getIdFactura(),
                factura.getIdPaciente(),
                total,
                pagado,
                pendiente,
                activos.size(),
                pendiente.signum() == 0
        );
    }

    private Facturas buscarEntidad(Long id) {
        return facturasRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la factura con ID: " + id));
    }

    // RN-132: toda factura pertenece a un paciente existente y activo
    private void validarPaciente(Long idPaciente) {
        if (idPaciente == null) {
            throw new ReglaNegocioException("RN-132: Toda factura debe pertenecer a un paciente existente.");
        }
        Paciente paciente = pacienteRepository.findById(idPaciente)
                .orElseThrow(() -> new ReglaNegocioException("RN-132: El paciente con ID " + idPaciente + " no existe"));
        if (!Boolean.TRUE.equals(paciente.getActivo())) {
            throw new ReglaNegocioException("El paciente seleccionado se encuentra inactivo");
        }
    }

    // RN-133: la hospitalización, si se indica, debe existir y ser del mismo paciente
    private void validarHospitalizacion(Long idHospitalizacion, Long idPaciente) {
        if (idHospitalizacion == null) {
            return;
        }
        Hospitalizaciones h = hospitalizacionesRepository.findById(idHospitalizacion)
                .orElseThrow(() -> new ReglaNegocioException("La hospitalización con ID " + idHospitalizacion + " no existe"));
        if (!Objects.equals(idPaciente, h.getIdPaciente())) {
            throw new ReglaNegocioException("RN-133: La hospitalización asociada (" + idHospitalizacion
                    + ") no pertenece al mismo paciente (" + idPaciente + ") de la factura.");
        }
    }

    // RN-137: el método de pago debe existir y estar activo
    private void validarMetodoPago(Long idMetodoPago) {
        if (idMetodoPago == null) {
            throw new ReglaNegocioException("RN-137: El método de pago es obligatorio.");
        }
        MetodosPago metodo = metodosPagoRepository.findById(idMetodoPago)
                .orElseThrow(() -> new ReglaNegocioException("RN-137: El método de pago con ID " + idMetodoPago + " no existe."));
        if (!Boolean.TRUE.equals(metodo.getActivo())) {
            throw new ReglaNegocioException("El método de pago seleccionado se encuentra inactivo.");
        }
    }
}
