package com.example.hospital.medicoespecialidades;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hospital.especialidadmedico.EspecialidadMedico;
import com.example.hospital.especialidadmedico.EspecialidadMedicoRepository;
import com.example.hospital.medico.MedicoRepository;
import com.example.hospital.medicoespecialidades.dto.MedicoEspecialidadesDTO;
import com.example.hospital.medicoespecialidades.impl.MedicoEspecialidadesServiceImpl;
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
class MedicoEspecialidadesServiceImplTest {

    @Mock MedicoEspecialidadesRepository medicoEspecialidadesRepository;
    @Mock MedicoRepository medicoRepository;
    @Mock EspecialidadMedicoRepository especialidadMedicoRepository;
    @InjectMocks MedicoEspecialidadesServiceImpl service;

    private final MedicoEspecialidadesDTO dto = new MedicoEspecialidadesDTO(2L, 5L);

    private EspecialidadMedico especialidad(boolean activa) {
        EspecialidadMedico e = new EspecialidadMedico();
        e.setIdEspecialidad(2L);
        e.setNombre("Cardiologia");
        e.setActivo(activa);
        return e;
    }

    @Test
    void crear_valido_guardaLaAsignacion() {
        when(medicoRepository.existsById(5L)).thenReturn(true);
        when(especialidadMedicoRepository.findById(2L)).thenReturn(Optional.of(especialidad(true)));
        when(medicoEspecialidadesRepository.existsByIdEspecialidadAndIdMedico(2L, 5L)).thenReturn(false);
        when(medicoEspecialidadesRepository.save(any(MedicoEspecialidades.class)))
                .thenAnswer(i -> i.getArgument(0));

        MedicoEspecialidadesDTO res = service.crear(dto);

        assertThat(res).isEqualTo(dto);
    }

    @Test
    void crear_conMedicoInexistente_lanzaReglaNegocio422_yNoGuarda() {
        when(medicoRepository.existsById(5L)).thenReturn(false);

        assertThrows(ReglaNegocioException.class, () -> service.crear(dto));

        verify(medicoEspecialidadesRepository, never()).save(any());
    }

    @Test
    void crear_conEspecialidadInactiva_lanzaReglaNegocio422_yNoGuarda() {
        when(medicoRepository.existsById(5L)).thenReturn(true);
        when(especialidadMedicoRepository.findById(2L)).thenReturn(Optional.of(especialidad(false)));

        assertThrows(ReglaNegocioException.class, () -> service.crear(dto));

        verify(medicoEspecialidadesRepository, never()).save(any());
    }

    @Test
    void crear_conAsignacionRepetida_lanzaDuplicado409_yNoGuarda() {
        when(medicoRepository.existsById(5L)).thenReturn(true);
        when(especialidadMedicoRepository.findById(2L)).thenReturn(Optional.of(especialidad(true)));
        when(medicoEspecialidadesRepository.existsByIdEspecialidadAndIdMedico(2L, 5L)).thenReturn(true);

        assertThrows(DuplicadoException.class, () -> service.crear(dto));

        verify(medicoEspecialidadesRepository, never()).save(any());
    }

    @Test
    void eliminar_inexistente_lanzaNoEncontrado404_yNoBorra() {
        when(medicoEspecialidadesRepository.existsById(any())).thenReturn(false);

        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminar(2L, 5L));

        verify(medicoEspecialidadesRepository, never()).deleteById(any());
    }
}
