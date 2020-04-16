package io.miscellanea.vertx.example;

import io.vertx.core.eventbus.EventBus;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides execution context to XML processors.
 *
 * @author Jason Hallford
 */
public class XmlEventProcessorContext {
  // Fields
  private Integer id;
  private EventBus eventBus;
  private String nextAddress;
  private String errorAddress;
  private String finishedAddress;
  private Map<String, String> properties = new HashMap<>();

  // Constructors
  public XmlEventProcessorContext(
      Integer id,
      EventBus eventBus,
      String nextAddress,
      String finishedAddress,
      String errorAddress) {
    assert id != null : "id must not be null.";
    this.id = id;

    assert eventBus != null : "eventBus must not be null";
    this.eventBus = eventBus;

    assert nextAddress != null && !nextAddress.isBlank() : "nextAddress must have a value.";
    this.nextAddress = nextAddress;

    assert finishedAddress != null && !finishedAddress.isBlank()
        : "finshedAddress must have a value.";
    this.finishedAddress = finishedAddress;

    assert errorAddress != null && !errorAddress.isBlank() : "errorAddress must have a value.";
    this.errorAddress = errorAddress;
  }

  // Properties
  public Integer getId() {
    return id;
  }

  public EventBus getEventBus() {
    return eventBus;
  }

  public String getNextAddress() {
    return nextAddress;
  }

  public String getErrorAddress() {
    return errorAddress;
  }

  public String getFinishedAddress() {
    return finishedAddress;
  }

  // Methods
  public void putProperty(String name, String value) {
    this.properties.put(name, value);
  }

  public String getProperty(String name) {
    return this.properties.get(name);
  }
}
