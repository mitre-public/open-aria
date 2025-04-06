FROM gradle:8.12-jdk17 AS build

WORKDIR /open-aria
COPY ../ .

RUN --mount=type=cache,target=/home/gradle/.gradle/caches \
    gradle --no-daemon --quiet :open-aria-deploy:shadowJar \
    && mv open-aria-deploy/build/libs/open-aria-uber.jar ./OpenARIA.jar

FROM eclipse-temurin:17-jre-ubi9-minimal AS deploy

WORKDIR open-aria
COPY --from=build /open-aria/OpenARIA.jar .
ENTRYPOINT ["java", "-jar", "/open-aria/OpenARIA.jar"]
