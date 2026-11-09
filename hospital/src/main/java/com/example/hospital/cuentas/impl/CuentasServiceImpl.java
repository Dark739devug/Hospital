package com.example.hospital.cuentas.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.hospital.cuentas.Cuentas;
import com.example.hospital.cuentas.CuentasRepository;
import com.example.hospital.cuentas.CuentasService;
import com.example.hospital.cuentas.dto.CuentasDTO;
import com.example.hospital.hospitalizaciones.Hospitalizaciones;
import com.example.hospital.hospitalizaciones.HospitalizacionesRepository;
import com.example.hospital.paciente.Paciente;
import com.example.hospital.paciente.PacienteRepository;
import com.example.hospital.pagos.PagosRepository;
import com.example.hospital.shared.exception.DuplicadoException;
import com.example.hospital.shared.exception.RecursoNoEncontradoException;
import com.example.hospital.shared.exception.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Cuentas hospitalarias. Modelo: una cuenta agrupa los cargos de un paciente (opcionalmente de una hospitalización);
 * `saldo` es un valor manual que solo cambia con `ajustarSaldo`. NO está ligado a pagos ni facturas (ver recomendación
 * en el informe): los pagos solo pueden referenciar la cuenta.
 *
 * Estados: Abierta -> Liquidada (automático al llegar el saldo a 0) -> Abierta (reapertura automática si se vuelve a
 * cargar saldo > 0, o manual con PUT); Cerrada es un estado manual en el que no se ajusta el saldo.
 */
@Service
public class CuentasServiceImpl implements CuentasService {

    private static final Logger log = LoggerFactory.getLogger(CuentasServiceImpl.class);

    public static final String ESTADO_ABIERTA = "Abierta";
    public static final String ESTADO_CERRADA = "Cerrada";
    public static final String ESTADO_LIQUIDADA = "Liquidada";
    private static final List<String> ESTADOS = List.of(ESTADO_ABIERTA, ESTADO_CERRADA, ESTADO_LIQUIDADA);
    // Límite de DECIMAL(10,2)
    private static final BigDecimal MAXIMO = new BigDecimal("99999999.99");

    private final CuentasRepository cuentasRepository;
    private final PacienteRepository pacienteRepository;
    private final HospitalizacionesRepository hospitalizacionesRepository;
    private final PagosRepository pagosRepository;

    public CuentasServiceImpl(CuentasRepository cuentasRepository,
                              PacienteRepository pacienteRepository,
                              HospitalizacionesRepository hospitalizacionesRepository,
                              PagosRepository pagosRepository) {
        this.cuentasRepository = cuentasRepository;
        this.pacienteRepository = pacienteRepository;
        this.hospitalizacionesRepository = hospitalizacionesRepository;
        this.pagosRepository = pagosRepository;
    }

    private CuentasDTO mapToDTO(Cuentas c) {
        return new CuentasDTO(
                c.getIdCuenta(),
                c.getIdPaciente(),
                c.getIdHospitalizacion(),
                c.getFechaApertura(),
                c.getEstado(),
                c.getSaldo(),
                c.getActivo()
        );
    }

