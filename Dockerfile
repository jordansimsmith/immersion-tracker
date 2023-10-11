FROM amazoncorretto:17-alpine-jdk AS build

WORKDIR /app

COPY gradle gradle
COPY build.gradle settings.gradle gradlew ./

RUN ./gradlew dependencies --refresh-dependencies

COPY src src
COPY build/generated-src build/generated-src

RUN ./gradlew build -x test -x generateJooq

FROM amazoncorretto:17-alpine AS run

WORKDIR /app

COPY --from=build /app/build/libs/immersion.tracker-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]