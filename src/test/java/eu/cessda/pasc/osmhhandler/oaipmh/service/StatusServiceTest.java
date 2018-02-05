package eu.cessda.pasc.osmhhandler.oaipmh.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;

/**
 * @author moses@doraventures.com
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class StatusServiceTest {

  @Mock
  private ObjectMapper objectMapperMock = mock(ObjectMapper.class);

  @InjectMocks
  @Autowired
  StatusService statusService;

  @Test
  public void shouldPrintOutConfiguration() throws JsonProcessingException {

    //given
    ObjectWriter prettyWriter = mock(ObjectWriter.class);
    given(objectMapperMock.writerWithDefaultPrettyPrinter()).willReturn(prettyWriter);
    given(prettyWriter.writeValueAsString(anyObject())).willReturn("{\"test\":\"value\"}");

    // when
    String outPut = statusService.printPaSCHandlerOaiPmhConfig();
    InOrder verifier = inOrder(prettyWriter);

    //then
    verifier.verify(prettyWriter, calls(2)).writeValueAsString(anyObject());
    then(outPut).isEqualTo("See logs.");
  }
}