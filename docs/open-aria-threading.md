## The `open-aria-threading` module

### Purpose of Module

This module:
1. Contains the code that converts a Stream of `Points` into a Stream of `Tracks`.
2. Contains a few convenience functions for applying this capability to the data found in a file.

### Important Classes within this Module

- `TrackMaker` This class converts a Stream of `Points` into a Stream of `Tracks`.
- `TrackMaking` This class contains convenience functions for reading Files and making Tracks

### Important Main methods & Launch Points
- `TrackMakingDemo` = Shows the TrackMaker in action.
- `PlotTracksOnMap` = Makes a map from NOP data.  (Requires an installed MapBox API token)
  -  **Important** - Using the MapBoxApi requires loading a valid Mapbox Access token into the property key/variable name "MAPBOX_ACCESS_TOKEN".  This can be done
    one of three ways:
      - Setting an environment variable, 
      - Setting a Java System property, 
      - Placing a file named `mapbox.token` in the user directory.
  - If you use the file-based approach then the `mapbox.token` file should look
         like: `MAPBOX_ACCESS_TOKEN=this.IsNotAValidKey.EvenThoughILookLikeOne`
