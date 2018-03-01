package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import eu.cessda.pasc.osmhhandler.oaipmh.models.errors.ErrorStatus;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.xpath.XPathFactory;

public class RecordResponseValidator {

  /**
   * Checks if the record has an <error> element.
   *
   * @param document the document to map to.
   * @param xFactory the Path Factory.
   * @return ErrorStatus of the record.
   */
  public static ErrorStatus validateResponse(Document document, XPathFactory xFactory) {

    ErrorStatus.ErrorStatusBuilder statusBuilder = ErrorStatus.builder();
    DocElementParser.getFirstElement(document, xFactory, OaiPmhConstants.ERROR_PATH)
        .ifPresent((Element element) ->
            statusBuilder.hasError(true).message(element.getAttributeValue(OaiPmhConstants.CODE_ATTR) + ": " + element.getText())
        );

    return statusBuilder.build();
  }
}