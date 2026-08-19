# Build stage
FROM docker.io/library/ibm-semeru-runtimes:open-25-jdk AS builder
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
FROM docker.io/library/ibm-semeru-runtimes:open-25-jre AS runner
WORKDIR /deployments

COPY --from=builder --chown=185:root /build/build/libs/*-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
