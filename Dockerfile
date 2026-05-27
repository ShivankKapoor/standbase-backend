# Build stage
FROM registry.access.redhat.com/ubi9/openjdk-25 AS builder
USER root
WORKDIR /build

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# Run stage
FROM registry.access.redhat.com/ubi9/openjdk-25-runtime AS runner
WORKDIR /deployments

COPY --from=builder --chown=185:root /build/build/libs/*-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
