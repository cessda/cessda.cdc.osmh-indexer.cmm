package eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy;

import lombok.Builder;
import lombok.Getter;

/**
 * Topic classification pojo
 *
 * @author moses@doraventures.com
 */
@Builder
@Getter
public class TermVocabAttributes {

  private String vocab;
  private String vocabUri;
  private String id;
  private String term;
}
