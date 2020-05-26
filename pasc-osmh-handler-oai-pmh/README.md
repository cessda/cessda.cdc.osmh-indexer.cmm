[![Build Status](https://jenkins.cessda.eu/buildStatus/icon?job=cessda.cdc.osmh-repository-handler.oai-pmh%2Fmaster)](https://jenkins.cessda.eu/job/cessda.cdc.osmh-repository-handler.oai-pmh/job/master/)
[![Bugs](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-osmh-handler-oai-pmh&metric=bugs)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-osmh-handler-oai-pmh)
[![Code Smells](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-osmh-handler-oai-pmh&metric=code_smells)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-osmh-handler-oai-pmh)
[![Coverage](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-osmh-handler-oai-pmh&metric=coverage)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-osmh-handler-oai-pmh)
[![Duplicated Lines (%)](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-osmh-handler-oai-pmh&metric=duplicated_lines_density)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-osmh-handler-oai-pmh)
[![Lines of Code](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-osmh-handler-oai-pmh&metric=ncloc)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-osmh-handler-oai-pmh)
[![Maintainability Rating](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-osmh-handler-oai-pmh&metric=sqale_rating)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-osmh-handler-oai-pmh)
[![Quality Gate Status](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-osmh-handler-oai-pmh&metric=alert_status)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-osmh-handler-oai-pmh)
[![Reliability Rating](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-osmh-handler-oai-pmh&metric=reliability_rating)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-osmh-handler-oai-pmh)
[![Security Rating](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-osmh-handler-oai-pmh&metric=security_rating)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-osmh-handler-oai-pmh)
[![Technical Debt](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-osmh-handler-oai-pmh&metric=sqale_index)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-osmh-handler-oai-pmh)
[![Vulnerabilities](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-osmh-handler-oai-pmh&metric=vulnerabilities)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-osmh-handler-oai-pmh)


# OSMH Handler OAI-PMH

CDC OSMH repository handler for harvesting OAI-PMH metadata format

## Quality - Software Maturity Level

The overall Software Maturity Level for this product and the individual scores for each attribute can be found in the  [SML](SML.md) file.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing
purposes. See deployment for notes on how to deploy the project on a live system.

### Test it
Runs all the Junit Test


    mvn clean test

### Sonar it

Static code quality with verification with SonarQube

    mvn sonar:sonar \
      -Dsonar.host.url=http://localhost:9000 \

### Build it
Prepares a clean jar package

    mvn clean package

### Run it
Runs the built jar

    java -Xms2G -Xmx4G -jar target/pasc-osmh-handler-oai-pmh*.jar

### Run it one-liner

    mvn clean package && java -Xms2G -Xmx4G -jar target/pasc-osmh-handler-oai-pmh*.jar


### Run it - with profile
The profile will apply the specified environment property for a given profile

    java -jar -Dspring.profiles.active=dev target/pasc-osmh-handler-oai-pmh*.jar
    java -jar -Dspring.profiles.active=uat target/pasc-osmh-handler-oai-pmh*.jar
    java -jar -Dspring.profiles.active=prod target/pasc-osmh-handler-oai-pmh*.jar

Note if no profile flag is set the default profile will be used. Which is non.

### Run it development
    mvn spring-boot:run
    mvn spring-boot:run -Drun.profiles=dev
    mvn spring-boot:run -Drun.jvmArguments="-Dspring.profiles.active=dev"

### Run it local docker as in CI

Log into GCR to be able to download the default java image (note the personal user token after `-p` flag)

    docker login -u _json_key -p "$(cat dev_files/Technical-Infrastructure-f626925a1edb.json)" https://eu.gcr.io

Then test you can manually download the image

    docker pull eu.gcr.io/cessda-development/cessda-java:latest

If successful then build image with docker file in the root `dir`

    docker build .

Run the docker image

    docker run -p 9091:9091 <image_id>  

# Debug/Test it with Swagger UI API documentation
   - [Localhost](http://localhost:9091/swagger-ui.html#/) http://localhost:<port>/<context-base>/swagger-ui.html#/

### Prerequisites
The following is expected to be install before building running.  To install see your preferred package manager.
On mac this can be done with `brew`
  - Java JDK 11
  - Maven

`brew tap caskroom/versions`

`brew cask install java11`

`brew cask info java`  // To verify which version it will install.

`brew install maven`

`mvn -version` // To verify which version it will install.


## Running the tests

### How to run the automated tests and sonar report in CI

`mvn clean install sonar:sonar -Dsonar.host.url=http://localhost:9000`


# Further detailed notes

### Break down into end to end tests

TODO: Explain what these tests test and why

```
Give an example
```

### And coding style tests

TODO: Explain what these tests test and why

```
Give an example
```

## Deployment

### At startup
Configuration is loaded and overwritten in this order
* application-[dev,local,prod].yml
* application.yml
* CLI parameters e.g. `--logging.level.=DEBUG` sets logging level for all classes

### At Runtime
If the app is registered at a [spring boot admin server](https://github.com/codecentric/spring-boot-admin)
all environment properties can be changed at runtime.

**CHANGES MADE AT RUNTIME WILL BE**
* **EFFECTIVE AFTER A CONTEXT RELOAD**
* **LOST AFTER AN APPLICATION RESTART UNLESS PERSISTED IN APPLICATION.yml**


## Built With

* [Maven](https://maven.apache.org/) - Dependency Management

## Contributing

Please read [Contributing to CESSDA Open Source Software](https://bitbucket.org/cessda/cessda.guidelines.public/src/master/CONTRIBUTING.md)
for information on contribution to CESSDA software.

## Versioning

## Authors

* **Moses Mansaray <moses AT doraventures DOT com>** - *Version 2.0.0 and 2.1.0 releases*

You can find the list of all contributors [here](CONTRIBUTORS.md)

## License

This project is licensed under the Apache 2 License - see the [LICENSE](LICENSE.txt) file for details

## Acknowledgments
N/A


## Edge Case and Assumptions:

* If publicationYear cannot be parsed to an Int we default to epoch year 1970
* Note the extra "/" workaround in the application.yml repository configuration for repositories that separate records with a different metadata prefix per language accessed with the same basic url. This url in a way act as a key, so the extra "/" distinguishes the two for the specific metadataPrefix to be retrieved. There must be a better way to handle this edge case. This workaround affects this project and the pasc-oci as well.
