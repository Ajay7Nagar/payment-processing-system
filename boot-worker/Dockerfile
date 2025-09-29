FROM eclipse-temurin:17-jre

ENV JAVA_OPTS="-Xms256m -Xmx512m"
WORKDIR /app

COPY build/libs/boot-worker-*.jar app.jar

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]


