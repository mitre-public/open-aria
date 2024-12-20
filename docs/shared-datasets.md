# Benchmark Datasets

**Table of Contents:**

- [OpenARIA is Requesting Datasets](#openarias-request-for-datasets)
- [Public Datasets are Vital](#public-datasets-are-vital)
- [Benefits to Data Providers](#benefits-to-data-providers)
- [How YOUR dataset can be protected](#sharing-data-responsibly)
- [What type of data is requested](#what-type-of-data-is-requested)

------

## OpenARIA's Request for Datasets

> **The OpenARIA community is looking for contributors who can provide aircraft position datasets.**

Our community benefits greatly when aircraft position datasets are public and available to everyone. **Please contact us
if you are considering contributing a dataset.** Every dataset we collect represents an opportunity for our community to
improve our understanding of aviation risk.

## Public Datasets are Vital

The **whole community benefits** when we have shared, publicly accessible datasets.  Here are some of the benefits:

1. The community can better demonstrate _"What the input is"_ and _"What the problem is"_.
2. The community can better demonstrate how we detect important events within the datasets we have.
3. The community can demonstrate how OpenARIA can process several different data formats
4. Having shared, publicly available datasets allows the community to compare how different event detection techniques
   perform when applied to a specific input dataset.
5. Having multiple shared, publicly available datasets allow us to learn how one event detection techniques performs
   when applied to different input dataset.

## Benefits to Data Providers

**Data provides gain unique advantages** by giving a sample of their data to the community. Here are some of those
benefits:

1. The OpenARIA community can more easily help you analyze your data when you can provide samples of your data.
   - An analysis may include a specific analysis of single TrackPair or an aggregate analysis of all air traffic in a
     dataset.
2. The OpenARIA project is more likely to support data formats it has tangible examples of. So, if you
   want the OpenARIA project to add support for your data format, then please provide a compelling example of data
   in your preferred data format.
3. One way to gain new insights about your dataset is to make it available to the public for joint analysis. Publicly
   available shared data greatly facilitate building collaboration with new people.

## Sharing Data Responsibly

The OpenARIA team is committed to ensuring sharing datasets with the community is as easy and hassle-free as possible.
To this end we are happy to work with data providers who want to release _"cleaned"_ editions of their data.

Pre-publication data cleaning steps may include:

- Changing date and times.
- Removing aircraft callsigns, Aircaft GUFIs, or other identifying information
- Removing sensitive aircraft (e.g. military aircraft)

### What Type of Data is Requested?

Ideally, we are seeking:

1. **Aviation Position data**:
   - i.e., A stream of location measurements that tell you where 1 or more aircraft are over time
   - Aircraft position data is essentially 4-d trajectory data (i.e., Time, Latitude, Longitude, Altitude) that often
     comes bundled with aircraft specific data (e.g. callsign, aircraft type, destination airport)
2. **Aviation Position data encoded in different formats.** For example:
   - ADS-B
   - ASTERIX (EUROCONTROL)
   - NOP, ASDEX, and SWIM (various FAA formats)
   - Arbitrary binary formats (e.g. aircraft position data encoded in `byte[]`s)
   - Arbitrary text-based formats (e.g. aircraft position data encoded in `String`s)
   - etc
3. **Aviation Position data harvest from different types of airspaces**
   - Busy Terminal airspaces where most air traffic is commercial IFR traffic
   - Mixed airspaces where IFR and VFR traffic frequently coexist
   - Rural airspaces where most air traffic is VFR

---

#### Sidenote on Synthetic Data

In the future, the OpenARIA project may create synthetic dataset derived from the real datasets we have. These synthetic
datasets will help the OpenARIA project ensure our algorithms are robust and correctly handle rarely occurring events.

To create a synthetic data we are likely to take an exist aviation location dataset and:

- Inject noise or otherwise "fuzz" the data
- Inject rare-events
- Label the new synthetic dataset so that all users know the data was manipulated for a document reason and with a
  documented adjustment.    
