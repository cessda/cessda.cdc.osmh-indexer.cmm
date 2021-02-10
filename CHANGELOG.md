# Changelog

All notable changes to the OSMH Consumer Indexer will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/) and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

*For each release, use the following sub-sections:*
*- Added (for new features)*
*- Changed (for changes in existing functionality)*
*- Deprecated (for soon-to-be removed features)*
*- Removed (for now removed features)*
*- Fixed (for any bug fixes)*
*- Security (in case of vulnerabilities)*

## [2.3.0] - 2021-02-09

[10.5281/zenodo.4525896](https://zenodo.org/record/4525896)

### Additions

- Add HTTP compression to the repository handler ([#167](https://bitbucket.org/cessda/cessda.cdc.version2/issues/167))
- Add Code of Conduct file ([#174](https://bitbucket.org/cessda/cessda.cdc.version2/issues/174))
- Add new PROGEDO endpoint ([#177](https://bitbucket.org/cessda/cessda.cdc.version2/issues/177))
- Harvest each repository endpoint with a dedicated thread ([#178](https://bitbucket.org/cessda/cessda.cdc.version2/issues/178), [#225](https://bitbucket.org/cessda/cessda.cdc.version2/issues/225))
- Add SODA endpoint ([#190](https://bitbucket.org/cessda/cessda.cdc.version2/issues/190))
- Add option to set default language as part of endpoint specification ([#192](https://bitbucket.org/cessda/cessda.cdc.version2/issues/192))
- Add more details to 'Configured Repos' log output ([#195](https://bitbucket.org/cessda/cessda.cdc.version2/issues/195))
- Add code as an additional field in the indexer model ([#199](https://bitbucket.org/cessda/cessda.cdc.version2/issues/199))
- Add ADP Kuha2 Endpoint ([#201](https://bitbucket.org/cessda/cessda.cdc.version2/issues/201))
- Add stopwords for Hungarian and Portuguese language analysers ([#204](https://bitbucket.org/cessda/cessda.cdc.version2/issues/204))
- Improve the logging of remote repository handlers ([#207](https://bitbucket.org/cessda/cessda.cdc.version2/issues/207))
- Implement a country filter so that only countries with ISO country codes are accepted ([#214](https://bitbucket.org/cessda/cessda.cdc.version2/issues/214))
- Delete inactive records from Elasticsearch ([#217](https://bitbucket.org/cessda/cessda.cdc.version2/issues/217))
- Add a `run_type` variable to the logs to distinguish different types of harvester runs ([#227](https://bitbucket.org/cessda/cessda.cdc.version2/issues/227))

### Changes

- Remove "not available" if no PID agency is present ([#156](https://bitbucket.org/cessda/cessda.cdc.version2/issues/156))
- Revise XML Schema Definition to ensure compliance with system implementation ([#59](https://bitbucket.org/cessda/cessda.cdc.version2/issues/59))
- Search Optimisation ([#131](https://bitbucket.org/cessda/cessda.cdc.version2/issues/131))
- Remove (not available) if no PID agency ([#156](https://bitbucket.org/cessda/cessda.cdc.version2/issues/156))
- Modify Harvester to output Required logs ([#159](https://bitbucket.org/cessda/cessda.cdc.version2/issues/159))
- Disable access to external XML entities in the repository handlers ([#176](https://bitbucket.org/cessda/cessda.cdc.version2/issues/176))
- Log statistics for created, deleted and updated studies ([#181](https://bitbucket.org/cessda/cessda.cdc.version2/issues/181))
- Cleaning Publisher filter ([#183](https://bitbucket.org/cessda/cessda.cdc.version2/issues/183))
- Update Elasticsearch to 5.6 ([#188](https://bitbucket.org/cessda/cessda.cdc.version2/issues/188))
- Support Spring Boot Admin 2 for metrics and remote management ([#191](https://bitbucket.org/cessda/cessda.cdc.version2/issues/191), [#211](https://bitbucket.org/cessda/cessda.cdc.version2/issues/211))
- Add more details to 'Configured Repos' log output ([#194](https://bitbucket.org/cessda/cessda.cdc.version2/issues/194))
- Change SODA publisher name ([#197](https://bitbucket.org/cessda/cessda.cdc.version2/issues/197))
- Update SND set spec ([#200](https://bitbucket.org/cessda/cessda.cdc.version2/issues/200))
- Refine the list of fields to be indexed ([#238](https://bitbucket.org/cessda/cessda.cdc.version2/issues/238))
- Map `langAvailableIn` as a keyword, so that it can be used for sorting and filtering ([#241](https://bitbucket.org/cessda/cessda.cdc.version2/issues/241))
- Add a search field for country metadata ([#252](https://bitbucket.org/cessda/cessda.cdc.version2/issues/252))

### Fixes

- Set the study url field from any language before replacing it with the language specific element ([#142](https://bitbucket.org/cessda/cessda.cdc.version2/issues/142))
- Fix alphabetical sorting issues caused by not normalising upper and lower case letters ([#171](https://bitbucket.org/cessda/cessda.cdc.version2/issues/171))
- Fix rejection reason not showing in the logs ([#184](https://bitbucket.org/cessda/cessda.cdc.version2/issues/184))
- Cleanup code ([#203](https://bitbucket.org/cessda/cessda.cdc.version2/issues/203))
- Fix title ascending/descending sort options not functioning ([#209](https://bitbucket.org/cessda/cessda.cdc.version2/issues/209))

## [2.2.1] - 2020-05-04

OSMH Consumer Indexer - [10.5281/zenodo.3786356](https://zenodo.org/record/3786356)

### Added

- new GESIS endpoint ([#162](https://bitbucket.org/cessda/cessda.cdc.version2/issues/162))
- file appender
- format error log message for successful indexing
- implemented correlation id using MDC.putClosable
- correlation ID to the log messages
- dependency for JSON logging support (logstash-logback-encoder 5.2)

### Changed

- changed GESIS endpoint from HTTP to HTTPS ([#162](https://bitbucket.org/cessda/cessda.cdc.version2/issues/162))
- use Java Time APIs for the PerfRequestSyncInterceptor stopwatch
- increased test coverage
- updated SonarQube scanner to 3.7.0
- updated Spring Boot to 1.5.21
- unified timeout and SSL verification settings
- refined error log for unsuccessful indexing
- marked all utility classes as final
- close the Elasticsearch client on shutdown
- revised and re-ordered list of endpoints
- use Jib to containerise the indexer
- updated Maven wrapper to 0.5.3
- refactored the error handling code in DaoBase.postForStringResponse to better align with Java best practices
- refactored exception handling to avoid catching RuntimeException and a cast
- print the config in StatusService.printPaSCHandlerOaiPmhConfig() directly
- change behaviour when Study PID Agency is not specified. Before: '10.5279/DK-SA-DDA-868 (not available)'.
    After: '10.5279/DK-SA-DDA-868 (Agency not available)' ([#156](https://bitbucket.org/cessda/cessda.cdc.version2/issues/156))
- log queries at the info level
- moved recursion out of the try-with-resources block to reduce resource consumption
- reformatted the message when the record headers could not be parsed (because the parser could have failed at any point and left the InputStream in an inconsistent state)
- use input streams instead of strings (avoids a double copy)
- renamed `dev` profile to `gcp`
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
