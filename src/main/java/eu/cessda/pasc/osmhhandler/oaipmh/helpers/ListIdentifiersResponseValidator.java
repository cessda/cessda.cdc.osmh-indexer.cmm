package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import eu.cessda.pasc.osmhhandler.oaipmh.models.errors.ErrorStatus;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Optional;

import static java.util.Optional.ofNullable;

public class ListIdentifiersResponseValidator {

  private static final String MISSING_ELEMENT_MISSING_OAI_ELEMENT = "MissingElement: Missing Oai element";

  private ListIdentifiersResponseValidator() {
    throw new UnsupportedOperationException("Utility class, instantiation not allow");
  }

  /**
   * Checks if the response has an <error> element.
   *
   * @param document the document to map to.
   * @return ErrorStatus of the record.
   */
  public static ErrorStatus validateResponse(Document document) {
    ErrorStatus.ErrorStatusBuilder statusBuilder = ErrorStatus.builder();
    Optional<NodeList> oAINode = ofNullable(document.getElementsByTagName(OaiPmhConstants.OAI_PMH));

    if (!oAINode.isPresent()) {
      return statusBuilder.hasError(true).message(MISSING_ELEMENT_MISSING_OAI_ELEMENT).build();
    }

    NodeList nodeList = oAINode.get();
    for (int i = 0; i < nodeList.getLength(); i++) {
      NodeList childNodes = nodeList.item(i).getChildNodes();
      for (int childNodeIndex = 0; childNodeIndex < childNodes.getLength(); childNodeIndex++) {
        Node item = childNodes.item(childNodeIndex);
        if (OaiPmhConstants.ERROR.equals(item.getLocalName())) {
          return statusBuilder.hasError(true)
              .message(item.getAttributes().getNamedItem("code").getTextContent() + ": " + item.getTextContent())
              .build();
        }
      }
    }

    return statusBuilder.build();
  }
}