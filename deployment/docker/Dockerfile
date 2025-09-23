FROM openjdk:17-jdk-slim

WORKDIR /app

# Копируем только build файлы
COPY server/build/libs/server-all.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]