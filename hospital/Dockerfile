# Imagen del backend para Render (Web Service con Docker, Root Directory = hospital).

# ---------- Etapa 1: compilar el jar con Maven y JDK 21 ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Primero solo el pom: las dependencias quedan en cache mientras el pom no cambie.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Las pruebas se ejecutan en local (mvnw test) antes de subir los cambios.
RUN mvn -B -q -DskipTests package

# ---------- Etapa 2: ejecutar con JRE 21 ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Ajustes para el plan gratuito de Render (512 MB de RAM, 0.1 CPU):
# - MaxRAMPercentage=65: el heap usa como maximo ~330 MB y deja memoria para metaspace e hilos.
# - SerialGC y TieredStopAtLevel=1: menos memoria y arranque mas rapido con poca CPU.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=65 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"

# Render define PORT; application.properties lo lee con server.port=${PORT:8080}.
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
