FROM openjdk:17
COPY target/bestwork-dev.jar /app.jar
CMD ["java", "-jar", "/app.jar"]
