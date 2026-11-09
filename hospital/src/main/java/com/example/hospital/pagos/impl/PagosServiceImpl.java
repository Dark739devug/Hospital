package com.example.hospital.pagos.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.hospital.cuentas.Cuentas;
import com.example.hospital.cuentas.CuentasRepository;
import com.example.hospital.facturas.CalculoImportes;
import com.example.hospital.facturas.Facturas;
import com.example.hospital.facturas.FacturasRepository;
import com.example.hospital.metodospago.MetodosPago;
import com.example.hospital.metodospago.MetodosPagoRepository;
import com.example.hospital.pagos.Pagos;
import com.example.hospital.pagos.PagosRepository;
import com.example.hospital.pagos.PagosService;
import com.example.hospital.pagos.dto.PagosDTO;
import com.example.hospital.shared.catalogos.EstadosCatalogo;
import com.example.hospital.shared.exception.RecursoNoEncontradoException;
import com.example.hospital.shared.exception.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PagosServiceImpl implements PagosService {

    private static final Logger log = LoggerFactory.getLogger(PagosServiceImpl.class);

    private final PagosRepository pagosRepository;
    private final FacturasRepository facturasRepository;
    private final MetodosPagoRepository metodosPagoRepository;
    private final CuentasRepository cuentasRepository;
    // Los estados de factura se resuelven por nombre, no por id fijo
    private final EstadosCatalogo estados;
    private final CalculoImportes calculo;

    public PagosServiceImpl(PagosRepository pagosRepository,
                            FacturasRepository facturasRepository,
                            MetodosPagoRepository metodosPagoRepository,
                            CuentasRepository cuentasRepository,
                            EstadosCatalogo estados,
                            CalculoImportes calculo) {
        this.pagosRepository = pagosRepository;
        this.facturasRepository = facturasRepository;
        this.metodosPagoRepository = metodosPagoRepository;
        this.cuentasRepository = cuentasRepository;
        this.estados = estados;
        this.calculo = calculo;
    }

    private PagosDTO mapToDTO(Pagos e) {
        return new PagosDTO(
                e.getIdPago(),
                e.getIdFactura(),
                e.getIdCuenta(),
                e.getIdMetodoPago(),
                e.getMonto(),
                e.getFechaPago(),
                e.getReferencia(),
                e.getActivo()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagosDTO> findAll() {
        return pagosRepository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PagosDTO findById(Long id) {
        return mapToDTO(buscar(id));
    }

    /**
     * Registra un pago. Reglas (422 si no se cumplen): factura activa y Pendiente; monto > 0 y no mayor al saldo
     * pendiente; método de pago y cuenta (si viene) existentes y activos; la cuenta debe ser del mismo paciente de la
     * factura. Si el saldo queda en 0, la factura pasa automáticamente a Pagada.
     */
    @Override
    @Transactional
    public PagosDTO crear(PagosDTO dto) {
        if (dto == null) {
            throw new ReglaNegocioException("Los datos del pago son requeridos");
        }
        Facturas factura = facturaDelCuerpo(dto.idFactura());
        if (!Boolean.TRUE.equals(factura.getActivo())) {
            throw new ReglaNegocioException("La factura " + factura.getIdFactura() + " está anulada: no admite pagos.");
        }
        if (!Objects.equals(factura.getIdEstadoFactura(), estados.facturaPendiente())) {
            throw new ReglaNegocioException("Solo se registran pagos sobre facturas Pendientes (la factura está Pagada o Cancelada).");
        }
        validarMetodoPago(dto.idMetodoPago());
        validarCuenta(dto.idCuenta(), factura);

        if (dto.monto() == null || dto.monto().signum() <= 0) {
            throw new ReglaNegocioException("El monto del pago debe ser mayor que 0");
        }
        if (dto.fechaPago() != null && dto.fechaPago().isAfter(LocalDateTime.now())) {
            throw new ReglaNegocioException("La fecha de pago no puede ser futura");
        }
        BigDecimal pagado = calculo.pagado(pagosRepository.findByIdFactura(factura.getIdFactura()));
        BigDecimal saldo = calculo.pendiente(factura.getTotal(), pagado);
        if (dto.monto().compareTo(saldo) > 0) {
            throw new ReglaNegocioException("El monto (" + dto.monto().setScale(2) + ") supera el saldo pendiente de la factura ("
                    + saldo + ").");
        }

        Pagos entity = new Pagos();
        entity.setIdFactura(dto.idFactura());
        entity.setIdCuenta(dto.idCuenta());
        entity.setIdMetodoPago(dto.idMetodoPago());
        entity.setMonto(dto.monto());
        entity.setFechaPago(dto.fechaPago() != null ? dto.fechaPago() : LocalDateTime.now());
        entity.setReferencia(limpiar(dto.referencia()));
        entity.setActivo(true);
        Pagos guardado = pagosRepository.save(entity);
        log.info("Pago {} registrado: factura {}, monto {}", guardado.getIdPago(), guardado.getIdFactura(), guardado.getMonto());

        // Saldo en 0 -> la factura queda Pagada
        if (saldo.subtract(dto.monto()).signum() == 0) {
            factura.setIdEstadoFactura(estados.facturaPagada());
            facturasRepository.save(factura);
        }
        return mapToDTO(guardado);
    }

    /**
     * Corrige SOLO el método de pago y la referencia. Factura, cuenta, monto y fecha no cambian (422 si el cuerpo
     * intenta cambiarlos), un pago anulado no se modifica ni se reactiva.
     */
    @Override
    @Transactional
    public PagosDTO actualizar(PagosDTO dto, Long id) {
        Pagos existente = buscar(id);
        if (dto == null) {
            throw new ReglaNegocioException("Los datos del pago son requeridos");
        }
        if (!Boolean.TRUE.equals(existente.getActivo())) {
            throw new ReglaNegocioException("El pago está anulado: no se puede modificar ni reactivar.");
        }
        if (!Objects.equals(dto.idFactura(), existente.getIdFactura())) {
            throw new ReglaNegocioException("No se puede cambiar la factura de un pago.");
        }
        if (dto.monto() == null || dto.monto().compareTo(existente.getMonto()) != 0) {
            throw new ReglaNegocioException("No se puede cambiar el monto de un pago: anúlelo y registre uno nuevo.");
        }
        if (dto.idCuenta() != null && !Objects.equals(dto.idCuenta(), existente.getIdCuenta())) {
            throw new ReglaNegocioException("No se puede cambiar la cuenta de un pago.");
        }
        if (Boolean.FALSE.equals(dto.activo())) {
            throw new ReglaNegocioException("Para anular un pago use DELETE.");
        }
        validarMetodoPago(dto.idMetodoPago());

        existente.setIdMetodoPago(dto.idMetodoPago());
        existente.setReferencia(limpiar(dto.referencia()));
        log.info("Pago {} corregido", id);
        return mapToDTO(pagosRepository.save(existente));
    }

    /**
     * Anula el pago (borrado lógico). Permitido con la factura Pendiente o Pagada; con la factura Cancelada, 422.
     * Si la factura estaba Pagada vuelve a Pendiente.
     */
    @Override
    @Transactional
    public void eliminar(Long id) {
        Pagos existente = buscar(id);
        if (!Boolean.TRUE.equals(existente.getActivo())) {
            throw new ReglaNegocioException("El pago ya se encuentra anulado.");
        }
        Facturas factura = facturasRepository.findById(existente.getIdFactura())
                .orElseThrow(() -> new ReglaNegocioException("La factura del pago no existe."));
        if (Objects.equals(factura.getIdEstadoFactura(), estados.facturaCancelada())) {
            throw new ReglaNegocioException("No se puede anular un pago de una factura Cancelada.");
        }
        existente.setActivo(false);
        pagosRepository.save(existente);
        log.info("Pago {} anulado: factura {}, monto {}", id, existente.getIdFactura(), existente.getMonto());

        if (Objects.equals(factura.getIdEstadoFactura(), estados.facturaPagada())) {
            factura.setIdEstadoFactura(estados.facturaPendiente());
            facturasRepository.save(factura);
                log.info("La factura {} vuelve a Pendiente por la anulación del pago {}", factura.getIdFactura(), id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagosDTO> findByFactura(Long idFactura, boolean soloActivos) {
        if (!facturasRepository.existsById(idFactura)) {
            throw new RecursoNoEncontradoException("No existe la factura con ID: " + idFactura);
        }
        return pagosRepository.findByIdFactura(idFactura).stream()
                .filter(p -> !soloActivos || Boolean.TRUE.equals(p.getActivo()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagosDTO> findByCuenta(Long idCuenta, boolean soloActivos) {
        if (!cuentasRepository.existsById(idCuenta)) {
            throw new RecursoNoEncontradoException("No existe la cuenta con ID: " + idCuenta);
        }
        return pagosRepository.findByIdCuenta(idCuenta).stream()
                .filter(p -> !soloActivos || Boolean.TRUE.equals(p.getActivo()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private Pagos buscar(Long id) {
        if (id == null) {
            throw new ReglaNegocioException("El ID del pago es obligatorio");
        }
        return pagosRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el pago con ID: " + id));
    }

    // Referencia del cuerpo: inexistente -> 422
    private Facturas facturaDelCuerpo(Long idFactura) {
        if (idFactura == null) {
            throw new ReglaNegocioException("La factura del pago es obligatoria");
        }
        return facturasRepository.findById(idFactura)
                .orElseThrow(() -> new ReglaNegocioException("No existe la factura con ID: " + idFactura));
    }

    private void validarMetodoPago(Long idMetodoPago) {
        if (idMetodoPago == null) {
            throw new ReglaNegocioException("El método de pago es obligatorio");
        }
        MetodosPago metodo = metodosPagoRepository.findById(idMetodoPago)
                .orElseThrow(() -> new ReglaNegocioException("No existe el método de pago con ID: " + idMetodoPago));
        if (!Boolean.TRUE.equals(metodo.getActivo())) {
            throw new ReglaNegocioException("El método de pago seleccionado se encuentra inactivo");
        }
    }

    private void validarCuenta(Long idCuenta, Facturas factura) {
        if (idCuenta == null) {
            return;
        }
        Cuentas cuenta = cuentasRepository.findById(idCuenta)
                .orElseThrow(() -> new ReglaNegocioException("No existe la cuenta con ID: " + idCuenta));
        if (!Boolean.TRUE.equals(cuenta.getActivo())) {
            throw new ReglaNegocioException("La cuenta seleccionada se encuentra inactiva");
        }
        if (!Objects.equals(cuenta.getIdPaciente(), factura.getIdPaciente())) {
            throw new ReglaNegocioException("La cuenta " + idCuenta + " no pertenece al mismo paciente de la factura.");
        }
    }

    private String limpiar(String texto) {
        return texto == null || texto.trim().isEmpty() ? null : texto.trim();
    }
}
