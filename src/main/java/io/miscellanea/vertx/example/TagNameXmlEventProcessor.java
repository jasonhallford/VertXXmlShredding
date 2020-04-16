package io.miscellanea.vertx.example;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.events.XMLEvent;

/**
 * Implements a SAX content handler that fires an event for each tag encountered during parsing.
 *
 * @author Jason Hallford
 */
public class TagNameXmlEventProcessor implements XmlEventProcessor {
  // Fields
  private static final Logger LOGGER = LoggerFactory.getLogger(TagNameXmlEventProcessor.class);

  private Vertx vertx;
  private int jobId;

  // Constructor
  public TagNameXmlEventProcessor(Vertx vertx, int jobId) {
    assert vertx != null : "vertx must not be null";
    this.vertx = vertx;
    this.jobId = jobId;
  }

  @Override
  public XmlEventProcessorResult process(XMLEvent xmlEvent) {
    XmlEventProcessorResult result = XmlEventProcessorResult.CONTINUE;

    switch (xmlEvent.getEventType()) {
      case XMLEvent.START_DOCUMENT:
        LOGGER.debug("Received START_DOCUMENT event.");
        vertx
            .eventBus()
            .send("processor.tag-name.begin", new JsonObject().put("job-id", this.jobId));
        break;
      case XMLEvent.START_ELEMENT:
        LOGGER.debug("Received START_ELEMENT event.");
        var element = xmlEvent.asStartElement();
        vertx
            .eventBus()
            .send(
                "processor.tag-name.begin-element",
                new JsonObject()
                    .put("job-id", jobId)
                    .put("element-name", element.getName().getLocalPart()));
        break;
      case XMLEvent.END_DOCUMENT:
        LOGGER.debug("Received END_DOCUMENT event.");
        vertx.eventBus().send("processor.tag-name.end", new JsonObject().put("job-id", this.jobId));
        result = XmlEventProcessorResult.COMPLETED;
        break;
    }

    return result;
  }
}
