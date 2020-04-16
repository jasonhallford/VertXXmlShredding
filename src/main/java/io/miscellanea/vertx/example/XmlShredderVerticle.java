package io.miscellanea.vertx.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

/**
 * A worker verticle that "shreds" XML documents by cooperating with one or more processor-provided
 * content handlers.
 *
 * @author Jason Hallford
 */
public class XmlShredderVerticle extends AbstractVerticle {
  private static class ShreddingContext {
    private XMLEventReader xmlEventReader;
    private XmlEventProcessor xmlEventProcessor;

    public ShreddingContext(XMLEventReader xmlEventReader, XmlEventProcessor xmlEventProcessor) {
      this.xmlEventReader = xmlEventReader;
      this.xmlEventProcessor = xmlEventProcessor;
    }

    public XMLEventReader getXmlEventReader() {
      return xmlEventReader;
    }

    public XmlEventProcessor getXmlEventProcessor() {
      return xmlEventProcessor;
    }
  }

  // Fields
  private static final Logger LOGGER = LoggerFactory.getLogger(XmlShredderVerticle.class);

  private List<XmlEventProcessorProviderSPI> providers = new ArrayList<>();
  private Map<Integer, ShreddingContext> contexts = new HashMap<>();

  private final String privateNextAddress = "xml.shred.next." + this.hashCode();
  private final String privateEndAddress = "xml.shred.end." + this.hashCode();
  private final String privateErrorAddress = "xml.shred.error." + this.hashCode();

  // Constructors
  public XmlShredderVerticle() {}

  // Vert.x lifecycle methods
  @Override
  public void start(Promise<Void> startPromise) {
    LOGGER.info("Starting XML Shredder verticle.");

    // Via the Java Service Loader, load all content handler providers.
    getVertx()
        .executeBlocking(
            this::loadContentHandlers,
            result -> {
              if (result.succeeded()) {
                // Registering shredding handler.
                getVertx().eventBus().consumer("xml.shred", this::shredDocument);
                LOGGER.debug("Registered interest in 'xml.shred' address.");

                // Create private address for XML event iteration.
                getVertx().eventBus().consumer(this.privateNextAddress, this::nextElement);
                getVertx().eventBus().consumer(this.privateEndAddress, this::endShredding);
                getVertx().eventBus().consumer(this.privateErrorAddress, this::handleError);
                LOGGER.debug("Registered private event bus addresses.");

                startPromise.complete();
              } else {
                startPromise.fail("Unable to initialize XML Shredder verticle.");
              }
            });
  }

  // Initialization
  private void loadContentHandlers(Promise<Object> promise) {
    LOGGER.debug("Loading content handler providers from classpath.");

    try {
      var loader = ServiceLoader.load(XmlEventProcessorProviderSPI.class);

      if (loader != null) {
        for (XmlEventProcessorProviderSPI provider : loader) {
          LOGGER.debug("Adding '{}' to provider list.", provider.getName());
          this.providers.add(provider);
        }
      } else {
        LOGGER.warn(
            "No content handler providers registered in class path; shredding is disabled.");
      }
      promise.complete();
    } catch (Exception e) {
      LOGGER.error("Unable to load content handlers; shredding is disabled.", e);
      promise.fail(e);
    }
  }

  // Vert.x handlers
  private void shredDocument(Message<JsonObject> message) {
    LOGGER.debug("Attempting to shred document.");

    // Find the first content provider that supports the specified doc type.
    var docType = message.body().getString("doc-type");
    var jobId = message.body().getInteger("job-id");

    LOGGER.debug("Looking for a content handler provider .");

    Optional<XmlEventProcessorProviderSPI> provider =
        this.providers.stream().filter(p -> p.handlesDocType(docType)).findFirst();
    provider.ifPresent(
        p -> {
          var pathToFile = message.body().getString("path-to-file");

          var processorContext =
              new XmlEventProcessorContext(
                  jobId,
                  getVertx().eventBus(),
                  this.privateNextAddress,
                  this.privateEndAddress,
                  this.privateErrorAddress);
          var processor = p.provide(processorContext);

          try {
            var factory = XMLInputFactory.newInstance();
            var xmlEventReader =
                factory.createXMLEventReader(new BufferedReader(new FileReader(pathToFile)));

            this.contexts.put(jobId, new ShreddingContext(xmlEventReader, processor.get()));
            vertx.eventBus().send(this.privateNextAddress, new JsonObject().put("job-id", jobId));
            LOGGER.info("Begin shredding for XML document '{}' (job = {})", pathToFile, jobId);
          } catch (Exception e) {
            LOGGER.error("Unable to parse document '" + pathToFile + "'.", e);
          }
        });
  }

  private void nextElement(Message<JsonObject> message) {
    var jobId = message.body().getInteger("job-id");

    LOGGER.debug("Received nextElement() for job {}.", jobId);
    var context = this.contexts.get(jobId);

    if (context.getXmlEventReader().hasNext()) {
      try {
        XMLEvent event = context.getXmlEventReader().nextEvent();
        context.getXmlEventProcessor().process(event);
      } catch (XMLStreamException e) {
        LOGGER.error("XML stream processing raised an exception.", e);

        // Notify the error handler
        this.getVertx()
            .eventBus()
            .send(
                this.privateErrorAddress,
                new JsonObject().put("job-id", jobId).put("error-message", e.getMessage()));
      }
    }
  }

  private void endShredding(Message<JsonObject> message) {
    var jobId = message.body().getInteger("job-id");

    LOGGER.info("Shredding completed for job {}. Cleaning up context.", jobId);
    this.cleanupContext(jobId);
  }

  private void handleError(Message<JsonObject> message) {
    var jobId = message.body().getInteger("job-id");
    var error = message.body().getString("error-message");

    LOGGER.warn("Shredding for job {} terminated with an error: {}", jobId,error);
    this.cleanupContext(jobId);
  }

  private void cleanupContext(Integer jobId){
    var context = this.contexts.get(jobId);
    try {
      context.getXmlEventReader().close();
    } catch (XMLStreamException e) {
      LOGGER.error(
              "Received an exception closing the XML stream reader for job "
                      + jobId
                      + "; cleanup will continue.",
              e);
    }

    this.contexts.remove(jobId);
  }
}
