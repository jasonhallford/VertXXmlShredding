package io.miscellanea.vertx.example;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.StringReader;

/**
 * Implements a SAX content handler that fires an event for each tag
 * encountered during parsing.
 *
 * @author Jason Hallford
 */
public class TagNameContentHandler extends DefaultHandler {
  // Fields
  private static final Logger LOGGER = LoggerFactory.getLogger(TagNameContentHandler.class);

  private Vertx vertx;
  private int jobId;

  // Constructor
  public TagNameContentHandler(Vertx vertx, int jobId) {
    assert vertx != null : "vertx must not be null";
    this.vertx = vertx;
    this.jobId = jobId;
  }

  // Handler methods
  @Override
  public InputSource resolveEntity(String publicId, String systemId) {
    InputSource source = null;

    LOGGER.debug("Attempting to resolve entity '{}:{}'.", publicId, systemId);
    if (systemId.endsWith(".dtd")) {
      source = new InputSource(new StringReader(""));
    }
    return source;
  }

  @Override
  public void startDocument() throws SAXException {
    LOGGER.debug("Document started.");

    var message = new JsonObject().put("job-id", this.jobId);
    this.vertx.eventBus().send("processor.tag-name.begin", message);
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    LOGGER.debug("Called start element.");

    var message = new JsonObject().put("job-id", this.jobId).put("element-name", qName);
    this.vertx.eventBus().send("processor.tag-name.begin-element", message);
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    LOGGER.debug("Called end element.");
  }

  @Override
  public void characters(char ch[], int start, int length) throws SAXException {
    LOGGER.debug("Called characters.");
  }

  @Override
  public void endDocument() {
    LOGGER.debug("Called end document.");

    var message = new JsonObject().put("job-id", this.jobId);
    this.vertx.eventBus().send("processor.tag-name.end", message);
  }
}
