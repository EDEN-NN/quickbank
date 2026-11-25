FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --home-dir /app --shell /usr/sbin/nologin quickbank
WORKDIR /app
COPY --from=build /src/quickbank-bootstrap/target/quickbank-bootstrap.jar /app/quickbank.jar
USER quickbank
EXPOSE 8088
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/quickbank.jar"]
