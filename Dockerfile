# Arch Hub — imagem da plataforma da linha Arch.
#   docker build -t arch-hub .
#   docker run -p 8080:8080 -v arch-hub-data:/app/data arch-hub
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
COPY brand ./brand
RUN mvn -q -B -DskipTests package && cp target/arch-hub-*.jar /arch-hub.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S archhub && adduser -S archhub -G archhub && mkdir -p /app/data && chown archhub:archhub /app/data
COPY --from=build /arch-hub.jar /app/arch-hub.jar
USER archhub
# data/systems.json + data/architecture.json (persistência do catálogo e dos scores)
VOLUME /app/data
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s \
  CMD wget -qO- http://localhost:8080/actuator/health >/dev/null || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/arch-hub.jar"]
