package com.example.hospital.consultas;

import com.example.hospital.shared.exception.ReglaNegocioException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Regla de edición de consultas: una consulta solo se puede modificar (datos clínicos y diagnósticos asociados)
 * durante las 24 horas siguientes a su fecha/hora de atención. Pasado ese plazo queda como registro histórico.
 */
public final class VentanaEdicionConsulta {

    public static final Duration VENTANA = Duration.ofHours(24);
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private VentanaEdicionConsulta() {
    }

    /** true mientras no hayan pasado 24 horas desde la fecha/hora de la consulta. */
    public static boolean esEditable(LocalDateTime fechaHora, LocalDateTime ahora) {
        return fechaHora == null || !ahora.isAfter(fechaHora.plus(VENTANA));
    }

    public static boolean esEditable(Consultas consulta) {
        return esEditable(consulta.getFechaHora(), LocalDateTime.now());
    }

    /** Lanza 422 si la consulta ya salió de la ventana de edición. */
    public static void exigirEditable(Consultas consulta) {
        if (!esEditable(consulta)) {
            throw new ReglaNegocioException("La consulta #" + consulta.getIdConsulta() + " del "
                    + consulta.getFechaHora().format(FORMATO)
                    + " ya no se puede modificar: las consultas solo se editan durante las 24 horas siguientes a su atención.");
        }
    }
}
