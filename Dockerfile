# ---------- BUILD STAGE ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY mvnw mvnw
COPY .mvn .mvn
COPY src src

RUN chmod +x mvnw
RUN ./mvnw -DskipTests package

# ---------- RUN STAGE ----------
FROM eclipse-temurin:21-jre AS run

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 7860

ENTRYPOINT ["java", "-jar", "app.jar"]

