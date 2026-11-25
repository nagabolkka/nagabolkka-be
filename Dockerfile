# 실행만 하는 이미지 (jar 는 GitHub Actions 에서 빌드해서 서버로 보냄)
# 서버(RAM 1GB)에서 Gradle 빌드를 하면 다른 컨테이너가 스왑으로 밀려나서 빌드를 서버 밖으로 옮김
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=Asia/Seoul

# 레포 루트의 app.jar (로컬에서 이미지를 만들 땐 ./gradlew bootJar → cp build/libs/*.jar app.jar)
COPY app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]