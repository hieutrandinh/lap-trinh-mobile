FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S -G app app
COPY build/libs/backend.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=UTC", "-jar", "/app/app.jar"]
