## The `open-aria-pairing` module

### Purpose of Module

This module:

1. Finds `PointPairs` where both points are close in time and space.
2. Finds `TrackPairs` where both aircraft were close in time and space.

### Important Classes within this Module

- `PointPairFinder` Efficiently finds points that are in close proximity.
- `TrackPairer` This class uses `PointPairFinder` to create `TrackPairs`. All Track creation, and Track pairing occurs in **IN A SINGLE PASS THROUGH THE DATA**

### Main methods & Launch Points

- None

### Data Flow for PointPairFinder

1. Create new `PointPairFinder` (PPF), during construction tell the PPF where output (e.g. newly found Point Pairs) should go. Do this by providing an "Output Mechanism" (e.g., `Consumer<Pair<Point, Point>> outputMechanism`)
    1. Note, the `PointPairFinder` is a `Consumer<Point>`, therefore it operates by receiving a stream of Point data through its `accept(Point)` method.
    2. When creating a `PointPairFinder` you also provide a `PairingConfig` that allows you to determine the distance between the Points that require "pairing".
2. Now you "pour" `Point` data into a `PointPairFinder`
    1. This data can come from any source (File, Kafka, etc.)
3. When the `PointPairFinder` detects two Points that are close in space (i.e., physical distance) and time it will emit a `Pair<Point, Point>` to the outputMechanism.
    1. This "pairing" is performed using a `MetricTree` that only holds 13 seconds of data. This is why data has to be received in chronological order.

### Data Flow for TrackPairer

1. Create new `TrackPairer` (TP), during construction tell the TP where output should go. Do this by providing two "Output Mechanisms" (e.g., `Consumer<TrackPair>` and `Consumer<Track>`)
    1. The `TrackPairer` contains a `PointPairFinder` **AND** a `TrackMaker`
    2. Notice, `TrackPairer`, `PointPairFinder`, and `TrackMaker` are all "Point Consumers" (e.g., `implement Consumer<Point>`). This means that all operate by receiving a stream of Point data through their `accept(Point)` method.
    3. When a `TrackPairer` receives a new `Point` object it sends that input Point **both** the `PointPairFinder` and the `TrackMaker`.
    4. This means the `TrackPairer` is responsible for: (1) "memorizing" the `Pair<Point, Point>` the `PointPairFinder` produces, (2) receiving all `Tracks` made by the `TrackMaker`, and (3) creating `TrackPair` by gradually filling out `OpenTrackPairs` with Track data until they can be converted to fully defined `TrackPair` objects. 
