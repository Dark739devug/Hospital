package com.example.hospital.tiposhabitacion.impl;

import com.example.hospital.shared.exception.DuplicadoException;
import com.example.hospital.shared.exception.ReglaNegocioException;
import com.example.hospital.habitacion.HabitacionRepository;
import com.example.hospital.shared.exception.RecursoNoEncontradoException;
import com.example.hospital.tiposhabitacion.TiposHabitacion;
import com.example.hospital.tiposhabitacion.TiposHabitacionRepository;
import com.example.hospital.tiposhabitacion.TiposHabitacionService;
import com.example.hospital.tiposhabitacion.dto.TiposHabitacionDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TiposHabitacionServiceImpl implements TiposHabitacionService {

    // Valores base de tipos de habitación (según Hosp_postgres.sql)
    public static final String INDIVIDUAL = "Individual";
    public static final String DOBLE = "Doble";
    public static final String COMPARTIDA = "Compartida";
    public static final String UCI = "UCI";
    public static final String PEDIATRICA = "Pediátrica";
    public static final String OBSERVACION = "Observación";

    private static final Set<String> TIPOS_BASE = Set.of(
            INDIVIDUAL, DOBLE, COMPARTIDA, UCI, PEDIATRICA, OBSERVACION
    );

    private final TiposHabitacionRepository tiposHabitacionRepository;
    private final HabitacionRepository habitacionRepository;

    public TiposHabitacionServiceImpl(TiposHabitacionRepository tiposHabitacionRepository,
                                      HabitacionRepository habitacionRepository) {
        this.tiposHabitacionRepository = tiposHabitacionRepository;
        this.habitacionRepository = habitacionRepository;
    }

    private TiposHabitacionDTO mapToDTO(TiposHabitacion t) {
        return new TiposHabitacionDTO(
                t.getIdTipoHabitacion(),
                t.getDescripcion(),
                t.getCostoDia(),
                t.getActivo()
        );
    }

    private TiposHabitacion mapToEntity(TiposHabitacionDTO dto) {
        TiposHabitacion t = new TiposHabitacion();
        t.setDescripcion(dto.descripcion() != null ? dto.descripcion().trim() : null);
        t.setCostoDia(dto.costoDia());
        t.setActivo(dto.activo() != null ? dto.activo() : true); // activo por defecto al crear
        return t;
    }

    private String obtenerNombreCanonica(String descripcion) {
        if (descripcion == null) return "";
        for (String base : TIPOS_BASE) {
            if (base.equalsIgnoreCase(descripcion.trim())) {
                return base;
            }
        }
        return descripcion.trim();
    }

    private void validarDatos(TiposHabitacionDTO dto, Long idActual) {
        if (dto == null || dto.descripcion() == null || dto.descripcion().trim().isEmpty()) {
            throw new ReglaNegocioException("La descripción del tipo de habitación es obligatoria");
        }
        if (dto.costoDia() == null || dto.costoDia().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException("El costo por día debe ser un valor mayor a cero");
        }

        String canonico = obtenerNombreCanonica(dto.descripcion());

        boolean existe;
        if (idActual == null) {
            existe = tiposHabitacionRepository.existsByDescripcionIgnoreCase(canonico);
        } else {
            existe = tiposHabitacionRepository.existsByDescripcionIgnoreCaseAndIdTipoHabitacionNot(canonico, idActual);
        }

        if (existe) {
            throw new DuplicadoException("Ya existe un tipo de habitación con la descripción: " + canonico);
        }
    }

    @Override
    public List<TiposHabitacionDTO> findAll() {
        return tiposHabitacionRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public TiposHabitacionDTO findById(Long id) {
        TiposHabitacion entity = tiposHabitacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el tipo de habitación con ID: " + id));
        return mapToDTO(entity);
    }

    @Override
    @Transactional
    public TiposHabitacionDTO crear(TiposHabitacionDTO dto) {
        validarDatos(dto, null);

        TiposHabitacion entity = mapToEntity(dto);
        entity.setDescripcion(obtenerNombreCanonica(dto.descripcion()));
        if (entity.getActivo() == null) {
            entity.setActivo(true);
        }

        TiposHabitacion guardado = tiposHabitacionRepository.save(entity);
        return mapToDTO(guardado);
    }

    @Override
    @Transactional
    public TiposHabitacionDTO actualizar(TiposHabitacionDTO dto, Long id) {
        if (id == null) {
            throw new ReglaNegocioException("El ID del tipo de habitación es obligatorio");
        }

        TiposHabitacion existente = tiposHabitacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el tipo de habitación con ID: " + id));

        // Proteger no renombrar los tipos base indispensables
        if (TIPOS_BASE.contains(existente.getDescripcion())) {
            if (dto.descripcion() != null && !dto.descripcion().equalsIgnoreCase(existente.getDescripcion())) {
                throw new ReglaNegocioException("No se permite renombrar el tipo de habitación base '" +
                        existente.getDescripcion() + "'");
            }
        }

        validarDatos(dto, id);

        existente.setDescripcion(obtenerNombreCanonica(dto.descripcion()));
        existente.setCostoDia(dto.costoDia());
        if (dto.activo() != null) {
            existente.setActivo(dto.activo());
        }

        TiposHabitacion actualizado = tiposHabitacionRepository.save(existente);
        return mapToDTO(actualizado);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (id == null) {
            throw new ReglaNegocioException("El ID del tipo de habitación es obligatorio");
        }

        TiposHabitacion existente = tiposHabitacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el tipo de habitación con ID: " + id));

        // Proteger no eliminar los tipos base
        if (TIPOS_BASE.contains(existente.getDescripcion())) {
            throw new ReglaNegocioException("No se permite eliminar el tipo de habitación base '" +
                    existente.getDescripcion() + "'");
        }

        // Regla de integridad: no eliminar si existen habitaciones asociadas
        if (habitacionRepository.existsByIdTipoHabitacion(id)) {
            throw new DuplicadoException("No se puede eliminar el tipo de habitación porque existen habitaciones asociadas a él");
        }

        // Eliminación lógica
        existente.setActivo(false);
        tiposHabitacionRepository.save(existente);
    }
}
