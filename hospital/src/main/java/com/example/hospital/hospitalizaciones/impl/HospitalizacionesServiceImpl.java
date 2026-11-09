package com.example.hospital.hospitalizaciones.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.hospital.clinicas.Clinicas;
import com.example.hospital.shared.catalogos.EstadosCatalogo;
import com.example.hospital.shared.exception.DuplicadoException;
import com.example.hospital.shared.exception.RecursoNoEncontradoException;
import com.example.hospital.shared.exception.ReglaNegocioException;
import com.example.hospital.clinicas.ClinicasRepository;
import com.example.hospital.habitacion.Habitacion;
import com.example.hospital.habitacion.HabitacionRepository;
import com.example.hospital.hospitalizaciones.Hospitalizaciones;
import com.example.hospital.hospitalizaciones.HospitalizacionesRepository;
import com.example.hospital.hospitalizaciones.HospitalizacionesService;
import com.example.hospital.estadoshospitalizacion.EstadosHospitalizacion;
import com.example.hospital.estadoshospitalizacion.EstadosHospitalizacionRepository;
import com.example.hospital.hospitalizaciones.dto.HospitalizacionResumenDTO;
import com.example.hospital.hospitalizaciones.dto.HospitalizacionesDTO;
import com.example.hospital.medico.Medico;
import com.example.hospital.medico.MedicoRepository;
import com.example.hospital.paciente.Paciente;
import com.example.hospital.paciente.PacienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HospitalizacionesServiceImpl implements HospitalizacionesService {

    private static final Logger log = LoggerFactory.getLogger(HospitalizacionesServiceImpl.class);

    private final HospitalizacionesRepository hospitalizacionesRepository;
    private final PacienteRepository pacienteRepository;
    private final HabitacionRepository habitacionRepository;
    private final MedicoRepository medicoRepository;
    private final ClinicasRepository clinicasRepository;
    // Los estados (Activa, Alta, Disponible, Ocupada, Mantenimiento) se resuelven por nombre, no por id fijo
    private final EstadosCatalogo estados;
    private final EstadosHospitalizacionRepository estadosHospitalizacionRepository;

    public HospitalizacionesServiceImpl(
            HospitalizacionesRepository hospitalizacionesRepository,
            PacienteRepository pacienteRepository,
            HabitacionRepository habitacionRepository,
            MedicoRepository medicoRepository,
            ClinicasRepository clinicasRepository,
            EstadosCatalogo estados,
            EstadosHospitalizacionRepository estadosHospitalizacionRepository) {
        this.hospitalizacionesRepository = hospitalizacionesRepository;
        this.pacienteRepository = pacienteRepository;
        this.habitacionRepository = habitacionRepository;
        this.medicoRepository = medicoRepository;
        this.clinicasRepository = clinicasRepository;
        this.estados = estados;
        this.estadosHospitalizacionRepository = estadosHospitalizacionRepository;
    }

    private HospitalizacionesDTO mapToDTO(Hospitalizaciones h) {
        return new HospitalizacionesDTO(
                h.getIdHospitalizacion(),
                h.getIdPaciente(),
                h.getIdHabitacion(),
                h.getIdMedicoResponsable(),
                h.getIdClinica(),
                h.getFechaIngreso(),
                h.getFechaAlta(),
                h.getMotivoIngreso(),
                h.getDiagnosticoIngreso(),
                h.getIdEstadoHosp(),
                h.getActivo()
        );
    }

    private Hospitalizaciones mapToEntity(HospitalizacionesDTO dto) {
        Hospitalizaciones h = new Hospitalizaciones();
        h.setIdPaciente(dto.idPaciente());
        h.setIdHabitacion(dto.idHabitacion());
        h.setIdMedicoResponsable(dto.idMedicoResponsable());
        h.setIdClinica(dto.idClinica());
        h.setFechaIngreso(dto.fechaIngreso());
        h.setFechaAlta(dto.fechaAlta());
        h.setMotivoIngreso(dto.motivoIngreso().trim());
        h.setDiagnosticoIngreso(dto.diagnosticoIngreso());
        h.setIdEstadoHosp(dto.idEstadoHosp());
        h.setActivo(dto.activo());
        return h;
    }

    @Override
    public List<HospitalizacionesDTO> findAll() {
        return hospitalizacionesRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public HospitalizacionesDTO findById(Long id) {
        Hospitalizaciones h = hospitalizacionesRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la hospitalización con ID: " + id));
        return mapToDTO(h);
    }

    /*
     * Convención de errores: los datos obligatorios y su formato los valida Bean Validation en
     * HospitalizacionesDTO (400 con detalles); las referencias inexistentes o inactivas y las reglas de
     * negocio responden 422; un conflicto con el estado actual (paciente ya hospitalizado, alta repetida,
     * desactivar una hospitalización Activa) responde 409.
     */
    @Override
    @Transactional
    public HospitalizacionesDTO registrar(HospitalizacionesDTO dto) {
        if (dto == null) {
            throw new ReglaNegocioException("Los datos de la hospitalización son requeridos");
        }
        Long activa = estados.hospActiva();

        // RN-01: El paciente debe existir y estar activo
        Paciente paciente = pacienteRepository.findById(dto.idPaciente())
                .orElseThrow(() -> new ReglaNegocioException("El paciente con ID " + dto.idPaciente() + " no existe"));
        if (Boolean.FALSE.equals(paciente.getActivo())) {
            throw new ReglaNegocioException("El paciente seleccionado se encuentra inactivo");
        }

        // RN-02: Un paciente no puede tener dos hospitalizaciones activas simultáneamente (conflicto: 409)
        if (hospitalizacionesRepository.existsByIdPacienteAndIdEstadoHosp(dto.idPaciente(), activa)) {
            throw new DuplicadoException("El paciente ya cuenta con una hospitalización Activa");
        }

        // RN-03: El médico responsable debe existir y estar activo
        Medico medico = medicoRepository.findById(dto.idMedicoResponsable())
                .orElseThrow(() -> new ReglaNegocioException("El médico con ID " + dto.idMedicoResponsable() + " no existe"));
        if (Boolean.FALSE.equals(medico.getEstado())) {
            throw new ReglaNegocioException("El médico responsable se encuentra inactivo");
        }

        // RN-04: Toda hospitalización debe tener una habitación asignada, existente y activa
        Habitacion habitacion = habitacionRepository.findById(dto.idHabitacion())
                .orElseThrow(() -> new ReglaNegocioException("La habitación con ID " + dto.idHabitacion() + " no existe"));
        if (Boolean.FALSE.equals(habitacion.getActivo())) {
            throw new ReglaNegocioException("La habitación seleccionada se encuentra inactiva");
        }

        // RN-06 / RN-16: No se pueden utilizar habitaciones en mantenimiento
        if (estados.habMantenimiento().equals(habitacion.getIdEstadoHabitacion())) {
            throw new ReglaNegocioException("La habitación se encuentra en mantenimiento y no puede recibir pacientes");
        }

        // RN-05, RN-11, RN-12, RN-13: Verificar capacidad y ocupación actual
        long ocupacionActual = hospitalizacionesRepository.countByIdHabitacionAndIdEstadoHosp(dto.idHabitacion(), activa);
        int capacidad = habitacion.getCapacidad() != null ? habitacion.getCapacidad() : 1;
        if (ocupacionActual >= capacidad) {
            throw new ReglaNegocioException("La habitación " + habitacion.getNumero() + " ya alcanzó su capacidad máxima (" + capacidad + ")");
        }

        // RN-07: La fecha de ingreso no puede ser posterior a la fecha actual
        LocalDateTime fechaIngreso = dto.fechaIngreso() != null ? dto.fechaIngreso() : LocalDateTime.now();
        if (fechaIngreso.isAfter(LocalDateTime.now())) {
            throw new ReglaNegocioException("La fecha de ingreso no puede ser posterior a la fecha actual");
        }

        // RN-08: El motivo de ingreso es obligatorio (lo valida Bean Validation en el DTO)

        // RN-09: La clínica asignada, cuando exista, debe estar activa
        if (dto.idClinica() != null) {
            validarClinica(dto.idClinica());
        }

        // Construir entidad
        Hospitalizaciones h = mapToEntity(dto);
        h.setFechaIngreso(fechaIngreso);
        // RN-10: Toda hospitalización nueva debe iniciar con estado Activa, sin alta
        h.setIdEstadoHosp(activa);
        h.setFechaAlta(null);
        h.setActivo(true);

        Hospitalizaciones guardada = hospitalizacionesRepository.save(h);
        log.info("Hospitalización {} registrada: paciente {}, habitación {}, médico {}", guardada.getIdHospitalizacion(), guardada.getIdPaciente(), guardada.getIdHabitacion(), guardada.getIdMedicoResponsable());

        // RN-14: Si con este ingreso la habitación alcanza su capacidad máxima, actualizar a Ocupada
        if ((ocupacionActual + 1) >= capacidad) {
            habitacion.setIdEstadoHabitacion(estados.habOcupada());
            habitacionRepository.save(habitacion);
        }

        return mapToDTO(guardada);
    }

    private void validarClinica(Long idClinica) {
        Clinicas clinica = clinicasRepository.findById(idClinica)
                .orElseThrow(() -> new ReglaNegocioException("La clínica con ID " + idClinica + " no existe"));
        if (Boolean.FALSE.equals(clinica.getEstadoClinica())) {
            throw new ReglaNegocioException("La clínica asignada no se encuentra activa");
        }
    }

    /**
     * PUT = reemplazo de los campos editables: médico responsable, habitación (traslado), clínica,
     * motivo y diagnóstico de ingreso. Un null en clínica o diagnóstico de ingreso los borra.
     * No se modifican el paciente, la fecha de ingreso, la fecha de alta ni el estado.
     */
    @Override
    @Transactional
    public HospitalizacionesDTO actualizar(HospitalizacionesDTO dto, Long id) {
        if (id == null) {
            throw new ReglaNegocioException("El ID de hospitalización es obligatorio");
        }

        Hospitalizaciones existente = hospitalizacionesRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la hospitalización con ID: " + id));
        Long activa = estados.hospActiva();

        // RN-17 / RN-22: Solo se podrán modificar hospitalizaciones que estén activas
        if (!activa.equals(existente.getIdEstadoHosp())) {
            throw new ReglaNegocioException("No se permite modificar una hospitalización finalizada o no activa");
        }

        // RN-21: No se podrá modificar el paciente asociado a una hospitalización ya registrada
        if (!dto.idPaciente().equals(existente.getIdPaciente())) {
            throw new ReglaNegocioException("No se permite modificar el paciente asociado a una hospitalización registrada");
        }

        // RN-18: El médico responsable podrá cambiarse únicamente por otro médico activo
        if (!dto.idMedicoResponsable().equals(existente.getIdMedicoResponsable())) {
            Medico nuevoMedico = medicoRepository.findById(dto.idMedicoResponsable())
                    .orElseThrow(() -> new ReglaNegocioException("El médico con ID " + dto.idMedicoResponsable() + " no existe"));
            if (Boolean.FALSE.equals(nuevoMedico.getEstado())) {
                throw new ReglaNegocioException("El nuevo médico responsable no está activo");
            }
            existente.setIdMedicoResponsable(dto.idMedicoResponsable());
        }

        // RN-19 / RN-20: Traslado a otra habitación
        if (!dto.idHabitacion().equals(existente.getIdHabitacion())) {
            Habitacion nuevaHabitacion = habitacionRepository.findById(dto.idHabitacion())
                    .orElseThrow(() -> new ReglaNegocioException("La nueva habitación no existe"));
            if (Boolean.FALSE.equals(nuevaHabitacion.getActivo())) {
                throw new ReglaNegocioException("La nueva habitación se encuentra inactiva");
            }
            if (estados.habMantenimiento().equals(nuevaHabitacion.getIdEstadoHabitacion())) {
                throw new ReglaNegocioException("La nueva habitación está en mantenimiento");
            }
            long ocupacionNueva = hospitalizacionesRepository.countByIdHabitacionAndIdEstadoHosp(dto.idHabitacion(), activa);
            int capacidadNueva = nuevaHabitacion.getCapacidad() != null ? nuevaHabitacion.getCapacidad() : 1;
            if (ocupacionNueva >= capacidadNueva) {
                throw new ReglaNegocioException("La nueva habitación ya alcanzó su capacidad máxima");
            }

            // Liberar o ajustar la habitación anterior
            Habitacion habitacionAnterior = habitacionRepository.findById(existente.getIdHabitacion()).orElse(null);
            if (habitacionAnterior != null) {
                long ocupacionAnterior = hospitalizacionesRepository.countByIdHabitacionAndIdEstadoHosp(habitacionAnterior.getIdHabitacion(), activa);
                // Si la ocupación anterior queda en 1 (que era este paciente que se va), pasará a Disponible
                if (ocupacionAnterior <= 1 && !estados.habMantenimiento().equals(habitacionAnterior.getIdEstadoHabitacion())) {
                    habitacionAnterior.setIdEstadoHabitacion(estados.habDisponible());
                    habitacionRepository.save(habitacionAnterior);
                }
            }

            // Actualizar la nueva habitación si se llena
            if ((ocupacionNueva + 1) >= capacidadNueva) {
                nuevaHabitacion.setIdEstadoHabitacion(estados.habOcupada());
                habitacionRepository.save(nuevaHabitacion);
            }
            existente.setIdHabitacion(dto.idHabitacion());
        }

        existente.setMotivoIngreso(dto.motivoIngreso().trim());
        // Reemplazo: null borra el diagnóstico de ingreso y la clínica
        existente.setDiagnosticoIngreso(dto.diagnosticoIngreso());
        if (dto.idClinica() != null) {
            validarClinica(dto.idClinica());
        }
        existente.setIdClinica(dto.idClinica());

        Hospitalizaciones actualizada = hospitalizacionesRepository.save(existente);
        log.info("Hospitalización {} actualizada (habitación {})", id, actualizada.getIdHabitacion());
        return mapToDTO(actualizada);
    }

    @Override
    @Transactional
    public HospitalizacionesDTO darAlta(Long id, LocalDateTime fechaAlta) {
        if (id == null) {
            throw new ReglaNegocioException("El ID de hospitalización es obligatorio");
        }

        Hospitalizaciones existente = hospitalizacionesRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la hospitalización con ID: " + id));

        // RN-23: Solo se podrá registrar el alta de una hospitalización activa
        // RN-25: No se podrá registrar dos veces el alta de la misma hospitalización (conflicto: 409)
        if (!estados.hospActiva().equals(existente.getIdEstadoHosp())) {
            throw new DuplicadoException("Solo se puede dar de alta una hospitalización que se encuentre Activa");
        }

        LocalDateTime fechaAltaEfectiva = fechaAlta != null ? fechaAlta : LocalDateTime.now();

        // RN-24: La fecha de alta no puede ser anterior a la fecha de ingreso
        if (existente.getFechaIngreso() != null && fechaAltaEfectiva.isBefore(existente.getFechaIngreso())) {
            throw new ReglaNegocioException("La fecha de alta no puede ser anterior a la fecha de ingreso");
        }

        // RN-27: Se cambia el estado a Alta (no podrá volver a Activa)
        existente.setIdEstadoHosp(estados.hospAlta());
        existente.setFechaAlta(fechaAltaEfectiva);

        Hospitalizaciones guardada = hospitalizacionesRepository.save(existente);
        log.info("Alta de la hospitalización {} el {}", id, guardada.getFechaAlta());

        // RN-15 / RN-26: Al registrar el alta, se deberá liberar la ocupación correspondiente de la habitación
        if (existente.getIdHabitacion() != null) {
            Habitacion habitacion = habitacionRepository.findById(existente.getIdHabitacion()).orElse(null);
            if (habitacion != null && !estados.habMantenimiento().equals(habitacion.getIdEstadoHabitacion())) {
                long ocupacionRestante = hospitalizacionesRepository.countByIdHabitacionAndIdEstadoHosp(habitacion.getIdHabitacion(), estados.hospActiva());
                if (ocupacionRestante == 0) {
                    habitacion.setIdEstadoHabitacion(estados.habDisponible());
                    habitacionRepository.save(habitacion);
                }
            }
        }

        // RN-29: El historial de hospitalización se conserva en base de datos
        return mapToDTO(guardada);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Hospitalizaciones existente = hospitalizacionesRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la hospitalización con ID: " + id));

        // Una hospitalización Activa mantiene ocupada la habitación y bloquea al paciente: primero hay que
        // darla de alta (conflicto con el estado actual: 409). Desactivar una con alta sí está permitido.
        if (estados.hospActiva().equals(existente.getIdEstadoHosp())) {
            throw new DuplicadoException("No se puede desactivar una hospitalización Activa; registre primero el alta del paciente");
        }

        // Borrado lógico para conservar historial (RN-29)
        existente.setActivo(false);
        hospitalizacionesRepository.save(existente);
        log.info("Hospitalización {} desactivada", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HospitalizacionesDTO> findActivas() {
        return hospitalizacionesRepository.findByIdEstadoHosp(estados.hospActiva())
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HospitalizacionResumenDTO> resumenPorPaciente(Long idPaciente) {
        if (!pacienteRepository.existsById(idPaciente)) {
            throw new RecursoNoEncontradoException("No existe el paciente con ID: " + idPaciente);
        }
        return hospitalizacionesRepository.findByIdPaciente(idPaciente).stream()
                .filter(h -> !Boolean.FALSE.equals(h.getActivo()))
                .sorted(java.util.Comparator.comparing(Hospitalizaciones::getFechaIngreso,
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .map(h -> new HospitalizacionResumenDTO(
                        h.getIdHospitalizacion(),
                        h.getFechaIngreso(),
                        h.getFechaAlta(),
                        h.getIdEstadoHosp() == null ? null : estadosHospitalizacionRepository.findById(h.getIdEstadoHosp())
                                .map(EstadosHospitalizacion::getDescripcion).orElse(null),
                        h.getIdHabitacion() == null ? null : habitacionRepository.findById(h.getIdHabitacion())
                                .map(Habitacion::getNumero).orElse(null)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HospitalizacionesDTO> findByPaciente(Long idPaciente) {
        if (!pacienteRepository.existsById(idPaciente)) {
            throw new RecursoNoEncontradoException("No existe el paciente con ID: " + idPaciente);
        }
        return hospitalizacionesRepository.findByIdPaciente(idPaciente)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
}
