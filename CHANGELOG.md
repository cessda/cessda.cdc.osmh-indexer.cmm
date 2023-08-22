# Changelog

All notable changes to the OSMH Consumer Indexer will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/) and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

*For each release, use the following subsections:*

- *Added (for new features)*
- *Changed (for changes in existing functionality)*
- *Deprecated (for soon-to-be removed features)*
- *Removed (for now removed features)*
- *Fixed (for any bug fixes)*
- *Security (in case of vulnerabilities)*

## [3.4.0] - 2023-08-29

### Changed

- Replaced the deprecated Elasticsearch `RestHighLevelClient` with the new Elasticsearch client ([#539](https://github.com/cessda/cessda.cdc.versions/issues/539))
  - See https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/8.7/migrate-hlrc.html for details on what's changed between the old and new clients
- Converted all the models to Java records ([PR-25](https://github.com/cessda/cessda.cdc.osmh-indexer.cmm/pull/25))
- Refactor the indexing pipeline so that each XML is parsed asynchronously ([PR-28](https://github.com/cessda/cessda.cdc.osmh-indexer.cmm/pull/28))

### Removed

- Removed the ability to harvest OAI-PMH endpoints ([#533]((https://github.com/cessda/cessda.cdc.versions/issues/533))

## [3.2.0] - 2022-12-08

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.7414076.svg)](https://doi.org/10.5281/zenodo.7414076)

### Added

- Parse multipart language codes such as en-GB ([#219](https://github.com/cessda/cessda.cdc.versions/issues/219))
- Parse `relPubl` DDI elements into related publications entries ([#471](https://github.com/cessda/cessda.cdc.versions/issues/471))
- Delete studies from the Elasticsearch index if the source XML is no longer present ([#486](https://github.com/cessda/cessda.cdc.versions/issues/486))
- Parse `universe` DDI elements ([#499](https://github.com/cessda/cessda.cdc.versions/issues/499))

### Changed

- Optimise the loading of repository configurations to avoid delays while repositories are being discovered ([#409](https://github.com/cessda/cessda.cdc.versions/issues/409))

## [3.0.2] - 2022-09-06

### Changed

- Update Elasticsearch to version 7 ([#429](https://github.com/cessda/cessda.cdc.versions/issues/429))

## [3.0.0] - 2022-06-07

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.6577776.svg)](https://doi.org/10.5281/zenodo.6577776)

### Added

- Filter out invalid URLs from the study URL field ([#390](https://github.com/cessda/cessda.cdc.versions/issues/390))
- Add a publisherFilter field ([#430](https://github.com/cessda/cessda.cdc.versions/issues/430))

### Changed

- Updated Elasticsearch imports ([#269](https://github.com/cessda/cessda.cdc.versions/issues/269))
- Generate identifiers based on the CDC identifier specification ([#386](https://github.com/cessda/cessda.cdc.versions/issues/386))
- Convert the indexer into a command line application that can be run as a scheduled task ([#392](https://github.com/cessda/cessda.cdc.versions/issues/392))
- Add support for defining repositories using a pipeline.json file. Update ReadMe file accordingly ([#409](https://github.com/cessda/cessda.cdc.versions/issues/409))
- Refactor the indexer, simplify configuration ([#428](https://github.com/cessda/cessda.cdc.versions/issues/428))

### Removed

- Remove Spring Data Elasticsearch as a dependency, use the Elasticsearch client directly ([#405](https://github.com/cessda/cessda.cdc.versions/issues/405))

## [2.5.0] - 2021-11-25

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.5710021.svg)](https://doi.org/10.5281/zenodo.5710021)

### Added

- Added support for indexing a repository from a folder on disk ([#146](https://github.com/cessda/cessda.cdc.versions/issues/146))
- Add support for Year-Month dates to Elasticsearch ([#352](https://github.com/cessda/cessda.cdc.versions/issues/352))

### Changed

- Updated UniData's Endpoint ([#366](https://github.com/cessda/cessda.cdc.versions/issues/366))
- Updated OpenJDK to 17 ([#269](https://github.com/cessda/cessda.cdc.versions/issues/269))
- Removed usages of Spring Data Elasticsearch and replaced them with direct use of the Elasticsearch client ([#146](https://github.com/cessda/cessda.cdc.versions/issues/146))

### Fixes

- Fixed the metadata prefix always being null [#385](https://github.com/cessda/cessda.cdc.versions/issues/385))
- Fixed some code smells identified by SonarQube ([#369](https://github.com/cessda/cessda.cdc.versions/issues/369))
- Fixed `ElasticsearchSet` throwing an `ArrayIndexOutOfBoundsException` when accessing an Elasticsearch scroll that does not have a scroll ID ([#INDEXER-2](https://github.com/cessda/cessda.cdc.osmh-indexer.cmm/issues/2))

## [2.4.0] - 2021-06-23

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.5017358.svg)](https://doi.org/10.5281/zenodo.5017358)

### Added

- Log the time it takes to harvest each repository ([#296](https://github.com/cessda/cessda.cdc.versions/issues/296))
- Use the autocomplete analyser in Elasticsearch for the abstract, country and term fields ([#285](https://github.com/cessda/cessda.cdc.versions/issues/285))

### Changed

- Update GESIS' repository endpoint URL ([#303](https://github.com/cessda/cessda.cdc.versions/issues/303))
- Update SoDaNet's repository endpoint URL ([#277](https://github.com/cessda/cessda.cdc.versions/issues/277))
- Support Elasticsearch 6.8, migrate to the Elasticsearch REST client and remove the Transport Client ([#312](https://github.com/cessda/cessda.cdc.versions/issues/312))
    - This allows the indexer to connect to secured Elasticsearch clusters
- Update Spring Boot to 2.2.13([#312](https://github.com/cessda/cessda.cdc.versions/issues/312))
- Disable dynamic Elasticsearch mapping ([#312](https://github.com/cessda/cessda.cdc.versions/issues/312))
- Add support for Elasticsearch security ([#321](https://github.com/cessda/cessda.cdc.versions/issues/321))

### Fixed

- Fix nested fields in Elasticsearch not being searchable ([#335](https://github.com/cessda/cessda.cdc.versions/issues/335))

## [2.3.1] - 2021-02-11

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.4534741.svg)](https://doi.org/10.5281/zenodo.4534741)

### Added

- Add ADP to the list of harvested endpoints ([#201](https://github.com/cessda/cessda.cdc.versions/issues/201))

### Changed

- Included DANS twice with different metadata parameters to pick up English and Dutch study versions ([#280](https://github.com/cessda/cessda.cdc.versions/issues/280))
- Improved the debug logging of studies dropped for having no languages with the minimum required fields ([#278](https://github.com/cessda/cessda.cdc.versions/issues/278))

## [2.3.0] - 2021-02-09

[10.5281/zenodo.4525896](https://zenodo.org/record/4525896)

### Added

- Add HTTP compression to the repository handler ([#167](https://github.com/cessda/cessda.cdc.versions/issues/167))
- Add Code of Conduct file ([#174](https://github.com/cessda/cessda.cdc.versions/issues/174))
- Add new PROGEDO endpoint ([#177](https://github.com/cessda/cessda.cdc.versions/issues/177))
- Harvest each repository endpoint with a dedicated thread ([#178](https://github.com/cessda/cessda.cdc.versions/issues/178), [#225](https://github.com/cessda/cessda.cdc.versions/issues/225))
- Add SODA endpoint ([#190](https://github.com/cessda/cessda.cdc.versions/issues/190))
- Add option to set default language as part of endpoint specification ([#192](https://github.com/cessda/cessda.cdc.versions/issues/192))
- Add more details to 'Configured Repos' log output ([#195](https://github.com/cessda/cessda.cdc.versions/issues/195))
- Add code as an additional field in the indexer model ([#199](https://github.com/cessda/cessda.cdc.versions/issues/199))
- Add ADP Kuha2 Endpoint ([#201](https://github.com/cessda/cessda.cdc.versions/issues/201))
- Add stopwords for Hungarian and Portuguese language analysers ([#204](https://github.com/cessda/cessda.cdc.versions/issues/204))
- Improve the logging of remote repository handlers ([#207](https://github.com/cessda/cessda.cdc.versions/issues/207))
- Implement a country filter so that only countries with ISO country codes are accepted ([#214](https://github.com/cessda/cessda.cdc.versions/issues/214))
- Delete inactive records from Elasticsearch ([#217](https://github.com/cessda/cessda.cdc.versions/issues/217))
- Add a `run_type` variable to the logs to distinguish different types of harvester runs ([#227](https://github.com/cessda/cessda.cdc.versions/issues/227))

### Changed

- Remove "not available" if no PID agency is present ([#156](https://github.com/cessda/cessda.cdc.versions/issues/156))
- Revise XML Schema Definition to ensure compliance with system implementation ([#59](https://github.com/cessda/cessda.cdc.versions/issues/59))
- Search Optimisation ([#131](https://github.com/cessda/cessda.cdc.versions/issues/131))
- Remove (not available) if no PID agency ([#156](https://github.com/cessda/cessda.cdc.versions/issues/156))
- Modify Harvester to output Required logs ([#159](https://github.com/cessda/cessda.cdc.versions/issues/159))
- Disable access to external XML entities in the repository handlers ([#176](https://github.com/cessda/cessda.cdc.versions/issues/176))
- Log statistics for created, deleted and updated studies ([#181](https://github.com/cessda/cessda.cdc.versions/issues/181))
- Cleaning Publisher filter ([#183](https://github.com/cessda/cessda.cdc.versions/issues/183))
- Update Elasticsearch to 5.6 ([#188](https://github.com/cessda/cessda.cdc.versions/issues/188))
- Support Spring Boot Admin 2 for metrics and remote management ([#191](https://github.com/cessda/cessda.cdc.versions/issues/191), [#211](https://github.com/cessda/cessda.cdc.versions/issues/211))
- Add more details to 'Configured Repos' log output ([#194](https://github.com/cessda/cessda.cdc.versions/issues/194))
- Change SODA publisher name ([#197](https://github.com/cessda/cessda.cdc.versions/issues/197))
- Update SND set spec ([#200](https://github.com/cessda/cessda.cdc.versions/issues/200))
- Refine the list of fields to be indexed ([#238](https://github.com/cessda/cessda.cdc.versions/issues/238))
- Map `langAvailableIn` as a keyword, so that it can be used for sorting and filtering ([#241](https://github.com/cessda/cessda.cdc.versions/issues/241))
- Add a search field for country metadata ([#252](https://github.com/cessda/cessda.cdc.versions/issues/252))

### Fixed

- Set the study url field from any language before replacing it with the language specific element ([#142](https://github.com/cessda/cessda.cdc.versions/issues/142))
- Fix alphabetical sorting issues caused by not normalising upper and lower case letters ([#171](https://github.com/cessda/cessda.cdc.versions/issues/171))
- Fix rejection reason not showing in the logs ([#184](https://github.com/cessda/cessda.cdc.versions/issues/184))
- Cleanup code ([#203](https://github.com/cessda/cessda.cdc.versions/issues/203))
- Fix title ascending/descending sort options not functioning ([#209](https://github.com/cessda/cessda.cdc.versions/issues/209))

## [2.2.1] - 2020-05-04

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.3786356.svg)](https://doi.org/10.5281/zenodo.3786356)

### Added

- new GESIS endpoint ([#162](https://github.com/cessda/cessda.cdc.versions/issues/162))
- file appender
- format error log message for successful indexing
- implemented correlation id using MDC.putClosable
- correlation ID to the log messages
- dependency for JSON logging support (logstash-logback-encoder 5.2)

### Changed

- changed GESIS endpoint from HTTP to HTTPS ([#162](https://github.com/cessda/cessda.cdc.versions/issues/162))
- use Java Time APIs for the `PerfRequestSyncInterceptor` stopwatch
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
- refactored the error handling code in `DaoBase.postForStringResponse()` to better align with Java best practices
- refactored exception handling to avoid catching `RuntimeException` and a cast
- print the config in `StatusService.printPaSCHandlerOaiPmhConfig()` directly
- change behaviour when Study PID Agency is not specified. Before: '10.5279/DK-SA-DDA-868 (not available)'.
    After: '10.5279/DK-SA-DDA-868 (Agency not available)' ([#156](https://github.com/cessda/cessda.cdc.versions/issues/156))
- log queries at the info level
- moved recursion out of the try-with-resources block to reduce resource consumption
- reformatted the message when the record headers could not be parsed (because the parser could have failed at any point and left the `InputStream` in an inconsistent state)
- use input streams instead of strings (avoids a double copy)
- renamed `dev` profile to `gcp`
- improved logging to help determine quality of harvested metadata ([#191](https://github.com/cessda/cessda.cdc.versions/issues/91))

### Removed

- caches of `RuntimeException` in `ESIngestService`
- option to disable HTTPS verification
- unnecessary `null` check

### Fixed

- compiler warnings, as recommended by Error Prone
- time zone bugs
- logging pattern for the file logger
- unused micrometer dependencies
- unused `DocumentBuilder` bean
- issues reported by SonarQube
- register DocumentBuilderFactories as beans instead of DocumentBuilders. DocumentBuilders are not thread safe and need resetting after use. `DocumentBuilderFactory.createDocumentBuilder()` is thread safe and should be used instead
- fixed logs not showing in Spring Boot Admin
- encoded the resumption token in case characters invalid for URIs are returned
- time zone bugs

### Security

- verify SSL
- removed the option to disable HTTPS verification

[3.4.0]: https://github.com/cessda/cessda.cdc.osmh-indexer.cmm/releases/tag/3.4.0
[3.2.1]: https://github.com/cessda/cessda.cdc.osmh-indexer.cmm/releases/tag/3.2.1
[3.2.0]: https://github.com/cessda/cessda.cdc.osmh-indexer.cmm/releases/tag/3.2.0
[3.0.2]: https://github.com/cessda/cessda.cdc.osmh-indexer.cmm/releases/tag/3.0.2
[3.0.0]: https://github.com/cessda/cessda.cdc.osmh-indexer.cmm/releases/tag/3.0.0
[2.5.0]: https://github.com/cessda/cessda.cdc.osmh-indexer.cmm/releases/tag/2.5.0
[2.4.0]: https://github.com/cessda/cessda.cdc.osmh-indexer.cmm/releases/tag/2.4.0
[2.3.1]: https://github.com/cessda/cessda.cdc.osmh-indexer.cmm/releases/tag/2.3.1
[2.3.0]: https://github.com/cessda/cessda.cdc.osmh-indexer.cmm/releases/tag/2.3.0
[2.2.1]: https://github.com/cessda/cessda.cdc.osmh-indexer.cmm/releases/tag/2.2.1
