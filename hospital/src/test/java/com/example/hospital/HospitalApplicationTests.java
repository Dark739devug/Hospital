package com.example.hospital;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Prueba de integración: levanta el contexto completo y necesita PostgreSQL.
 * Solo corre si está definida la variable de entorno DB_PASSWORD; sin base de datos
 * se omite, para que `mvnw test` (pruebas unitarias con mocks) pase en cualquier equipo.
 * JWT_SECRET y ADMIN_PASSWORD no tienen valor por defecto: aquí se fijan valores solo de prueba.
 */
@SpringBootTest(properties = {
		"app.jwt.secret=clave-solo-para-la-prueba-de-integracion-0123456789",
		"app.admin.password=ClavePrueba123*"
})
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class HospitalApplicationTests {

	@Test
	void contextLoads() {
	}

}
