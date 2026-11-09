package com.example.hospital.auth;

import com.example.hospital.auth.dto.LoginRequest;
import com.example.hospital.auth.dto.LoginResponse;
import com.example.hospital.auth.dto.RegisterRequest;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    /** Crea el usuario y devuelve su id. */
    Long register(RegisterRequest request);

    /** Permite al usuario autenticado cambiar su propia contraseña. */
    void cambiarPassword(String username, String passwordActual, String nuevaPassword);
}
