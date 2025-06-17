# Build stage
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY --chown=gradle:gradle . /app
RUN gradle bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-jammy

# Pasta onde ficará o banco SQLite
WORKDIR /app

# Copia o jar gerado no stage anterior
COPY --from=build /app/build/libs/*.jar app.jar

# Cria pasta para persistência do SQLite
RUN mkdir -p /app/data

# Exponha a porta padrão do Spring Boot
EXPOSE 8080

# Comando para rodar a aplicação
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
