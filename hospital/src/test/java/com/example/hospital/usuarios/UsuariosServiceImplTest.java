package com.example.hospital.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hospital.rol.Rol;
import com.example.hospital.rol.RolRepository;
import com.example.hospital.shared.exception.DuplicadoException;
import com.example.hospital.shared.exception.RecursoNoEncontradoException;
import com.example.hospital.shared.exception.ReglaNegocioException;
import com.example.hospital.usuarios.dto.UsuariosDTO;
import com.example.hospital.usuarios.impl.UsuariosServiceImpl;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuariosServiceImplTest {

    private static final Long ADMIN = 1L;
    private static final Long CAJERO = 6L;

    @Mock UsuariosRepository usuariosRepository;
    @Mock RolRepository rolRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UsuariosServiceImpl service;

    @BeforeEach
    void preparar() {
        lenient().when(usuariosRepository.save(any(Usuarios.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(rolRepository.findByNombre("ADMIN")).thenReturn(Optional.of(rol(ADMIN, "ADMIN", true)));
        lenient().when(rolRepository.findById(ADMIN)).thenReturn(Optional.of(rol(ADMIN, "ADMIN", true)));
        lenient().when(rolRepository.findById(CAJERO)).thenReturn(Optional.of(rol(CAJERO, "CAJERO", true)));
        lenient().when(passwordEncoder.encode(any())).thenReturn("hash");
        autenticarComo("admin");
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(String nombre) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(nombre, null));
    }

    private static Rol rol(Long id, String nombre, boolean activo) {
        Rol r = new Rol();
        r.setIdRol(id);
        r.setNombre(nombre);
        r.setActivo(activo);
        return r;
    }

    private Usuarios usuario(Long id, String nombre, Long idRol, boolean estado) {
        Usuarios u = new Usuarios();
        u.setIdUsuario(id);
        u.setNombreUsuario(nombre);
        u.setContrasenaHash("h");
        u.setEstado(estado);
        u.setIdRol(idRol);
        return u;
    }

    private UsuariosDTO nuevo(String nombre, String clave, Long idRol) {
        return new UsuariosDTO(null, nombre, clave, null, null, idRol, null, null);
    }

    // ---------- crear ----------

    @Test
    void crear_cifraLaContrasena_yNoDevuelveLaContrasena() {
        when(usuariosRepository.existsByNombreUsuarioIgnoreCase("nuevo")).thenReturn(false);

        UsuariosDTO r = service.crear(nuevo(" nuevo ", "Clave1234", CAJERO));

        assertThat(r.nombreUsuario()).isEqualTo("nuevo");
        assertThat(r.password()).isNull();
        assertThat(r.estado()).isTrue();
        assertThat(r.idRol()).isEqualTo(CAJERO);
        verify(passwordEncoder).encode("Clave1234");
    }

    @Test
    void crear_lanza422_sinRol_yNoAsignaRolPorDefecto() {
        assertThrows(ReglaNegocioException.class, () -> service.crear(nuevo("nuevo", "Clave1234", null)));

        verify(usuariosRepository, never()).save(any());
    }

    @Test
    void crear_lanza422_siElRolNoExisteOEstaInactivo() {
        when(usuariosRepository.existsByNombreUsuarioIgnoreCase("nuevo")).thenReturn(false);
        when(rolRepository.findById(99L)).thenReturn(Optional.empty());
        when(rolRepository.findById(7L)).thenReturn(Optional.of(rol(7L, "X", false)));

        assertThrows(ReglaNegocioException.class, () -> service.crear(nuevo("nuevo", "Clave1234", 99L)));
        assertThrows(ReglaNegocioException.class, () -> service.crear(nuevo("nuevo", "Clave1234", 7L)));

        verify(usuariosRepository, never()).save(any());
    }

    @Test
    void crear_lanza409_siElNombreDeUsuarioYaExiste() {
        when(usuariosRepository.existsByNombreUsuarioIgnoreCase("admin")).thenReturn(true);

        assertThrows(DuplicadoException.class, () -> service.crear(nuevo("admin", "Clave1234", CAJERO)));

        verify(usuariosRepository, never()).save(any());
    }

    @Test
    void crear_lanza422_siLaContrasenaTieneMenosDeOchoCaracteres() {
        assertThrows(ReglaNegocioException.class, () -> service.crear(nuevo("nuevo", "1234567", CAJERO)));
    }

    // ---------- actualizar ----------

    @Test
    void actualizar_lanza409_alRenombrarAUnNombreExistente_yNoDa500() {
        when(usuariosRepository.findById(8L)).thenReturn(Optional.of(usuario(8L, "x", CAJERO, true)));
        when(usuariosRepository.existsByNombreUsuarioIgnoreCaseAndIdUsuarioNot("admin", 8L)).thenReturn(true);

        assertThrows(DuplicadoException.class, () -> service.actualizar(new UsuariosDTO(null, "admin", null, null, null, null, null, null), 8L));
    }

    @Test
    void actualizar_lanza422_conRolInexistente_yConContrasenaCorta() {
        when(usuariosRepository.findById(8L)).thenReturn(Optional.of(usuario(8L, "x", CAJERO, true)));
        when(rolRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ReglaNegocioException.class, () -> service.actualizar(new UsuariosDTO(null, null, null, null, null, 99L, null, null), 8L));
        assertThrows(ReglaNegocioException.class, () -> service.actualizar(new UsuariosDTO(null, null, "corta", null, null, null, null, null), 8L));
    }

    @Test
    void actualizar_cambiaContrasenaYRol_yConservaLoDemas() {
        Usuarios u = usuario(8L, "x", CAJERO, true);
        when(usuariosRepository.findById(8L)).thenReturn(Optional.of(u));

        service.actualizar(new UsuariosDTO(null, null, "OtraClave99", null, null, ADMIN, null, null), 8L);

        assertThat(u.getNombreUsuario()).isEqualTo("x");
        assertThat(u.getEstado()).isTrue();
        assertThat(u.getIdRol()).isEqualTo(ADMIN);
        assertThat(u.getContrasenaHash()).isEqualTo("hash");
    }

    @Test
    void actualizar_conContrasenaNueva_laDejaTemporalPor24Horas_paraRestablecerElAcceso() {
        Usuarios u = usuario(8L, "x", CAJERO, true);
        u.setDebeCambiarPassword(true);
        u.setFechaExpiracionPassword(java.time.LocalDateTime.now().minusDays(2)); // temporal ya vencida
        when(usuariosRepository.findById(8L)).thenReturn(Optional.of(u));

        service.actualizar(new UsuariosDTO(null, null, "OtraClave99", null, null, null, null, null), 8L);

        assertThat(u.getDebeCambiarPassword()).isTrue();
        assertThat(u.getFechaExpiracionPassword()).isAfter(java.time.LocalDateTime.now().plusHours(23));
    }

    @Test
    void actualizar_sinContrasena_noTocaLaMarcaTemporal() {
        Usuarios u = usuario(8L, "x", CAJERO, true);
        when(usuariosRepository.findById(8L)).thenReturn(Optional.of(u));

        service.actualizar(new UsuariosDTO(null, "nuevo", null, null, null, null, null, null), 8L);

        assertThat(u.getDebeCambiarPassword()).isFalse();
        assertThat(u.getFechaExpiracionPassword()).isNull();
    }

    @Test
    void actualizar_unAdminNoPuedeDesactivarseNiQuitarseElRolAdAdminASiMismo_aunqueHayaOtrosAdmins() {
        Usuarios yo = usuario(1L, "admin", ADMIN, true);
        when(usuariosRepository.findById(1L)).thenReturn(Optional.of(yo));
        lenient().when(usuariosRepository.countByIdRolAndEstadoTrue(ADMIN)).thenReturn(3L);

        assertThrows(ReglaNegocioException.class, () -> service.actualizar(new UsuariosDTO(null, null, null, false, null, null, null, null), 1L));
        assertThrows(ReglaNegocioException.class, () -> service.actualizar(new UsuariosDTO(null, null, null, null, null, CAJERO, null, null), 1L));

        assertThat(yo.getEstado()).isTrue();
        assertThat(yo.getIdRol()).isEqualTo(ADMIN);
    }

    @Test
    void actualizar_noSePuedeDesactivarNiDegradarAlUltimoAdminActivo() {
        Usuarios otro = usuario(2L, "admin2", ADMIN, true);
        when(usuariosRepository.findById(2L)).thenReturn(Optional.of(otro));
        when(usuariosRepository.countByIdRolAndEstadoTrue(ADMIN)).thenReturn(1L);

        assertThrows(ReglaNegocioException.class, () -> service.actualizar(new UsuariosDTO(null, null, null, false, null, null, null, null), 2L));
        assertThrows(ReglaNegocioException.class, () -> service.actualizar(new UsuariosDTO(null, null, null, null, null, CAJERO, null, null), 2L));

        assertThat(otro.getEstado()).isTrue();
        assertThat(otro.getIdRol()).isEqualTo(ADMIN);
    }

    @Test
    void actualizar_sePuedeDesactivarAOtroAdmin_siQuedaAlMenosUnoActivo() {
        Usuarios otro = usuario(2L, "admin2", ADMIN, true);
        when(usuariosRepository.findById(2L)).thenReturn(Optional.of(otro));
        when(usuariosRepository.countByIdRolAndEstadoTrue(ADMIN)).thenReturn(2L);

        service.actualizar(new UsuariosDTO(null, null, null, false, null, null, null, null), 2L);

        assertThat(otro.getEstado()).isFalse();
    }

    @Test
    void actualizar_noRestringeAUsuariosQueNoSonAdmin() {
        Usuarios u = usuario(8L, "x", CAJERO, true);
        when(usuariosRepository.findById(8L)).thenReturn(Optional.of(u));

        service.actualizar(new UsuariosDTO(null, null, null, false, null, null, null, null), 8L);

        assertThat(u.getEstado()).isFalse();
    }

    @Test
    void actualizar_y_eliminar_lanzan404_siNoExiste() {
        when(usuariosRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.actualizar(new UsuariosDTO(null, "a", null, null, null, null, null, null), 99L));
        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminar(99L));
    }

    // ---------- eliminar (desactivar) ----------

    @Test
    void eliminar_desactivaAUnUsuarioNormal() {
        Usuarios u = usuario(8L, "x", CAJERO, true);
        when(usuariosRepository.findById(8L)).thenReturn(Optional.of(u));

        service.eliminar(8L);

        assertThat(u.getEstado()).isFalse();
        verify(usuariosRepository).save(u);
    }

    @Test
    void eliminar_noSePuedeDesactivarUnoMismoNiAlUltimoAdmin() {
        Usuarios yo = usuario(1L, "admin", ADMIN, true);
        Usuarios ultimo = usuario(2L, "admin2", ADMIN, true);
        when(usuariosRepository.findById(1L)).thenReturn(Optional.of(yo));
        when(usuariosRepository.findById(2L)).thenReturn(Optional.of(ultimo));
        lenient().when(usuariosRepository.countByIdRolAndEstadoTrue(ADMIN)).thenReturn(1L);

        assertThrows(ReglaNegocioException.class, () -> service.eliminar(1L));
        assertThrows(ReglaNegocioException.class, () -> service.eliminar(2L));

        assertThat(yo.getEstado()).isTrue();
        assertThat(ultimo.getEstado()).isTrue();
        verify(usuariosRepository, never()).save(any());
    }

    // ---------- DTO ----------

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> campos(UsuariosDTO dto, Class<?>... grupos) {
        return validator.validate(dto, grupos).stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    @Test
    void dto_alCrear_exigeUsuarioContrasenaYRol_ylaContrasenaMinimaDeOcho() {
        assertThat(campos(new UsuariosDTO(null, null, null, null, null, null, null, null), UsuariosDTO.Crear.class))
                .containsExactlyInAnyOrder("nombreUsuario", "password", "idRol");
        assertThat(campos(nuevo("u", "1234567", CAJERO), UsuariosDTO.Crear.class)).containsExactly("password");
        assertThat(campos(nuevo("u", "12345678", CAJERO), UsuariosDTO.Crear.class)).isEmpty();
        assertThat(campos(nuevo("x".repeat(51), "12345678", CAJERO), UsuariosDTO.Crear.class)).containsExactly("nombreUsuario");
    }

    @Test
    void dto_alActualizar_todoEsOpcional_peroSiSeEnviaLaContrasenaDebeTenerOchoCaracteres() {
        assertThat(campos(new UsuariosDTO(null, null, null, null, null, null, null, null))).isEmpty();
        assertThat(campos(new UsuariosDTO(null, null, "corta", null, null, null, null, null))).containsExactly("password");
    }

    @Test
    void dto_laContrasenaNuncaSeSerializa() {
        tools.jackson.databind.json.JsonMapper mapper = tools.jackson.databind.json.JsonMapper.builder().build();

        String json = mapper.writeValueAsString(new UsuariosDTO(1L, "u", "SecretaSecreta", true, null, CAJERO, null, null));

        assertThat(json).doesNotContain("password").doesNotContain("SecretaSecreta");
        // pero sí se acepta como entrada
        UsuariosDTO entrada = mapper.readValue("{\"nombreUsuario\":\"u\",\"password\":\"Clave1234\",\"idRol\":6}", UsuariosDTO.class);
        assertThat(entrada.password()).isEqualTo("Clave1234");
    }
}
