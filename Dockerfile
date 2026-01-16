# OpenJDK 21 버전의 이미지를 가져와 JVM 환경을 구축한다.
FROM eclipse-temurin:21-jre-alpine

# 작업 디렉토리를 설정
WORKDIR /app

# 빌드된 jar 파일을 복사
COPY build/libs/*.jar app.jar

# app.jar를 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
