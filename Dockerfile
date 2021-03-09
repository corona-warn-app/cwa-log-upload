FROM maven:3-openjdk-11 as build

COPY . .
RUN mvn clean install -Dhttps.proxyHost=sia-lb.telekom.de -Dhttps.proxyPort=8080


FROM gcr.io/distroless/java-debian10:11 as run

COPY --from=build ./target/*.jar /app.jar
#COPY scripts/Dpkg.java Dpkg.java
#RUN ["java", "Dpkg.java"]
USER 65534:65534
CMD ["app.jar"]
