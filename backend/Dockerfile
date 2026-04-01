FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S pharma && adduser -S pharma -G pharma
COPY --from=build /app/target/pharma-rag-backend-1.0.0.jar app.jar
RUN chown pharma:pharma app.jar
USER pharma
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
