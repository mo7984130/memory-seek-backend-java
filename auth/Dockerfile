FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

EXPOSE 9000

COPY target/*.jar app.jar
# 优化 JVM 启动速度
ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=9000"]