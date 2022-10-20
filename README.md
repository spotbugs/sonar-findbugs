# SonarQube Spotbugs Plugin
[![.github/workflows/build.yml](https://github.com/spotbugs/sonar-findbugs/actions/workflows/build.yml/badge.svg)](https://github.com/spotbugs/sonar-findbugs/actions/workflows/build.yml)
![FindBugs Rules](https://img.shields.io/badge/SpotBugs_rules-909-brightgreen.svg?maxAge=2592000)
[![Coverage Status](https://sonarcloud.io/api/project_badges/measure?project=com.github.spotbugs%3Asonar-findbugs-plugin&metric=coverage)](https://sonarcloud.io/component_measures?id=com.github.spotbugs:sonar-findbugs-plugin&metric=coverage)

## Description / Features

This plugin requires the [SonarJava Plugin](https://docs.sonarqube.org/display/PLUG/SonarJava), and uses [SpotBugs](https://spotbugs.github.io), [fb-contrib](http://fb-contrib.sourceforge.net/) and [Find Security Bugs](http://h3xstream.github.io/find-sec-bugs/) to provide coding rules.

### Supported Languages

The plugin works by analysing the compiled `.class` files and reporting the issues in the corresponding source files. The currently supported JVM languages are:

- Java
- JSP (Java Server Pages)
- Scala
- Kotlin

## Usage

In the quality profile, activate some rules from Spotbugs, fb-contrib or Find Security Bugs rule repositories and run an analysis on your project.

### Configuration
This plugin can be configured with sonar web interface (see the General Settings/Languages/Java section) or with project properties.

**Allow uncompiled code** (`sonar.findbugs.allowuncompiledcode`): Remove the compiled code requirement for all projects. It can lead to a false sense of security if the build process skips certain projects.
This option might be used to get around the `One (sub)project contains Java source files that are not compiled` error.

**Confidence level** (`sonar.findbugs.confidenceLevel`): Specifies the confidence threshold (previously called "priority") for reporting issues. If set to "low", confidence is not used to filter bugs. If set to "medium" (the default), low confidence issues are supressed. If set to "high", only high confidence bugs are reported.

**Effort** (`sonar.findbugs.effort`): Effort of the bug finders. Valid values are Min, Default and Max. Setting 'Max' increases precision but also increases memory consumption.

**Excludes** (`sonar.findbugs.excludesFilters`): Paths to findbugs filter-files with exclusions.

**Timeout** (`sonar.findbugs.timeout`): Specifies the amount of time, in milliseconds, that FindBugs may run before it is assumed to be hung and is terminated. The default is 600,000 milliseconds, which is ten minutes.

**Only analyze** (`sonar.findbugs.onlyAnalyze`): Restrict analysis to a comma-separated list of classes and packages. For large projects, this may greatly reduce the amount of time needed to run the analysis. (However, some detectors may produce inaccurate results if they aren’t run on the entire application.) Classes should be specified using their full classnames (including package), and packages should be specified in the same way they would in a Java import statement to import all classes in the package (i.e., add .* to the full name of the package). Replace .* with .- to also analyze all subpackages.

### Compiled code

FindBugs requires the compiled classes to run, if the project has JSP files they will need to be precompiled.

Make sure that you compile your source code with debug information on (to get the line numbers in the Java bytecode). Debug is usually on by default unless you're compiling with Ant, in which case, you will need to turn it on explicitly. If the debug information is not available, the issues raised by FindBugs will be displayed at the beginning of the file because the correct line numbers were not available.


## Compatibility

Since version 3.0, the plugin embed FindBugs 3.0.0 which supports analysis of Java 8 bytecode but requires Java 1.7 to run (see Compatibility section). Please find below the compatibility matrix of the plugin.
Versions 4.0.3 and below are not compatible with SonarQube 9.

Findbugs Plugin version|Embedded SpotBugs/Findbugs version|Embedded Findsecbugs version|Embedded FB-Contrib version|Minimal Java version|Supported SonarQube version|Minimum sonar-java version|
-----------------------|----------------------------------|----------------------------|---------------------------|--------------------|-----------------|------------------
3.10                   | 3.1.11 (SpotBugs)                | 1.8.0                      | 7.4.3sb                   | 1.8|7.6-8.9|5.10.1.16922
3.11.0                 | 3.1.12 (SpotBugs)                | 1.8.0                      | 7.4.3sb                   | 1.8|7.6-8.9|5.10.1.16922
4.0.0                  | 4.0.0 (SpotBugs)                 | 1.10.1                     | 7.4.7 (sb-contrib)        | 1.8|7.6-8.9|5.10.1.16922
4.0.1                  | 4.1.2 (SpotBugs)                 | 1.10.1                     | 7.4.7 (sb-contrib)        | 1.8|7.9-8.9|5.10.1.16922
4.0.2                  | 4.2.0 (SpotBugs)                 | 1.11.0                     | 7.4.7 (sb-contrib)        | 1.8|7.9-8.9|5.10.1.16922
4.0.3                  | 4.2.0 (SpotBugs)                 | 1.11.0                     | 7.4.7 (sb-contrib)        | 1.8|7.9-8.9|5.10.1.16922
4.0.4                  | 4.4.0 (SpotBugs)                 | 1.11.0                     | 7.4.7 (sb-contrib)        | 1.8|7.9~|5.10.1.16922
4.0.5                  | 4.5.0 (SpotBugs)                 | 1.11.0                     | 7.4.7 (sb-contrib)        | 1.8|7.9~|5.10.1.16922
4.0.6                  | 4.5.2 (SpotBugs)                 | 1.11.0                     | 7.4.7 (sb-contrib)        | 1.8|7.9~|5.10.1.16922
4.1.4                  | 4.6.0 (SpotBugs)                 | 1.12.0                     | 7.4.7 (sb-contrib)        | 1.8|7.9~|5.10.1.16922
4.1.5                  | 4.7.0 (SpotBugs)                 | 1.12.0                     | 7.4.7 (sb-contrib)        | 1.8|7.9~|5.10.1.16922
4.1.6                  | 4.7.0 (SpotBugs)                 | 1.12.0                     | 7.4.7 (sb-contrib)        | 1.8|7.9~|5.10.1.16922
4.2.0                  | 4.7.1 (SpotBugs)                 | 1.12.0                     | 7.4.7 (sb-contrib)        | 1.8|7.9~|5.10.1.16922
4.2.1                  | 4.7.2 (SpotBugs)                 | 1.12.0                     | 7.4.7 (sb-contrib)        | 1.8|7.9~|5.10.1.16922
4.2.2-SNAPSHOT         | 4.7.3 (SpotBugs)                 | 1.12.0                     | 7.4.7 (sb-contrib)        | 1.8|7.9~|5.10.1.16922
