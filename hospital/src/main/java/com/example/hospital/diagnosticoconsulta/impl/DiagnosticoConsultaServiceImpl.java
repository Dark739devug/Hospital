package com.example.hospital.diagnosticoconsulta.impl;

import com.example.hospital.consultas.Consultas;
import com.example.hospital.shared.exception.DuplicadoException;
import com.example.hospital.shared.exception.RecursoNoEncontradoException;
import com.example.hospital.shared.exception.ReglaNegocioException;
import com.example.hospital.consultas.ConsultasRepository;
import com.example.hospital.consultas.VentanaEdicionConsulta;
import com.example.hospital.diagnosticoconsulta.DiagnosticoConsulta;
import com.example.hospital.diagnosticoconsulta.DiagnosticoConsultaRepository;
import com.example.hospital.diagnosticoconsulta.DiagnosticoConsultaService;
import com.example.hospital.diagnosticoconsulta.dto.DiagnosticoConsultaDTO;
import com.example.hospital.diagnosticos.Diagnosticos;
import com.example.hospital.diagnosticos.DiagnosticosRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DiagnosticoConsultaServiceImpl implements DiagnosticoConsultaService {

    private final DiagnosticoConsultaRepository diagnosticoConsultaRepository;
    private final DiagnosticosRepository diagnosticosRepository;
    private final ConsultasRepository consultasRepository;

    public DiagnosticoConsultaServiceImpl(DiagnosticoConsultaRepository diagnosticoConsultaRepository,
                                         DiagnosticosRepository diagnosticosRepository,
                                         ConsultasRepository consultasRepository) {
        this.diagnosticoConsultaRepository = diagnosticoConsultaRepository;
        this.diagnosticosRepository = diagnosticosRepository;
        this.consultasRepository = consultasRepository;
    }

    private DiagnosticoConsultaDTO mapToDTO(DiagnosticoConsulta d) {
        return new DiagnosticoConsultaDTO(
                d.getIdDiagnostico(),
                d.getIdConsulta()
        );
    }

    @Override
    public List<DiagnosticoConsultaDTO> findAll() {
        return diagnosticoConsultaRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DiagnosticoConsultaDTO> findByConsulta(Long idConsulta) {
        if (idConsulta == null) {
            throw new ReglaNegocioException("El ID de consulta es obligatorio");
        }
        return diagnosticoConsultaRepository.findByIdConsulta(idConsulta)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DiagnosticoConsultaDTO> findByDiagnostico(Long idDiagnostico) {
        if (idDiagnostico == null) {
            throw new ReglaNegocioException("El ID de diagnóstico es obligatorio");
        }
        return diagnosticoConsultaRepository.findByIdDiagnostico(idDiagnostico)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public DiagnosticoConsultaDTO findById(Long idDiagnostico, Long idConsulta) {
        if (idDiagnostico == null || idConsulta == null) {
            throw new ReglaNegocioException("Ambos identificadores (idDiagnostico e idConsulta) son obligatorios");
        }
        DiagnosticoConsulta.DiagnosticoConsultaId id =
                new DiagnosticoConsulta.DiagnosticoConsultaId(idDiagnostico, idConsulta);

        DiagnosticoConsulta entity = diagnosticoConsultaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la asociación entre diagnóstico " +
                        idDiagnostico + " y consulta " + idConsulta));
        return mapToDTO(entity);
    }

    @Override
    @Transactional
    public DiagnosticoConsultaDTO crear(DiagnosticoConsultaDTO dto) {
        if (dto == null || dto.idDiagnostico() == null || dto.idConsulta() == null) {
            throw new ReglaNegocioException("Los IDs de diagnóstico y de consulta son obligatorios");
        }

        // 1. Validar que la consulta exista y esté activa
        Consultas consulta = consultasRepository.findById(dto.idConsulta())
                .orElseThrow(() -> new ReglaNegocioException("La consulta con ID " + dto.idConsulta() + " no existe"));

        if (consulta.getActivo() != null && !consulta.getActivo()) {
            throw new ReglaNegocioException("La consulta médica seleccionada se encuentra inactiva");
        }
        // Solo se agregan diagnósticos a consultas dentro de su ventana de edición (24 horas)
        VentanaEdicionConsulta.exigirEditable(consulta);

        // 2. Validar que el diagnóstico exista y esté activo
        Diagnosticos diagnostico = diagnosticosRepository.findById(dto.idDiagnostico())
                .orElseThrow(() -> new ReglaNegocioException("El diagnóstico con ID " + dto.idDiagnostico() + " no existe"));

        if (diagnostico.getActivo() != null && !diagnostico.getActivo()) {
            throw new ReglaNegocioException("El diagnóstico médico seleccionado se encuentra inactivo");
        }

        // 3. Validar que no se duplique el par (idDiagnostico, idConsulta)
        if (diagnosticoConsultaRepository.existsByIdDiagnosticoAndIdConsulta(dto.idDiagnostico(), dto.idConsulta())) {
            throw new DuplicadoException("El diagnóstico ya fue asignado previamente a esta consulta");
        }

        DiagnosticoConsulta entity = new DiagnosticoConsulta(dto.idDiagnostico(), dto.idConsulta());
        DiagnosticoConsulta guardado = diagnosticoConsultaRepository.save(entity);
        return mapToDTO(guardado);
    }

    @Override
    @Transactional
    public void eliminar(Long idDiagnostico, Long idConsulta) {
        if (idDiagnostico == null || idConsulta == null) {
            throw new ReglaNegocioException("Ambos IDs son obligatorios para remover la asociación");
        }

        DiagnosticoConsulta.DiagnosticoConsultaId id =
                new DiagnosticoConsulta.DiagnosticoConsultaId(idDiagnostico, idConsulta);

        if (!diagnosticoConsultaRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("No existe la asociación de diagnóstico " +
                    idDiagnostico + " para la consulta " + idConsulta);
        }

        // Tampoco se quitan diagnósticos de una consulta pasada (fuera de la ventana de 24 horas)
        consultasRepository.findById(idConsulta).ifPresent(VentanaEdicionConsulta::exigirEditable);

        // Al ser tabla de enlace puro de clave compuesta sin bandera propia, se remueve el vínculo
        diagnosticoConsultaRepository.deleteById(id);
    }
}
