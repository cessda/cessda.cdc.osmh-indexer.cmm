# Changelog
All notable changes to the OSMH Handler OAI-PMH  will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

*For each release, use the following sub-sections:*  
*- Added (for new features)*  
*- Changed (for changes in existing functionality)*  
*- Deprecated (for soon-to-be removed features)*  
*- Removed (for now removed features)*  
*- Fixed (for any bug fixes)*  
*- Security (in case of vulnerabilities)*

## [Unreleased]
- Added Code of Conduct


## [2.2.1] - 2020-05-04    

OSMH Handler OAI-PMH - [10.5281/zenodo.3786446](https://zenodo.org/deposit/3786446)


### Added
- file appender
- format error log message for successful indexing
- implemented correlation id using MDC.putClosable
- correlation ID to the log messages
- dependency for JSON logging support (logstash-logback-encoder 5.2)

### Changed
- use Java Time APIs for the PerfRequestSyncInterceptor stopwatch
- increased test coverage
- updated SonarQube scanner to 3.7.0
- updated Spring Boot to 1.5.21
- unified timeout and SSL verification settings
- refined error log for unsuccessful indexing
- marked all utility classes as final
- revised and re-ordered list of endpoints
- use Jib to containerise the indexer
- updated Maven wrapper to 0.5.3
- refactored the error handling code in DaoBase.postForStringResponse to better align with Java best practices
- refactored exception handling to avoid catching RuntimeException and a cast
- print the config in StatusService.printPaSCHandlerOaiPmhConfig() directly
- change behaviour when Study PID Agency is not specified. Before: '10.5279/DK-SA-DDA-868 (not available)'. After: '10.5279/DK-SA-DDA-868 (Agency not available)' ([#156](https://bitbucket.org/cessda/cessda.cdc.version2/issues/156))
- log queries at the info level
- moved recursion out of the try-with-resources block to reduce resource consumption
- use input streams instead of strings (avoids a double copy)
- renamed 'dev' profile to *gcp*
- improved logging to help determine quality of harvested metadata ([#191](https://bitbucket.org/cessda/cessda.cdc.version2/issues/91))

### Deprecated
- N/A

### Removed
- caches of RuntimeException in ESIngestService
- option to disable HTTPS verification
- unnecessary null check

### Fixed
- compiler warnings, as recommended by Error Prone
- time zone bugs
- logging pattern for the file logger
- unused micrometer dependencies
- unused DocumentBuilder bean
- issues reported by SonarQube
- register DocumentBuilderFactories as beans instead of DocumentBuilders. DocumentBuilders are not thread safe and need resetting after use. DocumentBuilderFactory.createDocumentBuilder() is thread safe and should be used instead
- fixed logs not showing in Spring Boot Admin
- encoded the resumption token in case characters invalid for URIs are returned
- time zone bugs

### Security
- verify SSL
- removed the option to disable HTTPS verification
