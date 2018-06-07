package eu.cessda.pasc.oci.service.helpers;

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 * Component responsible for extracting and mapping languages in which a given CMMStudy is available.
 * <p>
 * Note the CMM record must meet the minimum CMM Fields requirements.
 *
 * @author moses@doraventures.com
 */
@Component
public class LanguageAvailabilityMapper {

  private AppConfigurationProperties appConfigurationProperties;

  @Autowired
  public LanguageAvailabilityMapper(AppConfigurationProperties appConfigurationProperties) {
    this.appConfigurationProperties = appConfigurationProperties;
  }

  public void setAvailableLanguages(CMMStudy cmmStudy) {

    final List<String> propertiesLanguages = appConfigurationProperties.getLanguages();
    propertiesLanguages.forEach(lang -> {
      if (hasMinimumCmmFields(lang, cmmStudy)) {
        cmmStudy.getLangAvailableIn().add(lang);
      }
    });
  }

  private boolean hasMinimumCmmFields(String languageIsoCode, CMMStudy cmmStudy) {

    if (null == cmmStudy) {
      return false;
    }

    // the CMM record must meet the minimum CMM Fields requirements for given Lang Iso Code
    return hasTitle(languageIsoCode, cmmStudy) &&
        hasAbstract(languageIsoCode, cmmStudy) &&
        hasStudyNumber(cmmStudy) &&
        hasPublisher(languageIsoCode, cmmStudy);
  }

  private boolean hasPublisher(String languageIsoCode, CMMStudy cmmStudy) {
    Optional<Map<String, Publisher>> publisherOpt = ofNullable(cmmStudy.getPublisher());
    return publisherOpt.isPresent() && ofNullable(publisherOpt.get().get(languageIsoCode)).isPresent();
  }

  private boolean hasStudyNumber(CMMStudy cmmStudy) {
    Optional<String> studyNumberOpt = ofNullable(cmmStudy.getStudyNumber());
    return studyNumberOpt.isPresent() && !studyNumberOpt.get().isEmpty();
  }

  private boolean hasAbstract(String languageIsoCode, CMMStudy cmmStudy) {
    Optional<Map<String, String>> abstractFieldOpt = ofNullable(cmmStudy.getAbstractField());
    return abstractFieldOpt.isPresent() && ofNullable(abstractFieldOpt.get().get(languageIsoCode)).isPresent();
  }

  private boolean hasTitle(String languageIsoCode, CMMStudy cmmStudy) {
    Optional<Map<String, String>> titleStudyOpt = ofNullable(cmmStudy.getTitleStudy());
    return titleStudyOpt.isPresent() && ofNullable(titleStudyOpt.get().get(languageIsoCode)).isPresent();
  }
}