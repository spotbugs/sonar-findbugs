import groovy.xml.MarkupBuilder;
import FsbClassifier;
import static FsbClassifier.*;
@Grapes([

    @Grab(group='com.github.spotbugs', module='spotbugs', version='3.1.0'),
    @Grab(group='com.mebigfatguy.fb-contrib', module='fb-contrib', version='7.0.5.sb'),
    @Grab(group='com.h3xstream.findsecbugs' , module='findsecbugs-plugin', version='1.7.1')]
)


FB = new Plugin(groupId: 'com.github.spotbugs', artifactId: 'spotbugs', version: '3.1.0')
CONTRIB = new Plugin(groupId: 'com.mebigfatguy.fb-contrib', artifactId: 'fb-contrib', version: '7.0.5.sb')
FSB = new Plugin(groupId: 'com.h3xstream.findsecbugs', artifactId: 'findsecbugs-plugin', version: '1.7.1')


////////////// Generate rules files

def getSonarPriority(String type,String category, String description) {
    String priority = FsbClassifier.getPriorityFromType(type,category);
    if(priority != null) return priority

    //Findbugs critical base on the type or message
    if(type.contains("IMPOSSIBLE")) {
        return "CRITICAL"
    }
    if(description =~ /will result in [\w]+Exception at runtime/ || description =~ /will always throw a [\w]+Exception/) {
        return "CRITICAL"
    }

    //Findbugs general
    if(category in ["CORRECTNESS", "PERFORMANCE", "SECURITY","MULTI-THREADING","BAD_PRACTICE"]) return "MAJOR";
    if(category in ["STYLE", "MALICIOUS_CODE", "I18N","EXPERIMENTAL"]) return "INFO"

    println("Unknown priority for "+type+" ("+category+")")
    return "INFO";
}

/**
 * Plugin definition.
 * Utility that read the messages and metadata from the plugin.
 * It expecting that the jars are already present on disk (See Grape annotation that fetch each dependency)
 */
class Plugin {
    String groupId = ""
    String artifactId = ""
    String version = ""
    private Node fbConfXml = null;

    private String getFile() {
        def homeDir = System.getProperty("user.home");
        if(!(new File(homeDir+"/.groovy")).exists()) {
            println "[WARN] Unable to find groovy cache directory. Expected \$home/.groovy";
        }
        def file = homeDir + "/.groovy/grapes/"+groupId+"/"+artifactId+"/jars/"+artifactId+"-"+version+".jar"
        //println(file)
        return file
    }

    InputStream getMessages() {
        URL urlMsg1 = new URL("jar:file:"+getFile()+"!/messages.xml")
        return urlMsg1.openStream()
    }
    InputStream getFindbugsConf() {
        URL urlMsg1 = new URL("jar:file:"+getFile()+"!/findbugs.xml")
        return urlMsg1.openStream()
    }
    String getCategory(String bugType) {
        if(fbConfXml == null)
            fbConfXml = new XmlParser().parse(getFindbugsConf())

        def bug = fbConfXml."**".BugPattern.find { node-> node.@type == bugType}
        if(bug == null) return null
        if(bug == "NOISE") return null
        return bug.@category
    }
}

String getFindBugsCategory(List<Plugin> plugins, String bugType) {
    for(plugin in plugins) {
        category = plugin.getCategory(bugType)
        if(category != null) {
            return category;
        }
    }
    return "EXPERIMENTAL"
}

/**
 *
 * @param rulesSetName Name of the rules set generate. The filename will be rules-RULESSETNAME.xml
 * @param sources Handle of stream to the messages
 * @param includedBugs Bug type to include
 * @return
 */
