package com.example.hospital.citas;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface CitasRepository extends JpaRepository<Citas, Long> {

    // Validar conflicto de horario del médico (en creación). `idsEstadoLibres`: estados que liberan el horario
    // (Cancelada, No asistió): esas citas no lo ocupan aunque sigan activas.
    boolean existsByIdMedicoAndFechaAndHoraAndActivoTrueAndIdEstadoNotIn(
            Long idMedico, LocalDate fecha, LocalTime hora, Collection<Long> idsEstadoLibres);

    // Validar conflicto de horario del médico (en actualización excluyendo la propia cita)
    boolean existsByIdMedicoAndFechaAndHoraAndActivoTrueAndIdEstadoNotInAndIdCitaNot(
            Long idMedico, LocalDate fecha, LocalTime hora, Collection<Long> idsEstadoLibres, Long idCita);

    // Listar citas activas
    List<Citas> findByActivoTrue();

    // Listar citas por paciente
    List<Citas> findByIdPaciente(Long idPaciente);

    boolean existsByIdPacienteAndActivoTrue(Long idPaciente);

    // Listar citas por médico
    List<Citas> findByIdMedico(Long idMedico);

    boolean existsByIdMedicoAndActivoTrue(Long idMedico);

    // Listar citas por fecha
    List<Citas> findByFecha(LocalDate fecha);

    // Agenda diaria de un médico ordenada por hora
    List<Citas> findByIdMedicoAndFechaAndActivoTrueOrderByHoraAsc(Long idMedico, LocalDate fecha);

    long countByFechaAndActivoTrue(LocalDate fecha);

    // Agenda de citas pendientes: activas y en alguno de los estados indicados, en orden cronológico
    List<Citas> findByActivoTrueAndIdEstadoInOrderByFechaAscHoraAsc(Collection<Long> idsEstado);
}
