package io.miscellanea.vertx.example;

import java.util.Optional;

/**
 * Produces a content handler for a specific XML document. This is a service provider interface
 * (SPI) that is implemented for each processing module.
 */
public interface XmlEventProcessorProviderSPI {
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
   * @param context The XML processor's execution context.
   * @return A <code>ContentHandler</code> instance
   */
  Optional<XmlEventProcessor> provide(XmlEventProcessorContext context);
}
