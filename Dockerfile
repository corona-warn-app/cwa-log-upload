FROM maven:3-openjdk-17 as build

ARG MAVEN_PASSWORD
ARG MAVEN_USERNAME

COPY . .
RUN mvn clean install \
    --settings ./settings.xml \
    --define app.packages.username=${MAVEN_USERNAME} \
    --define app.packages.password=${MAVEN_PASSWORD}


FROM gcr.io/distroless/java17-debian11:latest as run

COPY --from=build ./target/*.jar /app.jar
USER 65534:65534
CMD ["app.jar"]
