FROM eclipse-temurin:21-jdk

WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

RUN chmod +x ./gradlew && ./gradlew --no-daemon bootJar

EXPOSE 8080

CMD ["java", "-jar", "build/libs/coupon-publish-0.0.1-SNAPSHOT.jar"]
