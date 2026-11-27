FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY backend/ .
RUN chmod +x mvnw && sed -i 's/\r$//' mvnw && ./mvnw -B -DskipTests package
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
