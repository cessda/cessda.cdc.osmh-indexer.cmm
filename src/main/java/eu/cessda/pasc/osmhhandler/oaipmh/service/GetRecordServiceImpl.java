package eu.cessda.pasc.osmhhandler.oaipmh.service;

import eu.cessda.pasc.osmhhandler.oaipmh.configuration.PaSCHandlerOaiPmhConfig;
import eu.cessda.pasc.osmhhandler.oaipmh.dao.GetRecordDoa;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.CustomHandlerException;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.ExternalSystemException;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.InternalSystemException;
import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.osmhhandler.oaipmh.models.configuration.OaiPmh;
import eu.cessda.pasc.osmhhandler.oaipmh.models.errors.ErrorStatus;
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
 * Service Class responsible for querying the repository to fetch remote records.
 *
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
  public CMMStudy getRecord(String repository, String studyId) throws CustomHandlerException {
    CMMStudy.CMMStudyBuilder builder = CMMStudy.builder();
    log.debug("Querying Repo [{}] for StudyID [{}]", repository, studyId);
    String recordXML = getRecordDoa.getRecordXML(repository, studyId);

    try {
      mapDDIRecordToCMMStudy(recordXML, builder);
    } catch (JDOMException | IOException e) {
      throw new InternalSystemException("Unable to parse xml :" + e.getMessage());
    }
    return builder.build();
  }

  private void mapDDIRecordToCMMStudy(String recordXML, CMMStudy.CMMStudyBuilder builder)
      throws JDOMException, IOException, ExternalSystemException {

    OaiPmh oaiPmh = handlerConfig.getOaiPmh();
    InputStream recordXMLStream = IOUtils.toInputStream(recordXML, Charsets.UTF_8);
    SAXBuilder saxBuilder = new SAXBuilder();
    Document document = saxBuilder.build(recordXMLStream);

    if (log.isTraceEnabled()) {
      log.trace("Record XML String [{}]", document.toString());
    }

    // We exit if the record has an <error> element
    ErrorStatus errorStatus = validateRecord(document, X_FACTORY);
    if (errorStatus.isHasError()) {
      log.debug("Returned Record has error message [{}] ", errorStatus.getMessage());
      throw new ExternalSystemException(errorStatus.getMessage());
    }

    // Short-Circuit. We carry on to parse beyond the headers only if record is active
    boolean isActiveRecord = parseHeaderElement(builder, document, X_FACTORY);
    if (isActiveRecord) {
      parseStudyTitle(builder, document, X_FACTORY, oaiPmh);
      parseAbstract(builder, document, X_FACTORY, oaiPmh);
      parsePidStudies(builder, document, X_FACTORY, oaiPmh);
      parseCreator(builder, document, X_FACTORY, oaiPmh);
      parseAccessClass(builder, document, X_FACTORY);
      parseDataAccess(builder, document, X_FACTORY, oaiPmh);
      parseClassifications(builder, document, X_FACTORY, oaiPmh);
      parseKeywords(builder, document, X_FACTORY, oaiPmh);
      parseTypeOfTimeMethod(builder, document, X_FACTORY, oaiPmh);
      parseStudyAreaCountries(builder, document, X_FACTORY, oaiPmh);
      parseUnitTypes(builder, document, X_FACTORY, oaiPmh);
      parsePublisher(builder, document, X_FACTORY, oaiPmh);
      parseYrOfPublication(builder, document, X_FACTORY);
      parseFileLanguages(builder, document, X_FACTORY);
      parseTypeOfSamplingProcedure(builder, document, X_FACTORY, oaiPmh);
      parseSamplingProcedureFreeTexts(builder, document, X_FACTORY, oaiPmh);
      parseTypeOfModeOfCollection(builder, document, X_FACTORY, oaiPmh);
      parseDataCollectionDates(builder, document, X_FACTORY);
      parseDataCollectionFreeTexts(builder, document, X_FACTORY, oaiPmh);
      log.debug("Successfully parsed record with no known errors");
    }
  }
}