    /** Estado normalizado a la forma canónica (Abierta/Cerrada/Liquidada) o 422 si no es válido. */
    private String estadoValido(String estado) {
        return ESTADOS.stream()
                .filter(e -> e.equalsIgnoreCase(estado == null ? "" : estado.trim()))
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException("El estado de la cuenta debe ser Abierta, Cerrada o Liquidada."));
    }

    /**
     * Reglas de negocio (422 / 409): el paciente debe existir y estar activo; la hospitalización, si viene, debe
     * existir y ser del mismo paciente, y no puede tener otra cuenta activa (409).
     */
    private void validarReglasDeNegocio(CuentasDTO dto, Long idCuentaActual) {
        if (dto == null) {
            throw new ReglaNegocioException("Los datos de la cuenta hospitalaria no pueden ser nulos");
        }
        if (dto.idPaciente() == null) {
            throw new ReglaNegocioException("El ID del paciente es obligatorio");
        }
        Paciente paciente = pacienteRepository.findById(dto.idPaciente())
                .orElseThrow(() -> new ReglaNegocioException("El paciente con ID " + dto.idPaciente() + " no existe"));
        if (!Boolean.TRUE.equals(paciente.getActivo())) {
            throw new ReglaNegocioException("El paciente seleccionado se encuentra inactivo");
        }

        if (dto.idHospitalizacion() != null) {
            Hospitalizaciones hosp = hospitalizacionesRepository.findById(dto.idHospitalizacion())
                    .orElseThrow(() -> new ReglaNegocioException("La hospitalización con ID " + dto.idHospitalizacion() + " no existe"));
            if (!Objects.equals(hosp.getIdPaciente(), dto.idPaciente())) {
                throw new ReglaNegocioException("La hospitalización no corresponde al paciente seleccionado");
            }
            boolean existeOtraActiva = idCuentaActual == null
                    ? cuentasRepository.existsByIdHospitalizacionAndActivoTrue(dto.idHospitalizacion())
                    : cuentasRepository.existsByIdHospitalizacionAndActivoTrueAndIdCuentaNot(dto.idHospitalizacion(), idCuentaActual);
            if (existeOtraActiva) {
                throw new DuplicadoException("Ya existe una cuenta hospitalaria activa para esta hospitalización");
            }
        }
        if (dto.saldo() != null && dto.saldo().signum() < 0) {
            throw new ReglaNegocioException("El saldo de la cuenta no puede ser negativo");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentasDTO> findAll() {
        return cuentasRepository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentasDTO> findActivas() {
        return cuentasRepository.findByActivoTrue().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentasDTO> findByPaciente(Long idPaciente) {
        if (idPaciente == null) {
            throw new ReglaNegocioException("El ID del paciente es obligatorio");
        }
        return cuentasRepository.findByIdPaciente(idPaciente).stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentasDTO> findByHospitalizacion(Long idHospitalizacion) {
        if (idHospitalizacion == null) {
            throw new ReglaNegocioException("El ID de hospitalización es obligatorio");
        }
        return cuentasRepository.findByIdHospitalizacion(idHospitalizacion).stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentasDTO> buscar(Long idPaciente, Long idHospitalizacion, String estado, boolean soloActivas) {
        String estadoFiltro = estado == null || estado.trim().isEmpty() ? null : estadoValido(estado);
        return cuentasRepository.findAll().stream()
                .filter(c -> !soloActivas || Boolean.TRUE.equals(c.getActivo()))
                .filter(c -> idPaciente == null || idPaciente.equals(c.getIdPaciente()))
                .filter(c -> idHospitalizacion == null || idHospitalizacion.equals(c.getIdHospitalizacion()))
                .filter(c -> estadoFiltro == null || estadoFiltro.equalsIgnoreCase(c.getEstado()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CuentasDTO findById(Long id) {
        return mapToDTO(buscarEntidad(id));
    }

    @Override
    @Transactional
    public CuentasDTO crear(CuentasDTO dto) {
        validarReglasDeNegocio(dto, null);

        // La cuenta se abre siempre Abierta y activa, con la fecha del servidor; solo el saldo inicial lo define el cliente
        Cuentas entity = new Cuentas();
        entity.setIdPaciente(dto.idPaciente());
        entity.setIdHospitalizacion(dto.idHospitalizacion());
        entity.setFechaApertura(LocalDateTime.now());
        entity.setEstado(ESTADO_ABIERTA);
        entity.setSaldo(dto.saldo() != null ? dto.saldo() : BigDecimal.ZERO);
        entity.setActivo(true);
        Cuentas guardada = cuentasRepository.save(entity);
        log.info("Cuenta {} abierta: paciente {}, hospitalización {}", guardada.getIdCuenta(), guardada.getIdPaciente(), guardada.getIdHospitalizacion());
        return mapToDTO(guardada);
    }

    @Override
    @Transactional
    public CuentasDTO actualizar(CuentasDTO dto, Long id) {
        if (id == null) {
            throw new ReglaNegocioException("El ID de la cuenta es obligatorio");
        }
        Cuentas existente = buscarEntidad(id);
        validarReglasDeNegocio(dto, id);
        if (!Boolean.TRUE.equals(existente.getActivo())) {
            throw new ReglaNegocioException("La cuenta está desactivada y no se puede modificar.");
        }
        if (!Objects.equals(existente.getIdPaciente(), dto.idPaciente()) && pagosRepository.existsByIdCuenta(id)) {
            throw new ReglaNegocioException("No se puede cambiar el paciente de una cuenta que tiene pagos registrados.");
        }

        // El saldo solo cambia con ajustarSaldo y `activo` solo con DELETE: ambos se ignoran en el PUT
        if (dto.estado() != null && !dto.estado().trim().isEmpty()) {
            String nuevo = estadoValido(dto.estado());
            BigDecimal saldo = existente.getSaldo() != null ? existente.getSaldo() : BigDecimal.ZERO;
            if (ESTADO_LIQUIDADA.equals(nuevo) && saldo.signum() != 0) {
                throw new ReglaNegocioException("Una cuenta solo puede quedar Liquidada con saldo 0.");
            }
            existente.setEstado(nuevo);
        }
        existente.setIdPaciente(dto.idPaciente());
        existente.setIdHospitalizacion(dto.idHospitalizacion());
        return mapToDTO(cuentasRepository.save(existente));
    }

    /**
     * Suma (positivo) o resta (negativo) al saldo. No admite 0, más de 2 decimales, saldo resultante negativo o mayor
     * a 99,999,999.99, ni cuentas desactivadas o Cerradas (422). Con saldo 0 una cuenta Abierta pasa a Liquidada; una
     * Liquidada vuelve a Abierta si el nuevo saldo es mayor que 0.
     */
    @Override
    @Transactional
    public CuentasDTO ajustarSaldo(Long id, BigDecimal montoAjuste) {
        if (id == null) {
            throw new ReglaNegocioException("El ID de la cuenta es obligatorio");
        }
        Cuentas existente = buscarEntidad(id);
        if (montoAjuste == null || montoAjuste.signum() == 0) {
            throw new ReglaNegocioException("El monto de ajuste es obligatorio y no puede ser cero.");
        }
        if (montoAjuste.stripTrailingZeros().scale() > 2) {
            throw new ReglaNegocioException("El monto de ajuste admite como máximo 2 decimales.");
        }
        if (!Boolean.TRUE.equals(existente.getActivo())) {
            throw new ReglaNegocioException("No se puede ajustar el saldo de una cuenta desactivada.");
        }
        if (ESTADO_CERRADA.equalsIgnoreCase(existente.getEstado())) {
            throw new ReglaNegocioException("No se puede ajustar el saldo de una cuenta Cerrada: reábrala antes.");
        }

        BigDecimal actual = existente.getSaldo() != null ? existente.getSaldo() : BigDecimal.ZERO;
        BigDecimal nuevo = actual.add(montoAjuste);
        if (nuevo.signum() < 0) {
            throw new ReglaNegocioException("El ajuste resultaría en un saldo negativo no permitido (" + nuevo + ")");
        }
        if (nuevo.compareTo(MAXIMO) > 0) {
            throw new ReglaNegocioException("El saldo resultante supera el máximo permitido de 99,999,999.99.");
        }

        existente.setSaldo(nuevo);
        if (nuevo.signum() == 0 && ESTADO_ABIERTA.equalsIgnoreCase(existente.getEstado())) {
            existente.setEstado(ESTADO_LIQUIDADA);
        } else if (nuevo.signum() > 0 && ESTADO_LIQUIDADA.equalsIgnoreCase(existente.getEstado())) {
            existente.setEstado(ESTADO_ABIERTA);
        }
        log.info("Saldo de la cuenta {} ajustado en {}: nuevo saldo {}", id, montoAjuste, existente.getSaldo());
        return mapToDTO(cuentasRepository.save(existente));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (id == null) {
            throw new ReglaNegocioException("El ID de la cuenta es obligatorio");
        }
        Cuentas existente = buscarEntidad(id);
        if (!Boolean.TRUE.equals(existente.getActivo())) {
            throw new ReglaNegocioException("La cuenta ya se encuentra desactivada.");
        }
        // Conflicto con dependencias: no desactivar una cuenta con pagos registrados
        if (pagosRepository.existsByIdCuenta(id)) {
            throw new DuplicadoException("No se puede desactivar la cuenta hospitalaria porque contiene pagos registrados");
        }
        existente.setActivo(false);
        cuentasRepository.save(existente);
        log.info("Cuenta {} desactivada", id);
    }

    private Cuentas buscarEntidad(Long id) {
        return cuentasRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la cuenta hospitalaria con ID: " + id));
    }
}
