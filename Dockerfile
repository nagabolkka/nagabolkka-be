# ---------- 1단계: 빌드 (JDK로 jar 만들기) ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 서버 메모리(1GB)가 작아서 Gradle이 쓰는 메모리를 제한
ENV GRADLE_OPTS="-Xmx512m -Dorg.gradle.daemon=false"

# 의존성 파일 먼저 복사 → 코드만 바뀌면 의존성 다운로드를 캐시로 건너뜀
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null 2>&1 || true

COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# ---------- 2단계: 실행 (JRE만 담아서 가볍게) ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=Asia/Seoul

COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
