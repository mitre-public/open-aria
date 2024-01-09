## The `open-aria-airborne` module

### Purpose of Module

The purpose of this module is to:

1. Define ARIA's Airborne Event Detection algorithm
2. Define the Airborne output data (e.g., What information is in the output data)
3. Define the output data format (e.g., How is the information encoded? JSON? AVRO?)
4. Provide "launch points" that allow you to run this event detection algorithm on aircraft position data.


### Important Classes within this Module

- `AirborneEvent` This class **is** the output data.
    - Output JSON records are created by converting instances of `AirborneEvent` into JSON text (using Google's GSON tool)
- `AirborneAnalysis` is where a `TrackPair` is analyzed to create "separation over time data". Once computed this analysis is the "raw input" to the risk evaluation logic.
- `AirborneAria` The risk detection algorithm is defined here
- `AirbornePairConsumer` This class applies the `AirborneAria` algorithm to a `TrackPair`. Output is sent to an output channel `Consumer<AirborneEvent>`
- The mathematics underpinning the closest point of approach computation is [here](cpaComputation.md).

### Important Main methods & Launch Points
- `RunAirborneOnKafkaNop` = **The Production Airborne ARIA main method**
- `RunAirborneOnFile` = Applies the Airborne ARIA directly on the NOP data found in a file
- `RunAirborneViaStdIn` = Applies the Airborne ARIA directly on the NOP data found in standard-in
- `ReprocessLabeledEvents` = Evaluates the current Airborne Algorithm against the SME-evaluated training-set 