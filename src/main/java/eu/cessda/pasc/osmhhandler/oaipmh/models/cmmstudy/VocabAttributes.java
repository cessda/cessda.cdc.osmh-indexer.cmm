package eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy;

import lombok.Builder;
import lombok.Getter;

/**
 * VocabAttributes pojo to hold the control vocab attributes of element:
 * <p>
 * {@code
 * <keyword xml:lang="sv" ID="w8357xx45" vocab="ELSST" vocabURI="http://sv.ac.sv">
 * </keyword>
 * }
 *
 * @author moses@doraventures.com
 */
@Builder
@Getter
public class VocabAttributes {
  private String vocab;
  private String vocabUri;
  private String id;
}
