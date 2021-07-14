# SonarQube Spotbugs Plugin
[![.github/workflows/build.yml](https://github.com/spotbugs/sonar-findbugs/actions/workflows/build.yml/badge.svg)](https://github.com/spotbugs/sonar-findbugs/actions/workflows/build.yml)
![FindBugs Rules](https://img.shields.io/badge/SpotBugs_rules-883-brightgreen.svg?maxAge=2592000)
[![Coverage Status](https://sonarcloud.io/api/project_badges/measure?project=com.github.spotbugs%3Asonar-findbugs-plugin&metric=coverage)](https://sonarcloud.io/component_measures?id=com.github.spotbugs:sonar-findbugs-plugin&metric=coverage)

## Description / Features

This plugin requires the [SonarJava Plugin](https://docs.sonarqube.org/display/PLUG/SonarJava), and uses [SpotBugs](https://spotbugs.github.io), [fb-contrib](http://fb-contrib.sourceforge.net/) and [Find Security Bugs](http://h3xstream.github.io/find-sec-bugs/) to provide coding rules.


## Usage

In the quality profile, activate some rules from Spotbugs, fb-contrib or Find Security Bugs rule repositories and run an analysis on your project.

### Configuration
This plugin can be configured with sonar web interface (see General/Java section) or with project properties.

**Confidence level** (sonar.findbugs.confidenceLevel): Specifies the confidence threshold (previously called "priority") for reporting issues. If set to "low", confidence is not used to filter bugs. If set to "medium" (the default), low confidence issues are supressed. If set to "high", only high confidence bugs are reported.

**Effort** (sonar.findbugs.effort): Effort of the bug finders. Valid values are Min, Default and Max. Setting 'Max' increases precision but also increases memory consumption.

**Excludes** (sonar.findbugs.excludesFilters): Paths to findbugs filter-files with exclusions.

**Timeout** (sonar.findbugs.timeout): Specifies the amount of time, in milliseconds, that FindBugs may run before it is assumed to be hung and is terminated. The default is 600,000 milliseconds, which is ten minutes.


### Compiled code

FindBugs requires the compiled classes to run.

Make sure that you compile your source code with debug information on (to get the line numbers in the Java bytecode). Debug is usually on by default unless you're compiling with Ant, in which case, you will need to turn it on explicitly. If the debug information is not available, the issues raised by FindBugs will be displayed at the beginning of the file because the correct line numbers were not available.


## Compatibility

Since version 3.0, the plugin embed FindBugs 3.0.0 which supports analysis of Java 8 bytecode but requires Java 1.7 to run (see Compatibility section). Please find below the compatibility matrix of the plugin.

Findbugs Plugin version|Embedded SpotBugs/Findbugs version|Embedded Findsecbugs version|Embedded FB-Contrib version|Minimal Java version|Supported SonarQube version|Minimum sonar-java version|
-----------------------|----------------------------------|----------------------------|---------------------------|--------------------|-----------------|------------------
3.10                   | 3.1.11 (SpotBugs)                | 1.8.0                      | 7.4.3sb                   | 1.8|7.6~|5.10.1.16922
3.11.0                 | 3.1.12 (SpotBugs)                | 1.8.0                      | 7.4.3sb                   | 1.8|7.6~|5.10.1.16922
4.0.0                  | 4.0.0 (SpotBugs)                 | 1.10.1                     | 7.4.7 (sb-contrib)        | 1.8|7.6~|5.10.1.16922
4.0.1                  | 4.1.2 (SpotBugs)                 | 1.10.1                     | 7.4.7 (sb-contrib)        | 1.8|7.6~|5.10.1.16922
4.0.2                  | 4.2.0 (SpotBugs)                 | 1.11.0                     | 7.4.7 (sb-contrib)        | 1.8|7.6~|5.10.1.16922
4.0.3-SNAPSHOT         | 4.2.0 (SpotBugs)                 | 1.11.0                     | 7.4.7 (sb-contrib)        | 1.8|7.6~|5.10.1.16922
