## Introduction
This repository demonstrates a method for _shredding_ XML documents in an [Eclipse Vert.x](http://vertx.io) application.
The core idea it that a worker verticle, called the _shredder_, interacts with a _content handler_ (the shredder is built 
on SAX to reduce memory footprint) that, in turn, generates a stream of events consumed by a _processor_. The handler 
and processor are developed together, and are exposed to the shredded via the Java Service Loader framework. 

## Requirements
To build this example, you will need the following:
1. A version of Git capable of cloning this repository from Git Hub
1. Apache Maven v3.5 or greater
1. The latest patch release of OpenJDK 11 (build produced by the [AdoptOpenJDK Project](https://adoptopenjdk.net/) work
nicely)

## Building the Project
You may build a JAR from source using [Apache Maven](http://maven.apache.org). Assuming a version >= 3.5.0 you can build it  by
executing `mvn package` at the command line. Maven will produce a fat JAR named __vertx-xml-shredding-1.0-fat.jar__ that
you may execute by running `java -jar vertx-xml-shredding-1.0-fat.jar`.

## Configuring the Example
The example includes a default configuration that deploys a single shredder verticle and shreds a document, provided on
the command line, once. The number of shredders and processing iterations can be adjusted by setting the following 
properties either as OS environment variables or JRE system proprties (i.e. "-D" properties). The latter have a higher 
priority than the former.

| Property          | Notes                                                        |
| ----------------- | ------------------------------------------------------------ |
| job-count     | An integer that sets the number of processing iterations; defaults to 1. |
| shred-verticle-count | An integer that sets the number of shredding worker verticles; defaults to 1. | 

## Running the Example
This example is run from the command line. For example, to launch a test that shreds a document named 'my.xml' five 
times using two shredders, you would run the following command:

```shell script
$ java -jar ./vertx-xml-shredding-1.0-fat.jar -Djob-count=5 -Dshred-verticle-count=5 ~/my.xml
```
