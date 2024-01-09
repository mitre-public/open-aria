### Computing an Event's `uniqueId`

### Two Potential Methods
- **Method 1:** Inject the `uniqueId` field into an Aria Event's output JSON (e.g. manually edit an event's JSON)
- **Method 2:** Let `uniqueId` be a field of the Event object (e.g. `AirborneEvent`, `TrackPairSurfaceEvent`, and `CfitOutputRecord`)   

---

### Cost/Benefit

**Method 1: Injecting the `uniqueId` field** (e.g. Manually editing an event's JSON)
- **Upside:** Enables the strongest guarantee that an injected `uniqueId` always reflects the **entire JSON record**.
- **Downside:** Replaces a one-stage event publication pipeline (e.g., just serialize event) with a two-stage event publication pipeline (e.g., serialize event then manipulate serialized record by adding the `uniqueId` field)
- **Downside:** Publishing Events as avro records will be hard (because the source `class` doesn't contain a `uniqueId` field)


**Method 2: Adding a `uniqueId` field to the event record**
- **Upside:** Allows GSON tooling to directly create publishable records
- **Upside:** Allows AVRO tooling to directly create publishable records
- **Upside:** Simplifies the "emit event as JSON" pipeline
- **Downside:** Requires the "event record class" to compute its own `uniqueId` (which can be error-prone)
- **Downside:** Opens a seam where a flawed method for computing the `uniqueId` can seep in.

---

### History
**Before** October 11, 2022, Method 1 (e.g., manually editing the JSON) was preferred. \
**After** October 11, 2022, Method 2 (e.g., adding a `uniqueId` field to the record class) was preferred.

The two primary reasons for this change are: (1) We need to support avro for the back-compute effort, and (2) protecting against bugs that pop-up during code changes is less important because there is far less churn in the event record classes.

The migration from Method 1 to Method 2 has occurred for:
- Airborne ARIA on October 11, 2022
- Surface ARIA on October 11, 2022
- (todo, no blockers) CFIT ARIA

---


#### Unexpected Non-Change
Method 1 and method 2 produce the exact same `uniqueId` hash value!  This occurs because the GSON tooling ignores the null `uniqueId` when it is invoked to compute the `uniqueId` field (as the last line in the constructor)