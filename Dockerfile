# ===== build =====
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# ===== run =====
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*-SNAPSHOT.jar app.jar
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseZGC"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
