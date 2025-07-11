FROM eclipse-temurin:17-jdk-alpine

COPY ./build/libs/*SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=default"]