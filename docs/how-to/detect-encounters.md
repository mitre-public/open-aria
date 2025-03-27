# How to use `OpenARIA` to detect encounters

---

## NOTICE: Docker-base tutorial coming soon!!

- A docker-base tutorial will be easier to use because it won't require users to install Java. In the revised tutorial
  you'll simply run `RunAirborneOnFile` on a file of location data.

---

## What you'll do

1. **Download:** `OpenARIA's` _uber-jar_, a configuration file, and a sample dataset.
2. **Run:** OpenARIA's `RunAirborneOnFile` program on the sample dataset.

## What you'll need

- A locally installed version of java (JDK 17+).

---

# Instructions

### Step 0: Verify your Java Install

- OpenARIA (currently) requires a local Java install.
- Run the command: `java --version` to verify your java installation.
- This command should produce something like:
    ```
   openjdk 17.0.6 2023-01-17
   OpenJDK Runtime Environment Temurin-17.0.6+10 (build 17.0.6+10)
   OpenJDK 64-Bit Server VM Temurin-17.0.6+10 (build 17.0.6+10, mixed mode)
    ```
- Java 17+ is required.
- If you don't have Java installed locally, download it from [here](https://adoptium.net/).

### Step 1: Download a recent `OpenARIA` compiled jar

- Navigate to: [OpenARIA's release page](https://github.com/mitre-public/open-aria/releases)
- Pick a recent release version (e.g. [0.3.0](https://github.com/mitre-public/open-aria/releases/tag/0.3.0))
- Download the compiled jar (e.g. `open-aria-0.3.0.jar`)
  - This jar contains all needed software assets (including 3rd-party dependencies!) gathered together in one _"
    uber-jar"_.

### Step 2: Download a sample dataset

- Download [sampleNopData.txt.gz](https://github.com/mitre-public/open-aria/blob/main/open-aria-airborne/src/main/resources/sampleNopData.txt.gz)
from the repo's directory of test assets.
- Use the "Download raw file" button in the upper right hand corner
- This file contains about 10 minutes of aircraft location data.

### Step 3: Download a config file

- Copy the configuration
  file [sampleNopConfig.yaml](https://github.com/mitre-public/open-aria/blob/main/open-aria-airborne/src/main/resources/sampleNopConfig.yaml)
  from the repo's directory of test assets.
- Use the "Download raw file" button in the upper right hand corner
- This file configures `OpenARIA's` event detection algorithm

### Step 3: Co-locate the data and uber-jar

- Create a new directory.
- Copy the uber-jar (e.g. `open-aria-0.3.0.jar`) into this directory.
- Copy the dataset (e.g. `sampleNopData.txt.gz`) into this directory.
- Copy the config file (e.g. `sampleConfig.yaml`) into this directory.

### Step 4: Run the `RunAirborneOnFile` program

- Run: `java -cp open-aria-0.3.0.jar org.mitre.openaria.RunAirborneOnFile -f sampleNopData.txt.gz -c sampleNopConfig.yaml`

### Step 5: Inspect the output

**Inspect the output**

- Look at your output in `detectedEvents` and `eventMaps`
- Each encounter found by ARIA is written as a separate `.json` file in the `detectedEvents` directory.
- The default configuration (which uses a `MapSink`) will also produce a plain map graphic in the `eventMaps` directory
- The "sensitivity" of OpenARIA's event detection algorithm, and hence the number of events detected, can be adjusted by
  changing the configuration's `maxReportableScore` variable.


- The contents of an example event are shown below. This event record shows:
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