FROM gradle:5.6.3-jdk8 as packager
MAINTAINER Alex Simons "alexsimons9999@gmail.com"
USER root
WORKDIR /app

RUN mkdir -p /opt/gradle/.gradle
ENV GRADLE_USER_HOME=/opt/gradle/.gradle

#Cache gradle dependencies, will only run if any of these files changes
ADD build.gradle ./
ADD gradle.properties ./
ADD settings.gradle ./

#Add rest of source code :)
ADD . .

RUN gradle clean shadowJar

FROM java:8u111-alpine

WORKDIR /app

COPY --from=packager /app/build/libs /app

ENTRYPOINT ["java", "-jar", "SOGoS-API-1.0.0-BETA-fat.jar"]





