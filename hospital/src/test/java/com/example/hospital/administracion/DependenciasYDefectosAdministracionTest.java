package com.example.hospital.administracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hospital.asignacionenfermeros.AsignacionEnfermerosRepository;
import com.example.hospital.clinicas.Clinicas;
import com.example.hospital.clinicas.ClinicasRepository;
import com.example.hospital.clinicas.impl.ClinicasServiceImpl;
import com.example.hospital.departamentos.Departamentos;
import com.example.hospital.departamentos.DepartamentosRepository;
import com.example.hospital.departamentos.dto.DepartamentosDTO;
import com.example.hospital.departamentos.impl.DepartamentosServiceImpl;
import com.example.hospital.enfermeros.Enfermeros;
import com.example.hospital.enfermeros.EnfermerosRepository;
import com.example.hospital.enfermeros.dto.EnfermerosDTO;
import com.example.hospital.enfermeros.impl.EnfermerosServiceImpl;
import com.example.hospital.usuarios.GeneradorCredencialesService;
import com.example.hospital.especialidadclinica.EspecialidadClinica;
import com.example.hospital.especialidadclinica.EspecialidadClinicaRepository;
import com.example.hospital.especialidadclinica.impl.EspecialidadClinicaServiceImpl;
import com.example.hospital.facturas.FacturasRepository;
import com.example.hospital.hospitalizaciones.HospitalizacionesRepository;
import com.example.hospital.metodospago.MetodosPago;
import com.example.hospital.metodospago.MetodosPagoRepository;
import com.example.hospital.metodospago.impl.MetodosPagoServiceImpl;
import com.example.hospital.pagos.PagosRepository;
import com.example.hospital.shared.catalogos.EstadosCatalogo;
import com.example.hospital.shared.exception.DuplicadoException;
import com.example.hospital.shared.exception.ReglaNegocioException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** DELETE valida dependencias (409) y los catálogos nacen activos aunque el cuerpo no traiga `activo` (antes: 500 por NOT NULL). */
@ExtendWith(MockitoExtension.class)
class DependenciasYDefectosAdministracionTest {

    @Mock DepartamentosRepository departamentosRepository;
    @Mock ClinicasRepository clinicasRepository;
    @Mock EnfermerosRepository enfermerosRepository;
    @Mock AsignacionEnfermerosRepository asignacionEnfermerosRepository;
    @Mock HospitalizacionesRepository hospitalizacionesRepository;
    @Mock EstadosCatalogo estados;
    @Mock EspecialidadClinicaRepository especialidadClinicaRepository;
    @Mock MetodosPagoRepository metodosPagoRepository;
    @Mock FacturasRepository facturasRepository;
    @Mock PagosRepository pagosRepository;
    @Mock GeneradorCredencialesService generadorCredencialesService;
    @InjectMocks DepartamentosServiceImpl departamentos;
    @InjectMocks EnfermerosServiceImpl enfermeros;
    @InjectMocks ClinicasServiceImpl clinicas;
    @InjectMocks EspecialidadClinicaServiceImpl especialidadesClinica;
    @InjectMocks MetodosPagoServiceImpl metodosPago;

    private Departamentos depto() {
        Departamentos d = new Departamentos();
        d.setIdDepartamento(1L);
        d.setNombre("Pediatria");
        d.setEstado(true);
        return d;
    }

    // ---------- departamentos ----------

    @Test
    void departamento_eliminar_409_conClinicasActivas() {
        when(departamentosRepository.findById(1L)).thenReturn(Optional.of(depto()));
        when(clinicasRepository.existsByIdDepartamentoAndEstadoClinicaTrue(1L)).thenReturn(true);

        assertThrows(DuplicadoException.class, () -> departamentos.eliminar(1L));

        verify(departamentosRepository, never()).save(any());
    }

    @Test
    void departamento_eliminar_409_conEnfermerosActivos() {
        when(departamentosRepository.findById(1L)).thenReturn(Optional.of(depto()));
        when(clinicasRepository.existsByIdDepartamentoAndEstadoClinicaTrue(1L)).thenReturn(false);
        when(enfermerosRepository.existsByIdDepartamentoAndActivoTrue(1L)).thenReturn(true);

        assertThrows(DuplicadoException.class, () -> departamentos.eliminar(1L));

        verify(departamentosRepository, never()).save(any());
    }

    @Test
    void departamento_eliminar_desactiva_sinDependenciasActivas() {
        Departamentos d = depto();
        when(departamentosRepository.findById(1L)).thenReturn(Optional.of(d));

        departamentos.eliminar(1L);

        assertThat(d.getEstado()).isFalse();
        verify(departamentosRepository).save(d);
    }

