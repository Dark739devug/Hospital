package com.example.hospital.medico.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.hospital.shared.catalogos.EstadosCatalogo;
import com.example.hospital.shared.exception.DuplicadoException;
import com.example.hospital.citas.CitasRepository;
import com.example.hospital.emergencia.Emergencia;
import com.example.hospital.emergencia.EmergenciaRepository;
import com.example.hospital.hospitalizaciones.HospitalizacionesRepository;
import com.example.hospital.medico.Medico;
import com.example.hospital.medico.MedicoRepository;
import com.example.hospital.medico.MedicoService;
import com.example.hospital.medico.dto.MedicoDTO;
import com.example.hospital.shared.exception.RecursoNoEncontradoException;
import com.example.hospital.shared.exception.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class MedicoServiceImpl implements MedicoService {

    private static final Logger log = LoggerFactory.getLogger(MedicoServiceImpl.class);

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final MedicoRepository medicoRepository;
    private final CitasRepository citasRepository;
    private final HospitalizacionesRepository hospitalizacionesRepository;
    private final EmergenciaRepository emergenciaRepository;
    // El estado de hospitalización Activa se resuelve por nombre, no por id fijo
    private final EstadosCatalogo estados;

    public MedicoServiceImpl(MedicoRepository medicoRepository,
                             CitasRepository citasRepository,
                             HospitalizacionesRepository hospitalizacionesRepository,
                             EmergenciaRepository emergenciaRepository,
                             EstadosCatalogo estados) {
        this.medicoRepository = medicoRepository;
        this.citasRepository = citasRepository;
        this.hospitalizacionesRepository = hospitalizacionesRepository;
        this.emergenciaRepository = emergenciaRepository;
        this.estados = estados;
    }

    private MedicoDTO mapToDTO(Medico m) {
        return new MedicoDTO(
                m.getIdMedico(),
                m.getNoColegiado(),
                m.getNombre(),
                m.getApellidoPaterno(),
                m.getApellidoMaterno(),
                m.getTelefono(),
                m.getEmail(),
                m.getEstado()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicoDTO> findAll() {
        return medicoRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicoDTO> findActivos() {
        return medicoRepository.findByEstadoTrue()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MedicoDTO findById(Long id) {
        Medico entity = medicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el médico con ID: " + id));
        return mapToDTO(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public MedicoDTO findByNoColegiado(String noColegiado) {
        if (noColegiado == null || noColegiado.trim().isEmpty()) {
            throw new ReglaNegocioException("El número de colegiado es obligatorio.");
        }
        Medico entity = medicoRepository.findByNoColegiado(noColegiado.trim())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el médico con número de colegiado: " + noColegiado));
        return mapToDTO(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicoDTO> buscar(String criterio) {
        if (criterio == null || criterio.trim().isEmpty()) {
            return findActivos();
        }
        String query = criterio.trim();
        return medicoRepository.findByNombreContainingIgnoreCaseOrApellidoPaternoContainingIgnoreCase(query, query)
                .stream()
                .filter(m -> Boolean.TRUE.equals(m.getEstado()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicoDTO> buscar(String criterio, boolean soloActivos) {
        String q = criterio == null ? "" : criterio.trim().toLowerCase();
        return medicoRepository.findAll().stream()
                .filter(m -> !soloActivos || Boolean.TRUE.equals(m.getEstado()))
                .filter(m -> q.isEmpty()
                        || contiene(m.getNombre(), q) || contiene(m.getApellidoPaterno(), q)
                        || contiene(m.getApellidoMaterno(), q) || contiene(m.getNoColegiado(), q))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private boolean contiene(String texto, String q) {
        return texto != null && texto.toLowerCase().contains(q);
    }

    @Override
    @Transactional
    public MedicoDTO crear(MedicoDTO dto) {
        validarDatosMedico(dto, null);

        Medico medico = new Medico();
        medico.setNoColegiado(dto.noColegiado().trim());
        medico.setNombre(dto.nombre().trim());
        medico.setApellidoPaterno(dto.apellidoPaterno().trim());
        medico.setApellidoMaterno(dto.apellidoMaterno() != null ? dto.apellidoMaterno().trim() : null);
        medico.setTelefono(dto.telefono() != null ? dto.telefono().trim() : null);
        medico.setEmail(dto.email() != null ? dto.email().trim().toLowerCase() : null);
        medico.setEstado(dto.estado() != null ? dto.estado() : true);

        Medico guardado = medicoRepository.save(medico);
        return mapToDTO(guardado);
    }

    @Override
    @Transactional
    public MedicoDTO actualizar(MedicoDTO dto, Long id) {
        if (id == null) {
            throw new ReglaNegocioException("El ID del médico es obligatorio para actualizar.");
        }

        Medico existente = medicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el médico con ID: " + id));

        validarDatosMedico(dto, id);

        // Si se intenta inactivar el médico, validar compromisos activos
        if (Boolean.TRUE.equals(existente.getEstado()) && Boolean.FALSE.equals(dto.estado())) {
            validarCompromisosActivos(id);
        }

        existente.setNoColegiado(dto.noColegiado().trim());
        existente.setNombre(dto.nombre().trim());
        existente.setApellidoPaterno(dto.apellidoPaterno().trim());
        existente.setApellidoMaterno(dto.apellidoMaterno() != null ? dto.apellidoMaterno().trim() : null);
        existente.setTelefono(dto.telefono() != null ? dto.telefono().trim() : null);
        existente.setEmail(dto.email() != null ? dto.email().trim().toLowerCase() : null);
        if (dto.estado() != null) {
            existente.setEstado(dto.estado());
        }

        Medico actualizado = medicoRepository.save(existente);
        return mapToDTO(actualizado);
    }

    @Override
    @Transactional
    public MedicoDTO cambiarEstado(Long id, Boolean nuevoEstado) {
        if (id == null) {
            throw new ReglaNegocioException("El ID del médico es obligatorio.");
        }
        if (nuevoEstado == null) {
            throw new ReglaNegocioException("El nuevo estado no puede ser nulo.");
        }

        Medico existente = medicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el médico con ID: " + id));

        if (Boolean.FALSE.equals(nuevoEstado)) {
            validarCompromisosActivos(id);
        }

        existente.setEstado(nuevoEstado);
        Medico actualizado = medicoRepository.save(existente);
        log.info("Médico {} {}", id, Boolean.TRUE.equals(actualizado.getEstado()) ? "activado" : "desactivado");
        return mapToDTO(actualizado);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (id == null) {
            throw new ReglaNegocioException("El ID del médico es obligatorio.");
        }

        Medico existente = medicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el médico con ID: " + id));

        if (!Boolean.TRUE.equals(existente.getEstado())) {
            throw new ReglaNegocioException("El médico ya se encuentra inactivo.");
        }

        // RN-MED-05: Integridad referencial con citas, hospitalizaciones y emergencias
        validarCompromisosActivos(id);

        // RN-MED-06: Borrado lógico sistemático
        existente.setEstado(false);
        medicoRepository.save(existente);
        log.info("Médico {} desactivado", id);
    }

    private void validarDatosMedico(MedicoDTO dto, Long idActual) {
        if (dto == null) {
            throw new ReglaNegocioException("Los datos del médico son obligatorios.");
        }

        // RN-MED-01: Número de colegiado obligatorio y único
        if (dto.noColegiado() == null || dto.noColegiado().trim().isEmpty()) {
            throw new ReglaNegocioException("El número de colegiado del médico es obligatorio.");
        }
        String colegiado = dto.noColegiado().trim();

        if (idActual == null) {
            if (medicoRepository.existsByNoColegiado(colegiado)) {
                throw new DuplicadoException("Ya existe un médico registrado con el número de colegiado: " + colegiado);
            }
        } else {
            if (medicoRepository.existsByNoColegiadoAndIdMedicoNot(colegiado, idActual)) {
                throw new DuplicadoException("Ya existe otro médico registrado con el número de colegiado: " + colegiado);
            }
        }

        // RN-MED-02: Nombre y apellido paterno obligatorios
        if (dto.nombre() == null || dto.nombre().trim().isEmpty()) {
            throw new ReglaNegocioException("El nombre del médico es obligatorio.");
        }
        if (dto.apellidoPaterno() == null || dto.apellidoPaterno().trim().isEmpty()) {
            throw new ReglaNegocioException("El apellido paterno del médico es obligatorio.");
        }

        // RN-MED-03: Email formato y unicidad si se proporciona
        if (dto.email() != null && !dto.email().trim().isEmpty()) {
            String email = dto.email().trim().toLowerCase();
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new ReglaNegocioException("El formato del correo electrónico no es válido: " + email);
            }
            if (idActual == null) {
                if (medicoRepository.existsByEmailIgnoreCase(email)) {
                    throw new DuplicadoException("Ya existe un médico registrado con el correo electrónico: " + email);
                }
            } else {
                if (medicoRepository.existsByEmailIgnoreCaseAndIdMedicoNot(email, idActual)) {
                    throw new DuplicadoException("Ya existe otro médico registrado con el correo electrónico: " + email);
                }
            }
        }

        // RN-MED-04: Teléfono validación de longitud si se proporciona
        if (dto.telefono() != null && !dto.telefono().trim().isEmpty()) {
            String tel = dto.telefono().trim();
            if (tel.length() < 7 || tel.length() > 20) {
                throw new ReglaNegocioException("El teléfono debe contener entre 7 y 20 caracteres.");
            }
        }
    }

    private void validarCompromisosActivos(Long idMedico) {
        // Citas activas
        if (citasRepository.existsByIdMedicoAndActivoTrue(idMedico)) {
            throw new ReglaNegocioException("No se puede desactivar o eliminar el médico porque tiene citas médicas activas programadas.");
        }

        // Hospitalizaciones activas
        if (hospitalizacionesRepository.existsByIdMedicoResponsableAndIdEstadoHosp(idMedico, estados.hospActiva())) {
            throw new ReglaNegocioException("No se puede desactivar o eliminar el médico porque tiene hospitalizaciones activas a su cargo.");
        }

        // Emergencias activas
        List<Emergencia> emergenciasActivas = emergenciaRepository.findByIdMedicoAndActivoTrue(idMedico);
        if (emergenciasActivas != null && !emergenciasActivas.isEmpty()) {
            throw new ReglaNegocioException("No se puede desactivar o eliminar el médico porque tiene atenciones de emergencia activas asignadas.");
        }
    }
}
