FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Step 1: Sirf pom.xml copy karo aur dependencies download karo
COPY pom.xml .
RUN mvn dependency:resolve dependency:resolve-plugins -B

# Step 2: Source code copy karo aur build karo
COPY src src
RUN mvn clean package -DskipTests -B -o

# ---------- RUN STAGE ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 7860
ENTRYPOINT ["java", "-jar", "app.jar"]
