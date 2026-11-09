package com.example.hospital.habitacion.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.hospital.emergencia.EmergenciaRepository;
import com.example.hospital.shared.catalogos.EstadosCatalogo;
import com.example.hospital.shared.exception.DuplicadoException;
import com.example.hospital.shared.exception.RecursoNoEncontradoException;
import com.example.hospital.estadohabitacion.EstadoHabitacion;
import com.example.hospital.estadohabitacion.EstadoHabitacionRepository;
import com.example.hospital.habitacion.Habitacion;
import com.example.hospital.habitacion.HabitacionRepository;
import com.example.hospital.habitacion.HabitacionService;
import com.example.hospital.habitacion.dto.HabitacionDTO;
import com.example.hospital.hospitalizaciones.HospitalizacionesRepository;
import com.example.hospital.shared.exception.ReglaNegocioException;
import com.example.hospital.tiposhabitacion.TiposHabitacion;
import com.example.hospital.tiposhabitacion.TiposHabitacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HabitacionServiceImpl implements HabitacionService {

    private static final Logger log = LoggerFactory.getLogger(HabitacionServiceImpl.class);

    private final HabitacionRepository habitacionRepository;
    private final EstadoHabitacionRepository estadoHabitacionRepository;
    private final TiposHabitacionRepository tiposHabitacionRepository;
    private final HospitalizacionesRepository hospitalizacionesRepository;
    private final EmergenciaRepository emergenciaRepository;
    // Los estados (Disponible, Ocupada, Mantenimiento, Activa) se resuelven por nombre, no por id fijo
    private final EstadosCatalogo estados;

    public HabitacionServiceImpl(HabitacionRepository habitacionRepository,
                                 EstadoHabitacionRepository estadoHabitacionRepository,
                                 TiposHabitacionRepository tiposHabitacionRepository,
                                 HospitalizacionesRepository hospitalizacionesRepository,
                                 EmergenciaRepository emergenciaRepository,
                                 EstadosCatalogo estados) {
        this.habitacionRepository = habitacionRepository;
        this.estadoHabitacionRepository = estadoHabitacionRepository;
        this.tiposHabitacionRepository = tiposHabitacionRepository;
        this.hospitalizacionesRepository = hospitalizacionesRepository;
        this.emergenciaRepository = emergenciaRepository;
        this.estados = estados;
    }

    private HabitacionDTO mapToDTO(Habitacion h) {
        return new HabitacionDTO(
                h.getIdHabitacion(),
                h.getNumero(),
                h.getPiso(),
                h.getCapacidad(),
                h.getIdTipoHabitacion(),
                h.getIdEstadoHabitacion(),
                h.getActivo()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<HabitacionDTO> findAll() {
        return habitacionRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HabitacionDTO> findActivas() {
        return habitacionRepository.findByActivoTrue()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HabitacionDTO> buscar(Integer piso, Long idTipoHabitacion, Long idEstadoHabitacion,
                                      boolean soloActivas, boolean soloDisponibles) {
        Long disponible = soloDisponibles ? estados.habDisponible() : null;
        return habitacionRepository.findAll()
                .stream()
                .filter(h -> piso == null || piso.equals(h.getPiso()))
                .filter(h -> idTipoHabitacion == null || idTipoHabitacion.equals(h.getIdTipoHabitacion()))
                .filter(h -> idEstadoHabitacion == null || idEstadoHabitacion.equals(h.getIdEstadoHabitacion()))
                // soloDisponibles implica habitaciones activas en estado Disponible
                .filter(h -> !(soloActivas || soloDisponibles) || Boolean.TRUE.equals(h.getActivo()))
                .filter(h -> disponible == null || disponible.equals(h.getIdEstadoHabitacion()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public HabitacionDTO findById(Long id) {
        Habitacion entity = habitacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la habitación con ID: " + id));
        return mapToDTO(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public HabitacionDTO findByNumero(String numero) {
        if (numero == null || numero.trim().isEmpty()) {
            throw new ReglaNegocioException("El número de habitación es obligatorio");
        }
        Habitacion entity = habitacionRepository.findByNumero(numero.trim())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la habitación con número: " + numero));
        return mapToDTO(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HabitacionDTO> findDisponibles() {
        return habitacionRepository.findByIdEstadoHabitacionAndActivoTrue(estados.habDisponible())
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public HabitacionDTO crear(HabitacionDTO dto) {
        validarReglasDeNegocio(dto, null);
        // Una habitación nueva no tiene pacientes: no puede nacer Ocupada (ni, por tanto, requerir otras reglas)
        validarCambioDeEstado(null, dto.idEstadoHabitacion(), dto.capacidad());

        Habitacion entity = new Habitacion();
        entity.setNumero(dto.numero().trim());
        entity.setPiso(dto.piso());
        entity.setCapacidad(dto.capacidad());
        entity.setIdTipoHabitacion(dto.idTipoHabitacion());
        entity.setIdEstadoHabitacion(dto.idEstadoHabitacion());
        entity.setActivo(dto.activo() != null ? dto.activo() : true);

        Habitacion guardada = habitacionRepository.save(entity);
        return mapToDTO(guardada);
    }

    @Override
    @Transactional
    public HabitacionDTO actualizar(HabitacionDTO dto, Long id) {
        if (id == null) {
            throw new ReglaNegocioException("El ID de habitación es obligatorio para la actualización");
        }

        Habitacion existente = habitacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la habitación con ID: " + id));

        validarReglasDeNegocio(dto, id);
        // Si cambia el estado (o la capacidad), se aplican las mismas reglas que en PATCH /estado
        if (!dto.idEstadoHabitacion().equals(existente.getIdEstadoHabitacion())
                || !dto.capacidad().equals(existente.getCapacidad())) {
            validarCambioDeEstado(id, dto.idEstadoHabitacion(), dto.capacidad());
        }

        existente.setNumero(dto.numero().trim());
        existente.setPiso(dto.piso());
        existente.setCapacidad(dto.capacidad());
        existente.setIdTipoHabitacion(dto.idTipoHabitacion());
        existente.setIdEstadoHabitacion(dto.idEstadoHabitacion());
        if (dto.activo() != null) {
            existente.setActivo(dto.activo());
        }

        Habitacion actualizada = habitacionRepository.save(existente);
        return mapToDTO(actualizada);
    }

    @Override
    @Transactional
    public HabitacionDTO cambiarEstado(Long id, Long nuevoEstadoId) {
        if (id == null || nuevoEstadoId == null) {
            throw new ReglaNegocioException("El ID de la habitación y el nuevo estado son requeridos");
        }

        Habitacion existente = habitacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la habitación con ID: " + id));

        EstadoHabitacion estado = estadoHabitacionRepository.findById(nuevoEstadoId)
                .orElseThrow(() -> new ReglaNegocioException("El estado de habitación con ID " + nuevoEstadoId + " no existe"));
        if (!Boolean.TRUE.equals(estado.getActivo())) {
            throw new ReglaNegocioException("El estado de habitación seleccionado se encuentra inactivo");
        }

        validarCambioDeEstado(id, nuevoEstadoId, existente.getCapacidad());

        existente.setIdEstadoHabitacion(nuevoEstadoId);
        Habitacion guardada = habitacionRepository.save(existente);
        log.info("Habitación {} cambió al estado {}", id, nuevoEstadoId);
        return mapToDTO(guardada);
    }

    /**
     * Reglas del estado según la ocupación real (hospitalizaciones Activas en la habitación):
     * - Disponible: no si la habitación está llena (ocupación >= capacidad).
     * - Ocupada: no si no hay pacientes (ocupación = 0).
     * - Mantenimiento: no si hay pacientes (ocupación > 0).
     *
     * @param idHabitacion null al crear (ocupación 0)
     */
    private void validarCambioDeEstado(Long idHabitacion, Long nuevoEstadoId, Integer capacidad) {
        long ocupacion = idHabitacion == null
                ? 0
                : hospitalizacionesRepository.countByIdHabitacionAndIdEstadoHosp(idHabitacion, estados.hospActiva());
        int cap = capacidad != null ? capacidad : 1;

        if (nuevoEstadoId.equals(estados.habDisponible()) && ocupacion >= cap) {
            throw new ReglaNegocioException("No se puede marcar como Disponible una habitación llena (" + ocupacion + " de " + cap + " camas ocupadas)");
        }
        if (nuevoEstadoId.equals(estados.habOcupada()) && ocupacion == 0) {
            throw new ReglaNegocioException("No se puede marcar como Ocupada una habitación sin pacientes hospitalizados");
        }
        if (nuevoEstadoId.equals(estados.habMantenimiento()) && ocupacion > 0) {
            throw new ReglaNegocioException("No se puede pasar a Mantenimiento una habitación con pacientes hospitalizados (" + ocupacion + ")");
        }
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (id == null) {
            throw new ReglaNegocioException("El ID no puede ser nulo");
        }

        Habitacion existente = habitacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la habitación con ID: " + id));

        // RN-HAB-06: Integridad clínica antes de desactivar
        long hospitalizados = hospitalizacionesRepository.countByIdHabitacionAndIdEstadoHosp(id, estados.hospActiva());
        if (hospitalizados > 0) {
            throw new ReglaNegocioException("No se puede desactivar la habitación porque tiene pacientes actualmente hospitalizados en ella.");
        }
        if (!emergenciaRepository.findByIdHabitacionAndActivoTrue(id).isEmpty()) {
            throw new ReglaNegocioException("No se puede desactivar la habitación porque tiene ingresos de emergencia activos asignados.");
        }

        // RN-HAB-07: Borrado lógico sistemático
        existente.setActivo(false);
        habitacionRepository.save(existente);
        log.info("Habitación {} desactivada", id);
    }

    /**
     * Número, piso, capacidad (> 0), tipo y estado obligatorios: Bean Validation en HabitacionDTO
     * (400 con detalles). Aquí quedan las reglas de negocio: número único (409) y tipo/estado existentes y activos (422).
     */
    private void validarReglasDeNegocio(HabitacionDTO dto, Long idActualizacion) {
        if (dto == null) {
            throw new ReglaNegocioException("Los datos de la habitación son requeridos");
        }

        // RN-HAB-01: Número único
        String num = dto.numero().trim();
        if (idActualizacion == null) {
            if (habitacionRepository.existsByNumero(num)) {
                throw new DuplicadoException("Ya existe una habitación registrada con el número: " + num);
            }
        } else {
            if (habitacionRepository.existsByNumeroAndIdHabitacionNot(num, idActualizacion)) {
                throw new DuplicadoException("Ya existe otra habitación registrada con el número: " + num);
            }
        }

        // RN-HAB-04: Tipo de habitación existente y activo
        TiposHabitacion tipo = tiposHabitacionRepository.findById(dto.idTipoHabitacion())
                .orElseThrow(() -> new ReglaNegocioException("El tipo de habitación con ID " + dto.idTipoHabitacion() + " no existe"));
        if (!Boolean.TRUE.equals(tipo.getActivo())) {
            throw new ReglaNegocioException("El tipo de habitación seleccionado se encuentra inactivo");
        }

        // RN-HAB-05: Estado de habitación existente y activo
        EstadoHabitacion estado = estadoHabitacionRepository.findById(dto.idEstadoHabitacion())
                .orElseThrow(() -> new ReglaNegocioException("El estado de habitación con ID " + dto.idEstadoHabitacion() + " no existe"));
        if (!Boolean.TRUE.equals(estado.getActivo())) {
            throw new ReglaNegocioException("El estado de habitación seleccionado se encuentra inactivo");
        }
    }
}
