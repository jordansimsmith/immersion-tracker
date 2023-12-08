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

RUN apk update && apk add fontconfig && apk add ttf-dejavu && apk add curl

RUN curl -L -O https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar

COPY --from=build /app/build/libs/immersion.tracker-0.0.1-SNAPSHOT.jar app.jar

ENV JAVA_TOOL_OPTIONS="-javaagent:/app/opentelemetry-javaagent.jar"
ENV OTEL_SERVICE_NAME="immersion-tracker-server"
ENV OTEL_EXPORTER_OTLP_ENDPOINT=

ENTRYPOINT ["java", "-jar", "/app/app.jar"]