# APIBridge

This project requires a running instance of Neo4j database. The Neo4j Desktop can be downloaded from their [site](https://neo4j.com/download/neo4j-desktop/?edition=desktop&flavour=osx&release=2.0.5&offline=false).

The application assumes that neo4j is running with default port settings.


## Neo4j Setup

Set up an instance of Neo4j database and restore the db dump from ```neo4j.dump``` and ```apibridge.dump```.

```sh
sudo neo4j-admin database load --from-path=/full-path/data/dumps <database> --overwrite-destination=true
```

### Build project

Edit the configuration for database credentials.

In the APIBridge folder, run the build using:

```./mvnw clean package ```

To run, in the target package:

``` java -jar apibridge*.jar```
