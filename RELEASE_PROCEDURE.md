# Release procedure

When you release fixed version of SonarQube SpotBugs Plugin, please follow these procedures.

* create topic branch from `master` branch
* change version number in `pom.xml` to stable version (e.g. `1.2.3`), then commit changes
* change version number in `pom.xml` to next development SNAPSHOT version (e.g. `1.2.4-SNAPSHOT`), then commit changes
* push your topic branch and propose a pull request
* after merging your pull request, tag the commit which has stable version in `pom.xml`, and push this tag

## Release to Maven Central

When we push tag, the build result on Travis CI will be deployed to [SonaType Nexus](https://oss.sonatype.org/).
Check [Sonatype official page](http://central.sonatype.org/pages/apache-maven.html) for detail.

## Release to SonarQube plugin update center

TBU
