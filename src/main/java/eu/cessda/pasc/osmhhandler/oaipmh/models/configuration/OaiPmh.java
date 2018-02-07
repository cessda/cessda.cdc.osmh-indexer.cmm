package eu.cessda.pasc.osmhhandler.oaipmh.models.configuration;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * OaiPmh configuration model
 *
 * @author moses@doraventures.com
 */
@Getter
@Setter
public class OaiPmh {

  private List<String> supportedApiVersions;
  private List<String> supportedRecordTypes;
  private List<Repo> repos;
  private Integer publicationYearDefault;
  private MetadataParsingDefaultLang metadataParsingDefaultLang;
  private boolean concatRepeatedElements;
  private String concatSeparator;
}
