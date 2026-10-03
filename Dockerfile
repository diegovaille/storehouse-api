# Build stage
FROM gradle:8.8-jdk21 AS build
WORKDIR /app
COPY --chown=gradle:gradle . /app
RUN sh ./gradlew bootJar --no-daemon --max-workers=1 -Dorg.gradle.jvmargs=-Xmx768m -Pkotlin.compiler.execution.strategy=in-process

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
