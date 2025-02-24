## The `open-aria-core` module

### Purpose of Module

The purpose of this module is to:

1. Define important interfaces like `Point` and `Track`
2. Define important classes like `NopPoint` and `TrackPair`
3. Define the track smoothing algorithms used remove noise from raw radar data (see `org.mitre.openaria.smoothing`)
4. Contain "reusable code" that could be (re)used in: Airborne ARIA, Surface ARIA, and CFIT ARIA.

### Smoothing Summary
- Smoothing is applied to `Track` objects
- Smoothing is crucial for ensuring the ARIA algorithms are stable and resilient to radar noise.

### Important Classes within this Module

- `Point` This interface is essentially a radar hit that can come from NOP or ASDEX
- `CommonPoint` A concrete implementation of Point. This implementation is immutable
- `EphemeralPoint` A Mutable implementation of Point whose underlying data can be changed
- `AriaEvent` this interface is extended by AirborneEvent, SurfaceEvent, and CfitEvent
- `PointPair` = Two Points with the same timestamp. This pre-requisite allows the collision risk between the aircraft to be calculated.
- `StrictTimeSortEnforcer`, `ApproximateTimeSorter`, and `StreamingTimeSorter` together, these classes allow ARIA algorithms to assume all input data is in time order (which simplifies their implementation)
- `ClosestPointOfApproach` is a **prediction** of how long it will take for two aircraft to arrive at their closest proximity (given current locations, speeds, and directions).  Learn more about the computation [here](./cpaComputation.md)

### Notes on Module

- Contains ARIA-specific general purpose code.

### Main methods & Program Launch Points

- No important executables
