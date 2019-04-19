package eu.cessda.pasc.oci.models.cmmstudy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * TermVocabAttributes pojo this represents the attributes and terms from such a element:
 * <p>
 * {@code
 * <keyword xml:lang="sv" ID="w8357xx45" vocab="ELSST" vocabURI="http://sv.ac.sv">
 * Nationellt val
 * </keyword>
 * }
 *
 * @author moses@doraventures.com
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TermVocabAttributes {
  private String vocab;
  private String vocabUri;
  private String id;
  private String term;
}
