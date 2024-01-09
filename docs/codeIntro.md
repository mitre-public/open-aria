# The OpenARIA codebase

## Multi-Module Maven Project

OpenARIA is as a [multi-module maven project](https://www.baeldung.com/maven-multi-module). This means:

- The source code is split into "modules".
    - Each module can be built and packaged separately.
- This source code is compiled and packaged using [maven](https://maven.apache.org/).
    - Build the project using the maven command `mvn clean install`
    - After a successful build each module's target directory will contain a module-specific jar file.
        - e.g. `open-aria/open-aria-core/target/open-aria-core-0.1.0.jar`
    - However, the last module `open-aria-deploy` will contain an uber-jar that contains all modules and all
      dependencies

## The OpenARIA Modules

Here are modules listed from _"core functionality"_ to _"optional functionality"_

### open-aria-core

- Contains code that needs to be shared across multiple ARIA modules.
- Should not contain _"truly general purpose code"_. General purpose code (that could be useful outside OpenARIA) should
  be migrated to [this Commons library](https://github.com/mitre-public/commons/).
- click [here](open-aria-core.md) for more details

### open-aria-threading

- Contains source code for Transforming a stream of _n_ Point into a stream of _m_ Tracks
- click [here](open-aria-threading.md) for more details

### open-aria-pairing

- Very efficiently finds all `TrackPairs` in which the pair of aircraft involved in the encounter come "close to"
  one another.
- click [here](open-aria-pairing.md) for more details

### open-aria-airborne

- Contains the Airborne ARIA source code.
- click [here](open-aria-airborne.md) for more details

### open-aria-kafka

- Contains code for interacting with Kafka
- click [here](open-aria-kafka.md) for more details   
