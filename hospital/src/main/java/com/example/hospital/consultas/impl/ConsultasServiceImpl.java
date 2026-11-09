package com.example.hospital.consultas.impl;

import com.example.hospital.citas.Citas;
import com.example.hospital.shared.exception.DuplicadoException;
import com.example.hospital.shared.exception.RecursoNoEncontradoException;
import com.example.hospital.shared.exception.ReglaNegocioException;
import com.example.hospital.citas.CitasRepository;
import com.example.hospital.consultas.Consultas;
import com.example.hospital.consultas.ConsultasRepository;
import com.example.hospital.consultas.ConsultasService;
import com.example.hospital.consultas.VentanaEdicionConsulta;
import com.example.hospital.consultas.dto.ConsultasDTO;
import com.example.hospital.diagnosticoconsulta.DiagnosticoConsultaRepository;
import com.example.hospital.estadocita.EstadoCita;
import com.example.hospital.estadocita.EstadoCitaRepository;
import com.example.hospital.estadocita.EstadoCitaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ConsultasServiceImpl implements ConsultasService {

    private static final String CONFIRMADA = "Confirmada";
    private static final String EN_CURSO = "En curso";
    private static final String ATENDIDA = "Atendida";

    private final ConsultasRepository consultasRepository;
    private final CitasRepository citasRepository;
    private final EstadoCitaRepository estadoCitaRepository;
    private final DiagnosticoConsultaRepository diagnosticoConsultaRepository;
    private final EstadoCitaService estadoCitaService;

    public ConsultasServiceImpl(ConsultasRepository consultasRepository,
                                CitasRepository citasRepository,
                                EstadoCitaRepository estadoCitaRepository,
                                DiagnosticoConsultaRepository diagnosticoConsultaRepository,
                                EstadoCitaService estadoCitaService) {
        this.consultasRepository = consultasRepository;
        this.citasRepository = citasRepository;
        this.estadoCitaRepository = estadoCitaRepository;
        this.diagnosticoConsultaRepository = diagnosticoConsultaRepository;
        this.estadoCitaService = estadoCitaService;
    }

    private ConsultasDTO mapToDTO(Consultas c) {
        return new ConsultasDTO(
                c.getIdConsulta(),
                c.getFechaHora(),
                c.getSintomas(),
                c.getPeso(),
                c.getTalla(),
                c.getTemperatura(),
                c.getPresionArterial(),
                c.getFrecuenciaCardiaca(),
                c.getIdCita(),
                c.getActivo()
        );
    }

    private Consultas mapToEntity(ConsultasDTO dto) {
        Consultas c = new Consultas();
        c.setFechaHora(dto.fechaHora());
        c.setSintomas(dto.sintomas() != null ? dto.sintomas().trim() : null);
        c.setPeso(dto.peso());
        c.setTalla(dto.talla());
        c.setTemperatura(dto.temperatura());
        c.setPresionArterial(dto.presionArterial() != null ? dto.presionArterial().trim() : null);
        c.setFrecuenciaCardiaca(dto.frecuenciaCardiaca());
        c.setIdCita(dto.idCita());
        c.setActivo(dto.activo());
        return c;
    }

    /**
     * Valida las reglas de negocio de la consulta médica. Los campos obligatorios y los rangos
     * clínicos (peso, talla, temperatura, frecuencia cardíaca) los valida Bean Validation en
     * ConsultasDTO (400 con detalles por campo).
     * 1. La cita médica asociada debe existir y estar activa.
     * 2. No duplicar consultas para la misma cita médica (409).
     *
     * @return la cita asociada
     */
    private Citas validarReglasDeNegocio(ConsultasDTO dto, Long idConsultaActual) {
        if (dto == null) {
            throw new ReglaNegocioException("Los datos de la consulta no pueden ser nulos");
        }
        // RN-01: La cita debe existir y estar activa
        Citas cita = citasRepository.findById(dto.idCita())
                .orElseThrow(() -> new ReglaNegocioException("La cita médica con ID " + dto.idCita() + " no existe"));
        if (cita.getActivo() != null && !cita.getActivo()) {
            throw new ReglaNegocioException("La cita médica asociada se encuentra inactiva o cancelada");
        }

        // RN-02: No duplicar consultas para la misma cita
        boolean consultaDuplicada;
        if (idConsultaActual == null) {
            consultaDuplicada = consultasRepository.existsByIdCita(dto.idCita());
        } else {
            consultaDuplicada = consultasRepository.existsByIdCitaAndIdConsultaNot(dto.idCita(), idConsultaActual);
        }
        if (consultaDuplicada) {
            throw new DuplicadoException("Ya existe una consulta médica registrada para la cita ID: " + dto.idCita());
        }
        return cita;
    }

    @Override
    public List<ConsultasDTO> findAll() {
        return consultasRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ConsultasDTO> findActivas() {
        return consultasRepository.findByActivoTrue()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ConsultasDTO findById(Long id) {
        Consultas entity = consultasRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la consulta médica con ID: " + id));
        return mapToDTO(entity);
    }

    @Override
    public ConsultasDTO findByCita(Long idCita) {
        if (idCita == null) {
            throw new ReglaNegocioException("El ID de cita es obligatorio");
        }
        Consultas entity = consultasRepository.findByIdCita(idCita)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe consulta registrada para la cita ID: " + idCita));
        return mapToDTO(entity);
    }

    @Override
    public List<ConsultasDTO> listarPorCita(Long idCita) {
        if (idCita == null) {
            throw new ReglaNegocioException("El ID de cita es obligatorio");
        }
        Optional<Consultas> consulta = consultasRepository.findByIdCita(idCita);
        return consulta.map(this::mapToDTO).map(List::of).orElseGet(List::of);
    }

    @Override
    @Transactional
    public ConsultasDTO crear(ConsultasDTO dto) {
        Citas cita = validarReglasDeNegocio(dto, null);

        // RN-03: solo se atiende una cita Confirmada o En curso cuya fecha ya llegó (fecha <= hoy)
        EstadoCita estadoActual = estadoCitaRepository.findById(cita.getIdEstado())
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado actual de la cita no encontrado"));
        String descripcionActual = estadoActual.getDescripcion();
        if (!CONFIRMADA.equalsIgnoreCase(descripcionActual) && !EN_CURSO.equalsIgnoreCase(descripcionActual)) {
            throw new ReglaNegocioException("Solo se puede registrar la consulta de una cita Confirmada o En curso (la cita está '"
                    + descripcionActual + "')");
        }
        if (cita.getFecha() != null && cita.getFecha().isAfter(LocalDate.now())) {
            throw new ReglaNegocioException("No se puede registrar la consulta de una cita con fecha futura (" + cita.getFecha() + ")");
        }

        Consultas entity = mapToEntity(dto);
        // Fecha/hora de atención asignada por el servidor si no viene especificada. No puede ser futura:
        // de ella depende la ventana de 24 horas en que la consulta se puede editar.
        if (entity.getFechaHora() == null) {
            entity.setFechaHora(LocalDateTime.now());
        } else if (entity.getFechaHora().isAfter(LocalDateTime.now())) {
            throw new ReglaNegocioException("La fecha y hora de la consulta no puede ser futura");
        }
        if (entity.getActivo() == null) {
            entity.setActivo(true);
        }

        // Pasar la cita a "Atendida" respetando la máquina de estados:
        // Confirmada -> En curso -> Atendida, o En curso -> Atendida.
        EstadoCita enCurso = buscarEstado(EN_CURSO);
        EstadoCita atendida = buscarEstado(ATENDIDA);
        if (CONFIRMADA.equalsIgnoreCase(descripcionActual)) {
            estadoCitaService.validarTransicion(CONFIRMADA, EN_CURSO);
        }
        estadoCitaService.validarTransicion(EN_CURSO, ATENDIDA);

        Consultas guardada = consultasRepository.save(entity);
        cita.setIdEstado(atendida.getIdEstado());
        citasRepository.save(cita);
        return mapToDTO(guardada);
    }

    private EstadoCita buscarEstado(String descripcion) {
        return estadoCitaRepository.findByDescripcionIgnoreCase(descripcion)
                .orElseThrow(() -> new ReglaNegocioException("Falta el estado '" + descripcion + "' en el catálogo de estados de cita"));
    }

    @Override
    @Transactional
    public ConsultasDTO actualizar(ConsultasDTO dto, Long id) {
        if (id == null) {
            throw new ReglaNegocioException("El ID de la consulta es obligatorio");
        }

        Consultas existente = consultasRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la consulta médica con ID: " + id));

        // Las consultas pasadas son históricas: solo se editan dentro de las 24 horas siguientes a su atención.
        VentanaEdicionConsulta.exigirEditable(existente);

        validarReglasDeNegocio(dto, id);

        // La fecha/hora de atención no se cambia al editar (se ignora la del cuerpo): si se pudiera mover,
        // también se podría reabrir una consulta pasada.
        existente.setSintomas(dto.sintomas() != null ? dto.sintomas().trim() : null);
        existente.setPeso(dto.peso());
        existente.setTalla(dto.talla());
        existente.setTemperatura(dto.temperatura());
        existente.setPresionArterial(dto.presionArterial() != null ? dto.presionArterial().trim() : null);
        existente.setFrecuenciaCardiaca(dto.frecuenciaCardiaca());
        existente.setIdCita(dto.idCita());
        if (dto.activo() != null) {
            existente.setActivo(dto.activo());
        }

        Consultas actualizada = consultasRepository.save(existente);
        return mapToDTO(actualizada);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (id == null) {
            throw new ReglaNegocioException("El ID de la consulta es obligatorio");
        }

        Consultas existente = consultasRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la consulta médica con ID: " + id));

        // Integridad referencial: no eliminar consulta si tiene diagnósticos asociados
        if (diagnosticoConsultaRepository.existsByIdConsulta(id)) {
            throw new IllegalStateException("No se puede eliminar la consulta médica porque tiene diagnósticos clínicos asociados");
        }

        // Borrado lógico
        existente.setActivo(false);
        consultasRepository.save(existente);
    }
}