def writeRules(String rulesSetName,List<Plugin> plugins,List<String> includedBugs,List<String> excludedBugs = []) {


    //Output file
    File f = new File("out_sonar","rules-"+rulesSetName+".xml")
    printf("Building ruleset %s (%s)%n", rulesSetName, f.getCanonicalPath())

    //XML construction of the rules file
    def xml = new MarkupBuilder(new PrintWriter(f))
    xml.rules {
        mkp.comment "This file is auto-generated."

        def buildPattern = { pattern ->

            category = getFindBugsCategory(plugins, pattern.attribute("type"))

            if(category == "NOISE" || pattern.attribute("type") in ["TESTING", "TESTING1", "TESTING2", "TESTING3", "UNKNOWN"]) return;
            if(category == "MT_CORRECTNESS") category = "MULTI-THREADING"


            if((includedBugs.isEmpty() || includedBugs.contains(pattern.attribute("type"))) && !excludedBugs.contains(pattern.attribute("type"))) {

                rule(key: pattern.attribute("type"),
                        priority: getSonarPriority(pattern.attribute("type"),category,pattern.Details.text())) {

                    name(category.toLowerCase().capitalize().replace("_"," ") + " - " +pattern.ShortDescription.text())
                    configKey(pattern.attribute("type"))
                    description(pattern.Details.text().trim())

                    //OWASP TOP 10 2013
                    if (pattern.Details.text().toLowerCase().contains('injection') || pattern.Details.text().contains('A1-Injection')) {
                        tag("owasp-a1")
                        tag("injection")
                    }
                    if (pattern.Details.text().contains('A2-Broken_Authentication_and_Session_Management')) {
                        tag("owasp-a2")
                    }
                    if (pattern.attribute("type").contains("XSS") || pattern.Details.text().contains('A3-Cross-Site_Scripting')) {
                        tag("owasp-a3")
                    }
                    if (pattern.Details.text().contains('A4-Insecure_Direct_Object_References') || pattern.Details.text().contains('Path_Traversal')) {
                        tag("owasp-a4")
                    }
                    if (pattern.Details.text().contains('A5-Security_Misconfiguration')) {
                        tag("owasp-a5")
                    }
                    if (pattern.attribute('type').equals('HARD_CODE_PASSWORD') ||
                            pattern.attribute("type") in cryptoBugs ||
                            pattern.Details.text().contains('A6-Sensitive_Data_Exposure')) {
                        tag("owasp-a6")
                        tag("cryptography")
                    }
                    if (pattern.Details.text().contains('A7-Missing_Function_Level_Access_Control')) {
                        tag("owasp-a7")
                    }
                    if (pattern.Details.text().toLowerCase().contains('A8-Cross-Site_Request_Forgery')) {
                        tag("owasp-a8")
                    }
                    if (pattern.Details.text().toLowerCase().contains('A9-Using_Components_with_Known_Vulnerabilities')) {
                        tag("owasp-a9")
                    }
                    if (pattern.Details.text().toLowerCase().contains('A10-Unvalidated_Redirects_and_Forwards')) {
                        tag("owasp-a10")
                    }

                    //Misc tags

                    if (pattern.Details.text().toLowerCase().contains('wasc')) {
                        tag("wasc")
                    }
                    if (pattern.Details.text().toLowerCase().contains('cwe')) {
                        tag("cwe")
                    }
                    if (pattern.ShortDescription.text().toLowerCase().contains('android')) {
                        tag("android")
                    }
                    if (pattern.attribute("type").contains("JSP")) {
                        tag("jsp")
                    }

                    //Category related
                    tag(category.toLowerCase().replace("_","-"))

                    if(category in ['PERFORMANCE','CORRECTNESS','MULTI-THREADING']) {
                        tag("bug")
                    }

                }
                //name: pattern.ShortDescription.text(),
                //  'description': pattern.Details.text(),
                // 'type':pattern.attribute("type")])
            }
        }

        plugins.forEach { plugin ->
            patternsXml = new XmlParser().parse(plugin.getMessages())
            patternsXml.BugPattern.each(buildPattern);
        }
    }
}

def securityJspRules = majorJspBugs + criticalJspBugs

//FindBugs
writeRules("findbugs", [FB], [], securityJspRules)
//Find Security Bugs
writeRules("findsecbugs", [FSB], informationnalPatterns + cryptoBugs + majorBugs + criticalBugs, securityJspRules)
writeRules("jsp", [FSB,FB], securityJspRules)
//FB-contrib
writeRules("fbcontrib", [CONTRIB], [])

////////////// Generate the profile files

def writeProfile(String profileName,List<String> includedBugs,List<String> excludedBugs = []) {

    File f = new File("out_sonar","profile-"+profileName+".xml")
    printf("Building profile %s (%s)%n",profileName,f.getCanonicalPath())

    def countBugs=0;

    def xml = new MarkupBuilder(new PrintWriter(f))
    xml.FindBugsFilter {
        mkp.comment "This file is auto-generated."


        includedBugs.forEach { patternName ->

            if(!excludedBugs.contains(patternName)) {
                Match {
                    Bug(pattern: patternName)

                    countBugs++
                }
            }
        }

    }

    return countBugs
}


def getAllPatternsFromPlugin(Plugin plugin) {
    def patterns = [];

    patternsXml = new XmlParser().parse(plugin.getMessages())
    patternsXml.BugPattern.each { pattern ->

        category = getFindBugsCategory([plugin], pattern.attribute("type"))

        if (category == "EXPERIMENTAL" || category == "NOISE") return;
        //if (category == "MT_CORRECTNESS") category = "MULTI-THREADING";

        patterns << pattern.attribute("type")
    }

    return patterns;
}

totalCount = 0
writeProfile("findbugs-only", getAllPatternsFromPlugin(FB), securityJspRules)
totalCount += writeProfile("findbugs-and-fb-contrib", getAllPatternsFromPlugin(FB) + getAllPatternsFromPlugin(CONTRIB), securityJspRules)
totalCount += writeProfile("findbugs-security-audit", getAllPatternsFromPlugin(FSB) - exclusions + findBugsPatterns, securityJspRules)
writeProfile("findbugs-security-minimal", getAllPatternsFromPlugin(FSB) - informationnalPatterns - exclusions + findBugsPatterns, securityJspRules)
totalCount += writeProfile("findbugs-security-jsp", securityJspRules)


//unclassifiedBugs = getAllPatternsFromPlugin(FSB) - (informationnalPatterns + cryptoBugs + majorBugs + majorBugsAuditOnly + criticalBugs + findBugsPatterns + exclusions + criticalJspBugs + majorJspBugs)
//unclassifiedBugs.each {b -> println(b)}

println "Total bugs patterns "+totalCount
