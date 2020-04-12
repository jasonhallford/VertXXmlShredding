package io.miscellanea.vertx.example;

import io.vertx.core.Vertx;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Optional;

/**
 * Produces a content handler for a specific XML document. This is a service provider interface
 * (SPI) that is implemented for each processing module.
 */
public interface ContentHandlerProviderSPI {
  /**
   * Returns this provider's name.
   *
   * @return The provider's name.
   */
  String getName();

  /**
   * Determines if this provider handles a given document type.
   *
   * @param docTypeIdentifier The document type identifier.
   * @return <code>true</code> if this module can provide a handler for the specified document type;
   *     otherwise, <code>false</code>
   */
  boolean handlesDocType(String docTypeIdentifier);

  /**
   * Provides a SAX <code>DefaultContentHandler</code> for the specified XML document type.
   *
   * @param docTypeIdentifier The document type identifier.
   * @param vertx A Vert.x instance.
   * @param jobId The id of the job associated with this handler.
   * @return A <code>ContentHandler</code> instance
   */
  Optional<DefaultHandler> provide(String docTypeIdentifier, Vertx vertx, int jobId);
}
