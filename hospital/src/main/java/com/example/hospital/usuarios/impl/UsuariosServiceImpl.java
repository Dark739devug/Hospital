package com.example.hospital.usuarios.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.hospital.rol.Rol;
import com.example.hospital.rol.RolRepository;
import com.example.hospital.shared.exception.DuplicadoException;
import com.example.hospital.shared.exception.RecursoNoEncontradoException;
import com.example.hospital.shared.exception.ReglaNegocioException;
import com.example.hospital.usuarios.Usuarios;
import com.example.hospital.usuarios.UsuariosRepository;
import com.example.hospital.usuarios.UsuariosService;
import com.example.hospital.usuarios.dto.UsuariosDTO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Reglas de usuarios:
 * - `idRol` es obligatorio al crear (sin rol por defecto) y debe existir y estar activo (422).
 * - El nombre de usuario no se repite (409).
 * - Contraseña mínima de {@value #LARGO_MINIMO_CLAVE} caracteres (Bean Validation en el DTO: 400 con `detalles`).
 * - Un ADMIN no puede desactivarse a sí mismo ni quitarse el rol ADMIN, y nunca se puede desactivar ni degradar al último
 *   ADMIN activo (422).
 */
@Service
public class UsuariosServiceImpl implements UsuariosService {

    private static final Logger log = LoggerFactory.getLogger(UsuariosServiceImpl.class);

    public static final int LARGO_MINIMO_CLAVE = 8;
    public static final String ROL_ADMIN = "ADMIN";

    private final UsuariosRepository usuariosRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuariosServiceImpl(UsuariosRepository usuariosRepository, RolRepository rolRepository, PasswordEncoder passwordEncoder) {
        this.usuariosRepository = usuariosRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private UsuariosDTO mapToDTO(Usuarios u) {
        return UsuariosDTO.sinPassword(u.getIdUsuario(), u.getNombreUsuario(), u.getEstado(), u.getUltimoAcceso(), u.getIdRol());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuariosDTO> findAll() {
        return usuariosRepository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UsuariosDTO findById(Long id) {
        return mapToDTO(buscar(id));
    }

    @Override
    @Transactional
    public UsuariosDTO crear(UsuariosDTO dto) {
        if (dto == null || dto.nombreUsuario() == null || dto.nombreUsuario().isBlank()) {
            throw new ReglaNegocioException("El nombre de usuario es obligatorio");
        }
        if (dto.password() == null || dto.password().length() < LARGO_MINIMO_CLAVE) {
            throw new ReglaNegocioException("La contraseña debe tener al menos " + LARGO_MINIMO_CLAVE + " caracteres");
        }
        if (dto.idRol() == null) {
            throw new ReglaNegocioException("El rol es obligatorio");
        }
        String nombre = dto.nombreUsuario().trim();
        if (usuariosRepository.existsByNombreUsuarioIgnoreCase(nombre)) {
            throw new DuplicadoException("El nombre de usuario ya está en uso");
        }
        rolActivo(dto.idRol());

        Usuarios usuario = new Usuarios();
        usuario.setNombreUsuario(nombre);
        // Siempre se hashea la contraseña
        usuario.setContrasenaHash(passwordEncoder.encode(dto.password()));
        usuario.setEstado(dto.estado() != null ? dto.estado() : true);
        usuario.setIdRol(dto.idRol());
        Usuarios guardado = usuariosRepository.save(usuario);
        log.info("Usuario '{}' creado con el rol {} (id {})", guardado.getNombreUsuario(), guardado.getIdRol(), guardado.getIdUsuario());
        return mapToDTO(guardado);
    }

    @Override
    @Transactional
    public UsuariosDTO actualizar(UsuariosDTO dto, Long id) {
        if (dto == null || id == null) {
            throw new ReglaNegocioException("Datos para actualizar inválidos");
        }
        Usuarios existente = buscar(id);

        if (dto.nombreUsuario() != null && !dto.nombreUsuario().isBlank()) {
            String nombre = dto.nombreUsuario().trim();
            if (usuariosRepository.existsByNombreUsuarioIgnoreCaseAndIdUsuarioNot(nombre, id)) {
                throw new DuplicadoException("El nombre de usuario ya está en uso");
            }
            existente.setNombreUsuario(nombre);
        }
        if (dto.password() != null && !dto.password().isBlank()) {
            if (dto.password().length() < LARGO_MINIMO_CLAVE) {
                throw new ReglaNegocioException("La contraseña debe tener al menos " + LARGO_MINIMO_CLAVE + " caracteres");
            }
            existente.setContrasenaHash(passwordEncoder.encode(dto.password()));
        }
        if (dto.idRol() != null) {
            rolActivo(dto.idRol());
        }

        boolean seDesactiva = dto.estado() != null && !dto.estado() && !Boolean.FALSE.equals(existente.getEstado());
        boolean cambiaRol = dto.idRol() != null && !Objects.equals(dto.idRol(), existente.getIdRol());
        if (seDesactiva || cambiaRol) {
            protegerAdministradores(existente, seDesactiva, cambiaRol);
        }

        if (dto.estado() != null) {
            existente.setEstado(dto.estado());
        }
        if (dto.idRol() != null) {
            existente.setIdRol(dto.idRol());
        }
        log.info("Usuario {} actualizado (rol: {}, estado: {}, contraseña cambiada: {})", id, existente.getIdRol(), existente.getEstado(), dto.password() != null && !dto.password().isBlank());
        return mapToDTO(usuariosRepository.save(existente));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Usuarios usuario = buscar(id);
        if (!Boolean.FALSE.equals(usuario.getEstado())) {
            protegerAdministradores(usuario, true, false);
        }
        usuario.setEstado(false);
        usuariosRepository.save(usuario);
        log.info("Usuario {} desactivado", id);
    }

    /**
     * Protege a los administradores (422): nadie se desactiva ni se quita el rol ADMIN a sí mismo, y no se puede desactivar ni
     * degradar al último ADMIN activo.
     */
    private void protegerAdministradores(Usuarios objetivo, boolean seDesactiva, boolean cambiaRol) {
        Long idAdmin = rolRepository.findByNombre(ROL_ADMIN).map(Rol::getIdRol).orElse(null);
        if (idAdmin == null || !Objects.equals(objetivo.getIdRol(), idAdmin) || Boolean.FALSE.equals(objetivo.getEstado())) {
            return; // el usuario objetivo no es un ADMIN activo
        }
        if (Objects.equals(usuarioActual(), objetivo.getNombreUsuario())) {
            throw new ReglaNegocioException(seDesactiva
                    ? "Un administrador no puede desactivarse a sí mismo."
                    : "Un administrador no puede quitarse a sí mismo el rol ADMIN.");
        }
        if (usuariosRepository.countByIdRolAndEstadoTrue(idAdmin) <= 1) {
            throw new ReglaNegocioException("No se puede " + (seDesactiva ? "desactivar" : "degradar") + " al último administrador activo.");
        }
    }

    /** Nombre del usuario autenticado en esta petición (null si no hay contexto de seguridad). */
    private String usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    // Referencia del cuerpo: el rol debe existir y estar activo (422)
    private void rolActivo(Long idRol) {
        Rol rol = rolRepository.findById(idRol)
                .orElseThrow(() -> new ReglaNegocioException("El rol con ID " + idRol + " no existe"));
        if (Boolean.FALSE.equals(rol.getActivo())) {
            throw new ReglaNegocioException("El rol '" + rol.getNombre() + "' está inactivo");
        }
    }

    private Usuarios buscar(Long id) {
        return usuariosRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario con id: " + id));
    }
}
