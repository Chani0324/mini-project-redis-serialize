# Dockerfile (멀티스테이지 빌드, Gradle + Java 21 + protoc 설치)

# 1) 빌드 스테이지: Gradle로 앱을 빌드 (Java 21 기반 Debian)
FROM gradle:8-jdk21 AS build
WORKDIR /app

# 1-1) protoc(Protocol Buffers 컴파일러) 설치
USER root
RUN apt-get update && \
    apt-get install -y --no-install-recommends protobuf-compiler && \
    rm -rf /var/lib/apt/lists/*

# 1-2) Gradle 캐시 최적화: build.gradle, settings.gradle, gradlew, gradle/만 먼저 복사
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# 1-3) 의존성 다운로드(캐시 레이어 생성)
RUN ./gradlew clean --no-daemon

# 1-4) 소스 전체 복사 및 JAR 생성
COPY src ./src
RUN ./gradlew bootJar --no-daemon

# 2) 런타임 스테이지: 경량화된 OpenJDK 21-Slim 이미지
FROM openjdk:21-jdk-slim
WORKDIR /app

# 2-1) 빌드 스테이지에서 생성된 JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 2-2) 컨테이너 시작 시 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
