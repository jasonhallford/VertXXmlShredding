package io.miscellanea.vertx.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * A worker verticle that counts the unique tag names XML document by working in tandem with <code>
 * TagNameXmlEventProcessor</code>.
 *
 * @author Jason Hallford
 */
public class TagNameProcessorVerticle extends AbstractVerticle {
  // Fields
  private static final Logger LOGGER = LoggerFactory.getLogger(TagNameProcessorVerticle.class);

  private Map<Integer, TagStats> stats = new HashMap<>();
  private MessageConsumer<JsonObject> consumer;

  // Constructors
  public TagNameProcessorVerticle() {}

  // Vert.x lifecycle methods
  @Override
  public void start() {
    // Register handlers
    getVertx().eventBus().consumer("processor.tag-name.begin", this::beginJob);
    getVertx().eventBus().consumer("processor.tag-name.end", this::endJob);
    getVertx().eventBus().consumer("processor.tag-name.begin-element", this::beginElement);

    LOGGER.info("Tag name processing verticle started.");
  }

  // Vert.x handlers
  private void beginJob(Message<JsonObject> message) {
    var payload = message.body();

    int jobId = payload.getInteger("job-id");
    if (!stats.containsKey(jobId)) {
      stats.put(jobId, new TagStats());
    }

    var jobStats = this.stats.get(jobId);
    jobStats.startMs = System.currentTimeMillis();
    LOGGER.info("Job {} started.", jobId);
  }

  private void beginElement(Message<JsonObject> message) {
    var payload = message.body();
    int jobId = payload.getInteger("job-id");
    String elementName = payload.getString("element-name");

    var jobStats = this.stats.get(jobId);
    if (jobStats != null) {
      if (!jobStats.counters.containsKey(elementName)) {
        LOGGER.debug("Creating new counter for element {}.", elementName);
        jobStats.counters.put(elementName, new Counter());
      }

      LOGGER.debug("Incrementing counter for element {}.", elementName);
      jobStats.counters.get(elementName).increment();
    } else {
      LOGGER.warn("Received begin element for unknown job {}.", jobId);
    }
  }

  private void endJob(Message<JsonObject> message) {
    var payload = message.body();
    int jobId = payload.getInteger("job-id");

    var jobStats = this.stats.get(jobId);
    if (jobStats != null) {
      jobStats.endMs = System.currentTimeMillis();
      LOGGER.info("Job {} finished. Statistics: {}", jobId, jobStats.toString());
    } else {
      LOGGER.warn("Received end event for unknown job {}", jobId);
    }
  }
}
