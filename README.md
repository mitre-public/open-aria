
![aria logo picture](./docs/assets/DFW-Airspace-Graph.gif)

[![Java CI with Gradle](https://github.com/mitre-public/open-aria/actions/workflows/gradle.yml/badge.svg)](https://github.com/mitre-public/open-aria/actions/workflows/gradle.yml)
[![License](https://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

# Welcome to OpenARIA

This repository contains an open-source edition of the _Aviation Risk Identification and Assessment_ (ARIA) software
program developed by MITRE on behalf of the Federal Aviation Administration's (FAA) Safety and Technical Training (AJI)
Service Unit.

## OpenARIA's Goal

Our goal is to build a community focused on improving aviation safety & efficiency by extracting value from aircraft
location data.

## How OpenARIA can achieve this Goal

1. Provide a publicly available solution for **detecting aviation risks** within aircraft location data.
    - This tangible working solution can be critiqued by the community and improved as necessary.

2. Provide a publicly available solution for detecting **and then aggregating** aviation risks for bulk
   analysis.
    - Someone operating `OpenARIA` for a day will have one day's worth of output
    - Someone operating `OpenARIA` for a year will have a year's worth of output.
    - We must facilitate capturing and utilizing large amounts of output data.

3. Provide a publicly available solution for **archiving and replaying** aircraft location data
    - E.g., when `OpenARIA` detects _an event_, we will want to be able to replay the event to understand what happened.

4. Provide solutions that work with near-real time data streams as well as archival data.

---

# Getting Started

- To **Inspect a dataset of aircraft location data** see [here](./docs/how-to/inspect-dataset.md)
- To **Detect aviation events** see [here](./docs/how-to/detect-encounters.md)


- Learn about the [supported data formats](./docs/csv-data-format.md)
- Learn about the ongoing work to support new data formats [here](./docs/ADRs/supportingNewFormats.md)

--- 

# Coming Soon ...

- Coming soon ... _**archiving and replaying aircraft location data**_ 
- Coming soon ... _**detecting and aggregating aviation events**_

---

## Building from Source

- Instruction on building from source are [here](./docs/how-to/build-from-source.md)

## Downloading Pre-Built Artifact
There are 2 places to download a pre-built artifacts:
1. Download an official full release from [here](https://github.com/mitre-public/open-aria/releases)
2. Download the artifact produced during a recent execution of the CI/CD system.
    - The [Github CI/CD executions](https://github.com/mitre-public/open-aria/actions/workflows/gradle.yml) list recent builds
    - Click on any build from the last 90 days (GitHub stores build artifacts for 90-days)
    - Download the Artifact named: `Deployable-Uber-Jar`

---

# Documentation

- [High-level source code summary](docs/codeIntro.md)


- **About ARIA's Airborne Event Data**
    - An example of this JSON output data is [here](open-aria-airborne/src/test/resources/scaryTrackOutput.json)
    - A PDF describing the output data is [here](open-aria-airborne/airborneDataSpec_v3.pdf)


- **Architectural Decision Records (ADRs)**
    - [Supporting New Data Formats](./docs/ADRs/supportingNewFormats.md)
    - [Critique of Point and Track interfaces](docs/ADRs/pointInterfaceCritique.md)
    - [Why YAML configs are preferable to Properties](docs/ADRs/yamlOverProperties.md)
    - [How to compute an event's uniqueId](docs/ADRs/computingUniqueId.md)

---

# Using and Contributing

First of all, **Welcome to the community!**

### Contributing as a User

- **Please submit feedback.**
- Do you have a technical question? If so, please ask. We are here to help. Your question could lead to improvements.
  User questions lead to improved documentation, understanding defects, and eventually code improvements the reach
  everyone in the community.
- Do you have a feature request? If so, please ask. We'll see what we can do given the development time we have
  available.

### Contributing as a Developer

- We will use GitHub's Issue tracking features when the project launches.
- Anyone interested in making technical contributions is welcomed to communicate with the dev team on GitHub. Feel free
  to submit issues, fix issues, and submit PRs.
- We may write a _"contributing guidelines"_ document in the future should the need arise. But for now, our focus will
  be on making high-quality, high-value improvements to the code (not policy documents).

### Contributing as a Data Provider

- OpenARIA is **extremely** interested in collecting shareable aircraft position datasets. Publicly available datasets
  can become the benchmarks dataset by which OpenARIA algorithms are measured and optimized. Read more [here](docs/shared-datasets.md) about
  the fundamental project need.

---

# Near-Term Project RoadMap

![Road Map Figure](docs/assets/OpenARIA-Roadmap.png)

## Release Notes

See [here](./docs/release-notes.md) for a summary of the changes and features included in each release.

## Versioning and Release Process

- The "current version" (e.g. `v1.2.3`) of this project is computed automatically form git tags and the recent commit
  messages.
    - The GitHub Action: [ietf-tools/semver-action](https://github.com/ietf-tools/semver-action) does this work.
- Consequently, we have also adopted [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/).
    - Commit message like: `feat!: Made breaking change`, `feat: Added feature`, and `fix: patched issue123` result in
      MAJOR, MINOR, and PATCH version bumps.
- When it is time for a release the `release.yaml` workflow is run manually.
    - This workflow computes the correct version number, makes a GitHub release, and adds a tag to the current git hash.


- You'll notice the gradle configuration does NOT have a version number. In other words, `version=X.Y.Z` does not exist
  in the `gradle.properties` file. This is an intentional choice. Official, properly versioned, builds come from CI --
  not your local build environment.

### MITRE Public Release

- Content approved for public release via The MITRE Corporation's "Public Release System" (PRS)
- Reference:  `Public Release Case Number: 23-3623`

### Legal Statement

- **Copyright:** The contents of this project is copyright `The MITRE Corporation`. See details [here](COPYRIGHT.txt) .
- **Open Source License:** This project is released under the Apache License Version 2.0. See details [here](LICENSE).