package com.example.hospital.consultas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hospital.citas.CitasRepository;
import com.example.hospital.consultas.dto.ConsultasDTO;
import com.example.hospital.consultas.impl.ConsultasServiceImpl;
import com.example.hospital.diagnosticoconsulta.DiagnosticoConsultaRepository;
import com.example.hospital.diagnosticoconsulta.dto.DiagnosticoConsultaDTO;
import com.example.hospital.diagnosticoconsulta.impl.DiagnosticoConsultaServiceImpl;
import com.example.hospital.diagnosticos.DiagnosticosRepository;
import com.example.hospital.estadocita.EstadoCitaRepository;
import com.example.hospital.estadocita.EstadoCitaService;
import com.example.hospital.shared.exception.ReglaNegocioException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Consultas pasadas en solo lectura: ventana de 24 horas desde la atención. */
class EdicionConsultaTest {

    private static Consultas consultaDel(LocalDateTime fechaHora) {
        Consultas c = new Consultas();
        c.setIdConsulta(10L);
        c.setFechaHora(fechaHora);
        c.setActivo(true);
        c.setIdCita(1L);
        return c;
    }

    @Nested
    class Ventana {
        @Test
        void esEditableDentroDeLas24Horas_yNoDespues() {
            LocalDateTime atencion = LocalDateTime.of(2026, 11, 9, 10, 0);
            assertThat(VentanaEdicionConsulta.esEditable(atencion, atencion.plusHours(23).plusMinutes(59))).isTrue();
            assertThat(VentanaEdicionConsulta.esEditable(atencion, atencion.plusHours(24))).isTrue();
            assertThat(VentanaEdicionConsulta.esEditable(atencion, atencion.plusHours(24).plusMinutes(1))).isFalse();
        }

        @Test
        void exigirEditable_rechazaUnaConsultaDeHaceDosDias() {
            assertThrows(ReglaNegocioException.class,
                    () -> VentanaEdicionConsulta.exigirEditable(consultaDel(LocalDateTime.now().minusDays(2))));
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class ActualizarConsulta {
        @Mock ConsultasRepository consultasRepository;
        @Mock CitasRepository citasRepository;
        @Mock EstadoCitaRepository estadoCitaRepository;
        @Mock DiagnosticoConsultaRepository diagnosticoConsultaRepository;
        @Mock EstadoCitaService estadoCitaService;
        @InjectMocks ConsultasServiceImpl service;

        @Test
        void consultaPasada_noSePuedeEditar_yNoSeGuarda() {
            when(consultasRepository.findById(10L)).thenReturn(Optional.of(consultaDel(LocalDateTime.now().minusDays(3))));
            ConsultasDTO cambios = new ConsultasDTO(null, null, "Otro síntoma", null, null, null, null, null, 1L, null);

            assertThrows(ReglaNegocioException.class, () -> service.actualizar(cambios, 10L));
            verify(consultasRepository, never()).save(any());
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class DiagnosticosDeConsultaPasada {
        @Mock DiagnosticoConsultaRepository diagnosticoConsultaRepository;
        @Mock DiagnosticosRepository diagnosticosRepository;
        @Mock ConsultasRepository consultasRepository;
        @InjectMocks DiagnosticoConsultaServiceImpl service;

        @Test
        void noSeAsociaUnDiagnosticoAUnaConsultaPasada() {
            when(consultasRepository.findById(10L)).thenReturn(Optional.of(consultaDel(LocalDateTime.now().minusDays(2))));

            assertThrows(ReglaNegocioException.class, () -> service.crear(new DiagnosticoConsultaDTO(1L, 10L)));
            verify(diagnosticoConsultaRepository, never()).save(any());
        }

        @Test
        void noSeQuitaUnDiagnosticoDeUnaConsultaPasada() {
            when(diagnosticoConsultaRepository.existsById(any())).thenReturn(true);
            when(consultasRepository.findById(10L)).thenReturn(Optional.of(consultaDel(LocalDateTime.now().minusDays(2))));

            assertThrows(ReglaNegocioException.class, () -> service.eliminar(1L, 10L));
            verify(diagnosticoConsultaRepository, never()).deleteById(any());
        }
    }
}
