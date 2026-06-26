FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY . .
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/study-backend-*.jar app.jar
RUN mkdir -p /app/data/files /app/data/temp
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
