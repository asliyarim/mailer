# --- build stage ---
# Yerelde JDK kurmak zorunda kalmamak icin derleme burada yapilir; makinede
# JAVA_HOME ayarli olmasa da "docker compose build" calisir.
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -q -DskipTests package && \
    cp target/*.jar app.jar

# --- run stage ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/app.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
