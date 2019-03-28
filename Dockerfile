# OpenJDK Base Container
FROM openjdk:8-jre

# Container Information
LABEL maintainer='CESSDA-ERIC "support@cessda.eu"'

# Create Volume tmp and add JAR artifacts
VOLUME /tmp
ADD ./target/pasc-oci*.jar pasc-oci.jar

# Java options
ENV JAVA_OPTS "-Xms2G -Xmx4G"

# Entrypoint - Start Admin
ENTRYPOINT exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar -Dspring.profiles.active=dev /pasc-oci.jar
