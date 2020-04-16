package io.miscellanea.vertx.example;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Implementation of the provider SPI that produces a content handler that counts
 * tags by name.
 *
 * @author Jason Hallford
 */
public class TagNameXmlEventProcessorProviderSPI implements XmlEventProcessorProviderSPI {
  // Fields
  private static final Logger LOGGER =
      LoggerFactory.getLogger(TagNameXmlEventProcessorProviderSPI.class);
  private static final String TAG_NAME_CONTENT_HANDLER_PROVIDER =
      "Tag Name Content Handler Provider";

  // Constructors
  public TagNameXmlEventProcessorProviderSPI() {}

  // XmlEventProcessorProviderSPI
  @Override
  public String getName() {
    return TAG_NAME_CONTENT_HANDLER_PROVIDER;
  }

  @Override
  public boolean handlesDocType(String docTypeIdentifier) {
    return true;
  }

  @Override
  public Optional<XmlEventProcessor> provide(String docTypeIdentifier, Vertx vertx, int jobId) {
    LOGGER.debug("Creating new content handler for job {}.", jobId);
    return Optional.ofNullable(new TagNameXmlEventProcessor(vertx, jobId));
  }
}
