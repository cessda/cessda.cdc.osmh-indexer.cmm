package eu.cessda.pasc.oci.service;

import eu.cessda.pasc.oci.configurations.PascOciConfig;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Language Document Extractor.  Helper to Extracts a custom document for each language IsoCode found in the config.
 *
 * @author moses@doraventures.com
 */
@Component
public class LanguageDocumentExtractor {

  @Autowired
  private PascOciConfig pascOciConfig;

  /**
   * Extracts a custom document for each language IsoCode found in the config.
   *
   * @param cmmStudies raw list of studies which generally holds fields for all languages.
   * @return map extracted documents for each language iso code.
   */
  public Map<String, List<CMMStudyOfLanguage>> mapLanguageDoc(List<CMMStudy> cmmStudies) {
    Map<String, List<CMMStudyOfLanguage>> languageDocMap = new HashMap<>();

    List<String> languages = pascOciConfig.getLanguages();
    languages.forEach(languageIsoCode -> {
          List<CMMStudyOfLanguage> collectLanguageCmmStudy = cmmStudies
              .parallelStream()
              .map(cmmStudy -> CMMStudyOfLanguage.builder()
                  .studyNumber(cmmStudy.getStudyNumber())
                  .titleStudy(cmmStudy.getTitleStudy().get(languageIsoCode))
                  .abstractField(cmmStudy.getAbstractField().get(languageIsoCode))
                  .classifications(cmmStudy.getClassifications().get(languageIsoCode))
                  .keywords(cmmStudy.getKeywords().get(languageIsoCode))
                  .typeOfTimeMethods(cmmStudy.getTypeOfTimeMethods().get(languageIsoCode))
                  .studyAreaCountries(cmmStudy.getStudyAreaCountries().get(languageIsoCode))
                  .unitTypes(cmmStudy.getUnitTypes().get(languageIsoCode))
                  .publisher(cmmStudy.getPublisher().get(languageIsoCode))
                  .publicationYear(cmmStudy.getPublicationYear())
                  .pidStudies(cmmStudy.getPidStudies().get(languageIsoCode))
                  .fileLanguages(cmmStudy.getFileLanguages())
                  .creators(cmmStudy.getCreators().get(languageIsoCode))
                  .typeOfSamplingProcedures(cmmStudy.getTypeOfSamplingProcedures().get(languageIsoCode))
                  .samplingProcedureFreeTexts(cmmStudy.getSamplingProcedureFreeTexts().get(languageIsoCode))
                  .typeOfModeOfCollections(cmmStudy.getTypeOfModeOfCollections().get(languageIsoCode))
                  .dataCollectionPeriodStartdate(cmmStudy.getDataCollectionPeriodStartdate())
                  .dataCollectionPeriodEnddate(cmmStudy.getDataCollectionPeriodEnddate())
                  .dataCollectionFreeTexts(cmmStudy.getDataCollectionFreeTexts().get(languageIsoCode))
                  .dataAccessFreeTexts(cmmStudy.getDataAccessFreeTexts().get(languageIsoCode))
                  .lastModified(cmmStudy.getLastModified())
                  .active(cmmStudy.isActive())
                  .build()).collect(Collectors.toList());
          languageDocMap.put(languageIsoCode, collectLanguageCmmStudy);
        }
    );
    return languageDocMap;
  }
}
