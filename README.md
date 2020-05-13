# FreeMarker IDE Eclipse plugin

## Summary

*Please note: This is a maintenance fork of the discontinued JBoss Tools FreeMarker plugin, which was developed at JBoss (Red Hat), not by me.*

This is an Eclipse pluging that provides an editor for [Apache FreeMarker](https://freemarker.apache.org/) `.ftl` (and `.ftlh`, `.ftlx`, etc.) files with *syntax error markers* as you type, syntax highlighting (for the template language only), and some code completion.

If you are looking at the fork of user ddekany, then the last released version should be
[on the Eclipse Marketplace](https://marketplace.eclipse.org/content/freemarker-ide),

<a href="http://marketplace.eclipse.org/marketplace-client-intro?mpc_install=4770582" class="drag" title="Drag and drop onto your running Eclipse toolbar. Requires Eclipse Marketplace Client"><img typeof="foaf:Image" class="img-responsive" src="https://marketplace.eclipse.org/sites/all/themes/solstice/public/images/marketplace/btn-install.png" alt="Drag and drop onto your running Eclipse toolbar. Requires Eclipse Marketplace Client" /></a>

and also on [the Eclipse update site on Bintray](https://dl.bintray.com/freemarker/freemarker-ide/).

## Building

This command will run the build:

    $ mvn clean verify

If you just want to check if things compiles/builds you can run:

    $ mvn clean verify -DskipTest=true

While this project is not part of JBoss Tools anymore, this is still possibly relevant:
[How to Build JBoss Tools with Maven 3](https://github.com/jbosstools/jbosstools-devdoc/blob/master/building/how_to_build_jbosstools_4.adoc)

## Install

If the latest version is not on Eclipse Marketplace, then build it like above, and then in Eclipse
"Help" / "Install New Software..." / "Add...", and point to `jbosstools-freemarker\site\target\repository`,
then select "FreeMarker IDE".

## Develop

1. It's recommended to download and install "Eclipse for committers" instead of a regular Eclipse.
2. Install plugins needed for running the tests:
   1. `jbosstools-base`:
      - Get it from <https://github.com/jbosstools/jbosstools-base>.
      - Build it: `mvn verify -DskipTests=true`
      - In Ecpise "Install new software" from location, `jbosstools-base\site\target\repository`, and select "JBoss Tools Test Framework"
   2. `jbosstools-locus`:
      - Get it from <https://github.com/jbosstools/jbosstools-locus>.
      - Similar procedure as with jbosstools-base. Install from the `site/target/repository`, select "Mockito Plug-in"
3. Add a default "API Baseline" in Eclipse under "Window" / "Preferences".
4. Eclipse: Import `jbosstools-freemarker` as Eclipse projects, NOT as Maven project (Maven is only to build in CI or from command line)!
5. Eclipse will complain about some lines of `pom.xml`; on the same place it will offer installing the missing Maven connectors (Tycho, etc.), so do that.

Note:
On Mars, if after running JUnit tests it starts to log "AERI failed with an error." stack traces in infinite loop, this
to `eclipse.ini`: `-Dorg.eclipse.epp.logging.aeri.ui.skipReports=true`
(See also: <https://bugs.eclipse.org/bugs/show_bug.cgi?id=488868>)

## Release

Maven release plugin didn't work for some reason, and anyway we need to change OSGi versions in MANIFEST.MF-s too, so I just do this:

```
# It's assumed that the version number is already on $releaseVersion-SNAPSHOT. That was also set by these commands, if they were used for the previous release, as you will see later.
releaseVersion='1.5.303'
nextVersion='1.5.304'

# Attention: You must be in the project root directory!
find -type f -name "pom.xml" -not -path '*/target/*' -exec sed -Ei 's/('${releaseVersion//./\\.}')-SNAPSHOT/\1/g' {} \;
find -type f \( -name "MANIFEST.MF" -o -name "feature.xml" -o -name "category.xml" \) -not -path '*/target/*' -exec sed -Ei 's/('${releaseVersion//./\\.}')\.qualifier/\1/g' {} \;

git add .
git commit -m "Updated version for release"

# Build with the release version
mvn clean verify

# Upload to Bintray
version=$releaseVersion ./bintray-uploader/upload.sh

git tag -a "v${releaseVersion}" -m "Release"
git push --follow-tags

find -type f -name "pom.xml" -not -path '*/target/*' -exec sed -Ei 's/'${releaseVersion//./\\.}'/'$nextVersion'-SNAPSHOT/g' {} \;
find -type f \( -name "MANIFEST.MF" -o -name "feature.xml" -o -name "category.xml" \) -not -path '*/target/*' -exec sed -Ei 's/'${releaseVersion//./\\.}'/'$nextVersion'.qualifier/g' {} \;

git add .
git commit -m "Updated version for development"
git push
```

You should attach `site/target/freemarker.site-${releaseVersion}.zip` to the release on GitHub.

You should also add the new version on the Eclipse Marketplace, [here](https://marketplace.eclipse.org/content/freemarker-ide).


## Change log (version history)

### 15.0.305

Date of release: 2020-03-07

- Updated embedded FreeMarker to 2.3.30

### 15.0.304

Date of release: 2019-09-01

- Several built-ins and the `continue` directive were missing from the auto-completion proposals. Also ensured that the unit test that detects this is part of the test suite.
- Several context (data-model) completion proposal (Code Assist, Ctrl+Space) fixes and improvements (fixes JBIDE-23705 and more):
  - It almost never showed any Java methods (but still showed the Bean properties), as the filter logic there was broken.
    Now it shows them, except it deliberately filters out methods/properties defined in Object, and Bean property reader methods.
  - Now shows Bean properties before methods. (Templates normally want to read Bean properties, and not call methods directly.)
  - Now it discovers Bean properties and methods that were defined by Java 8 default methods (FreeMarker supports that when properly configured).
  - Didn't work for subvariables, as at the 2nd dot (like after `${user.phone.`) it lost track of the type.
  - For properties/methods with primitive return value the built-in completion proposals weren't filtered by left hand operand type.
  - When the left hand operand of a built-ins was a boolean, all built-ins were proposed, instead of just the boolean built-ins.

### 15.0.303

Date of release: 2019-08-23

- Updated embedded FreeMarker to 2.3.29
- Created P2 repo on Bintray, added shell script and description to help the process
- Created product on Eclipse Market place for this fork
