#!/bin/bash

# This Script facilitates MANUALLY releasing software artifacts to Maven Central
# This Script generates .zip files we can upload to Maven Central to publish our open-source artifacts
# After running this scrip MANUALLY upload the .zip files to: https://central.sonatype.com/

# This script replaces various Gradle or CI/CD plugins that can publish to Maven Central
# This tradeoff may or may not make sense for your release cadence

# This Script:
# 1 -- Uses gradle to clean & build the project
# 2 -- Gathers the artifacts we need for a Maven Central publication (e.g., jar, sources, javadocs, pom) into a dir
# 3 -- GPG signs the project artifacts
# 4 -- Computes MD5 checksums for the project artifacts
# 5 -- Computes SHA1 checksums for the project artifacts
# 6 -- Compresses these artifacts into "MC-publishable-VERSION.zip"
# 7 -- Deletes the dir where artifacts were gathered

# PRE-REQUISITES
# -- Your project's build must produce artifacts Maven Central will accept (e.g. complete POM, sources, docs, etc)
# -- GPG must be configured on the host machine to sign an artifact with a credential Maven Central recognizes
# -- The "md5sum" command must be available
# -- The "sha1sum" command must be available


GROUP_TOP_LEVEL="org";
GROUP_SECOND_LEVEL="mitre";
PROJECT="openaria";
VERSION="0.4.0";

echo ""
echo "Bundling openaria version: $VERSION"

verifyVersion() {
  echo "Verifying version from this script \"$VERSION\" is same as the version in gradle.properties"

  if grep 'version=' gradle.properties | grep $VERSION; then
    echo "Expected version $VERSION found in gradle.properties"
  else
    echo "Expected version $VERSION NOT FOUND in gradle.properties! .... quiting"
    exit
  fi
}

verifyVersion



packageModule() {
  local MODULE_NAME="$1"

  echo ""
  echo "Packaging: $MODULE_NAME"
  # e.g. = "org/mitre/openaria/open-aria-code/VERSION/{jar, pom, sources, javadoc, ...}
  local TARGET_DIR="$GROUP_TOP_LEVEL/$GROUP_SECOND_LEVEL/$PROJECT/$MODULE_NAME/$VERSION";

  CWD=$(pwd)

  # STEP 2 -- GATHER OUR ARTIFACTS

  mkdir -p $TARGET_DIR

  # Gathers: XYZ.jar, XYZ-sources.jar, and XYZ-javadoc.jar
  cp $MODULE_NAME/build/libs/* $TARGET_DIR

  # Gathers: pom-default.xml (and renames it too!)
  cp $MODULE_NAME/build/publications/mavenJava/pom-default.xml $TARGET_DIR/$MODULE_NAME-$VERSION.pom

  cd $TARGET_DIR || exit
  files=(*)  # store filenames in an array
  for file in "${files[@]}"; do

    echo "Processing $file"

    # EXIT if the "file" we are processing does not contain the expected version
    echo $file | grep $VERSION || exit

    echo "GPG Signing $file"
    gpg -ab $file

    echo "Computing MD5 checksum of $file"
    md5sum $file | cut -d " " -f 1 > $file.md5

    echo "Computing SHA1 checksum of $file"
    sha1sum $file | cut -d " " -f 1 > $file.sha1
  done

  cd $CWD || exit

  # STEP 6 -- CREATE THE ZIP
  zip -r PUBLISHABLE-$MODULE_NAME-$VERSION.zip $GROUP_TOP_LEVEL

  # STEP 7 -- CLEAN UP
  rm -r $GROUP_TOP_LEVEL
}

packageModule "open-aria-core"
packageModule "open-aria-threading"
packageModule "open-aria-pairing"
packageModule "open-aria-kafka"
packageModule "open-aria-system"
packageModule "open-aria-airborne"


#
## Maven Central expects a .zip that contains a directory structure similar to our .m2 repository
#TARGET_DIR="$GROUP_TOP_LEVEL/$GROUP_SECOND_LEVEL/$ARTIFACT_ID/$VERSION";
#
##save the current working directory for later..
#CWD=$(pwd)
#
#
## Step 1 -- BUILD THE PROJECT
#
#./gradlew clean
#./gradlew build
#./gradlew publishToMavenLocal
#
#
## STEP 2 -- GATHER OUR ARTIFACTS
#
#mkdir -p $TARGET_DIR
#
## Gathers: XYZ.jar, XYZ-sources.jar, and XYZ-javadoc.jar
#cp build/libs/* $TARGET_DIR
## Gathers: pom-default.xml (and renames it too!)
#cp build/publications/mavenJava/pom-default.xml $TARGET_DIR/$ARTIFACT_ID-$VERSION.pom
#
#
## STEPS 3, 4, and 5 -- SIGN ARTIFACTS & COMPUTE CHECKSUMS
#
#cd $TARGET_DIR || exit
#files=(*)  # store filenames in an array
#for file in "${files[@]}"; do
#  echo "GPG Signing $file"
#  gpg -ab $file
#
#  echo "Computing MD5 checksum of $file"
#  md5sum $file | cut -d " " -f 1 > $file.md5
#
#  echo "Computing SHA1 checksum of $file"
#  sha1sum $file | cut -d " " -f 1 > $file.sha1
#done
#
#cd $CWD || exit
#
## STEP 6 -- CREATE THE ZIP
#zip -r MC-publishable-$VERSION.zip $GROUP_TOP_LEVEL
#
## STEP 7 -- CLEAN UP
#rm -r $GROUP_TOP_LEVEL