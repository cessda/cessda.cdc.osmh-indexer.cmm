[![Build Status](https://jenkins.cessda.eu/buildStatus/icon?job=cessda.cdc.osmh-indexer.cmm%2Fmaster)](https://jenkins.cessda.eu/job/cessda.cdc.osmh-indexer.cmm/job/master/)
[![Bugs](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-oci&metric=bugs)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-oci)
[![Code Smells](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-oci&metric=code_smells)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-oci)
[![Coverage](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-oci&metric=coverage)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-oci)
[![Duplicated Lines (%)](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-oci&metric=duplicated_lines_density)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-oci)
[![Lines of Code](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-oci&metric=ncloc)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-oci)
[![Maintainability Rating](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-oci&metric=sqale_rating)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-oci)
[![Quality Gate Status](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-oci&metric=alert_status)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-oci)
[![Reliability Rating](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-oci&metric=reliability_rating)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-oci)
[![Security Rating](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-oci&metric=security_rating)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-oci)
[![Technical Debt](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-oci&metric=sqale_index)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-oci)
[![Vulnerabilities](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-oci&metric=vulnerabilities)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-oci)

# OSMH Consumer Indexer (PaSC-OCI)

CESSDA CDC Consumer Indexer (an OSMH Consumer) for Metadata harvesting and ingestion into Elasticsearch.
See the [OSMH System Architecture Document](https://docs.google.com/document/d/1RrXjpbyUGdd5FKSjrnQmRdbzaCQzE2W-92lYKs1KeCA/edit) for more information about The Open Source Metadata Harvester (OSMH).

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing
purposes. See deployment for notes on how to deploy the project on a live system.

### Test it

    mvn clean test

### Sonar it

Static code quality with verification with SonarQube

    mvn sonar:sonar \
      -Dsonar.host.url=http://localhost:9000 \

### Build it

    mvn clean package

### Run it

    java -Xms2G -Xmx4G -jar target/pasc-oci*.jar

### Run it - with profile

    java -jar -Dspring.profiles.active=dev target/pasc-oci*.jar
    java -jar -Dspring.profiles.active=uat target/pasc-oci*.jar
    java -jar -Dspring.profiles.active=prod target/pasc-oci*.jar

Note if no profile flag is set the default profile will be used. Which is non.

### Prerequisites

The following is expected to be install before building running.  To install see your preferred package manager like.
On mac this can be done with `brew`

* Java JDK 11
* Maven

`brew tap caskroom/versions`

`brew cask install java11`

`brew cask info java`  // To verify which version it will install.

`brew install maven`

`mvn -version` // To verify which version it will install.

## Running the tests

### How to run the automated tests and sonar report in CI

`mvn clean install sonar:sonar -Dsonar.host.url=http://localhost:9000`

## Further detailed notes

### Break down into end to end tests

* Makes use of TDD
* For integrations test, loads up an embedded Elasticsearch server with tests against it

## Deployment

### At startup

Configuration is loaded and overwritten in this order

* application-[dev,local,prod].yml
* application.yml
* CLI parameters e.g. `--logging.level.=DEBUG` sets logging level for all classes

Note that usernames

*${SECURITY_USER_NAME}*

*${SPRING_BOOT_ADMIN_USERNAME}*

and passwords

*${SECURITY_USER_PASSWORD}*

*${SPRING_BOOT_ADMIN_PASSWORD}*

are defined externally, and consumed by *application.yml* at runtime.

### At Runtime

If the app is registered at a [spring boot admin server](https://github.com/codecentric/spring-boot-admin)
all environment properties can be changed at runtime.

**Changes made at runtime will be effective after a context reload but are lost
after an application restart unless persisted in** *application.yml*


## Timers Properties

Harvesting Schedule timers:

```yaml
osmhConsumer:
 delay:
    # Auto Starts after delay of 60 seconds at startup
    initial: '60000'
```

 The timer schedule for GCP use is defined in [CDC deployment repository's template-deployment.yaml](https://bitbucket.org/cessda/cessda.cdc.deploy/src/master/osmh-indexer/infrastructure/k8s/template-deployment.yaml), but if you are deploying the software elsewhere, then the timer settings in [application.yaml](/src/main/resources/application.yaml) are relevant. The profiles are defined in [application.yaml](/src/main/resources/application.yaml) and selected in [Dockerfile](Dockerfile/).

Take care with the daily/Sunday timer settings, otherwise all running instances may attempt to reharvest the same endpoints at the same time.

## Built With

* [Maven](https://maven.apache.org/) - Dependency Management

## Contributing

Please read [Contributing to CESSDA Open Source Software](https://bitbucket.org/cessda/cessda.guidelines.public/src/master/CONTRIBUTING.md)
for information on contribution to CESSDA software.


## Versioning

## Authors

* **Moses Mansaray <moses AT doravenetures DOT com>** - *Initial work, first version release*

You can find the list of all contributors [here](CONTRIBUTORS.md)

## License

This project is licensed under the Apache 2 License - see the [LICENSE](LICENSE) file for details

## Acknowledgments

## Edge Case and Assumptions

* Note the extra "/" workaround in the [application.yml](src/main/resources/application.yml) repository configuration for repositories that separate records with a different metadata prefix per language accessed with the same basic url.  This url in a way act as a key, so the extra "/" distinguishes the two for the specific metadataPrefix to be retrieved.  There must be a better way to handle this edge case.  This workaround affects this project and the pasc-osmh-handler-oai-pmh as well.
