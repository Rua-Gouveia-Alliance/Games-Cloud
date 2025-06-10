## Autoscaler

This project contains Autoscaler functionality.

### How to build

1. Make sure your `JAVA_HOME` environment variable is set to Java 11+ distribution
2. Run `mvn clean package`

### How to run locally

To run Autoscaler locally in CLI, execute this command:

```
java -cp target/autoscaler-1.0.0-SNAPSHOT-jar-with-dependencies.jar pt.ulisboa.tecnico.cnv.autoscaler.Autoscaler <lambda> <B>
```
