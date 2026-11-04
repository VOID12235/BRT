# syntax=docker/dockerfile:1

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# 使用阿里云 Maven 镜像加速依赖下载（国内网络必需）
RUN mkdir -p /root/.m2 && printf '<settings><mirrors><mirror><id>aliyun</id><mirrorOf>central</mirrorOf><url>https://maven.aliyun.com/repository/public</url></mirror></mirrors></settings>' > /root/.m2/settings.xml

# 先复制依赖描述，利用 Docker 层缓存
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# ---------- 运行阶段 ----------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r btr && useradd -r -g btr btr \
    && mkdir -p /app/uploads && chown -R btr:btr /app

COPY --from=build /build/target/btr-platform.jar /app/app.jar

ENV TZ=Asia/Shanghai \
    SPRING_PROFILES_ACTIVE=prod \
    UPLOAD_DIR=/app/uploads

USER btr
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
