package io.miscellanea.vertx.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
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

  private SAXParser parser;
  private final String privateAddress = "xml.shred." + this.hashCode();
  private List<XmlEventProcessorProviderSPI> providers = new ArrayList<>();
  private Map<Integer, ShreddingContext> contexts = new HashMap<>();

  // Constructors
  public XmlShredderVerticle() {}

  // Vert.x lifecycle methods
  @Override
  public void start(Promise<Void> startPromise) {
    LOGGER.info("Starting XML Shredder verticle.");

    // Via the Java Service Loader, load all content handler providers.
    getVertx()
        .executeBlocking(
            this::initializeVerticle,
            result -> {
              if (result.succeeded()) {
                // Registering shredding handler.
                getVertx().eventBus().consumer("xml.shred", this::shredDocument);
                LOGGER.debug("Registered interest in 'xml.shred' address.");

                // Create private address for XML event iteration.
                getVertx().eventBus().consumer(this.privateAddress, this::nextElement);
                LOGGER.debug("Registered private event bus address.");
                startPromise.complete();
              } else {
                startPromise.fail("Unable to initialize XML Shredder verticle.");
              }
            });
  }

  // Initialization
  private void initializeVerticle(Promise<Object> promise) {
    if (this.createXmlParser()) {
      this.loadContentHandlers();
      LOGGER.debug("Content handlers successfully initialized.");
      promise.complete();
    } else {
      promise.fail("Unable to create SAX parser.");
    }
  }

  private boolean createXmlParser() {
    boolean created = false;

    LOGGER.debug("Creating SAX parser.");
    try {
      var factory = SAXParserFactory.newInstance();
      factory.setNamespaceAware(false);
      factory.setValidating(false);
      this.parser = factory.newSAXParser();

      LOGGER.debug("Parser successfully created.");
      created = true;
    } catch (Exception e) {
      LOGGER.error("Unable to create SAX parser; shredding is disabled.", e);
    }

    return created;
  }

  private void loadContentHandlers() {
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
    } catch (Exception e) {
      LOGGER.error("Unable to load content handlers; shredding is disabled.", e);
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
          var processor = p.provide(docType, getVertx(), jobId);

          try {
            var factory = XMLInputFactory.newInstance();
            var xmlEventReader =
                factory.createXMLEventReader(new BufferedReader(new FileReader(pathToFile)));

            this.contexts.put(jobId, new ShreddingContext(xmlEventReader, processor.get()));
            vertx.eventBus().send(this.privateAddress, new JsonObject().put("job-id", jobId));
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
        var event = context.getXmlEventReader().nextEvent();
        var result = context.getXmlEventProcessor().process(event);

        switch (result) {
          case CONTINUE:
            vertx.eventBus().send(this.privateAddress, new JsonObject().put("job-id", jobId));
            break;
          case HALT:
          case COMPLETED:
            LOGGER.info("Shredding completed ({}).", result);
            context.getXmlEventReader().close();
            this.contexts.remove(jobId);
            break;
        }
      } catch (Exception e) {
        LOGGER.error("Received a non-recoverable error while shredding XML document!", e);
      }
    }
  }
}
