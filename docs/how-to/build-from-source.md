
# How to Build from Source

1. Clone the repo using: `git clone git@github.com:mitre-public/open-aria.git`
2. Navigate to project directory `cd {PATH_TO_REPOSITORY}/open-aria`
3. Run: `./gradlew shadowJar`
    - This command launches a build plugin that collects all compiled code and dependencies into a single
      jar (aka _uber jar_, _shadow jar_, or _fat jar_)
4. Find the resulting _uber jar_ in:
    - `{PATH_TO_REPOSITORY}/open-aria/open-aria-deploy/build/libs`
    - The _uber jar_ will have a name like: `open-aria-{VERSION}-uber.jar` or `open-aria-uber.jar`