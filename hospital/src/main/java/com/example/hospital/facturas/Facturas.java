package com.example.hospital.facturas;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "facturas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Facturas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idFactura;
    private Long idPaciente;
    private Long idHospitalizacion;
    // Consulta externa que originó la factura (medicamentos recetados en la consulta); null en las demás
    private Long idConsulta;
    private LocalDateTime fechaEmision;
    private BigDecimal subtotal;
    private BigDecimal iva;
    private BigDecimal total;
    private Long idMetodoPago;
    private Long idEstadoFactura;
    private Boolean activo;

}
