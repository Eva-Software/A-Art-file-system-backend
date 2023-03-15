FROM gradle:7-jdk11 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle installDist --no-daemon --info

FROM openjdk:11
ENV HOST=0.0.0.0
ENV PORT=8080
EXPOSE 8080:8080
RUN mkdir /app
COPY --from=build /home/gradle/src/build/install/aart-simple-backend/ /app/
WORKDIR /app
ENTRYPOINT ["/app/bin/aart-simple-backend"]