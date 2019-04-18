package eu.cessda.pasc.oci.models.cmmstudy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class VocabAttributes {
  private String vocab;
  private String vocabUri;
  private String id;
}
