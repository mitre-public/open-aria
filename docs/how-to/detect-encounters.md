# How to use `OpenARIA` to detect encounters

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
- Copy the configuration file [here](../../open-aria-airborne/src/test/resources/sampleConfig.yaml) to your new
  directory
- Copy the `OpenARIA.jar` from `{BASE_DIR}/open-aria/open-aria-deploy/target/OpenARIA-{VERSION}.jar` to your new
  directory

**Run the project**

- Run `java -cp OpenARIA-{VERSION}.jar org.mitre.openaria.RunAirborneOnFile -f sampleData.txt.gz -c sampleConfig.yaml`
- Look at your output in `/myStuff/open-aria-demo/detectedEvents`
    - The number of events in this output directory can be adjusted changing your configuration yaml.

**Inspect the output**

Each encounter found by ARIA is written as a separate `.json` file and placed in the `detectedEvents` directory.
The contents of `2018-03-24--D21--N518SP--1200--54180.json` are shown below. This event record shows:

- **A near midair collision**
- The two aircraft that came within `33ft` vertically, and `0.058 NM` laterally.
- An event score of `1.36`
- One aircraft was flying level at `2600ft`
- The other aircraft was `descending`
- The "time series" data describing the encounter is shown at the bottom

```json
{
  "uniqueId": "06cfbd1c52882a94eaa4b54c88c42280",
  "facility": "D21",
  "eventScore": 1.36204,
  "eventDate": "2018-03-24",
  "eventTime": "15:03:00.107",
  "eventEpochMsTime": 1521903780107,
  "title": "D21--N518SP--1200",
  "latitude": 42.28303,
  "longitude": -83.75898,
  "timeToCpaInMilliSec": 0,
  "atEventTime": {
    "timestamp": "2018-03-24T15:03:00.107Z",
    "epochMsTime": 1521903780107,
    "score": 1.36204,
    "trueVerticalFt": 33.11037,
    "trueLateralNm": 0.05851,
    "angleDelta": 113,
    "vertClosureRateFtPerMin": -2006.68896,
    "lateralClosureRateKt": -138.71506,
    "estTimeToCpaMs": 0,
    "estVerticalAtCpaFt": 33.11037,
    "estLateralAtCpaNm": 0.05851
  },
  
  ... redacted for simplicity ... ,
  
  "isLevelOffEvent": false,
  "courseDelta": 113,
  "conflictAngle": "CROSSING",
  "aircraft_0": {
    "callsign": "N518SP",
    "uniqueId": "aa54aaa2dacf1e390e3946113a71fe29",
    "altitudeInFeet": 2566,
    "climbStatus": "DESCENDING",
    "speedInKnots": 112,
    "direction": "SOUTH",
    "course": 178,
    "beaconcode": "5256",
    "trackId": "3472",
    "aircraftType": "C172",
    "latitude": 42.28255,
    "longitude": -83.75895,
    "ifrVfrStatus": "VFR",
    "climbRateInFeetPerMin": -1131,
    "aircraftClass": "FIXED_WING",
    "engineType": "PISTON",
    "pilotSystem": "MANNED",
    "isMilitary": "FALSE"
  },
  "aircraft_1": {
    "callsign": "UNKNOWN",
    "uniqueId": "cd2c54bea878b9c1be75c631fc56dadb",
    "altitudeInFeet": 2600,
    "climbStatus": "LEVEL",
    "speedInKnots": 66,
    "direction": "WEST_NORTH_WEST",
    "course": 291,
    "beaconcode": "1200",
    "trackId": "2643",
    "aircraftType": "UNKNOWN",
    "latitude": 42.28352,
    "longitude": -83.75901,
    "ifrVfrStatus": "VFR",
    "climbRateInFeetPerMin": 0,
    "aircraftClass": "UNKNOWN",
    "engineType": "UNKNOWN",
    "pilotSystem": "UNKNOWN",
    "isMilitary": "UNKNOWN"
  },
  "schemaVersion": "3",
  "airborneDynamics": {
    "epochMsTime": [1521903392607, 1521903395107, 1521903397607, 1521903400107, ...],
    "trueVerticalFt": [1700.0, 1700.0, 1700.0, 1700.0, ... ],
    "trueLateralNm": [14.62, 14.5, 14.37, 14.25, ... ],
    "estTimeToCpaMs": [292232, 288150, 284166, 280406, ... ],
    "estVerticalAtCpaFt": [1700.0, 1700.0, 1700.0, 141.11, ... ],
    "estLateralAtCpaNm": [2.29, 2.25, 2.2, 2.18, ... ],
    "score": [121.08, 118.22, 1176.61, 1043.12, ...]
  }
}
```