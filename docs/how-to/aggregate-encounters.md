# How to use `OpenARIA` to detect and aggregate encounters

---

## NOTICE: Docker-base tutorial coming soon!!

- After `OpenARIA` hits GitHub this tutorial will be updated.
- The updated tutorial will use a publicly available docker image that **cannot** be available until OpenARIA is
  publicly released.
- This docker-base tutorial will be easier to use because it won't require users to install Java, install maven, or
  build the project. In the revised tutorial you'll simply RUN to project.

---

## What you'll do

You will build the `OpenARIA` software from source code. Then you will apply OpenARIA's airborne risk detection logic to
a file's worth of aircraft location data.

## What you'll need

- A locally installed version of java (JDK 17+).
- A locally installed version of maven.


- The provided file of aircraft surveillance data.
- The provided configuration file.
- The compiled `OpenARIA.jar` file.

# Instructions

- **Prerequisite:** Install Java (JDK 17+ is required).
    - If you don't have Java installed locally, download it from [here](https://adoptium.net/).
    - Use the command `java -version` to verify your installation
- **Prerequisite:** Install Maven.
    - If you don't have maven installed locally, download it from [see](https://maven.apache.org/download.cgi).
    - Use the command `mvn -version` to verify your installation

**Build the project**

- Clone this git repo using `git clone git@github.com:mitre-public/openaria.git`
- Navigate to the repo directory `cd open-aria`
- Build the project using `mvn clean install`
    - This generates the uber-jar file: `OpenARIA-{VERSION}.jar`. This file contains the software and all the necessary
      dependencies combined into one file
    - A successful build will generate this file at: `open-aria/open-aria-deploy/target/OpenARIA-{VERSION}.jar`

**Gather the data, configuration, and uber-jar**

- Make a new directory where your `OpenARIA` installation will go
    - e.g. `mkdir /myStuff/open-aria-demo`
- Copy the sample data from [here](../../open-aria-airborne/src/test/resources/sampleData.txt.gz) to your new directory
- Copy the configuration file [here](../../open-aria-airborne/src/test/resources/sampleConfig2.yaml) to your new
  directory
- Copy the `OpenARIA.jar` from `{BASE_DIR}/open-aria/open-aria-deploy/target/OpenARIA-{VERSION}.jar` to your new
  directory

**Run the project**

- Run `java -cp OpenARIA-{VERSION}.jar org.mitre.openaria.RunAirborneOnFile -f sampleData.txt.gz -c sampleConfig2.yaml`
- Look at your output in `/myStuff/open-aria-demo/detectedEvents`
    - The number of events in this output directory can be adjusted changing your configuration yaml.

**Inspect the output**

This demo reports output in two ways

1. Each encounter found by ARIA is written as a separate `.json` file
2. All encounters are placed in a single `.avro` file. Reporting output using the `AvroOutputSink` allows one large
   batch of surveillance data to become one succinct `.avro` file.

