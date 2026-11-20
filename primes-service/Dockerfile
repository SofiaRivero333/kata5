FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /project
COPY pom.xml .
COPY primes-service/pom.xml primes-service/pom.xml
COPY primes-client/pom.xml primes-client/pom.xml
COPY primes-service/src primes-service/src
RUN mvn -B -pl primes-service -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /project/primes-service/target/primes-service-1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]