package io.miscellanea.vertx.example;

import io.vertx.core.eventbus.MessageProducer;
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

  private XmlEventProcessorContext context;
  private JsonObject jobIdMessage;
  private MessageProducer<JsonObject> beginElementProducer;
  private String endDocumentAddress;

  // Constructor
  public TagNameXmlEventProcessor(XmlEventProcessorContext context) {
    assert context != null : "context must not be null";
    this.context = context;

    this.jobIdMessage = new JsonObject().put("job-id", this.context.getId());
  }

  @Override
  public void process(XMLEvent xmlEvent) {
    assert xmlEvent != null : "xmlEvent must not be null.";

    boolean processNextEvent = true;

    switch (xmlEvent.getEventType()) {
      case XMLEvent.START_DOCUMENT:
        LOGGER.debug("Received START_DOCUMENT event.");
        this.context
            .getEventBus()
            .request(
                "processor.tag-name.begin",
                jobIdMessage,
                reply -> {
                  if (reply.succeeded()) {
                    LOGGER.debug(
                        "Received reply from tag name processor verticle; recording private addresses.");
                    JsonObject body = (JsonObject) reply.result().body();
                    var beginElementAddress = body.getString("begin-element-address");
                    this.beginElementProducer =
                        this.context.getEventBus().sender(beginElementAddress);

                    this.endDocumentAddress = body.getString("end-address");

                    // Process the next event.
                    this.context.getEventBus().send(this.context.getNextAddress(), jobIdMessage);
                  } else {
                    // We've got an error; terminate processing.
                    this.context
                        .getEventBus()
                        .send(
                            this.context.getErrorAddress(),
                            new JsonObject()
                                .put("job-id", this.context.getId())
                                .put(
                                    "error-message",
                                    "Processor verticle rejected attempt to begin processing."));
                  }
                });

        // Don't advance to the next element; this will be done when we receive a reply
        processNextEvent = false;
        break;
      case XMLEvent.START_ELEMENT:
        LOGGER.debug("Received START_ELEMENT event.");
        var element = xmlEvent.asStartElement();

        if (!this.beginElementProducer.writeQueueFull()) {
          this.beginElementProducer.write(
              new JsonObject()
                  .put("job-id", this.context.getId())
                  .put("element-name", element.getName().getLocalPart()));
        } else {
          LOGGER.debug("Write queue is full; installing drain handler.");
          this.beginElementProducer.drainHandler(
              handler -> {
                LOGGER.debug("Drain handler executing");

                this.beginElementProducer.write(
                    new JsonObject()
                        .put("job-id", this.context.getId())
                        .put("element-name", element.getName().getLocalPart()));

                // Restart normal processing
                this.context.getEventBus().send(this.context.getNextAddress(), jobIdMessage);
              });
        }
        break;
      case XMLEvent.END_DOCUMENT:
        LOGGER.debug("Received END_DOCUMENT event.");
        this.context
            .getEventBus()
            .send(this.endDocumentAddress, new JsonObject().put("job-id", this.context.getId()));

        // Send the end event
        this.context.getEventBus().send(this.context.getFinishedAddress(), jobIdMessage);
        processNextEvent = false;
        break;
    }

    if (processNextEvent) {
      this.context.getEventBus().send(this.context.getNextAddress(), jobIdMessage);
    }
  }
}
