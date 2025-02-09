# How to use `OpenARIA` to Inspect a Dataset

---

## NOTICE: Docker-base tutorial coming soon!!

- A docker-base tutorial will be easier to use because it won't require users to install Java or build
  the project. In the revised tutorial you'll simply run `InspectDataset` on a file of location data.

---

## What you'll do

1. You will build `OpenARIA` from source code.
2. Then you will run OpenARIA's `InspectDataset` program on a file's worth of aircraft location data.

## What you'll need

- A locally installed version of java (JDK 17+).

- A file of aircraft surveillance data.
- The compiled `OpenARIA.jar` file.

---

# Instructions

- **Prerequisite:** Install Java (JDK 17+ is required).
  - If you don't have Java installed locally, download it from [here](https://adoptium.net/).
  - Use the command `java -version` to verify your installation

### Clone and build the project

- Navigate to the directory where you keep your git repos: e.g. `cd {PATH_TO_GIT_PROJECTS}`
- Clone this git repo using `git clone git@github.com:mitre-public/openaria.git`
  - This creates a new directory named `open-aria` filled with the contents of this git repo.
- Navigate into the repo directory `cd open-aria`

### Build the project

1. Run the command: `./gradlew build`
   - This command builds the project from the source code.
   - This command builds the 7 different modules of the project (e.g. `open-aria-core`, `open-aria-threading`, ...)
   - This command DOES NOT gather all the compiled artifacts into one simple archive.

2. Run the command: `./gradlew shadowJar`
   - This command gathers all needed software assets together (including 3rd-party dependencies!) and puts them in one _"uber-jar"_.
   - The _uber-jar_ file is named: `open-aria-{VERSION}-uber.jar`
   - The _uber-jar_ file is created at:`open-aria/open-aria-deploy/build/libs/open-aria-{VERSION}-uber.jar`

- These commands can be combined with: `./gradlew build shadowJar`

### Co-locate the data and uber-jar

- Create a new directory.
- In this directory we'll  **ONLY** what we need to run the `InspectDataset` program.
- Copy the sample data from `open-aria/open-aria-airborne/src/test/resources/sampleData.txt.gz` into the directory
- Copy the _uber-jar_ from `open-aria/open-aria-deploy/build/libs/open-aria-{VERSION}-uber.jar` into the directory

### Run the `InspectDataset` utility program

- Run: `java -cp open-aria-{VERSION}-uber.jar org.mitre.openaria.InspectDataset -f sampleData.txt.gz --nop`
- This program describes the location data found inside the supplied file.
- Its output will look like:

    ```
    == Histogram of Points per 60sec ==
    2018-03-24T14:55:00Z: 596 points
    2018-03-24T14:56:00Z: 1731 points
    2018-03-24T14:57:00Z: 1711 points
    2018-03-24T14:58:00Z: 1709 points
    2018-03-24T14:59:00Z: 1652 points
    2018-03-24T15:00:00Z: 1656 points
    2018-03-24T15:01:00Z: 1645 points
    2018-03-24T15:02:00Z: 1643 points
    2018-03-24T15:03:00Z: 1659 points
    2018-03-24T15:04:00Z: 1663 points
    2018-03-24T15:05:00Z: 1652 points
    2018-03-24T15:06:00Z: 1643 points
    2018-03-24T15:07:00Z: 1040 points
    
    == Statistics on Points per Track ==
    Num Tracks: 364
    Min Track Size: 1
    Avg Track Size: 54.57
    Max Track Size: 205
    StandardDev of Track Size: 58.88
    
    == Statistics on Track Duration ==
    Num Tracks: 364
    Min Track Duration: 0.0sec
    Avg Track Duration: 297.49sec
    Max Track Duration: 718.0sec
    StandardDev of Track Duration: 290.16
    
    == Statistics on Track Points Per Minute ==
    Num Tracks: 300
    Min Track Points Per Minute: 5.01
    Avg Track Points Per Minute: 11.89
    Max Track Points Per Minute: 120.00
    StandardDev of Points Per Minute: 8.10
    
    == Track start times ==
    2018-03-24T14:55:00Z: 161 points
    2018-03-24T14:56:00Z: 20 points
    2018-03-24T14:57:00Z: 18 points
    2018-03-24T14:58:00Z: 14 points
    2018-03-24T14:59:00Z: 9 points
    2018-03-24T15:00:00Z: 21 points
    2018-03-24T15:01:00Z: 12 points
    2018-03-24T15:02:00Z: 15 points
    2018-03-24T15:03:00Z: 19 points
    2018-03-24T15:04:00Z: 22 points
    2018-03-24T15:05:00Z: 21 points
    2018-03-24T15:06:00Z: 19 points
    2018-03-24T15:07:00Z: 13 points
    
    == Track end times ==
    2018-03-24T14:55:00Z: 5 points
    2018-03-24T14:56:00Z: 16 points
    2018-03-24T14:57:00Z: 23 points
    2018-03-24T14:58:00Z: 16 points
    2018-03-24T14:59:00Z: 10 points
    2018-03-24T15:00:00Z: 21 points
    2018-03-24T15:01:00Z: 12 points
    2018-03-24T15:02:00Z: 16 points
    2018-03-24T15:03:00Z: 20 points
    2018-03-24T15:04:00Z: 21 points
    2018-03-24T15:05:00Z: 22 points
    2018-03-24T15:06:00Z: 22 points
    2018-03-24T15:07:00Z: 160 points
    
    == Making Map of Input Data ==
    Map Created, see: map-of-sampleData.txt.png
    ```

### Run `InspectDataset` and generate a plain Map

- Add `--map` to the command, and you'll also get a plain map with a black background
- e.g. Run: `java -cp open-aria-{VERSION}-uber.jar org.mitre.openaria.InspectDataset -f sampleData.txt.gz --nop --map`
- The map looks like:
  ![map](./../assets/plain-map-of-sampleData.txt.png)

### Run `InspectDataset` and generate a properly titled Map

- Add `--map --mapBoxTiles` to the command, and you'll also get a map drawn on top of MapBox tiles
- e.g. Run: `java -cp open-aria-{VERSION}-uber.jar org.mitre.openaria.InspectDataset -f sampleData.txt.gz --nop --map --mapBoxTiles`
- Using the `--mapBoxTiles` flag requires an API token for the MapBox service.
- See: the "Map making" documentation in the MITRE Commons library [here](https://github.com/mitre-public/commons/blob/main/docs/mapping.md)
- The map looks like:
  ![map](./../assets/tiled-map-of-sampleData.txt.png)

### Run `InspectDataset` and generate a custom Color & Alpha Map

- Add any combination of `--red {VALUE}`, `--green {VALUE}`, `--blue {VALUE}`, `--alpha {VALUE}` where VALUE is between
  0 and 255 (except alpha, it's minimum value is 1)
- Add `--zoomLevel {VALUE}` where VALUE is between 1 and 15
- e.g. Run:
  `java -cp open-aria-{VERSION}-uber.jar org.mitre.openaria.InspectDataset -f sampleData.txt.gz --nop --map --green 255 --alpha 35`
- The map looks like:
  ![map](./../assets/green-map-of-sampleData.txt.png)