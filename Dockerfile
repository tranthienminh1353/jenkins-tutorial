FROM maven:3.8.3-openjdk-17

WORKDIR /jenkins-tutorial
RUN mkdir /bestwork
COPY pom.xml /bestwork
COPY src /bestwork/src
WORKDIR /bestwork

RUN mvn clean package -DskipTests

EXPOSE 8080

CMD ["java", "-jar", "target/bestwork-dev.jar"]
