package io.miscellanea.vertx.example;

/**
 * Raised by XML event processors when they encounter a non-recoverable error.
 *
 * @author Jason Hallford
 */
public class XmlEventProcessorException extends RuntimeException{
    // Constructors
    public XmlEventProcessorException(String message){
        super(message);
    }

    public XmlEventProcessorException(String message, Throwable cause){
        super(message,cause);
    }
}
