package com.example.hospital.reportes.dto;

import com.example.hospital.citas.dto.CitasDTO;
import com.example.hospital.consultas.dto.ConsultasDTO;
import com.example.hospital.cuentas.dto.CuentasDTO;
import com.example.hospital.diagnosticoconsulta.dto.DiagnosticoConsultaDTO;
import com.example.hospital.diagnosticos.dto.DiagnosticosDTO;
import com.example.hospital.emergencia.dto.EmergenciaDTO;
import com.example.hospital.facturas.dto.FacturasDTO;
import com.example.hospital.hospitalizaciones.dto.HospitalizacionesDTO;
import com.example.hospital.paciente.dto.PacienteResponse;
import com.example.hospital.tratamiento.dto.TratamientoDTO;
import com.example.hospital.tratamientomedicamento.dto.TratamientoMedicamentoDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Expediente consolidado de un paciente: datos generales y todo su historial clínico y administrativo")
public record HistorialPacienteDTO(
        @Schema(description = "Datos generales del paciente (incluye `activo`: un paciente inactivo conserva su historial)")
        PacienteResponse paciente,
        @Schema(description = "Citas del paciente")
        List<CitasDTO> citas,
        @Schema(description = "Hospitalizaciones del paciente")
        List<HospitalizacionesDTO> hospitalizaciones,
        @Schema(description = "Ingresos a emergencia del paciente")
        List<EmergenciaDTO> emergencias,
        @Schema(description = "Cuentas hospitalarias. Vacía si el usuario no es ADMIN (minimización de datos): ver `incluyeDatosFinancieros`")
        List<CuentasDTO> cuentas,
        @Schema(description = "Facturas emitidas. Vacía si el usuario no es ADMIN (minimización de datos): ver `incluyeDatosFinancieros`")
        List<FacturasDTO> facturas,
        @Schema(description = "Consultas realizadas en las citas del paciente")
        List<ConsultasDTO> consultas,
        @Schema(description = "Relación consulta-diagnóstico (idConsulta, idDiagnostico) de esas consultas")
        List<DiagnosticoConsultaDTO> diagnosticosConsulta,
        @Schema(description = "Diagnósticos asociados a esas consultas")
        List<DiagnosticosDTO> diagnosticos,
        @Schema(description = "Tratamientos de esos diagnósticos")
        List<TratamientoDTO> tratamientos,
        @Schema(description = "Medicamentos recetados en cada tratamiento (idMedicamento, idTratamiento)")
        List<TratamientoMedicamentoDTO> medicamentosTratamiento,
        @Schema(description = "true si la respuesta incluye cuentas y facturas (solo ADMIN: es el único rol con acceso a este endpoint que puede verlas); false si se omitieron por el rol del usuario")
        boolean incluyeDatosFinancieros,
        @Schema(description = "Eco del parámetro: true si se incluyen registros anulados (activo=false) en todas las secciones")
        boolean incluirAnulados
) {}
