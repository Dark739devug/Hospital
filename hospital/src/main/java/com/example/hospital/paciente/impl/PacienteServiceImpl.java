package com.example.hospital.paciente.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.hospital.shared.catalogos.EstadosCatalogo;
import com.example.hospital.alergias.Alergias;
import com.example.hospital.alergias.AlergiasRepository;
import com.example.hospital.citas.CitasRepository;
import com.example.hospital.cuentas.CuentasRepository;
import com.example.hospital.emergencia.EmergenciaRepository;
import com.example.hospital.hospitalizaciones.HospitalizacionesRepository;
import com.example.hospital.paciente.Paciente;
import com.example.hospital.paciente.PacienteMapper;
import com.example.hospital.paciente.PacienteRepository;
import com.example.hospital.paciente.PacienteService;
import com.example.hospital.paciente.dto.PacienteRequest;
import com.example.hospital.paciente.dto.PacienteResumenDTO;
import com.example.hospital.paciente.dto.PacienteResponse;
import com.example.hospital.shared.exception.DuplicadoException;
import com.example.hospital.shared.exception.RecursoNoEncontradoException;
import com.example.hospital.shared.exception.ReglaNegocioException;
import com.example.hospital.tiposangre.TipoSangre;
import com.example.hospital.tiposangre.TipoSangreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PacienteServiceImpl implements PacienteService {

    private static final Logger log = LoggerFactory.getLogger(PacienteServiceImpl.class);

    private final PacienteRepository repository;
    private final AlergiasRepository alergiasRepository;
    private final TipoSangreRepository tipoSangreRepository;
    private final CitasRepository citasRepository;
    private final HospitalizacionesRepository hospitalizacionesRepository;
    private final EmergenciaRepository emergenciaRepository;
    private final CuentasRepository cuentasRepository;
    private final PacienteMapper mapper;
    // El estado de hospitalización Activa se resuelve por nombre, no por id fijo
    private final EstadosCatalogo estados;

    @Override
    @Transactional(readOnly = true)
    public List<PacienteResponse> listar() {
        return repository.findByActivoTrue().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PacienteResponse> listarTodos() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PacienteResponse obtener(Long id) {
        return mapper.toResponse(buscarOLanzar(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PacienteResponse obtenerDetalle(Long id) {
        return mapper.toResponse(repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente " + id + " no encontrado")));
    }

    @Override
    @Transactional(readOnly = true)
    public PacienteResponse buscarPorDpi(String dpi) {
        if (dpi == null || dpi.trim().isEmpty()) {
            throw new ReglaNegocioException("El DPI de búsqueda no puede estar vacío");
        }
        Paciente p = repository.findByDpiPacienteAndActivoTrue(dpi.trim())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró paciente activo con DPI: " + dpi));
        return mapper.toResponse(p);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PacienteResponse> buscar(String criterio) {
        return buscar(criterio, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PacienteResponse> buscar(String criterio, boolean todos) {
        if (criterio == null || criterio.trim().isEmpty()) {
            return todos ? listarTodos() : listar();
        }
        String valor = criterio.trim();
        return repository.findByNombreContainingIgnoreCaseOrApellidoPaternoContainingIgnoreCaseOrApellidoMaternoContainingIgnoreCaseOrDpiPacienteContainingIgnoreCase(valor, valor, valor, valor)
                .stream()
                .filter(p -> todos || Boolean.TRUE.equals(p.getActivo()))
                .map(mapper::toResponse)
                .toList();
    }

    // Mínimo privilegio para caja: búsqueda de identificación; con menos de 2 caracteres devuelve lista vacía
    private static final int LARGO_MINIMO_BUSQUEDA = 2;
    private static final int MAXIMO_RESULTADOS = 20;

    private PacienteResumenDTO toResumen(Paciente p) {
        String nombre = java.util.stream.Stream.of(p.getNombre(), p.getApellidoPaterno(), p.getApellidoMaterno())
                .filter(t -> t != null && !t.isBlank())
                .collect(java.util.stream.Collectors.joining(" "));
        return new PacienteResumenDTO(p.getIdPaciente(), nombre, p.getDpiPaciente(), p.getActivo());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PacienteResumenDTO> buscarResumen(String q) {
        if (q == null || q.trim().length() < LARGO_MINIMO_BUSQUEDA) {
            return List.of();
        }
        String valor = q.trim();
        return repository.findByNombreContainingIgnoreCaseOrApellidoPaternoContainingIgnoreCaseOrApellidoMaternoContainingIgnoreCaseOrDpiPacienteContainingIgnoreCase(valor, valor, valor, valor)
                .stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .map(this::toResumen)
                .sorted(java.util.Comparator.comparing(PacienteResumenDTO::nombreCompleto, String.CASE_INSENSITIVE_ORDER))
                .limit(MAXIMO_RESULTADOS)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PacienteResumenDTO resumen(Long id) {
        return toResumen(repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente " + id + " no encontrado")));
    }

    @Override
    @Transactional
    public PacienteResponse crear(PacienteRequest request) {
        validarReglasDeNegocio(request, null);

        Paciente nuevo = mapper.toEntity(request);
        nuevo.setFechaRegistro(LocalDateTime.now());   // Asignado por el servidor
        nuevo.setActivo(true);
        Paciente guardado = repository.save(nuevo);
        // Solo el id: el log no debe contener datos personales del paciente
        log.info("Paciente {} registrado", guardado.getIdPaciente());
        return mapper.toResponse(guardado);
    }

    @Override
    @Transactional
    public PacienteResponse actualizar(Long id, PacienteRequest request) {
        Paciente existente = buscarOLanzar(id);
        validarReglasDeNegocio(request, id);

        mapper.updateEntity(existente, request);
        return mapper.toResponse(repository.save(existente));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Paciente existente = buscarOLanzar(id);

        // RN-PAC-08: Integridad referencial clínica antes de desactivación
        if (citasRepository.existsByIdPacienteAndActivoTrue(id)) {
            throw new ReglaNegocioException("No se puede desactivar el paciente porque tiene citas médicas activas.");
        }

        if (hospitalizacionesRepository.existsByIdPacienteAndIdEstadoHosp(id, estados.hospActiva())) {
            throw new ReglaNegocioException("No se puede desactivar el paciente porque tiene una hospitalización activa en curso.");
        }

        if (emergenciaRepository.existsByIdPacienteAndActivoTrue(id)) {
            throw new ReglaNegocioException("No se puede desactivar el paciente porque tiene un registro activo en emergencias.");
        }

        if (cuentasRepository.existsByIdPacienteAndActivoTrue(id)) {
            throw new ReglaNegocioException("No se puede desactivar el paciente porque tiene cuentas hospitalarias activas.");
        }

        // Borrado lógico sistemático
        existente.setActivo(false);
        repository.save(existente);
        log.info("Paciente {} desactivado", id);
    }

    private Paciente buscarOLanzar(Long id) {
        return repository.findByIdPacienteAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente " + id + " no encontrado"));
    }

    private void validarReglasDeNegocio(PacienteRequest r, Long idActualizacion) {
        if (r == null) {
            throw new ReglaNegocioException("Los datos del paciente son obligatorios");
        }

        // RN-PAC-01: Unicidad de DPI
        if (idActualizacion == null) {
            if (repository.existsByDpiPaciente(r.dpiPaciente())) {
                throw new DuplicadoException("Ya existe un paciente con el DPI " + r.dpiPaciente());
            }
        } else {
            if (repository.existsByDpiPacienteAndIdPacienteNot(r.dpiPaciente(), idActualizacion)) {
                throw new DuplicadoException("Ya existe otro paciente con el DPI " + r.dpiPaciente());
            }
        }

        // RN-PAC-03: Coherencia de fecha de nacimiento
        if (r.fechaNacimiento() != null) {
            if (!r.fechaNacimiento().isBefore(LocalDate.now())) {
                throw new ReglaNegocioException("La fecha de nacimiento debe ser una fecha en el pasado");
            }
            if (r.fechaNacimiento().isBefore(LocalDate.now().minusYears(130))) {
                throw new ReglaNegocioException("La fecha de nacimiento no puede ser anterior a 130 años");
            }
        }

        // RN-PAC-05: Unicidad de correo electrónico si se proporciona
        if (r.correoElectronico() != null && !r.correoElectronico().trim().isEmpty()) {
            String correo = r.correoElectronico().trim();
            if (idActualizacion == null) {
                if (repository.existsByCorreoElectronico(correo)) {
                    throw new DuplicadoException("Ya existe un paciente registrado con el correo electrónico " + correo);
                }
            } else {
                if (repository.existsByCorreoElectronicoAndIdPacienteNot(correo, idActualizacion)) {
                    throw new DuplicadoException("Ya existe otro paciente registrado con el correo electrónico " + correo);
                }
            }
        }

        // RN-PAC-06: Validación de Alergia activa
        if (r.idAlergias() != null) {
            Alergias alergia = alergiasRepository.findById(r.idAlergias())
                    .orElseThrow(() -> new ReglaNegocioException("La alergia con ID " + r.idAlergias() + " no existe"));
            if (!Boolean.TRUE.equals(alergia.getActivo())) {
                throw new ReglaNegocioException("La alergia seleccionada se encuentra inactiva");
            }
        }

        // RN-PAC-07: Validación de Tipo de Sangre activo
        if (r.idTipoDeSangre() != null) {
            TipoSangre tipoSangre = tipoSangreRepository.findById(r.idTipoDeSangre())
                    .orElseThrow(() -> new ReglaNegocioException("El tipo de sangre con ID " + r.idTipoDeSangre() + " no existe"));
            if (!Boolean.TRUE.equals(tipoSangre.getActivo())) {
                throw new ReglaNegocioException("El tipo de sangre seleccionado se encuentra inactivo");
            }
        }
    }
}
