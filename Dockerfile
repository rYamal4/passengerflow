FROM maven:3.9.11-eclipse-temurin-17-noble AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17.0.16_8-jre-noble
WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring

COPY --from=build /app/target/passengerflow-0.0.1-SNAPSHOT.jar app.jar

RUN chown spring:spring app.jar
USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]