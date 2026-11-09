package com.example.hospital.citas;

import com.example.hospital.citas.dto.CitasDTO;

import java.time.LocalDate;
import java.util.List;

public interface CitasService {

    List<CitasDTO> findAll();

    List<CitasDTO> findActivas();

    /** Filtros opcionales y combinables (todos se aplican a la vez). */
    List<CitasDTO> buscar(Long idPaciente, Long idMedico, LocalDate fecha, boolean soloActivas);

    List<CitasDTO> findByPaciente(Long idPaciente);

    List<CitasDTO> findByMedico(Long idMedico);

    List<CitasDTO> findByFecha(LocalDate fecha);

    CitasDTO findById(Long id);

    CitasDTO crear(CitasDTO dto);

    CitasDTO actualizar(CitasDTO dto, Long id);

    CitasDTO cambiarEstado(Long id, Long nuevoIdEstado);

    void eliminar(Long id);

    List<CitasDTO> agendaMedico(Long idMedico, LocalDate fecha);

    /**
     * Citas pendientes de atender (activas y en estado Programada, Confirmada o Reprogramada), en orden cronológico.
     * Filtros opcionales y combinables: médico y rango de fechas [desde, hasta] (inclusivo).
     */
    List<CitasDTO> pendientes(Long idMedico, LocalDate desde, LocalDate hasta);
}
