## Relax Timer

A simple **Hierarchical Timing Wheels** timer implementation 'copy' from kafka and rewrite with java from scala, so we can use it both in scala and java.

Related resources:

- [What is the Hashed Timer?](https://github.com/ifesdjeen/hashed-wheel-timer)
- [Apache Kafka, Purgatory, and Hierarchical Timing Wheels](https://www.confluent.io/blog/apache-kafka-purgatory-hierarchical-timing-wheels/)

## Usage

```xml
<dependency>
    <groupId>com.infilos</groupId>
    <artifactId>relax-timer</artifactId>
    <version>LATEST</version>
</dependency>
```

And, add a binding for slf4j:

```xml
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>LATEST</version>
</dependency>
```

And then create time instance and submit tasks:

```java
Timer timer = Timer.create("test-timer").startup();

// print "run..." after 2 seconds
timer.submit(new Runnable() {
    @Override
    public void run() {
        System.out.println("run...");
    }
}, 2000L);
```

## Release

- Snapshot: `mvn clean deploy`
- Release: `mvn clean package source:jar gpg:sign install:install deploy:deploy`
