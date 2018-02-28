package eu.cessda.pasc.oci.service.helpers;

import eu.cessda.pasc.oci.configurations.PascOciConfig;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Language Document Extractor.  Helper to Extracts a custom document for each language IsoCode found in the config.
 *
 * @author moses@doraventures.com
 */
@Component
@Slf4j
public class LanguageDocumentExtractor {

  @Autowired
  private PascOciConfig pascOciConfig;

  /**
   * Extracts a custom document for each language IsoCode found in the config.
   *
   * @param cmmStudies raw list of studies which generally holds fields for all languages.
   * @return map extracted documents for each language iso code.
   */
  public Map<String, List<CMMStudyOfLanguage>> mapLanguageDoc(List<Optional<CMMStudy>> cmmStudies, String spName) {

    log.info("Mapping CMMStudy to CMMStudyOfLanguage for SP[{}] with [{}] records", spName, cmmStudies.size());
    Map<String, List<CMMStudyOfLanguage>> languageDocMap = new HashMap<>();

    String idPrefix = spName.trim().replace(" ", "-") + "__"; // UK Data Service = UK-Data-Service__

    // TODO REFACTOR this for a single pass on the date list instead
    pascOciConfig.getLanguages()
        .forEach(languageIsoCode -> {

              long startTime = System.currentTimeMillis();
              log.info("Extract CMMStudyOfLanguage for [{}] language code - STARTED", languageIsoCode);
              List<CMMStudyOfLanguage> collectLanguageCmmStudy = cmmStudies
                  .stream()
                  .filter(Optional::isPresent)
                  .map(Optional::get)
                  .map(cmmStudy -> getCmmStudyOfLanguage(idPrefix, languageIsoCode, cmmStudy))
                  .collect(Collectors.toList());
              languageDocMap.put(languageIsoCode, collectLanguageCmmStudy);

              long took = System.currentTimeMillis() - startTime / 1000;
          String formatMsg = "Extract CMMStudyOfLanguage for [{}] language code - COMPLETED in [{}]Seconds";
          log.info(formatMsg, languageIsoCode, took);
            }
        );

    logDetailedExtractionsReport(languageDocMap);
    return languageDocMap;
  }

  private CMMStudyOfLanguage getCmmStudyOfLanguage(String idPrefix, String lang, CMMStudy cmmStudy) {
    log.debug("Extracting CMMStudyOfLanguage from CMMStudyNumber [{}] for lang [{}]", cmmStudy.getStudyNumber(), lang);
    CMMStudyOfLanguage.CMMStudyOfLanguageBuilder builder = CMMStudyOfLanguage.builder();

    builder.id(idPrefix + cmmStudy.getStudyNumber())
        .studyNumber(cmmStudy.getStudyNumber())
        .active(cmmStudy.isActive())
        .lastModified(cmmStudy.getLastModified())
        .publicationYear(cmmStudy.getPublicationYear())
        .fileLanguages(cmmStudy.getFileLanguages())
        .dataCollectionPeriodStartdate(cmmStudy.getDataCollectionPeriodStartdate())
        .dataCollectionPeriodEnddate(cmmStudy.getDataCollectionPeriodEnddate());

    log.trace("StudyNumber Record [{}]: Deal with retrieval from nullable fields", cmmStudy.getStudyNumber());
    ofNullable(cmmStudy.getTitleStudy()).ifPresent(map -> builder.titleStudy(map.get(lang)));
    ofNullable(cmmStudy.getAbstractField()).ifPresent(map -> builder.abstractField(map.get(lang)));
    ofNullable(cmmStudy.getKeywords()).ifPresent(map -> builder.keywords(map.get(lang)));
    ofNullable(cmmStudy.getClassifications()).ifPresent(map -> builder.classifications(map.get(lang)));
    ofNullable(cmmStudy.getTypeOfTimeMethods()).ifPresent(map -> builder.typeOfTimeMethods(map.get(lang)));
    ofNullable(cmmStudy.getStudyAreaCountries()).ifPresent(map -> builder.studyAreaCountries(map.get(lang)));
    ofNullable(cmmStudy.getUnitTypes()).ifPresent(map -> builder.unitTypes(map.get(lang)));
    ofNullable(cmmStudy.getPublisher()).ifPresent(map -> builder.publisher(map.get(lang)));
    ofNullable(cmmStudy.getPidStudies()).ifPresent(map -> builder.pidStudies(map.get(lang)));
    ofNullable(cmmStudy.getCreators()).ifPresent(map -> builder.creators(map.get(lang)));
    ofNullable(cmmStudy.getTypeOfSamplingProcedures()).ifPresent(map -> builder.typeOfSamplingProcedures(map.get(lang)));
    ofNullable(cmmStudy.getSamplingProcedureFreeTexts()).ifPresent(map -> builder.samplingProcedureFreeTexts(map.get(lang)));
    ofNullable(cmmStudy.getTypeOfModeOfCollections()).ifPresent(map -> builder.typeOfModeOfCollections(map.get(lang)));
    ofNullable(cmmStudy.getTitleStudy()).ifPresent(map -> builder.titleStudy(map.get(lang)));
    ofNullable(cmmStudy.getDataCollectionFreeTexts()).ifPresent(map -> builder.dataCollectionFreeTexts(map.get(lang)));
    ofNullable(cmmStudy.getDataAccessFreeTexts()).ifPresent(map -> builder.dataAccessFreeTexts(map.get(lang)));
    log.trace("Extracted StudyNumber Record [{}]", cmmStudy.getStudyNumber());

    return builder.build();
  }

  private void logDetailedExtractionsReport(Map<String, List<CMMStudyOfLanguage>> languageDocMap) {
    if (log.isDebugEnabled()) {
      languageDocMap.forEach((langIsoCode, cmmStudyOfLanguages) -> {
        String formatTemplate = "langIsoCode [{}] has [{}] records passed";
        log.debug(formatTemplate, langIsoCode, cmmStudyOfLanguages.size());
      });
    }
  }
}
