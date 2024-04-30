# Architectural Decision Record

## Title: Supporting New Data Formats

**Date:** April 15th, 2024

## Bottom Line:

- A significant refactor is warranted, and will begin.
- The goal of this refactor is to help add support for additional data formats.
- At the end of this work package choosing an input data format will be supported via configuration.

## Context

- **It is vital for the `OpenARIA` project to support additional location data formats.**
- [Prior analysis](./pointInterfaceCritique.md) found `Point` and `Track` have technical debt that complicates
  supporting new data formats.
- That analysis also found refactoring `Point` and `Track` would only be warranted if an overwhelming need was found.
- **We now have that need!**

### Technical Context

- The `Point` and `Track` interfaces are based on legacy code from about 10 years ago. When these interfaces were
  developed the focus was providing programmatic access to a specific FAA surveillance data format (e.g., NOP data).
- The `Point` interface assumes the existence of data fields that may not be available in all types of location data (
  e.g., `callsign`, `beacon code`, `IFR/VFR status`, etc.).
- The `Track` interface carries forward these incorrect assumptions.
- Some _"track smoothers"_ (e.g. `DataCleaner<MutableTrack>`) mutate the input Track and its component Points.

### Work to be done

- Remove methods from the `Point` interface (e.g., `callsign`, `beacon code`, `IFR/VFR status`, `speed`, `course`).
    - This will make implementing `Point` easier.
    - This change will also break the implicit assumption that OpenARIA requires an FAA-specific data format.
- Remove support for mutable Points and Tracks (i.e. delete `MutablePoint` and `MutableTrack`). Mutating Points and
  Tracks in place can yield minor performance improvements. However, improving code clarity and reducing maintenance
  burden (e.g. lines-of-code) are more important priorities.
- Make `Point` and `Track` generic in type `T` where `T` = the raw data format.
- Evaluate the viability of transitioning `Point` and `Track` to records.
- Demonstrate ingesting data that requires filtering out individual Points (e.g. some Points are flawed)
- Demonstrate ingesting data that requires filtering out individual Tracks (e.g. some Tracks are sensitive)
- Add "which input parser to use" to the OpenARIA's configuration layer.

### Goals:

- Enable Support for multiple location data types including:
    - FAA NOP and ASDEX
    - ADS-B data
    - ASTERIX data
- Support location data that does **not**
  contain: `speed`, `climb rate`, `heading`, `callsign`, `beacon code`, `aircraft type`, `etc.`
- Ensure an `AirborneEvent` based on any data format can contain the raw source data from which the event was derived.
- Ensure "smoothing" a `Track` does not mutate the raw source data.
- Enable data filtering based on raw source data type `T`. For example, if an `AirborneEvent` is derived from ASTERIX
  data we need to be able to sort, filter, and otherwise manipulate the `AirborneEvent<ASTERIX>`

### Status

- **Underway**
