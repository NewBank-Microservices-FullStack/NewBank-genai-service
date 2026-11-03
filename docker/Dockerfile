FROM eclipse-temurin:17-jre AS run
WORKDIR /app
COPY target/newbank-genai-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
