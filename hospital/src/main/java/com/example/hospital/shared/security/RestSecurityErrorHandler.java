package com.example.hospital.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class RestSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(RestSecurityErrorHandler.class);

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
        log.debug("401 sin autenticación válida en {} {}", req.getMethod(), req.getRequestURI());
        escribir(req, res, 401, "Unauthorized", "Token ausente, inválido o expirado");
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) throws IOException {
        log.warn("403 acceso denegado a {} {} para el usuario {}", req.getMethod(), req.getRequestURI(),
                req.getUserPrincipal() != null ? req.getUserPrincipal().getName() : "anónimo");
        escribir(req, res, 403, "Forbidden", "No tiene permisos para realizar esta acción");
    }

    private void escribir(HttpServletRequest req, HttpServletResponse res, int status, String error, String mensaje)
            throws IOException {
        String path = req.getRequestURI().replace("\"", "");
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write(String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                LocalDateTime.now(), status, error, mensaje, path));
    }
}
