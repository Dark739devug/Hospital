package com.example.hospital.usuarios;

import com.example.hospital.rol.Rol;
import com.example.hospital.rol.RolRepository;
import com.example.hospital.shared.exception.ReglaNegocioException;
import com.example.hospital.usuarios.dto.CredencialesGeneradasDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.LocalDateTime;

@Service
public class GeneradorCredencialesService {

    private static final Logger log = LoggerFactory.getLogger(GeneradorCredencialesService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String LETRAS_MIN = "abcdefghjkmnpqrstuvwxyz"; // sin l, i, o para evitar confusiones
    private static final String LETRAS_MAY = "ABCDEFGHJKLMNPQRSTUVWXYZ"; // sin I, O
    private static final String NUMEROS = "23456789"; // sin 0, 1
    private static final String ESPECIALES = "!@#$%&*";

    private final UsuariosRepository usuariosRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public GeneradorCredencialesService(UsuariosRepository usuariosRepository,
                                       RolRepository rolRepository,
                                       PasswordEncoder passwordEncoder) {
        this.usuariosRepository = usuariosRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CredencialesGeneradasDTO generarCredenciales(String prefijo, String nombre, String apellidoPaterno, String nombreRol) {
        String baseUsuario = generarBaseUsuario(prefijo, nombre, apellidoPaterno);
        String username = resolverUsuarioUnico(baseUsuario);
        String passwordTemporal = generarPasswordTemporal();

        Rol rol = rolRepository.findByNombre(nombreRol.trim().toUpperCase())
                .filter(r -> !Boolean.FALSE.equals(r.getActivo()))
                .orElseThrow(() -> new ReglaNegocioException("No se encontró el rol '" + nombreRol + "' o está inactivo"));

        // 1 día de vigencia para la contraseña temporal
        LocalDateTime expiracion = LocalDateTime.now().plusDays(1);

        Usuarios usuario = new Usuarios();
        usuario.setNombreUsuario(username);
        usuario.setContrasenaHash(passwordEncoder.encode(passwordTemporal));
        usuario.setEstado(true);
        usuario.setIdRol(rol.getIdRol());
        usuario.setDebeCambiarPassword(true);
        usuario.setFechaExpiracionPassword(expiracion);

        usuariosRepository.save(usuario);
        log.info("Credenciales temporales generadas para {} {} (usuario: '{}', rol: {}, vence: {})",
                nombre, apellidoPaterno, username, rol.getNombre(), expiracion);

        String titulo = "MEDICO".equalsIgnoreCase(nombreRol) ? "Dr. " : "Enf. ";
        String nombreCompleto = titulo + nombre.trim() + " " + apellidoPaterno.trim();

        return new CredencialesGeneradasDTO(
                username,
                passwordTemporal,
                expiracion,
                true,
                rol.getNombre(),
                nombreCompleto
        );
    }

    private String generarBaseUsuario(String prefijo, String nombre, String apellidoPaterno) {
        String p = prefijo != null && !prefijo.isBlank() ? limpiarTexto(prefijo) + "." : "";
        String primerNombre = limpiarTexto(nombre.trim().split("\\s+")[0]);
        String primerApellido = limpiarTexto(apellidoPaterno.trim().split("\\s+")[0]);

        char inicial = primerNombre.isEmpty() ? 'u' : primerNombre.charAt(0);
        String base = p + inicial + primerApellido;
        if (base.length() > 40) {
            base = base.substring(0, 40);
        }
        return base;
    }

    private String resolverUsuarioUnico(String base) {
        String candidato = base;
        int secuencia = 1;
        while (usuariosRepository.existsByNombreUsuarioIgnoreCase(candidato)) {
            candidato = base + secuencia;
            secuencia++;
        }
        return candidato;
    }

    private String generarPasswordTemporal() {
        // Garantizar al menos 1 mayúscula, 1 minúscula, 1 número y 1 símbolo especial
        StringBuilder sb = new StringBuilder(10);
        sb.append(LETRAS_MAY.charAt(RANDOM.nextInt(LETRAS_MAY.length())));
        sb.append(LETRAS_MIN.charAt(RANDOM.nextInt(LETRAS_MIN.length())));
        sb.append(NUMEROS.charAt(RANDOM.nextInt(NUMEROS.length())));
        sb.append(ESPECIALES.charAt(RANDOM.nextInt(ESPECIALES.length())));

        String todos = LETRAS_MIN + LETRAS_MAY + NUMEROS + ESPECIALES;
        for (int i = 4; i < 10; i++) {
            sb.append(todos.charAt(RANDOM.nextInt(todos.length())));
        }

        // Barajar
        char[] array = sb.toString().toCharArray();
        for (int i = array.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
        return new String(array);
    }

    private String limpiarTexto(String texto) {
        if (texto == null) return "";
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD);
        return normalizado.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }
}

