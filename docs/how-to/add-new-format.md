# How to Add a New Data Format

Adding a data format requires implementing the `Format` interface (found in `open-aria-core`).

This interface has two methods:

```java
    /** Convert a file of location data written in some unknown format into an Iterator of Points. */
    Iterator<Point<T>> parseFile(File file);
    
    /**
     * Given a positionReport (of format T) provide a json-friendly String that describes the
     * positionReport. When no JSON-friendly String exists consider converting the positionReport to
     * a byte[] and using the "Format.asBase64(byte[])" helper method.
     * <p>
     * Required to copy input location data into output event records. This allows output event
     * records to say: "This input data caused this output event"
     */
    String asRawString(T positionReport);
```

## Approach 1:  Implement & submit a Pull-Request

In a branch ... :

1. Add a new package (i.e. directory) in `open-aria-core` under org.mitre.openaria.core.formats.NEW_FORMAT`
2. Write a class that "is" your position report. Your class will look something like: `AriaCsvHit` and `NopHit`
3. Write a class that implements `org.mitre.openaria.core.formats.Format`. The generic type of this Format will be
   whatever class you wrote in step 2.
4. Test your Parser
5. Update the Demos and Documentation to support a new Command Line format flag (i.e. `--adsb`)

## Approach 2:  Implement in a separate code base

@todo -- NOT YET FULLY SUPPORTED

In a separate code base ... :

1. Add OpenARIA's "open-aria-core" module to your classpath
2. Write a class that "is" your position report. Your class will look something like: `AriaCsvHit` and `NopHit`
3. Write a class that implements `org.mitre.openaria.core.formats.Format`. The generic type of this Format will be
   whatever class you wrote in step 2.
4. Test your Parser
5. **Submit a GitHub issue:** We need to add support to inject a Format using a plugin from the config file.  
