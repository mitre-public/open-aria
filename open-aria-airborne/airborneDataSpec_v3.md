---
title:  Airborne ARIA Event Data Schema
subtitle: VERSION 3
author: Jon Parker
date: 2021-09-27
geometry:
    - top=1in
    - bottom=1in
    - left=0.75in
    - right=0.75in
papersize: letter
colorlinks: true
linkcolor: red
# note: convert to pdf via: pandoc airborneDataSpec_v3.md --from markdown+simple_tables+header_attributes+multiline_tables --pdf-engine=xelatex --number-sections --table-of-contents -o airborneDataSpec_v3.pdf
...

------------------

# Introduction {#introduction}

Airborne ARIA emits events in JSON format (see an example in [Appendix A](#example-json)). Output Airborne ARIA events can be to sent to one (or more) of these targets:

* to disk
* to standard out
* to Kafka

## Kafka Partitions

If JSON events are sent to Kafka, they are sent to a single Kafka topic (e.g. `ARIA_AIRBORNE_EVENTS`), but to **different partitions** of that topic. Airborne ARIA assigns each NOP Facility its own partition (within the event destination topic). E.g., all events for A80 will be sent to partition 1, all events for D10 will be sent to partition 45.

\newpage

# Data Schema {#event-data-schema}

This section breaks down the fields in the JSON schema, beginning with top-level fields and moving down into nested fields. 

## Top-Level Airborne ARIA Event JSON Fields

These fields are at the top of the JSON schema. Some of these fields have nested sub-fields, the nested fields are described later.

Field                             Nullable?  Example  
-------------------------------- ----------- -----------------
uniqueId                          NO         "3e13ce400cd5cd05ecf01fb7d2e4dcbe"  
facility                          NO         "SCT"   
eventScore                        NO         48.831343719532775
eventDate                         NO         "2021-09-19"
eventEpochMsTime                  NO         1632009648317
title                             NO         "SCT--DAL335--N502SX"
latitude                          NO         33.9370183364953
longitude                         NO         -118.27507592566856
isInsideAirspace                  NO         true
isNearTower                       NO         true
closestTower                      YES        [See Section](#closestTower) \ref{closestTower}
timeToCpaInMilliSec               NO         -60002
atEventTime                       NO         [See Section](#atEventTime) \ref{atEventTime}
atEstimatedCpaTime                YES        [See Section](#snapshot) \ref{snapshot}
atClosestLateral                  NO         [See Section](#snapshot) \ref{snapshot}
atClosestLateralWith1kVert        YES        [See Section](#snapshot) \ref{snapshot}
atClosestVerticalWith3Nm          YES        [See Section](#snapshot) \ref{snapshot}
atClosestVerticalWith5Nm          YES        [See Section](#snapshot) \ref{snapshot}
isLevelOffEvent                   NO         true
courseDelta                       NO         1
conflictAngle                     NO         "SAME"
aircraft_0                        NO         [See Section](#aircraft) \ref{aircraft}
aircraft_1                        NO         [See Section](#aircraft) \ref{aircraft}
schemaVersion                     NO         "3"
airborneDynamics                  NO         [See Section](#airborneDynamics) \ref{airborneDynamics}

Table: The top-level Airborne ARIA Event JSON Fields


\newpage

# Top-Level Airborne ARIA Event JSON Field Definitions

The table below provides definitions for the fields within the Airborne ARIA event JSON data.

-------------------------------------------------------------------------------------------
Top-Level Field               Definition                                                     
----------------------------- ------------------------------------------------------------- 
airborneDynamics              Contains the raw data to plot 6 different time series graphs

aircraft_0                    A collection of data describing the 1st aircraft. [See Section](#aircraft) \ref{aircraft}

aircraft_1                    A collection of data describing the 2nd aircraft. [See Section](#aircraft) \ref{aircraft}

atClosestLateral              This is an Airborne Dynamics Snapshot. [See Section](#snapshot) \ref{snapshot}

atClosestLateralWith1kVert    This is an Airborne Dynamics Snapshot. [See Section](#snapshot) \ref{snapshot}   

atClosestVerticalWith3Nm      This is an Airborne Dynamics Snapshot. [See Section](#snapshot) \ref{snapshot}

atClosestVerticalWith5Nm      This is an Airborne Dynamics Snapshot. [See Section](#snapshot) \ref{snapshot}

atEstimatedCpaTime            This is an Airborne Dynamics Snapshot. [See Section](#snapshot) \ref{snapshot}

atEventTime                   This is an Airborne Dynamics Snapshot with Separation Predictions. [See Section](#atEventTime) \ref{atEventTime}

conflictAngle                 Derived from `courseDelta`.  Will be `SAME`, `CROSSING`, `OPPOSITE`

courseDelta                   The number of degrees between the heading of the two aircraft

closestTower                  The field only appears when `isNearTower` is true.  It provides the 
                              tower's three digit code and its type.  Possible types are: `TRACON`,
                              `CTT`, `TOWER`, and `CENTER`

eventDate                     A UTC date for the event in the format "YYYY-MM-DD"

eventEpochMsTime              An unix epoch time for the event [Unix Time](https://en.wikipedia.org/wiki/Unix_time)

eventScore                    The Airborne ARIA score for this event.  Lower scores are
                              riskier than higher scores.

facility                      The NOP Data Feed from which this event was found.  Note: this
                              does **not** indicate which airspace the event occured in 

isInsideAirspace              True or False:  Is the event's (latitude, longitude, altitude)
                              location inside the source facilitie's airspace.  This field is
                              currently used to deduplicate events that are detected within 
                              multiple NOP Facility data feeds (e.g. D10 and ZFW)

isLevelOffEvent               True or False: Are **all** of these contraints met:  (1) One of the aircraft was flying level,
                              (2) the other aircraft was climbing or descending towards the aircraft flying level,
                              (3) the climbing/descending aircraft stopped short of the other aircraft and _leveled off_

isNearTower                   True if an event occurs within a Tower's _Hockey Puck_.  A tower's
                              _Hockey Puck_ is a cylinder with a 5 NM radius and a height of 
                              3,000ft.  **NOTE** These towers {DWH, FFZ, GPM, HEF, PWA} have had 
                              the height of their hockey puck's reduces to 2,000ft 

latitude                      The average latitude of the two aircraft involved in the encounter.
                              This value is computed at the `eventEpochMsTime`

longitude                     The average latitude of the two aircraft involved in the encounter.
                              This value is computed at the `eventEpochMsTime`

timeToCpaInMilliSec           The estimated number of milliseconds until the two aircraft will 
                              arrive at their closest point of approach (CPA). This is computed by
                              assuming both aircraft's current speed and direction will not change

title                         A human readable title for an event that includes Facility, 
                              callsign 1, and callsign 2.  If a callsign is not available
                              the aircraft's beacon code will be used in its place.  Example
                              "SCT--DAL335--N502SX"

schemaVersion                 A version number of the data

uniqueId                      A universally unique identifier for an event (or track)      
                              computed by ARIA and assigned at creation time.

-------------------------------------------------------------------------------------------


\newpage

## Closest Tower Field {#closestTower}

The `closestTower` field may be missing.  It only exists when `isNearTower` is `true`

Example:
```json
  "closestTower": {
    "airport": "HHR",
    "type": "TOWER"
  }
```

\newpage

## Snapshots of Airborne Dynamics Taken at Arbitrary Time {#snapshot}

A "Snapshot" is an extraction from `airborneDynamics` taken at a specific moment in time.  Snapshots are only listed when the underlying data permits.

The snapshot fields are labeled:

* atEventTime
* atEstimatedCpaTime
* atClosestLateral
* atClosestLateralWith1kVert
* atClosestVerticalWith3Nm
* atClosestVerticalWith5Nm

Snapshots are extracted when:

* The minimum (i.e. riskiest) Airborne ARIA score is recorded  (**always exists**)
* The estimated Closest Point of Approach occurs (**may not exist**)
* The Aircraft are at their closest lateral proximity  (**always exists**)
* The Aircraft are at their closest lateral proximity AND vertical separation is 1,000 ft or less (**may not exist**)
* The Aircraft are at their closest vertical proximity AND lateral separation is 3 NM or less (**may not exist**)
* The Aircraft are at their closest vertical proximity AND lateral separation is 5 NM or less (**may not exist**)



Example
```json
  "atClosestLateral": {
    "timestamp": "2021-09-19T00:01:31.829Z",
    "epochMsTime": 1632009691829,
    "score": 53.483467355409665,
    "trueVerticalFt": 802.1219135802469,
    "trueLateralNm": 1.2715920589807321,
    "angleDelta": 4,
    "vertClosureRateFtPerMin": 1157.4074074074067,
    "lateralClosureRateKt": 12.691665845160934
  }
```

\newpage

## Snapshot of Airborne Dynamics Taken at ARIA Event Time {#atEventTime}

This is the Snapshot that corresponds to the moment the Airborne ARIA event was recorded.  It is a normal Snapshot with three extra fields: `estTimeToCpaMs`, `estVerticalAtCpaFt`, and `estLateralAtCpaNm`.

Example
```json
  "atEventTime": {
    "timestamp": "2021-09-19T00:00:48.317Z",
    "epochMsTime": 1632009648317,
    "score": 48.831343719532775,
    "trueVerticalFt": 898.0120812740251,
    "trueLateralNm": 1.4581844233095556,
    "angleDelta": 1,
    "vertClosureRateFtPerMin": 658.9785831960417,
    "lateralClosureRateKt": 19.799152987005236,
    "estTimeToCpaMs": 60002,
    "estVerticalAtCpaFt": 239.01153212521035,
    "estLateralAtCpaNm": 1.2826165850825844
  }
```

\newpage

## Single Aircraft Description (`aircraft_N`) {#aircraft}

An `aircraft_N` field extracts various pieces of information about one of the aircraft involved in an Airborne ARIA event.


Example
```json
  "aircraft_0": {
    "callsign": "DAL335",
    "uniqueId": "d25684e87a4897f39d3a5937f2cc42f6",
    "altitudeInFeet": 1898,
    "climbStatus": "DESCENDING",
    "speedInKnots": 170,
    "direction": "WEST",
    "course": 264,
    "beaconcode": "2015",
    "trackId": "1972",
    "aircraftType": "A321",
    "latitude": 33.94866717856433,
    "longitude": -118.27091266022427,
    "ifrVfrStatus": "IFR",
    "climbRateInFeetPerMin": -976,
    "aircraftClass": "FIXED_WING",
    "engineType": "JET",
    "pilotSystem": "MANNED",
    "isMilitary": "FALSE"
  }
```

\newpage

## The `airborneDynamics` Field {#airborneDynamics}

The `airborneDynamics` field provides the raw data to plot 6 different time series graphs:

* ARIA Score vs time
* Vertical Separation vs time
* Lateral Separation vs time
* Estimated Time to Closest Point of Approach vs time
* Estimated Vertical Separation at predicted Closest Point of Approach vs time
* Estimated Lateral Separation at predicted Closest Point of Approach vs time


These time series graphs are valid when there is NOP data for both aircraft involved in the event.  The "shared time values" (i.e. the x-coordinate of time-series graph) is provided by `epochMsTime`.  The y-values in the graphs are provided by the other arrays


Example
```json
  "airborneDynamics": {
    "epochMsTime": [ array of numbers ],
    "trueVerticalFt": [ array of numbers ],
    "trueLateralNm": [ array of numbers ],
    "estTimeToCpaMs": [ array of numbers ],
    "estVerticalAtCpaFt": [array of numbers ],
    "estLateralAtCpaNm": [ array of numbers ],
    "score": [ array of numbers ]
  }
```
 
\newpage


# Appendix A: Sample JSON Event {.unnumbered #example-json}

Below is an example of the JSON representation of an Airborne ARIA event.  

**Notice** the `atEstimatedCpaTime` Snapshot is missing from this sample event because data for one of the aircraft was not available at the predicted CPA

-------

```json
{
  "uniqueId": "3e13ce400cd5cd05ecf01fb7d2e4dcbe",
  "facility": "SCT",
  "eventScore": 48.831343719532775,
  "eventDate": "2021-09-19",
  "eventTime": "00:00:48.317",
  "eventEpochMsTime": 1632009648317,
  "title": "SCT--DAL335--N502SX",
  "latitude": 33.9370183364953,
  "longitude": -118.27507592566856,
  "isInsideAirspace": true,
  "isNearTower": true,
  "closestTower": {
    "airport": "HHR",
    "type": "TOWER"
  },
  "timeToCpaInMilliSec": 60002,
  "atEventTime": {
    "timestamp": "2021-09-19T00:00:48.317Z",
    "epochMsTime": 1632009648317,
    "score": 48.831343719532775,
    "trueVerticalFt": 898.0120812740251,
    "trueLateralNm": 1.4581844233095556,
    "angleDelta": 1,
    "vertClosureRateFtPerMin": 658.9785831960417,
    "lateralClosureRateKt": 19.799152987005236,
    "estTimeToCpaMs": 60002,
    "estVerticalAtCpaFt": 239.01153212521035,
    "estLateralAtCpaNm": 1.2826165850825844
  },
  "atClosestLateral": {
    "timestamp": "2021-09-19T00:01:31.829Z",
    "epochMsTime": 1632009691829,
    "score": 53.483467355409665,
    "trueVerticalFt": 802.1219135802469,
    "trueLateralNm": 1.2715920589807321,
    "angleDelta": 4,
    "vertClosureRateFtPerMin": 1157.4074074074067,
    "lateralClosureRateKt": 12.691665845160934
  },
  "atClosestLateralWith1kVert": {
    "timestamp": "2021-09-19T00:01:31.829Z",
    "epochMsTime": 1632009691829,
    "score": 53.483467355409665,
    "trueVerticalFt": 802.1219135802469,
    "trueLateralNm": 1.2715920589807321,
    "angleDelta": 4,
    "vertClosureRateFtPerMin": 1157.4074074074067,
    "lateralClosureRateKt": 12.691665845160934
  },
  "atClosestVerticalWith3Nm": {
    "timestamp": "2021-09-19T00:01:31.829Z",
    "epochMsTime": 1632009691829,
    "score": 53.483467355409665,
    "trueVerticalFt": 802.1219135802469,
    "trueLateralNm": 1.2715920589807321,
    "angleDelta": 4,
    "vertClosureRateFtPerMin": 1157.4074074074067,
    "lateralClosureRateKt": 12.691665845160934
  },
  "atClosestVerticalWith5Nm": {
    "timestamp": "2021-09-19T00:01:31.829Z",
    "epochMsTime": 1632009691829,
    "score": 53.483467355409665,
    "trueVerticalFt": 802.1219135802469,
    "trueLateralNm": 1.2715920589807321,
    "angleDelta": 4,
    "vertClosureRateFtPerMin": 1157.4074074074067,
    "lateralClosureRateKt": 12.691665845160934
  },
  "isLevelOffEvent": false,
  "courseDelta": 1,
  "conflictAngle": "SAME",
  "aircraft_0": {
    "callsign": "DAL335",
    "uniqueId": "d25684e87a4897f39d3a5937f2cc42f6",
    "altitudeInFeet": 1898,
    "climbStatus": "DESCENDING",
    "speedInKnots": 170,
    "direction": "WEST",
    "course": 264,
    "beaconcode": "2015",
    "trackId": "1972",
    "aircraftType": "A321",
    "latitude": 33.94866717856433,
    "longitude": -118.27091266022427,
    "ifrVfrStatus": "IFR",
    "climbRateInFeetPerMin": -976,
    "aircraftClass": "FIXED_WING",
    "engineType": "JET",
    "pilotSystem": "MANNED",
    "isMilitary": "FALSE"
  },
  "aircraft_1": {
    "callsign": "N502SX",
    "uniqueId": "6f6906ab0fb792da80a68d772bd05659",
    "altitudeInFeet": 1000,
    "climbStatus": "DESCENDING",
    "speedInKnots": 129,
    "direction": "WEST",
    "course": 266,
    "beaconcode": "1066",
    "trackId": "1238",
    "aircraftType": "GLF5",
    "latitude": 33.92536949442626,
    "longitude": -118.27923919111285,
    "ifrVfrStatus": "IFR",
    "climbRateInFeetPerMin": -402,
    "aircraftClass": "FIXED_WING",
    "engineType": "JET",
    "pilotSystem": "MANNED",
    "isMilitary": "FALSE"
  },
  "schemaVersion": "3",
  "airborneDynamics": {
    "epochMsTime": [ array of numbers ],
    "trueVerticalFt": [ array of numbers ],
    "trueLateralNm": [ array of numbers ],
    "estTimeToCpaMs": [ array of numbers ],
    "estVerticalAtCpaFt": [array of numbers ],
    "estLateralAtCpaNm": [ array of numbers ],
    "score": [ array of numbers ]
  }
}

```

-------
