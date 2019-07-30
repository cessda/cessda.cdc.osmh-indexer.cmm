# OSMH Consumer Indexer (PaSC-OCI)

CESSDA CDC Consumer Indexer (an OSMH Consumer) for Metadata harvesting  and ingestion into Elasticsearch.
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

* Java JDK 8
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
* For integrations test, loads up an embedded elasticsearch server with tests against it

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
**EFFECTIVE AFTER A CONTEXT RELOAD**
**LOST AFTER AN APPLICATION RESTART UNLESS PERSISTED IN APPLICATION.yml**

## Timers Properties

Harvesting Schedule timers (different for each instance):

```yaml
osmhConsumer:
 delay:
    # Auto Starts after delay of 1min at startup
    initial: '60000'
    # Yearly clean up run
    fixed: '315360000000'
daily:
   # Daily Harvest and Ingestion run at 00:01am.
    run: '0 01 00 * * *'
    # Then run every Sunday at 09:00
    sunday.run: '0 00 09 * * SUN'
```

## Built With

* [Maven](https://maven.apache.org/) - Dependency Management

## Contributing

Please read [CESSDA Guideline for developpers](https://bitbucket.org/cessda/cessda.guidelines.cit/wiki/Developers)
for details on our code of conduct, and the process for submitting pull requests to us.

## Versioning

## Authors

* **Moses Mansaray <moses AT doravenetures DOT com>** - *Initial work, first version release*

You can find the list of all contributors [here](CONTRIBUTORS.md)

## License

This project is licensed under the Apache 2 License - see the [LICENSE](LICENSE) file for details

## Acknowledgments

## Edge Case and Assumptions

* Note the dirty extra "/" workaround in the [application.yml](src/main/resources/application.yml) repository configuration for GESIS (and GESIS DE) who separate records to different metadata prefix per language hosted with the same repository url.  This url in a way act as a key, so the extra "/" distinguishes the two for the specific metadataPrefix to be retrieved.  There must be a better way to handle this edge case that is specific to GESIS. Not seen other SPs with this implementation that separate records in different languages by using a different metadataPrefix, so leaving this as is for now.  This workaround affects this project and the pasc-osmh-handler-oai-pmh
