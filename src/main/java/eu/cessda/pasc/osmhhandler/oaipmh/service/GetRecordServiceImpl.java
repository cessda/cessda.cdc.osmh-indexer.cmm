package eu.cessda.pasc.osmhhandler.oaipmh.service;

import eu.cessda.pasc.osmhhandler.oaipmh.configuration.PaSCHandlerOaiPmhConfig;
import eu.cessda.pasc.osmhhandler.oaipmh.dao.GetRecordDoa;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.InternalSystemException;
import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.osmhhandler.oaipmh.models.configuration.OaiPmh;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.CMMStudyMapper.*;

/**
 * @author moses@doraventures.com
 */
@Service
@Slf4j
public class GetRecordServiceImpl implements GetRecordService {

  @Autowired
  GetRecordDoa getRecordDoa;

  @Autowired
  PaSCHandlerOaiPmhConfig handlerConfig;

  private static final XPathFactory X_FACTORY = XPathFactory.instance();

  @Override
  public CMMStudy getRecord(String repository, String studyId) throws InternalSystemException {
    CMMStudy.CMMStudyBuilder builder = CMMStudy.builder();
    String recordXML = getRecordDoa.getRecordXML(repository, studyId);

    try {
      mapDDIRecordToCMMStudy(recordXML, builder);
    } catch (JDOMException | IOException e) {
      throw new InternalSystemException("Unable to parse xml :" + e.getMessage());
    }
    return builder.build();
  }

  private void mapDDIRecordToCMMStudy(String recordXML, CMMStudy.CMMStudyBuilder builder)
      throws JDOMException, IOException {

    OaiPmh oaiPmh = handlerConfig.getOaiPmh();
    InputStream recordXMLStream = IOUtils.toInputStream(recordXML, Charsets.UTF_8);
    SAXBuilder saxBuilder = new SAXBuilder();
    Document document = saxBuilder.build(recordXMLStream);

    // TODO:  Does it have an <Error> extract message and throw custom ExternalSystemError with message
    // use test payload "exampleXMLWithError()" to test this scenario
    // validateForError((builder, document, X_FACTORY);

    parseHeaderElement(builder, document, X_FACTORY);
    parseStudyTitle(builder, document, X_FACTORY, oaiPmh);
    parseAbstract(builder, document, X_FACTORY, oaiPmh);
    parseYrOfPublication(builder, document, X_FACTORY, oaiPmh);
    parsePidStudies(builder, document, X_FACTORY);
    parseCreator(builder, document, X_FACTORY);
    parseAccessClass(builder, document, X_FACTORY);
    parseDataAccess(builder, document, X_FACTORY, oaiPmh);
    parseDataCollectionPeriods(builder, document, X_FACTORY);
    parseInstitutionFullName(builder, document, X_FACTORY, oaiPmh);
    parseClassifications(builder, document, X_FACTORY);
    parseKeywords(builder, document, X_FACTORY);
    parseTypeOfTimeMethod(builder, document, X_FACTORY);
    parseStudyAreaCountries(builder, document, X_FACTORY);
    parseUnitTypes(builder, document, X_FACTORY);
    parsePublisher(builder, document, X_FACTORY);
    parseFileLanguages(builder, document, X_FACTORY);
    parseTypeOfSamplingProcedure(builder, document, X_FACTORY);
    parseSamplingProcedure(builder, document, X_FACTORY, oaiPmh);
    parseTypeOfModeOfCollection(builder, document, X_FACTORY);
  }
}
