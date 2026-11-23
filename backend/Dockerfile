# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 先复制 pom 拉取依赖，利用镜像层缓存，改代码时无需重新下载
COPY pom.xml .
RUN mvn -q -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -B -DskipTests package

# ---------- 运行阶段 ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# 建表脚本按 application.yml 的 file:./db/01-schema.sql 读取
COPY db ./db
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
