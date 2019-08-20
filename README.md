# FreeMarker IDE Eclipse plugin

## Summary

This is an Eclipse pluging that provides and editor for [Apache FreeMarker](https://freemarker.apache.org/) `.ftl` (and `.ftlh` etc.) files with code completion and syntax highlighting.

This project was originally developed as part of [JBoss Tools](http://jboss.org/tools), but as of JBoss Tools 4.5.3 JBoss has removed it.
Thus, it was forked for further maintenance.

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
