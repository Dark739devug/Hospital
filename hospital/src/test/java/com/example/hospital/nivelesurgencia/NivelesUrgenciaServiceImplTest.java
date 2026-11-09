package com.example.hospital.nivelesurgencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hospital.nivelesurgencia.dto.NivelesUrgenciaDTO;
import com.example.hospital.nivelesurgencia.impl.NivelesUrgenciaServiceImpl;
import com.example.hospital.shared.exception.DuplicadoException;
import com.example.hospital.shared.exception.RecursoNoEncontradoException;
import com.example.hospital.shared.exception.ReglaNegocioException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NivelesUrgenciaServiceImplTest {

    @Mock NivelesUrgenciaRepository nivelesUrgenciaRepository;
    @InjectMocks NivelesUrgenciaServiceImpl service;

    private NivelesUrgenciaDTO dto(String codigo, Integer espera) {
        return new NivelesUrgenciaDTO(null, codigo, " Critico ", espera, "rojo", null);
    }

    @Test
    void crear_valido_guardaCodigoEnMayusculasYActivo() {
        when(nivelesUrgenciaRepository.existsByCodigoIgnoreCase("n1")).thenReturn(false);
        when(nivelesUrgenciaRepository.save(any(NivelesUrgencia.class))).thenAnswer(i -> i.getArgument(0));

        NivelesUrgenciaDTO res = service.crear(dto("n1", 0));

        assertThat(res.codigo()).isEqualTo("N1");
        assertThat(res.descripcion()).isEqualTo("Critico");
        assertThat(res.activo()).isTrue();
    }

    @Test
    void crear_conCodigoDeFormatoInvalido_lanzaReglaNegocio422_yNoGuarda() {
        assertThrows(ReglaNegocioException.class, () -> service.crear(dto("ROJO", 5)));

        verify(nivelesUrgenciaRepository, never()).save(any());
    }

    @Test
    void crear_conEsperaNegativa_lanzaReglaNegocio422_yNoGuarda() {
        assertThrows(ReglaNegocioException.class, () -> service.crear(dto("N2", -1)));

        verify(nivelesUrgenciaRepository, never()).save(any());
    }

    @Test
    void crear_conCodigoDuplicado_lanzaDuplicado409_yNoGuarda() {
        when(nivelesUrgenciaRepository.existsByCodigoIgnoreCase("N1")).thenReturn(true);

        assertThrows(DuplicadoException.class, () -> service.crear(dto("N1", 0)));

        verify(nivelesUrgenciaRepository, never()).save(any());
    }

    @Test
    void findById_inexistente_lanzaNoEncontrado404() {
        when(nivelesUrgenciaRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.findById(5L));
    }

    @Test
    void eliminar_haceBorradoLogico() {
        NivelesUrgencia n = new NivelesUrgencia();
        n.setIdNivelUrgencia(1L);
        n.setActivo(true);
        when(nivelesUrgenciaRepository.findById(1L)).thenReturn(Optional.of(n));

        service.eliminar(1L);

        assertThat(n.getActivo()).isFalse();
        verify(nivelesUrgenciaRepository).save(n);
    }
}
