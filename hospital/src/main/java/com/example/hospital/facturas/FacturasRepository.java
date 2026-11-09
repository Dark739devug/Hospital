package com.example.hospital.facturas;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacturasRepository extends JpaRepository<Facturas, Long> {

    List<Facturas> findByActivoTrue();

    List<Facturas> findByIdPaciente(Long idPaciente);

    List<Facturas> findByIdPacienteAndActivoTrue(Long idPaciente);

    List<Facturas> findByIdHospitalizacion(Long idHospitalizacion);

    List<Facturas> findByIdHospitalizacionAndActivoTrue(Long idHospitalizacion);
    List<Facturas> findByIdConsultaAndActivoTrue(Long idConsulta);

    List<Facturas> findByIdEstadoFactura(Long idEstadoFactura);

    boolean existsByIdPaciente(Long idPaciente);

    boolean existsByIdHospitalizacion(Long idHospitalizacion);

    boolean existsByIdMetodoPago(Long idMetodoPago);

    boolean existsByIdEstadoFactura(Long idEstadoFactura);

    long countByIdEstadoFacturaAndActivoTrue(Long idEstadoFactura);
}
