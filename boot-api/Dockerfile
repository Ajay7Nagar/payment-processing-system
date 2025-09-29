FROM eclipse-temurin:17-jre

ENV JAVA_OPTS="-Xms256m -Xmx512m"
WORKDIR /app

COPY build/libs/boot-api-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]


