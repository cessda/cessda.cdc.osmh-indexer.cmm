package eu.cessda.pasc.osmhhandler.oaipmh.models.configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * Defaults for parsing metadata fields with no xml:lang specified,
 * where lang is extracted content is to be mapped against a lang
 *
 * @author moses@doraventures.com
 */
@Getter
@Setter
public class MetadataParsingDefaultLang {

  private boolean active;
  private String lang;
}
