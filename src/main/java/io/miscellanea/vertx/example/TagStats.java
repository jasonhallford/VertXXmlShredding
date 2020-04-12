package io.miscellanea.vertx.example;

import java.util.HashMap;
import java.util.Map;

public class TagStats {
  // Fields
  public Map<String, Counter> counters = new HashMap<>();
  public long startMs;
  public long endMs;

  // Constructors
  public TagStats() {}

  // Methods
  @Override
  public String toString() {
    var builder = new StringBuilder();

    builder.append("[time = ").append((endMs - startMs) / 1000).append("sec, ");

    long totalElements = 0L;
    for (String name : this.counters.keySet()) {
      var counter = this.counters.get(name);
      builder.append(name).append(" = ").append(counter.getValue()).append(", ");
      totalElements += counter.getValue();
    }
    builder.append("total elements = ").append(totalElements).append("]");

    return builder.toString();
  }
}
