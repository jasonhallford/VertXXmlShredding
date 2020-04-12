package io.miscellanea.vertx.example;

/**
 * Simple counter that starts at zero and increments by one
 * each time <code>increment</code> is called.
 *
 * @author Jason Hallford
 */
public class Counter {
    // Fields
    private long counter = 0L;

    // Constructors
    public Counter(){}

    // Methods

    /**
     * Gets the counter's value.
     *
     * @return The value.
     */
    public long getValue(){
        return this.counter;
    }

    /**
     * Increments the counter by one.
     */
    public void increment(){
        this.counter++;
    }
}
