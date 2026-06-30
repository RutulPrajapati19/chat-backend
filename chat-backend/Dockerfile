FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY chat-backend/pom.xml .
RUN mvn dependency:go-offline -B
COPY chat-backend/src ./src
COPY chat-backend/src/main/resources/application.properties ./src/main/resources/application.properties
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENV JWT_SECRET=chatAppSuperSecretKeyForJWTTokenGenerationThatIsAtLeast256BitsLongEnough2024
ENV PORT=10000
ENTRYPOINT ["java", "-jar", "app.jar"]