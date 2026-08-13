FROM eclipse-temurin:21-jre-jammy
COPY target/hdkitservice-0.0.1.jar /app.jar
EXPOSE 3001
ENTRYPOINT ["java", "-jar", "/app.jar"]
