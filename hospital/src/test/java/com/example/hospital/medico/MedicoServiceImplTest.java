package com.example.hospital.medico;

import static org.mockito.Mockito.lenient;
import org.junit.jupiter.api.BeforeEach;
import com.example.hospital.shared.catalogos.EstadosCatalogo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hospital.citas.CitasRepository;
import com.example.hospital.emergencia.EmergenciaRepository;
import com.example.hospital.hospitalizaciones.HospitalizacionesRepository;
import com.example.hospital.medico.dto.MedicoDTO;
import com.example.hospital.medico.impl.MedicoServiceImpl;
import com.example.hospital.shared.exception.RecursoNoEncontradoException;
import com.example.hospital.shared.exception.ReglaNegocioException;
import java.util.List;
import java.util.Optional;
import com.example.hospital.usuarios.GeneradorCredencialesService;
import com.example.hospital.usuarios.dto.CredencialesGeneradasDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MedicoServiceImplTest {

    @Mock MedicoRepository medicoRepository;
    @Mock CitasRepository citasRepository;
    @Mock HospitalizacionesRepository hospitalizacionesRepository;
    @Mock EmergenciaRepository emergenciaRepository;
    @Mock EstadosCatalogo estados;
    @Mock GeneradorCredencialesService generadorCredencialesService;
    @InjectMocks MedicoServiceImpl service;

    @BeforeEach
    void catalogo() {
        // El estado Activa se resuelve por nombre: aquí le corresponde el id 1
        lenient().when(estados.hospActiva()).thenReturn(1L);
    }

    private MedicoDTO dto(String colegiado, String nombre, String email, String telefono) {
        return new MedicoDTO(null, colegiado, nombre, "Perez", null, telefono, email, null);
    }

    private Medico medico(Long id, boolean estado) {
        Medico m = new Medico();
        m.setIdMedico(id);
        m.setNoColegiado("12345");
        m.setNombre("Luis");
        m.setApellidoPaterno("Perez");
        m.setEstado(estado);
        return m;
    }

    @Test
    void crear_normalizaLosDatos_yLoDejaActivo() {
        when(medicoRepository.existsByNoColegiado("12345")).thenReturn(false);
        when(medicoRepository.existsByEmailIgnoreCase("luis@mail.com")).thenReturn(false);
        when(medicoRepository.save(any(Medico.class))).thenAnswer(inv -> {
            Medico m = inv.getArgument(0);
            m.setIdMedico(1L);
            return m;
        });
        when(generadorCredencialesService.generarCredenciales("dr", "Luis", "Perez", "MEDICO"))
                .thenReturn(new CredencialesGeneradasDTO("dr.lperez", "Temp#1234!", java.time.LocalDateTime.now().plusDays(1), true, "MEDICO", "Dr. Luis Perez"));

        MedicoDTO r = service.crear(dto(" 12345 ", " Luis ", " Luis@Mail.com ", "55551234"));

        assertThat(r.idMedico()).isEqualTo(1L);
        assertThat(r.noColegiado()).isEqualTo("12345");
        assertThat(r.nombre()).isEqualTo("Luis");
        assertThat(r.email()).isEqualTo("luis@mail.com");
        assertThat(r.estado()).isTrue();
        assertThat(r.credenciales()).isNotNull();
        assertThat(r.credenciales().username()).isEqualTo("dr.lperez");
        assertThat(r.credenciales().passwordTemporal()).isEqualTo("Temp#1234!");
    }

    @Test
    void crear_lanzaReglaNegocio_siElEmailTieneFormatoInvalido_yNoGuarda() {
        when(medicoRepository.existsByNoColegiado("12345")).thenReturn(false);

        assertThrows(ReglaNegocioException.class, () -> service.crear(dto("12345", "Luis", "no-es-correo", null)));

        verify(medicoRepository, never()).save(any());
    }

    @Test
    void crear_lanzaReglaNegocio_siElNombreEstaVacio_yNoGuarda() {
        when(medicoRepository.existsByNoColegiado("12345")).thenReturn(false);

        assertThrows(ReglaNegocioException.class, () -> service.crear(dto("12345", "  ", null, null)));

        verify(medicoRepository, never()).save(any());
    }

    @Test
    void crear_lanzaReglaNegocio_siElTelefonoEsMuyCorto_yNoGuarda() {
        when(medicoRepository.existsByNoColegiado("12345")).thenReturn(false);

        assertThrows(ReglaNegocioException.class, () -> service.crear(dto("12345", "Luis", null, "123")));

        verify(medicoRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_lanzaReglaNegocio_alDesactivarUnMedicoConCitasActivas_yNoGuarda() {
        when(medicoRepository.findById(1L)).thenReturn(Optional.of(medico(1L, true)));
        when(citasRepository.existsByIdMedicoAndActivoTrue(1L)).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> service.cambiarEstado(1L, false));

        verify(medicoRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_lanzaReglaNegocio_alDesactivarUnMedicoConHospitalizacionActiva_yNoGuarda() {
        when(medicoRepository.findById(1L)).thenReturn(Optional.of(medico(1L, true)));
        when(citasRepository.existsByIdMedicoAndActivoTrue(1L)).thenReturn(false);
        when(hospitalizacionesRepository.existsByIdMedicoResponsableAndIdEstadoHosp(1L, 1L)).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> service.cambiarEstado(1L, false));

        verify(medicoRepository, never()).save(any());
    }

    @Test
    void eliminar_haceBorradoLogico_siNoTieneCompromisosActivos() {
        Medico m = medico(1L, true);
        when(medicoRepository.findById(1L)).thenReturn(Optional.of(m));
        when(citasRepository.existsByIdMedicoAndActivoTrue(1L)).thenReturn(false);
        when(hospitalizacionesRepository.existsByIdMedicoResponsableAndIdEstadoHosp(1L, 1L)).thenReturn(false);
        when(emergenciaRepository.findByIdMedicoAndActivoTrue(1L)).thenReturn(List.of());

        service.eliminar(1L);

        assertThat(m.getEstado()).isFalse();
        verify(medicoRepository).save(m);
        verify(medicoRepository, never()).deleteById(any());
    }

    @Test
    void cambiarEstado_lanzaRecursoNoEncontrado_siElMedicoNoExiste_yNoGuarda() {
        when(medicoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.cambiarEstado(99L, false));

        verify(medicoRepository, never()).save(any());
    }

    @Test
    void eliminar_lanzaReglaNegocio_siElMedicoYaEstaInactivo_yNoGuarda() {
        when(medicoRepository.findById(1L)).thenReturn(Optional.of(medico(1L, false)));

        assertThrows(ReglaNegocioException.class, () -> service.eliminar(1L));

        verify(medicoRepository, never()).save(any());
    }

    @Test
    void buscar_sinCriterio_devuelveSoloLosMedicosActivos() {
        when(medicoRepository.findByEstadoTrue()).thenReturn(List.of(medico(1L, true)));

        assertThat(service.buscar("  ")).hasSize(1);
        verify(medicoRepository, never())
                .findByNombreContainingIgnoreCaseOrApellidoPaternoContainingIgnoreCase(any(), any());
    }

    private Medico medicoNombre(Long id, String nombre, String apellido, String colegiado, boolean activo) {
        Medico m = medico(id, activo);
        m.setNombre(nombre);
        m.setApellidoPaterno(apellido);
        m.setNoColegiado(colegiado);
        return m;
    }

    @Test
    void buscar_combinaCriterioYSoloActivos_ybuscaTambienPorColegiado() {
        when(medicoRepository.findAll()).thenReturn(List.of(
                medicoNombre(1L, "Marta", "Ramirez", "COL-9001", true),
                medicoNombre(2L, "Pedro", "Gomez", "COL-7001", true),
                medicoNombre(3L, "Pablo", "Inactivo", "COL-7002", false)));

        assertThat(service.buscar(null, false)).extracting(MedicoDTO::idMedico).containsExactly(1L, 2L, 3L);
        assertThat(service.buscar(null, true)).extracting(MedicoDTO::idMedico).containsExactly(1L, 2L);
        assertThat(service.buscar("col-7", false)).extracting(MedicoDTO::idMedico).containsExactly(2L, 3L);
        assertThat(service.buscar("col-7", true)).extracting(MedicoDTO::idMedico).containsExactly(2L);
        assertThat(service.buscar("RAMIREZ", true)).extracting(MedicoDTO::idMedico).containsExactly(1L);
        // el criterio respeta soloActivos: un inactivo se encuentra solo si no se pide soloActivos
        assertThat(service.buscar("inactivo", true)).isEmpty();
        assertThat(service.buscar("inactivo", false)).extracting(MedicoDTO::idMedico).containsExactly(3L);
    }

    @Test
    void crear_lanza409_siElColegiadoOElCorreoYaExisten() {
        when(medicoRepository.existsByNoColegiado("COL-1")).thenReturn(true);
        MedicoDTO dto = new MedicoDTO(null, "COL-1", "Ana", "Perez", null, null, null, null);

        assertThrows(com.example.hospital.shared.exception.DuplicadoException.class, () -> service.crear(dto));

        when(medicoRepository.existsByNoColegiado("COL-2")).thenReturn(false);
        when(medicoRepository.existsByEmailIgnoreCase("a@b.com")).thenReturn(true);
        assertThrows(com.example.hospital.shared.exception.DuplicadoException.class,
                () -> service.crear(new MedicoDTO(null, "COL-2", "Ana", "Perez", null, null, "a@b.com", null)));
    }
}
