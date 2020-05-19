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

CESSDA CDC Consumer Indexer (an OSMH Consumer) for Metadata harvesting and ingestion into Elasticsearch. See the [OSMH System Architecture Document](https://docs.google.com/document/d/1RrXjpbyUGdd5FKSjrnQmRdbzaCQzE2W-92lYKs1KeCA/edit) for more information about The Open Source Metadata Harvester (OSMH).

## Quality - Software Maturity Level

The overall Software Maturity Level for this product, and the individual scores for each attribute can be found in the [SML](SML.md) file.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

The following tools are expected to be installed before compiling

* Java JDK 11
* Maven

### Test it

    mvn clean test

### Sonar it

Static code quality with verification with SonarQube

    mvn sonar:sonar -Dsonar.host.url=http://localhost:9000

### Build it

    mvn clean package

### Run it

    mvn spring-boot:run

### Run it - with profile

    java -jar -Dspring.profiles.active=dev target/pasc-oci*.jar
    java -jar -Dspring.profiles.active=uat target/pasc-oci*.jar
    java -jar -Dspring.profiles.active=prod target/pasc-oci*.jar

Note if no profile flag is set the default profile will be used. This is configured to use a local Elasticsearch instance, as well as local OAI-PMH and NESSTAR repository handlers.

## Notes

* Makes use of TDD
* For integrations test, loads up an embedded Elasticsearch server with tests against it

## Deployment

### At startup

Configuration is loaded and overwritten in this order

* Environment Variables e.g. `SECURITY_USER_NAME`
    * Spring can use weak binding to convert environment variables into Java properties
    * e.g. `SPRING_BOOT_ADMIN_USERNAME` converts to `spring.boot.admin.username`
* application-[dev,local,prod].yml
  * dev, local and prod refer to Spring profiles, specified by the command line `--spring.profiles.active` or the environment variable `SPRING_PROFILES_ACTIVE`
  * See <https://docs.spring.io/spring-boot/docs/1.5.x/reference/html/boot-features-profiles.html> for more details
* application.yml
* CLI parameters e.g. `--logging.level.=DEBUG` sets logging level for all classes

Note that usernames (`${SECURITY_USER_NAME}` and `${SPRING_BOOT_ADMIN_USERNAME}`) and passwords (`${SECURITY_USER_PASSWORD}` and `${SPRING_BOOT_ADMIN_PASSWORD}`) are defined externally, and consumed by the indexer at runtime.

### At Runtime

If the app is registered at a [spring boot admin server](https://github.com/codecentric/spring-boot-admin) all environment properties can be changed at runtime.

**Changes made at runtime will be effective after a context reload but are lost after an application restart unless persisted in** `application.yml`

## Timers Properties

Harvesting Schedule timers:

```yaml
osmhConsumer:
 delay:
    # Auto Starts after delay of 60 seconds at startup
    initial: '60000'
```

The timer schedule for GCP use is defined in [CDC deployment repository's template-deployment.yaml](https://bitbucket.org/cessda/cessda.cdc.deploy/src/master/osmh-indexer/infrastructure/k8s/template-deployment.yaml), but if you are deploying the software elsewhere, then the timer settings in [application.yml](/src/main/resources/application.yml) are relevant. The profiles are defined in [application.yml](/src/main/resources/application.yml) and selected in [Dockerfile](Dockerfile).

Take care with the daily/Sunday timer settings, otherwise all running instances may attempt to reharvest the same endpoints at the same time.

## Built With

* [Maven](https://maven.apache.org/) - Dependency Management

## Contributing

Please read [Contributing to CESSDA Open Source Software](https://bitbucket.org/cessda/cessda.guidelines.public/src/master/CONTRIBUTING.md) for information on contribution to CESSDA software.

## Versioning

## Authors

* **Moses Mansaray <moses AT doraventures DOT com>** - *Initial work, first version release*

You can find the list of all contributors [here](CONTRIBUTORS.md)

## License

This project is licensed under the Apache 2 Licence - see the [LICENSE](LICENSE.txt) file for details

## Acknowledgments

## Edge Case and Assumptions

* Note the extra "/" workaround in the [application.yml](src/main/resources/application.yml) repository configuration for repositories that separate records with a different metadata prefix per language accessed with the same basic url.  This url in a way act as a key, so the extra "/" distinguishes the two for the specific metadataPrefix to be retrieved.  There must be a better way to handle this edge case.  This workaround affects this project, and the pasc-osmh-handler-oai-pmh as well.
