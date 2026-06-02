FROM openjdk:17-jdk-slim

WORKDIR /app

# 安装 curl 用于 healthcheck
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

ARG JAR_FILE=target/platform-api-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
