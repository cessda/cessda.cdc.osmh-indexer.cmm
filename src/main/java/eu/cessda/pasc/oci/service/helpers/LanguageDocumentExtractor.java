package eu.cessda.pasc.oci.service.helpers;

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
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
  private AppConfigurationProperties appConfigurationProperties;

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

    // TODO REFACTOR this for a single pass or - make handler return records with langCode as top node
    // with record embedded to each langCode, so that one can easily grab all records more efficiently for a langCode
    appConfigurationProperties.getLanguages().forEach(langCode -> {
          Instant start = Instant.now();
          log.info("Extract CMMStudyOfLanguage for [{}] language code - STARTED", langCode);
          List<CMMStudyOfLanguage> collectLanguageCmmStudy = getCmmStudiesOfLangCode(cmmStudies, idPrefix, langCode);
          languageDocMap.put(langCode, collectLanguageCmmStudy);
          logTimeTook(langCode, start);
        }
    );

    logDetailedExtractionsReport(languageDocMap);
    return languageDocMap;
  }

  private List<CMMStudyOfLanguage> getCmmStudiesOfLangCode(List<Optional<CMMStudy>> cmmStudies, String idPrefix,
                                                           String languageIsoCode) {
    return cmmStudies.stream()
        .filter(cmmStudy -> isValidCMMStudyForLang(languageIsoCode, cmmStudy))
        .map(Optional::get)
        .map(cmmStudy -> getCmmStudyOfLanguage(idPrefix, languageIsoCode, cmmStudy))
        .collect(Collectors.toList());
  }

  private boolean isValidCMMStudyForLang(String languageIsoCode, Optional<CMMStudy> cmmStudy) {

    if (!cmmStudy.isPresent()) {
      return false;
    } else if (!cmmStudy.get().isActive()) {
      return true; // Inactive = deleted record
    } else {

      // Active record Must have titleStudy
      Optional<Map<String, String>> titleStudy = ofNullable(cmmStudy.get().getTitleStudy());
      if (!titleStudy.isPresent() || !ofNullable(titleStudy.get().get(languageIsoCode)).isPresent()) {
        return false;
      }

      // Active record Must have abstract
      Optional<Map<String, String>> abstractField = ofNullable(cmmStudy.get().getAbstractField());
      return abstractField.isPresent() && ofNullable(abstractField.get().get(languageIsoCode)).isPresent();
    }
  }

  private CMMStudyOfLanguage getCmmStudyOfLanguage(String idPrefix, String lang, CMMStudy cmmStudy) {

    if (log.isTraceEnabled()) {
      log.trace("Extracting CMMStudyOfLang from CMMStudyNumber [{}] for lang [{}]", cmmStudy.getStudyNumber(), lang);
    }

    CMMStudyOfLanguage.CMMStudyOfLanguageBuilder builder = CMMStudyOfLanguage.builder();

    builder.id(idPrefix + cmmStudy.getStudyNumber())
        .studyNumber(cmmStudy.getStudyNumber())
        .active(cmmStudy.isActive())
        .lastModified(cmmStudy.getLastModified())
        .publicationYear(cmmStudy.getPublicationYear())
        .fileLanguages(cmmStudy.getFileLanguages())
        .dataCollectionPeriodStartdate(cmmStudy.getDataCollectionPeriodStartdate())
        .dataCollectionPeriodEnddate(cmmStudy.getDataCollectionPeriodEnddate());

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
    ofNullable(cmmStudy.getStudyUrl()).ifPresent(map -> builder.studyUrl(map.get(lang)));

    return builder.build();
  }

  private static void logTimeTook(String langCode, Instant startTime) {
    String formatMsg = "Extract CMMStudyOfLanguage for [{}] language code - COMPLETED. Duration [{}]";
    Instant end = Instant.now();
    log.info(formatMsg, langCode, Duration.between(startTime, end));
  }

  private static void logDetailedExtractionsReport(Map<String, List<CMMStudyOfLanguage>> languageDocMap) {
    if (log.isDebugEnabled()) {
      languageDocMap.forEach((langIsoCode, cmmStudyOfLanguages) -> {
        String formatTemplate = "langIsoCode [{}] has [{}] records passed";
        log.debug(formatTemplate, langIsoCode, cmmStudyOfLanguages.size());
      });
    }
  }
}
