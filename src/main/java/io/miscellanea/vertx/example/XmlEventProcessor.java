package io.miscellanea.vertx.example;

import javax.xml.stream.events.XMLEvent;

/**
 * Implemented by classes that process XML events generated by shredder verticles.
 *
 * @author Jason Hallford
 */
public interface XmlEventProcessor {
    XmlEventProcessorResult process(XMLEvent xmlEvent);
}