# Release Notes


### Version 0.3.0 (Released 2025-02-18)

- Added a `dataFormat` option to the standard yaml Configuration files
    - Current options are `nop` and `csv`
- Gathered sample datasets & configs in `open-aria-airborne/src/main/resources`
- Demos now use config files with the `dataFormat` field specified
- Added new Demo that process both CSV and NOP data (in separate runs)
- Incorporated Formats into InspectDataset
- Added Formats.nop()
- Added Formats.csv()
- Simplified RunAirborneOnFile

### Version 0.2.0 (Released 2025-02-11)

- Added GitHub Action [ietf-tools/semver-action](https://github.com/ietf-tools/semver-action) to auto-compute version number
- Adopted [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/)
- Added `MapSink` and `MapSinkSupplier` to render maps of all output events
- Added `InspectDataset` executable program
- Improved Lateral Outlier Detection
- Added Demo that processes CSV data
- Added Demo that processes NOP data


### Version 0.1.0 (Released 2024-05-24)

- The initial public release of the project.