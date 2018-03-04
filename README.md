# SonarQube Findbugs Plugin
[![Build Status](https://travis-ci.org/spotbugs/sonar-findbugs.svg?branch=master)](https://travis-ci.org/spotbugs/sonar-findbugs)
![FindBugs Rules](https://img.shields.io/badge/SpotBugs_rules-818-brightgreen.svg?maxAge=2592000)
[![Dependency Status](https://www.versioneye.com/user/projects/5a379f7c0fb24f61fa117ac4/badge.svg?style=flat)](https://www.versioneye.com/user/projects/5a379f7c0fb24f61fa117ac4)
[![Coverage Status](https://sonarcloud.io/api/badges/measure?key=com.github.spotbugs:sonar-findbugs-plugin&metric=coverage)](https://sonarcloud.io/component_measures?id=com.github.spotbugs:sonar-findbugs-plugin&metric=coverage)

## Description / Features

This plugin requires the [Java Plugin](http://docs.sonarqube.org/display/PLUG/Java+Plugin), and uses [SpotBugs](https://spotbugs.github.io), [fb-contrib](http://fb-contrib.sourceforge.net/) and [Find Security Bugs](http://h3xstream.github.io/find-sec-bugs/) to provide coding rules.


## Usage

In the quality profile, activate some rules from the FindBugs, fb-contrib or FindSecBugs rule repositories and run an analysis on your project.


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

Findbugs Plugin version|Embedded SpotBugs/Findbugs version|Embedded Findsecbugs version|Embedded FB-Contrib version|Minimal Java version
-----------------------|----------------------------------|----------------------------|---------------------------|--------------------
2.4                    | 2.0.3                            | N/A                        | 5.2.1                     | 1.6
3.0                    | 3.0.0                            | N/A                        | 6.0.0                     | 1.7
3.2                    | 3.0.1                            | 1.3.0                      | 6.0.0                     | 1.7
3.3                    | 3.0.1                            | 1.4.2                      | 6.2.3                     | 1.7
3.4                    | 3.0.1                            | 1.4.6                      | 6.6.1                     | 1.8
3.5                    | 3.1.0 RC1 (SpotBugs)             | 1.6.0                      | 7.0.0                     | 1.8
3.6                    | 3.1.0 RC4 (SpotBugs)             | 1.6.0                      | 7.0.0                     | 1.8
3.7-SNAPSHOT           | 3.1.2 (SpotBugs)                 | 1.7.1                      | 7.2.0sb                   | 1.8
