FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src src

# Use maven from image instead of wrapper
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre AS run
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 7860

ENTRYPOINT ["java", "-jar", "app.jar"]
