FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

COPY backend/settings.xml /root/.m2/settings.xml
COPY backend/pom.xml /app/backend/pom.xml
RUN mvn -f /app/backend/pom.xml -B -q -DskipTests dependency:go-offline

COPY backend/src /app/backend/src
COPY frontend/templates /app/backend/src/main/resources/templates
RUN mvn -f /app/backend/pom.xml -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/backend/target/backend-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8000
ENTRYPOINT ["java","-jar","/app/app.jar"]
