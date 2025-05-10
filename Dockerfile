FROM gradle:8.13-jdk17 AS build
WORKDIR /app

COPY build.gradle settings.gradle /app/
RUN gradle dependencies --no-daemon

COPY . /app
RUN gradle build --no-daemon -x test

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]