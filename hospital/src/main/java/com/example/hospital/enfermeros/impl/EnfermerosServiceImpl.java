package com.example.hospital.enfermeros.impl;

import com.example.hospital.shared.exception.DuplicadoException;
import com.example.hospital.shared.exception.ReglaNegocioException;
import com.example.hospital.asignacionenfermeros.AsignacionEnfermerosRepository;
import com.example.hospital.shared.exception.RecursoNoEncontradoException;
import com.example.hospital.departamentos.Departamentos;
import com.example.hospital.departamentos.DepartamentosRepository;
import com.example.hospital.enfermeros.Enfermeros;
import com.example.hospital.enfermeros.EnfermerosRepository;
import com.example.hospital.enfermeros.EnfermerosService;
import com.example.hospital.enfermeros.dto.EnfermerosDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.hospital.usuarios.GeneradorCredencialesService;
import com.example.hospital.usuarios.dto.CredencialesGeneradasDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EnfermerosServiceImpl implements EnfermerosService {

    private static final Logger log = LoggerFactory.getLogger(EnfermerosServiceImpl.class);

    private final EnfermerosRepository enfermerosRepository;
    private final DepartamentosRepository departamentosRepository;
    private final AsignacionEnfermerosRepository asignacionEnfermerosRepository;
    private final GeneradorCredencialesService generadorCredencialesService;

    public EnfermerosServiceImpl(EnfermerosRepository enfermerosRepository,
                                 DepartamentosRepository departamentosRepository,
                                 AsignacionEnfermerosRepository asignacionEnfermerosRepository,
                                 GeneradorCredencialesService generadorCredencialesService) {
        this.enfermerosRepository = enfermerosRepository;
        this.departamentosRepository = departamentosRepository;
        this.asignacionEnfermerosRepository = asignacionEnfermerosRepository;
        this.generadorCredencialesService = generadorCredencialesService;
    }

    private EnfermerosDTO mapToDTO(Enfermeros e) {
        return mapToDTO(e, null);
    }

    private EnfermerosDTO mapToDTO(Enfermeros e, CredencialesGeneradasDTO credenciales) {
        return new EnfermerosDTO(
                e.getIdEnfermero(),
                e.getCedulaProfesional(),
                e.getNombre(),
                e.getApellidoPaterno(),
                e.getApellidoMaterno(),
                e.getTelefono(),
                e.getIdDepartamento(),
                e.getActivo(),
                credenciales
        );
    }

    private Enfermeros mapToEntity(EnfermerosDTO dto) {
        Enfermeros e = new Enfermeros();
        e.setCedulaProfesional(dto.cedulaProfesional() != null ? dto.cedulaProfesional().trim() : null);
        e.setNombre(dto.nombre() != null ? dto.nombre().trim() : null);
        e.setApellidoPaterno(dto.apellidoPaterno() != null ? dto.apellidoPaterno().trim() : null);
        e.setApellidoMaterno(dto.apellidoMaterno() != null ? dto.apellidoMaterno().trim() : null);
        e.setTelefono(dto.telefono() != null ? dto.telefono().trim() : null);
        e.setIdDepartamento(dto.idDepartamento());
        e.setActivo(dto.activo() != null ? dto.activo() : true); // activo por defecto al crear
        return e;
    }

    private void validarDatos(EnfermerosDTO dto, Long idActual) {
        if (dto == null) {
            throw new ReglaNegocioException("Los datos del enfermero no pueden ser nulos");
        }

        // RN: Cédula profesional obligatoria
        if (dto.cedulaProfesional() == null || dto.cedulaProfesional().trim().isEmpty()) {
            throw new ReglaNegocioException("La cédula profesional del enfermero es obligatoria");
        }

        // RN: Nombre y apellido paterno obligatorios
        if (dto.nombre() == null || dto.nombre().trim().isEmpty()) {
            throw new ReglaNegocioException("El nombre del enfermero es obligatorio");
        }
        if (dto.apellidoPaterno() == null || dto.apellidoPaterno().trim().isEmpty()) {
            throw new ReglaNegocioException("El apellido paterno del enfermero es obligatorio");
        }

        // RN: Unicidad de cédula profesional
        String cedulaLimpia = dto.cedulaProfesional().trim();
        boolean cedulaDuplicada;
        if (idActual == null) {
            cedulaDuplicada = enfermerosRepository.existsByCedulaProfesional(cedulaLimpia);
        } else {
            cedulaDuplicada = enfermerosRepository.existsByCedulaProfesionalAndIdEnfermeroNot(cedulaLimpia, idActual);
        }

        if (cedulaDuplicada) {
            throw new DuplicadoException("Ya existe un enfermero registrado con la cédula profesional: " + cedulaLimpia);
        }

        // RN: El departamento debe existir y estar activo
        if (dto.idDepartamento() == null) {
            throw new ReglaNegocioException("El departamento es obligatorio para el enfermero");
        }

        Departamentos departamento = departamentosRepository.findById(dto.idDepartamento())
                .orElseThrow(() -> new ReglaNegocioException("El departamento con ID " + dto.idDepartamento() + " no existe"));

        if (departamento.getEstado() != null && !departamento.getEstado()) {
            throw new ReglaNegocioException("El departamento seleccionado se encuentra inactivo");
        }
    }

    @Override
    public List<EnfermerosDTO> findAll() {
        return enfermerosRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<EnfermerosDTO> findActivos() {
        return enfermerosRepository.findByActivoTrue()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public EnfermerosDTO findById(Long id) {
        Enfermeros entity = enfermerosRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el enfermero con ID: " + id));
        return mapToDTO(entity);
    }

    @Override
    @Transactional
    public EnfermerosDTO crear(EnfermerosDTO dto) {
        validarDatos(dto, null);

        Enfermeros entity = mapToEntity(dto);
        if (entity.getActivo() == null) {
            entity.setActivo(true); // Activo por defecto
        }

        Enfermeros guardado = enfermerosRepository.save(entity);

        // Mismo @Transactional: si no se pueden crear las credenciales, tampoco se registra el enfermero.
        CredencialesGeneradasDTO credenciales = generadorCredencialesService.generarCredenciales(
                "enf", guardado.getNombre(), guardado.getApellidoPaterno(), "ENFERMERO");

        return mapToDTO(guardado, credenciales);
    }

    @Override
    @Transactional
    public EnfermerosDTO actualizar(EnfermerosDTO dto, Long id) {
        if (id == null) {
            throw new ReglaNegocioException("El ID del enfermero es obligatorio");
        }

        Enfermeros existente = enfermerosRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el enfermero con ID: " + id));

        validarDatos(dto, id);

        existente.setCedulaProfesional(dto.cedulaProfesional().trim());
        existente.setNombre(dto.nombre().trim());
        existente.setApellidoPaterno(dto.apellidoPaterno().trim());
        existente.setApellidoMaterno(dto.apellidoMaterno() != null ? dto.apellidoMaterno().trim() : null);
        existente.setTelefono(dto.telefono() != null ? dto.telefono().trim() : null);
        existente.setIdDepartamento(dto.idDepartamento());
        if (dto.activo() != null) {
            existente.setActivo(dto.activo());
        }

        Enfermeros actualizado = enfermerosRepository.save(existente);
        return mapToDTO(actualizado);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (id == null) {
            throw new ReglaNegocioException("El ID del enfermero es obligatorio");
        }

        Enfermeros existente = enfermerosRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el enfermero con ID: " + id));

        // Conflicto con dependencias: asignaciones activas a hospitalizaciones
        if (asignacionEnfermerosRepository.existsByIdEnfermeroAndActivoTrue(id)) {
            throw new DuplicadoException("No se puede eliminar el enfermero porque tiene asignaciones activas");
        }

        // Borrado lógico (conserva el historial de asignaciones y turnos)
        existente.setActivo(false);
        enfermerosRepository.save(existente);
    }
}
