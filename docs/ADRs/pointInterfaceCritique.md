# Critique of Point and Track

This page describes `org.mitre.openaria.core.Point` and `org.mitre.openaria.core.Track`

## Bottom Line: 
- The `Point interface` has technical debt that is not worth addressing.
- The `Track interface` inherits and magnifies the technical debt in `Point`


### Downsides of Point
- The **main** problem (or nuisance) is that "Point" is not the best way to simultaneously support multiple types of raw data.
- The Point interface too closely resembles raw NOP data
- There is no seam to naturally support a different data type.  In other words, it's possible -- but unnatural -- to support raw ASDEX, raw ADS-B, and any future unknown data formats.
- In an ideal world Points would combine a "where is the aircraft" object with a "what metadata do we have about that aircraft" object.


### A Better Approach for Point
- If we started again today we would likely axe the Point interface completely and use `PositionRecord` and `KineticRecord` from the Commons package.
- Here is a small snippet of `PositionRecord`:
  ```
  public class PositionRecord<T> implements HasTime, HasPosition {
  
      private final T datum;
  
      private final Position position;
      
      ...
  
  }   
  ```
- This is a better approach because it more closely resembles how "surveillance data is similar AND different"!.
- Using this approach would require defining small easy to understand classes like `AsdexHitData`, `NopRhMessageData`, etc.


### Downsides of Track
- `Track` objects are awkward to work this because the "type of point metadata" is not easily accessible.  
-  In other words, `Track` should be a generic class.  But, this doesn't work out correctly because the underlying Points aren't generic.

### Potential Refactors
1. Refactor `Point` into a generic composite object (similar to `Map.Entry<K,V>`) where "part A" is the location data and "part B" is the aircaft metadata
1. Refactor `Track` into a generic interface that exposes the underlying Point's metadata class. This would look similar to how a `TreeMap<K,V>` exposes the Value class of `Map.Entry<K,V>`.

### Not a high priority fix

- Because the Point interface is so central to it doesn't make sense to alter this part of the code without a BIGGER GOAL in mind.
- The code is currently stable, and working correctly.  There is no reason to alter this aspect of the code right now.  