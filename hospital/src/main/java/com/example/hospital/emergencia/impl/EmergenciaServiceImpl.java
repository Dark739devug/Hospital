package com.example.hospital.emergencia.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.hospital.emergencia.Emergencia;
import com.example.hospital.shared.exception.DuplicadoException;
import com.example.hospital.shared.exception.RecursoNoEncontradoException;
import com.example.hospital.shared.exception.ReglaNegocioException;
import com.example.hospital.emergencia.EmergenciaRepository;
import com.example.hospital.emergencia.EmergenciaService;
import com.example.hospital.emergencia.dto.EmergenciaDTO;
import com.example.hospital.habitacion.Habitacion;
import com.example.hospital.habitacion.HabitacionRepository;
import com.example.hospital.medico.Medico;
import com.example.hospital.medico.MedicoRepository;
import com.example.hospital.paciente.Paciente;
import com.example.hospital.paciente.PacienteRepository;
import com.example.hospital.procedimientosemergencia.ProcedimientosEmergenciaRepository;
import com.example.hospital.triaje.Triaje;
import com.example.hospital.triaje.TriajeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmergenciaServiceImpl implements EmergenciaService {

    private static final Logger log = LoggerFactory.getLogger(EmergenciaServiceImpl.class);

    private final EmergenciaRepository emergenciaRepository;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final HabitacionRepository habitacionRepository;
    private final TriajeRepository triajeRepository;
    private final ProcedimientosEmergenciaRepository procedimientosEmergenciaRepository;

    public EmergenciaServiceImpl(EmergenciaRepository emergenciaRepository,
                                 PacienteRepository pacienteRepository,
                                 MedicoRepository medicoRepository,
                                 HabitacionRepository habitacionRepository,
                                 TriajeRepository triajeRepository,
                                 ProcedimientosEmergenciaRepository procedimientosEmergenciaRepository) {
        this.emergenciaRepository = emergenciaRepository;
        this.pacienteRepository = pacienteRepository;
        this.medicoRepository = medicoRepository;
        this.habitacionRepository = habitacionRepository;
        this.triajeRepository = triajeRepository;
        this.procedimientosEmergenciaRepository = procedimientosEmergenciaRepository;
    }

    private EmergenciaDTO mapToDTO(Emergencia e) {
        return new EmergenciaDTO(
                e.getIdEmergencia(),
                e.getNombreTemporal(),
                e.getFechaHoraIngreso(),
                e.getMotivo(),
                e.getSignosVitales(),
                e.getFechaHoraAlta(),
                e.getIdPaciente(),
                e.getIdHabitacion(),
                e.getIdMedico(),
                e.getIdTriaje(),
                e.getActivo()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmergenciaDTO> findAll() {
        return emergenciaRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmergenciaDTO> findActivas() {
        return emergenciaRepository.findByActivoTrue()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmergenciaDTO> buscar(Long idPaciente, boolean soloActivas) {
        return emergenciaRepository.findAll()
                .stream()
                .filter(e -> idPaciente == null || idPaciente.equals(e.getIdPaciente()))
                .filter(e -> !soloActivas || Boolean.TRUE.equals(e.getActivo()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EmergenciaDTO findById(Long id) {
        Emergencia entity = emergenciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el registro de emergencia con ID: " + id));
        return mapToDTO(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmergenciaDTO> findByPaciente(Long idPaciente) {
        if (idPaciente == null) {
            throw new ReglaNegocioException("El ID del paciente no puede ser nulo.");
        }
        return emergenciaRepository.findByIdPacienteAndActivoTrue(idPaciente)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EmergenciaDTO crear(EmergenciaDTO dto) {
        validarReglasDeNegocio(dto);

        Emergencia entity = new Emergencia();
        entity.setNombreTemporal(dto.nombreTemporal() != null ? dto.nombreTemporal().trim() : null);
        // RN-EM-07: Si no se especifica fecha de ingreso, el servidor la asigna automáticamente
        LocalDateTime fechaIngreso = dto.fechaHoraIngreso() != null ? dto.fechaHoraIngreso() : LocalDateTime.now();
        entity.setFechaHoraIngreso(fechaIngreso);
        entity.setMotivo(dto.motivo().trim());
        entity.setSignosVitales(dto.signosVitales() != null ? dto.signosVitales().trim() : null);
        // RN-EM-08: el alta solo se registra con PATCH /dar-alta; un ingreso nuevo nace sin alta
        entity.setFechaHoraAlta(null);
        entity.setIdPaciente(dto.idPaciente());
        entity.setIdHabitacion(dto.idHabitacion());
        entity.setIdMedico(dto.idMedico());
        entity.setIdTriaje(dto.idTriaje());
        entity.setActivo(dto.activo() != null ? dto.activo() : true);

        Emergencia guardada = emergenciaRepository.save(entity);
        log.info("Ingreso a emergencia {} registrado: paciente {}, triaje {}", guardada.getIdEmergencia(), guardada.getIdPaciente() != null ? guardada.getIdPaciente() : "sin identificar", guardada.getIdTriaje());
        return mapToDTO(guardada);
    }

    @Override
    @Transactional
    public EmergenciaDTO actualizar(EmergenciaDTO dto, Long id) {
        if (id == null) {
            throw new ReglaNegocioException("El ID de la emergencia es obligatorio para la actualización.");
        }

        Emergencia existente = emergenciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el registro de emergencia con ID: " + id));

        validarReglasDeNegocio(dto);

        existente.setNombreTemporal(dto.nombreTemporal() != null ? dto.nombreTemporal().trim() : null);
        if (dto.fechaHoraIngreso() != null) {
            if (existente.getFechaHoraAlta() != null && existente.getFechaHoraAlta().isBefore(dto.fechaHoraIngreso())) {
                throw new ReglaNegocioException("La fecha y hora de ingreso no puede ser posterior a la fecha de alta ("
                        + existente.getFechaHoraAlta() + ").");
            }
            existente.setFechaHoraIngreso(dto.fechaHoraIngreso());
        }
        existente.setMotivo(dto.motivo().trim());
        existente.setSignosVitales(dto.signosVitales() != null ? dto.signosVitales().trim() : null);
        // RN-EM-08: fechaHoraAlta NO se modifica aquí (se conserva); el alta se gestiona con PATCH /dar-alta
        existente.setIdPaciente(dto.idPaciente());
        existente.setIdHabitacion(dto.idHabitacion());
        existente.setIdMedico(dto.idMedico());
        existente.setIdTriaje(dto.idTriaje());
        if (dto.activo() != null) {
            existente.setActivo(dto.activo());
        }

        Emergencia actualizada = emergenciaRepository.save(existente);
        log.info("Emergencia {} actualizada", id);
        return mapToDTO(actualizada);
    }

    @Override
    @Transactional
    public EmergenciaDTO darAlta(Long id, LocalDateTime fechaHoraAlta) {
        if (id == null) {
            throw new ReglaNegocioException("El ID de la emergencia no puede ser nulo.");
        }

        Emergencia existente = emergenciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el registro de emergencia con ID: " + id));

        if (!Boolean.TRUE.equals(existente.getActivo())) {
            throw new ReglaNegocioException("No se puede dar de alta una emergencia inactiva.");
        }

        // Conflicto de estado: el alta ya fue registrada (409)
        if (existente.getFechaHoraAlta() != null) {
            throw new DuplicadoException("La emergencia ya cuenta con fecha y hora de alta registrada.");
        }

        LocalDateTime altaEfectiva = fechaHoraAlta != null ? fechaHoraAlta : LocalDateTime.now();

        if (existente.getFechaHoraIngreso() != null && altaEfectiva.isBefore(existente.getFechaHoraIngreso())) {
            throw new ReglaNegocioException("La fecha y hora de alta no puede ser anterior a la fecha de ingreso ("
                    + existente.getFechaHoraIngreso() + ").");
        }

        existente.setFechaHoraAlta(altaEfectiva);
        Emergencia guardada = emergenciaRepository.save(existente);
        log.info("Alta de la emergencia {} el {}", id, guardada.getFechaHoraAlta());
        return mapToDTO(guardada);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (id == null) {
            throw new ReglaNegocioException("El ID no puede ser nulo.");
        }

        Emergencia existente = emergenciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el registro de emergencia con ID: " + id));

        // RN-EM-09: Integridad con procedimientos de emergencia (conflicto con dependientes: 409)
        if (procedimientosEmergenciaRepository.existsByIdEmergenciaAndActivoTrue(id)) {
            throw new IllegalStateException("No se puede eliminar la emergencia porque tiene procedimientos médicos de emergencia activos asociados.");
        }

        // Borrado lógico sistemático
        existente.setActivo(false);
        emergenciaRepository.save(existente);
        log.info("Emergencia {} desactivada", id);
    }

    /**
     * Reglas de negocio del ingreso. El motivo, el nivel de triaje y los tamaños los valida Bean
     * Validation en EmergenciaDTO (400 con detalles). Las referencias inexistentes o inactivas en el
     * cuerpo responden 422.
     */
    private void validarReglasDeNegocio(EmergenciaDTO dto) {
        if (dto == null) {
            throw new ReglaNegocioException("Los datos de la emergencia son obligatorios.");
        }

        // RN-EM-02: Identificación del paciente
        boolean tienePacienteRegistrado = dto.idPaciente() != null;
        boolean tieneNombreTemporal = dto.nombreTemporal() != null && !dto.nombreTemporal().trim().isEmpty();
        if (!tienePacienteRegistrado && !tieneNombreTemporal) {
            throw new ReglaNegocioException("Debe especificarse un paciente registrado o un nombre temporal para el paciente no identificado.");
        }

        // RN-EM-03: Validación de paciente registrado
        if (dto.idPaciente() != null) {
            Paciente paciente = pacienteRepository.findById(dto.idPaciente())
                    .orElseThrow(() -> new ReglaNegocioException("El paciente con ID " + dto.idPaciente() + " no existe."));
            if (!Boolean.TRUE.equals(paciente.getActivo())) {
                throw new ReglaNegocioException("El paciente con ID " + dto.idPaciente() + " se encuentra inactivo.");
            }
        }

        // RN-EM-04: Validación de médico asignado
        if (dto.idMedico() != null) {
            Medico medico = medicoRepository.findById(dto.idMedico())
                    .orElseThrow(() -> new ReglaNegocioException("El médico con ID " + dto.idMedico() + " no existe."));
            if (!Boolean.TRUE.equals(medico.getEstado())) {
                throw new ReglaNegocioException("El médico con ID " + dto.idMedico() + " se encuentra inactivo.");
            }
        }

        // RN-EM-05: Validación de habitación / box
        if (dto.idHabitacion() != null) {
            Habitacion habitacion = habitacionRepository.findById(dto.idHabitacion())
                    .orElseThrow(() -> new ReglaNegocioException("La habitación con ID " + dto.idHabitacion() + " no existe."));
            if (!Boolean.TRUE.equals(habitacion.getActivo())) {
                throw new ReglaNegocioException("La habitación con ID " + dto.idHabitacion() + " se encuentra inactiva.");
            }
        }

        // RN-EM-06: Validación de triaje (obligatorio: lo exige Bean Validation)
        Triaje triaje = triajeRepository.findById(dto.idTriaje())
                .orElseThrow(() -> new ReglaNegocioException("El nivel de triaje con ID " + dto.idTriaje() + " no existe."));
        if (!Boolean.TRUE.equals(triaje.getActivo())) {
            throw new ReglaNegocioException("El nivel de triaje con ID " + dto.idTriaje() + " se encuentra inactivo.");
        }

        // RN-EM-07: Fecha de ingreso no futura (con tolerancia de 5 minutos)
        if (dto.fechaHoraIngreso() != null && dto.fechaHoraIngreso().isAfter(LocalDateTime.now().plusMinutes(5))) {
            throw new ReglaNegocioException("La fecha y hora de ingreso a emergencias no puede ser una fecha futura.");
        }
    }
}
