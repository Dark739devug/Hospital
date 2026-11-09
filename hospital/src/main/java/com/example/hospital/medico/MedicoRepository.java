package com.example.hospital.medico;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicoRepository extends JpaRepository<Medico, Long> {

    // Validar colegiado único
    boolean existsByNoColegiado(String noColegiado);

    boolean existsByNoColegiadoAndIdMedicoNot(String noColegiado, Long idMedico);

    Optional<Medico> findByNoColegiado(String noColegiado);

    // Lee el médico bloqueando su fila (SELECT ... FOR UPDATE) hasta el fin de la transacción: serializa las
    // reservas de citas del mismo médico para que dos peticiones simultáneas no ocupen el mismo horario.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Medico m where m.idMedico = :idMedico")
    Optional<Medico> findByIdParaReservar(@Param("idMedico") Long idMedico);

    List<Medico> findByEstadoTrue();

    // Validar email único si se proporciona
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdMedicoNot(String email, Long idMedico);

    // Búsqueda por nombre o apellido
    List<Medico> findByNombreContainingIgnoreCaseOrApellidoPaternoContainingIgnoreCase(
            String nombre, String apellidoPaterno);
}
