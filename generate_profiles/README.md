# XML files generation

This set of Groovy scripts generate all the XML files for the descriptions and the profiles.
All the descriptions are already present in the `messages.xml`/`findbugs.xml` of each respective plugins.  This script convert the description to SonarQube file formats.

 
## Running the script

```
$ groovy BuildXmlFiles.groovy

Building ruleset findbugs (C:\Code\workspace-java\sonar-findbugs\generate_profiles\out_sonar\rules-findbugs.xml)
Building ruleset findsecbugs (C:\Code\workspace-java\sonar-findbugs\generate_profiles\out_sonar\rules-findsecbugs.xml)
Building ruleset jsp (C:\Code\workspace-java\sonar-findbugs\generate_profiles\out_sonar\rules-jsp.xml)
Building ruleset fbcontrib (C:\Code\workspace-java\sonar-findbugs\generate_profiles\out_sonar\rules-fbcontrib.xml)
Building profile findbugs-only (C:\Code\workspace-java\sonar-findbugs\generate_profiles\out_sonar\profile-findbugs-only.xml)
Building profile findbugs-and-fb-contrib (C:\Code\workspace-java\sonar-findbugs\generate_profiles\out_sonar\profile-findbugs-and-fb-contrib.xml)
Building profile findbugs-security-audit (C:\Code\workspace-java\sonar-findbugs\generate_profiles\out_sonar\profile-findbugs-security-audit.xml)
Building profile findbugs-security-minimal (C:\Code\workspace-java\sonar-findbugs\generate_profiles\out_sonar\profile-findbugs-security-minimal.xml)
Building profile findbugs-security-jsp (C:\Code\workspace-java\sonar-findbugs\generate_profiles\out_sonar\profile-findbugs-security-jsp.xml)
Total bugs patterns 820
```

The generated XML files can be place in `src/main/resources/org/sonar/plugins/findbugs`.
Prior committing the new rules, please review with a diff of the changes.
 - Make sure you are not removing rules by mistake.
 - Make sure you are not changing the indent of the entire files, new line to carriage return, etc.

## Update the plugin

To update the description of a specific plugin, you must edit the script at two places before running it.

```groovy
@Grapes([
    @Grab(group='com.github.spotbugs', module='spotbugs', version='3.1.0'),
    @Grab(group='com.mebigfatguy.fb-contrib', module='fb-contrib', version='7.0.0'),
    @Grab(group='com.h3xstream.findsecbugs' , module='findsecbugs-plugin', version='1.6.0')]
)
[...]
FB = new Plugin(groupId: 'com.github.spotbugs', artifactId: 'spotbugs', version: '3.1.0')
CONTRIB = new Plugin(groupId: 'com.mebigfatguy.fb-contrib', artifactId: 'fb-contrib', version: '7.0.0')
FSB = new Plugin(groupId: 'com.h3xstream.findsecbugs', artifactId: 'findsecbugs-plugin', version: '1.6.0')

```