    @Test
    void departamento_crear_naceActivo_yElDuplicadoEs409() {
        when(departamentosRepository.save(any(Departamentos.class))).thenAnswer(inv -> inv.getArgument(0));
        assertThat(departamentos.crear(new DepartamentosDTO(null, "Cirugia", null, null, null)).estado()).isTrue();

        when(departamentosRepository.existsByNombreIgnoreCase("Cirugia")).thenReturn(true);
        assertThrows(DuplicadoException.class, () -> departamentos.crear(new DepartamentosDTO(null, "Cirugia", null, null, null)));
    }

    // ---------- enfermeros ----------

    private Enfermeros enfermero() {
        Enfermeros e = new Enfermeros();
        e.setIdEnfermero(5L);
        e.setActivo(true);
        return e;
    }

    @Test
    void enfermero_eliminar_409_conAsignacionesActivas() {
        when(enfermerosRepository.findById(5L)).thenReturn(Optional.of(enfermero()));
        when(asignacionEnfermerosRepository.existsByIdEnfermeroAndActivoTrue(5L)).thenReturn(true);

        assertThrows(DuplicadoException.class, () -> enfermeros.eliminar(5L));

        verify(enfermerosRepository, never()).save(any());
    }

    @Test
    void enfermero_eliminar_desactiva_sinAsignacionesActivas() {
        Enfermeros e = enfermero();
        when(enfermerosRepository.findById(5L)).thenReturn(Optional.of(e));

        enfermeros.eliminar(5L);

        assertThat(e.getActivo()).isFalse();
    }

    @Test
    void enfermero_crear_naceActivo_cedulaDuplicada409_departamentoInactivo422() {
        lenient().when(departamentosRepository.findById(1L)).thenReturn(Optional.of(depto()));
        when(enfermerosRepository.save(any(Enfermeros.class))).thenAnswer(inv -> inv.getArgument(0));
        EnfermerosDTO dto = new EnfermerosDTO(null, "ENF-1", "Rosa", "Diaz", null, null, 1L, null);

        assertThat(enfermeros.crear(dto).activo()).isTrue();

        when(enfermerosRepository.existsByCedulaProfesional("ENF-1")).thenReturn(true);
        assertThrows(DuplicadoException.class, () -> enfermeros.crear(dto));

        Departamentos inactivo = depto();
        inactivo.setEstado(false);
        when(enfermerosRepository.existsByCedulaProfesional("ENF-2")).thenReturn(false);
        when(departamentosRepository.findById(2L)).thenReturn(Optional.of(inactivo));
        assertThrows(ReglaNegocioException.class,
                () -> enfermeros.crear(new EnfermerosDTO(null, "ENF-2", "Rosa", "Diaz", null, null, 2L, null)));
    }

    // ---------- clinicas ----------

    @Test
    void clinica_eliminar_409_conHospitalizacionesActivas_yDesactivaSiNoHay() {
        Clinicas c = new Clinicas();
        c.setIdClinica(3L);
        c.setEstadoClinica(true);
        when(clinicasRepository.findById(3L)).thenReturn(Optional.of(c));
        when(estados.hospActiva()).thenReturn(1L);
        when(hospitalizacionesRepository.existsByIdClinicaAndIdEstadoHosp(3L, 1L)).thenReturn(true, false);

        assertThrows(DuplicadoException.class, () -> clinicas.eliminar(3L));
        assertThat(c.getEstadoClinica()).isTrue();

        clinicas.eliminar(3L);
        assertThat(c.getEstadoClinica()).isFalse();
    }

    // ---------- especialidad de clinica ----------

    @Test
    void especialidadClinica_eliminar_409_siLaUsanClinicasActivas() {
        EspecialidadClinica e = new EspecialidadClinica();
        e.setIdEspecialidad(2L);
        e.setActivo(true);
        when(especialidadClinicaRepository.findById(2L)).thenReturn(Optional.of(e));
        when(clinicasRepository.existsByIdEspecialidadAndEstadoClinicaTrue(2L)).thenReturn(true);

        assertThrows(DuplicadoException.class, () -> especialidadesClinica.eliminar(2L));

        assertThat(e.getActivo()).isTrue();
    }

    // ---------- metodos de pago ----------

    @Test
    void metodoPago_eliminar_409_siLoUsanFacturasOPagos() {
        MetodosPago m = new MetodosPago(1L, "Efectivo", true);
        when(metodosPagoRepository.findById(1L)).thenReturn(Optional.of(m));
        when(facturasRepository.existsByIdMetodoPago(1L)).thenReturn(true, false);
        when(pagosRepository.existsByIdMetodoPago(1L)).thenReturn(true, false);

        assertThrows(DuplicadoException.class, () -> metodosPago.eliminar(1L));
        assertThrows(DuplicadoException.class, () -> metodosPago.eliminar(1L));
        assertThat(m.getActivo()).isTrue();

        metodosPago.eliminar(1L);
        assertThat(m.getActivo()).isFalse();
    }
}
