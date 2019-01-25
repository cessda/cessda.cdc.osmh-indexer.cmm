# OpenJDK Base Container
FROM eu.gcr.io/cessda-development/cessda-java:latest

# Container Information
MAINTAINER CESSDA-ERIC "support@cessda.eu"

# Create Volume tmp and add JAR artifacts
VOLUME /tmp
ADD ./target/pasc-osmh-handler-oai-pmh*.jar pasc-osmh-handler-oai-pmh.jar

# Java options
ENV JAVA_OPTS "-Xms2G -Xmx4G"

# Entrypoint - Start Admin
ENTRYPOINT exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar -Dspring.profiles.active=dev /pasc-osmh-handler-oai-pmh.jar
