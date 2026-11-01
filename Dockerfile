# syntax=docker/dockerfile:1

# ---- Stage 1: build -------------------------------------------------------------------------------
# The JDK and Maven live only here: none of it reaches the image that runs in production.
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace

# Dependencies first, the code later: as long as pom.xml does not change, this layer comes from the cache.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Tests run in CI against real containers; here we only package what CI already tested.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests

# Split the fat jar into layers, from the least to the most often changed.
RUN java -Djarmode=tools -jar target/digital-banking-application-*.jar extract --layers --launcher \
        --destination target/extracted

# ---- Stage 2: run ---------------------------------------------------------------------------------
FROM eclipse-temurin:25-jre-alpine

# A dedicated user: whoever breaks into the application is not root in the container.
RUN addgroup -S bank && adduser -S bank -G bank
WORKDIR /app

# One COPY per layer: a code change rebuilds only the last, thinnest one.
COPY --from=build /workspace/target/extracted/dependencies/ ./
COPY --from=build /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/target/extracted/application/ ./

USER bank
EXPOSE 8080

# The heap follows the container's memory limit, not the memory of the whole machine.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

# "Running" is not "ready": Docker asks the application itself. Spring needs a while to start, hence start-period.
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

# exec: java becomes PID 1 and gets SIGTERM directly, so Spring shuts down gracefully on "docker stop".
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
