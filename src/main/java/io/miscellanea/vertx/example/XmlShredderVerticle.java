package io.miscellanea.vertx.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * A worker verticle that "shreds" XML documents by cooperating with one or more processor-provided
 * content handlers.
 *
 * @author Jason Hallford
 */
public class XmlShredderVerticle extends AbstractVerticle {
  // Fields
  private static final Logger LOGGER = LoggerFactory.getLogger(XmlShredderVerticle.class);

  private SAXParser parser;
  private List<ContentHandlerProviderSPI> providers = new ArrayList<>();

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
      var loader = ServiceLoader.load(ContentHandlerProviderSPI.class);

      if (loader != null) {
        for (ContentHandlerProviderSPI provider : loader) {
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

    Optional<ContentHandlerProviderSPI> provider =
        this.providers.stream().filter(p -> p.handlesDocType(docType)).findFirst();
    provider.ifPresent(
        p -> {
          var pathToFile = message.body().getString("path-to-file");
          var handler = p.provide(docType, getVertx(), jobId);

          try {
            LOGGER.info("Found provider; shredding document at '{}' as job {}.", pathToFile, jobId);
            long start = System.currentTimeMillis();
            this.parser.parse(new File(pathToFile), handler.get());
            LOGGER.info("Shredding job {} successfully completed.", jobId);
          } catch (Exception e) {
            LOGGER.error("Unable to parse document '" + pathToFile + "'.", e);
          }
        });
  }
}
