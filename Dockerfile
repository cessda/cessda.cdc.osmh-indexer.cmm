# OpenJDK Base Container
FROM eu.gcr.io/cessda-development/cessda-java:latest

# Container Information
MAINTAINER CESSDA-ERIC "support@cessda.eu"

# Create Volume tmp and add JAR artifacts
VOLUME /tmp
ADD ./target/pasc-oci*.jar pasc-oci.jar

# Java options
ENV JAVA_OPTS ""

# Entrypoint - Start Admin
<<<<<<< HEAD
ENTRYPOINT exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar -Dspring.profiles.active=live /pasc-oci.jar
=======
ENTRYPOINT exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar -Dspring.profiles.active=staging /pasc-oci.jar
>>>>>>> staging
