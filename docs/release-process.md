# Manually Publishing to Maven Central

This document contains notes on how to **manually** publish releases to maven central. 

Our open-source publications are intentionally left un-automated to ensure releases are always highly intentional actions.  This won't be the best choice if open-source releases are common. 

## Pre-Requisite Checklist

- [Setup GPG](https://central.sonatype.org/publish/requirements/gpg/) so you can sign your artifacts
    - [Official documentation](https://central.sonatype.org/publish/requirements/gpg/)

## The "Helper" Script
- See: [bundle-for-publishing.sh](../scripts/bundle-for-publishing.sh)
- Run the script from the project directory using the command: 
  ```
  ./scripts/bundle-for-publishing.sh
  ```

## About the Script

**The Goal:** Manually create "one publishable zip file" for each module.  These zip files will be uploaded to maven central [here](https://central.sonatype.com/)

- Official "how-to" documentation about releasing to Open-Source is [here](https://central.sonatype.org/publish/publish-portal-upload/)
    - Older, partially out-dated (but still useful) "how-to" documentation is [here](https://central.sonatype.org/publish/publish-manual/)
  - The `.zip` will contain a directory structure that mimics your local `.m2` repository
- Make build must create these resource:
    - `artifactId-X.Y.Z.jar`
    - `artifactId-X.Y.Z-source.jar`
    - `artifactId-X.Y.Z-javadoc.jar`
    - `artifactId-X.Y.Z.pom`
      - **Important:** This `.pom` must be _complete_ e.g., it **must** contain a Developer list, SCM Urls, Project Description, etc.
- Each of these artifacts needs a corresponding
  - gpg signature (`.asc` file)
  - md5 checksum (`.md5` file) 
  - sha1 checksum (`.sha1` file) 
- Sign all 4 artifacts with gpg with:
    - `gpg -ab {FILE}`
- **Action:** Generate the MD5 checksum of all 4 artifacts
    - **Run:** `md5sum {FILE} | cut -d " " -f 1 > {FILE}.md5`
- **Action:** Generate the SHA1 checksum of all 4 artifacts
    - **Run:** `sha1sum {FILE} | cut -d " " -f 1 > {FILE}.sha1`
- **Action:** Bundle all the artifacts into one .zip file.
    - The .zip requires using the standard "maven directory structure" (e.g. `org/mitre/artifactID/version`)
    - Create the directory tree, copy your file there (should look like your `.m2` repository directory)
    - create the `.zip` file 
    - Name the `.zip` whatever you like.  This is the file that gets uploaded to the portal
- Log into `https://central.sonatype.com/`
    - Select `Publish`
    - Upload your .zip
    - Wait for system to verify your upload. It will require jar, pom, sources, docs, md5 checksums, sha1 checksums, pgp signatures.
