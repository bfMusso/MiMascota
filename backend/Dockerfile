# Compilación
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Ejecución
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --uid 1001 --no-create-home app && mkdir -p /data/fotos && chown app /data/fotos
USER app
COPY --from=build /app/target/mimascota-backend-*.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
