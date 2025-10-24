# APIBridge


## Neo4j Setup

Set up an instance of Neo4j database and restore the db dump from ```neo4j.dump``` and ```apibridge.dump```.

### Build project

In the APIBridge folder, run the build using:

```./mvnw clean package ```

To run, in the target package:

``` java -jar apibridge*.jar```

## For Docker

### Build

```docker build . -t apibridge```

### Run

```docker run -p 8080:8080 -t apibridge -network host```

__NOTE:__ assumes neo4j is running on host network.
