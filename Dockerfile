#
# Copyright © 2017-2020 CESSDA ERIC (support@cessda.eu)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# OpenJDK Base Container
FROM openjdk:11-jre

# Container Information
LABEL maintainer='CESSDA-ERIC "support@cessda.eu"'

# Create Volume tmp and add JAR artifacts
VOLUME /tmp
ADD ./target/pasc-oci*.jar pasc-oci.jar

# Entrypoint - Start Admin
ENTRYPOINT exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar -Dspring.profiles.active=gcp /pasc-oci.jar
