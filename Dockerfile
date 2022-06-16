FROM defradigital/java:latest-jre

ARG BUILD_VERSION

USER root

RUN mkdir -p /usr/src/reach-upload-service
WORKDIR /usr/src/reach-upload-service

COPY ./target/reach-upload-service-${BUILD_VERSION}.jar /usr/src/reach-upload-service/reach-upload-service.jar
COPY ./target/agent/applicationinsights-agent.jar /usr/src/reach-upload-service/applicationinsights-agent.jar
COPY ./target/classes/applicationinsights.json /usr/src/reach-upload-service/applicationinsights.json

RUN chown jreuser /usr/src/reach-upload-service
USER jreuser

EXPOSE 8092

CMD java -javaagent:/usr/src/reach-upload-service/applicationinsights-agent.jar \
-Xmx${JAVA_MX:-512M} -Xms${JAVA_MS:-256M} -jar reach-upload-service.jar

