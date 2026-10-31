# Compilación
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Ejecución
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 app && mkdir -p /data/fotos && chown app /data/fotos
USER app
COPY --from=build /app/target/mimascota-backend-*.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
