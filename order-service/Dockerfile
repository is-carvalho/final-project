FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY order-service/pom.xml order-service/pom.xml

RUN chmod +x mvnw

RUN ./mvnw -f order-service/pom.xml dependency:go-offline -B

COPY order-service/src order-service/src

RUN ./mvnw -f order-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/order-service/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]