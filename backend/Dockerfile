FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

# Debian/Ubuntu-based runtime (not Alpine): ONNX Runtime, used for the in-process
# embedding model, ships glibc native libraries that do not load on Alpine's musl.
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# curl: used by the docker-compose healthcheck. --create-home: native libs/caches need a writable home.
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system pharma && useradd --system --gid pharma --create-home pharma
COPY --from=build /app/target/pharma-rag-backend-1.0.0.jar app.jar
RUN chown pharma:pharma app.jar
USER pharma
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
