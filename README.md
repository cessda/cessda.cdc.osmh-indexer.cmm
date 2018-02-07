# PaSC OSMH Handler OAI-PMH

Cessda PaSC OSMH repository handler for harvesting OAI-PMH metadata format

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
  - Java JDK 8
  - Maven

`brew tap caskroom/versions`

`brew cask install java8`

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

Please read [CESSDA Guideline for developpers](https://bitbucket.org/cessda/cessda.guidelines.cit/wiki/Developers) 
for details on our code of conduct, and the process for submitting pull requests to us.

## Versioning

## Authors

* **Moses Mansaray <moses@doraventures.com>** - *Initial work, first version release*

You can find the list of all contributors [here](CONTRIBUTORS.md)

## License

This project is licensed under the Apache 2 License - see the [LICENSE](LICENSE) file for details

## Acknowledgments



## Edge Case and Assumptions:

- If publicationYear cannot be parsed to an Int we default to epoch year 1970

## TODO 

- Should map out PID Study alongside their Agency name.
- Remove Principal Investigator
