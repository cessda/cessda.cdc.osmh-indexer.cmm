/*
# Copyright CESSDA ERIC 2017-2019
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License.
# You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
*/
package eu.cessda.pasc.osmhhandler.oaipmh.mock.data;


import eu.cessda.pasc.osmhhandler.oaipmh.models.response.RecordHeader;

import java.util.ArrayList;
import java.util.List;

/**
 * mock data for Record headers.
 *
 * @author moses AT doraventures DOT com
 */
public class RecordHeadersMock {

  public static List<RecordHeader> getRecordHeaders() {

    List<RecordHeader> recordHeaders = new ArrayList<>();

    recordHeaders.add(
        RecordHeader.builder()
            .lastModified("2016-11-30T23:59:59Z")
            .type("Question")
            .recordType("RecordHeader")
            .identifier("ESS1e06.5_Q2")
            .build());

    recordHeaders.add(
        RecordHeader.builder()
            .lastModified("2015-01-22T23:59:59Z")
            .type("Study")
            .recordType("RecordHeader")
            .identifier("MD1998")
            .build());

    recordHeaders.add(
        RecordHeader.builder()
            .lastModified("2015-01-22T23:59:59Z")
            .type("Question")
            .recordType("RecordHeader")
            .identifier("MD1998_Q119")
            .build());

    recordHeaders.add(
        RecordHeader.builder()
            .lastModified("2012-11-30T23:59:59Z")
            .type("Study")
            .recordType("RecordHeader")
            .identifier("MD1999")
            .build());

    recordHeaders.add(
        RecordHeader.builder()
            .lastModified("2012-11-30T23:59:59Z")
            .type("Variable")
            .recordType("RecordHeader")
            .identifier("MD1999_V1")
            .build());

    return recordHeaders;
  }

  public static String getListIdentifiersXMLResumptionEmpty() {
    return
        "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
            "<?xml-stylesheet type='text/xsl' href='oai2.xsl' ?>\n" +
            "<OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\"\n" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            " xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/\n" +
            " http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\">\n" +
            "    <responseDate>2018-01-10T16:31:14Z</responseDate>\n" +
            "    <request verb=\"ListIdentifiers\" metadataPrefix=\"ddi\" from=\"2016-06-01\">" +
            "https://oai.ukdataservice.ac.uk:8443/oai/provider</request>\n" +
            "    <ListIdentifiers>\n" +
            "        <header>\n" +
            "            <identifier>850229</identifier>\n" +
            "            <datestamp>2017-11-20T10:37:18Z</datestamp>\n" +
            "            <setSpec>DataCollections</setSpec>\n" +
            "        </header>\n" +
            "        <header>\n" +
            "            <identifier>850232</identifier>\n" +
            "            <datestamp>2017-11-20T10:37:18Z</datestamp>\n" +
            "            <setSpec>DataCollections</setSpec>\n" +
            "        </header>\n" +
            "        <header>\n" +
            "            <identifier>850235</identifier>\n" +
            "            <datestamp>2017-11-20T10:37:18Z</datestamp>\n" +
            "            <setSpec>DataCollections</setSpec>\n" +
            "        </header>\n" +
            "        <resumptionToken completeListSize=\"3\" cursor=\"0\"></resumptionToken>\n" +
            "    </ListIdentifiers>\n" +
            "</OAI-PMH>";
  }

  public static String getListIdentifiersXMLResumptionTokenNotMockedForInvalid() {
    return
        "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
            "<?xml-stylesheet type='text/xsl' href='oai2.xsl' ?>\n" +
            "<OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\"\n" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            " xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/\n" +
            " http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\">\n" +
            "    <responseDate>2018-01-10T16:31:14Z</responseDate>\n" +
            "    <request verb=\"ListIdentifiers\" metadataPrefix=\"ddi\" from=\"2016-06-01\">" +
            "https://oai.ukdataservice.ac.uk:8443/oai/provider</request>\n" +
            "    <ListIdentifiers>\n" +
            "        <header>\n" +
            "            <identifier>850235</identifier>\n" +
            "            <datestamp>2017-11-20T10:37:18Z</datestamp>\n" +
            "            <setSpec>DataCollections</setSpec>\n" +
            "        </header>\n" +
            "        <resumptionToken completeListSize=\"3\" cursor=\"0\">3/6/7/ddi/null/2017-01-01/null</resumptionToken>\n" +
            "    </ListIdentifiers>\n" +
            "</OAI-PMH>";
  }

  public static String getListIdentifiersXML() {
    return
        "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
            "<?xml-stylesheet type='text/xsl' href='oai2.xsl' ?>\n" +
            "<OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\"\n" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            " xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/\n" +
            " http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\">\n" +
            "    <responseDate>2018-01-10T16:31:14Z</responseDate>\n" +
            "    <request verb=\"ListIdentifiers\" metadataPrefix=\"ddi\" from=\"2016-06-01\">" +
            "https://oai.ukdataservice.ac.uk:8443/oai/provider</request>\n" +
            "    <ListIdentifiers>\n" +
            "        <header>\n" +
            "            <identifier>850229</identifier>\n" +
            "            <datestamp>2017-11-20T10:37:18Z</datestamp>\n" +
            "            <setSpec>DataCollections</setSpec>\n" +
            "        </header>\n" +
            "        <header>\n" +
            "            <identifier>850232</identifier>\n" +
            "            <datestamp>2017-11-20T10:37:18Z</datestamp>\n" +
            "            <setSpec>DataCollections</setSpec>\n" +
            "        </header>\n" +
            "        <header>\n" +
            "            <identifier>850235</identifier>\n" +
            "            <datestamp>2017-11-20T10:37:18Z</datestamp>\n" +
            "            <setSpec>DataCollections</setSpec>\n" +
            "        </header>\n" +
            "        <resumptionToken completeListSize=\"7\" cursor=\"0\">0/3/7/ddi/null/2016-06-01/null" +
            "</resumptionToken>\n" +
            "    </ListIdentifiers>\n" +
            "</OAI-PMH>";
  }

  public static String getListIdentifiersXMLWithResumption() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
        "<?xml-stylesheet type='text/xsl' href='oai2.xsl' ?>\n" +
        "<OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\"\n" +
        " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
        " xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/\n" +
        " http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\">\n" +
        "    <responseDate>2018-01-12T10:11:30Z</responseDate>\n" +
        "    <request verb=\"ListIdentifiers\" resumptionToken=\"6500/7000/7606/ddi/null/2017-01-01/null\">" +
        "https://oai.ukdataservice.ac.uk:8443/oai/provider</request>\n" +
        "    <ListIdentifiers>\n" +
        "        <header>\n" +
        "            <identifier>7753</identifier>\n" +
        "            <datestamp>2018-01-11T07:43:20Z</datestamp>\n" +
        "            <setSpec>DataCollections</setSpec>\n" +
        "        </header>\n" +
        "        <header>\n" +
        "            <identifier>8300</identifier>\n" +
        "            <datestamp>2018-01-11T07:43:20Z</datestamp>\n" +
        "            <setSpec>DataCollections</setSpec>\n" +
        "        </header>\n" +
        "        <header>\n" +
        "            <identifier>8301</identifier>\n" +
        "            <datestamp>2018-01-11T07:43:20Z</datestamp>\n" +
        "            <setSpec>DataCollections</setSpec>\n" +
        "        </header>\n" +
        "        <resumptionToken completeListSize=\"7\" cursor=\"3\">" +
        "3/6/7/ddi/null/2017-01-01/null</resumptionToken>\n" +
        "    </ListIdentifiers>\n" +
        "</OAI-PMH>";
  }

  public static String getListIdentifiersXMLWithResumptionLastList() {
    return
        "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
            "<?xml-stylesheet type='text/xsl' href='oai2.xsl' ?>\n" +
            "<OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\"\n" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            " xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/\n" +
            " http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\">\n" +
            "    <responseDate>2018-01-12T10:05:01Z</responseDate>\n" +
            "    <request verb=\"ListIdentifiers\" resumptionToken=\"7000/7500/7606/ddi/null/2017-01-01/null\">" +
            "https://oai.ukdataservice.ac.uk:8443/oai/provider</request>\n" +
            "    <ListIdentifiers>  \n" +
            "        <header>\n" +
            "            <identifier>998</identifier>\n" +
            "            <datestamp>2018-01-11T07:43:39Z</datestamp>\n" +
            "            <setSpec>DataCollections</setSpec>\n" +
            "        </header>\n" +
            "        <resumptionToken completeListSize=\"7\" cursor=\"6\"/>\n" +
            "    </ListIdentifiers>\n" +
            "</OAI-PMH>";
  }

  public static String getListIdentifiersXMLWithInvalidMetadataTokenError(){
    return
        "<OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\">\n" +
            "  <responseDate>2018-03-01T14:29:31Z</responseDate>\n" +
            "  <request>http://services.fsd.uta.fi/v0/oai</request>\n" +
            "  <error code=\"cannotDisseminateFormat\">Metadata format not supported by this repository</error>\n" +
            "</OAI-PMH>";
  }
}