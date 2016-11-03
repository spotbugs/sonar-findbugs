import groovy.xml.MarkupBuilder

@Grapes([
    @Grab(group='com.google.code.findbugs'  , module='findbugs', version='3.0.1'),
    @Grab(group='com.mebigfatguy.fb-contrib', module='fb-contrib', version='6.8.0'),
    @Grab(group='com.h3xstream.findsecbugs' , module='findsecbugs-plugin', version='1.5.0')]
)

//Includes all the bugs that are bundle with FindBugs by default
findBugsPatterns = ["XSS_REQUEST_PARAMETER_TO_SEND_ERROR",
                    "XSS_REQUEST_PARAMETER_TO_SERVLET_WRITER",
                    "HRS_REQUEST_PARAMETER_TO_HTTP_HEADER",
                    "HRS_REQUEST_PARAMETER_TO_COOKIE",
                    "DMI_CONSTANT_DB_PASSWORD",
                    "DMI_EMPTY_DB_PASSWORD",
                    "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE",
                    "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING",
]

//Informational stuff that will interest Security Reviewer but will annoys the developers.
informationnalPatterns = ["SERVLET_PARAMETER",
                          "SERVLET_CONTENT_TYPE",
                          "SERVLET_SERVER_NAME",
                          "SERVLET_SESSION_ID",
                          "SERVLET_QUERY_STRING",
                          "SERVLET_HEADER",
                          "SERVLET_HEADER_REFERER",
                          "SERVLET_HEADER_USER_AGENT",
                          "COOKIE_USAGE",
                          "WEAK_FILENAMEUTILS",
                          "JAXWS_ENDPOINT",
                          "JAXRS_ENDPOINT",
                          "TAPESTRY_ENDPOINT",
                          "WICKET_ENDPOINT",
                          "FILE_UPLOAD_FILENAME",
                          "STRUTS1_ENDPOINT",
                          "STRUTS2_ENDPOINT",
                          "SPRING_ENDPOINT",
                          "HTTP_RESPONSE_SPLITTING",
                          "CRLF_INJECTION_LOGS",
                          "EXTERNAL_CONFIG_CONTROL",
                          "STRUTS_FORM_VALIDATION",
                          "ESAPI_ENCRYPTOR",
                          "ANDROID_BROADCAST",
                          "ANDROID_GEOLOCATION",
                          "ANDROID_WEB_VIEW_JAVASCRIPT",
                          "ANDROID_WEB_VIEW_JAVASCRIPT_INTERFACE"]

//All the cryptography related bugs. Usually with issues related to confidentiality or integrity of data in transit.
cryptoBugs = [
        "WEAK_TRUST_MANAGER",
        "WEAK_HOSTNAME_VERIFIER",
        //"WEAK_MESSAGE_DIGEST", //Deprecated
        "WEAK_MESSAGE_DIGEST_MD5",
        "WEAK_MESSAGE_DIGEST_SHA1",
        "CUSTOM_MESSAGE_DIGEST",
        "HAZELCAST_SYMMETRIC_ENCRYPTION",
        "NULL_CIPHER",
        "UNENCRYPTED_SOCKET",
        "DES_USAGE",
        "RSA_NO_PADDING",
        "RSA_KEY_SIZE",
        "BLOWFISH_KEY_SIZE",
        "STATIC_IV",
        "ECB_MODE",
        "PADDING_ORACLE",
        "CIPHER_INTEGRITY"
]

majorBugsAuditOnly = [ //Mostly due to their high false-positive rate
        "TRUST_BOUNDARY_VIOLATION"
]

//Important bugs but that have lower chance to get full compromise of system (see critical).
majorBugs = [
        "PREDICTABLE_RANDOM",
        "PATH_TRAVERSAL_IN",
        "PATH_TRAVERSAL_OUT",
        "REDOS",
        "BAD_HEXA_CONVERSION",
        "HARD_CODE_PASSWORD",
        "HARD_CODE_KEY",
        "XSS_REQUEST_WRAPPER",
        "UNVALIDATED_REDIRECT",
        "ANDROID_EXTERNAL_FILE_ACCESS",
        "ANDROID_WORLD_WRITABLE",
        "INSECURE_COOKIE",
        "HTTPONLY_COOKIE",
        "TRUST_BOUNDARY_VIOLATION",
        "XSS_SERVLET",
]

criticalBugs = [ //RCE or powerful function
        "COMMAND_INJECTION",
        "XXE_SAXPARSER",
        "XXE_XMLREADER",
        "XXE_DOCUMENT",
        "SQL_INJECTION_HIBERNATE",
        "SQL_INJECTION_JDO",
        "SQL_INJECTION_JPA",
        "LDAP_INJECTION",
        "XPATH_INJECTION",
        "XML_DECODER",
        "SCRIPT_ENGINE_INJECTION",
        "SPEL_INJECTION",
        "SQL_INJECTION_SPRING_JDBC",
        "SQL_INJECTION_JDBC",
        "EL_INJECTION",
        "SEAM_LOG_INJECTION",
        "OBJECT_DESERIALIZATION",
        "MALICIOUS_XSLT"
]

majorJspBugs = ["XSS_REQUEST_PARAMETER_TO_JSP_WRITER",
        "XSS_JSP_PRINT", "JSP_JSTL_OUT"]

//RCE from JSP specific functions (taglibs)
criticalJspBugs = ["JSP_INCLUDE","JSP_SPRING_EVAL","JSP_XSLT"]

exclusions = ['CUSTOM_INJECTION']

deprecatedRules = ["XSS_REQUEST_PARAMETER_TO_JSP_WRITER"]

////////////// Generate rules files

def getSonarPriority(String type,String category, String description) {
    //FSB Specific
    if (type in criticalBugs || type in criticalJspBugs) return "CRITICAL";
    if (type in majorBugs || type in cryptoBugs || type in majorJspBugs) return "MAJOR";
    if (type in informationnalPatterns) return "INFO"

    //Findbugs critical base on the type or message
    if(type.contains("IMPOSSIBLE")) {
        return "CRITICAL"
    }
    if(description =~ /will result in [\w]+Exception at runtime/ || description =~ /will always throw a [\w]+Exception/) {
        return "CRITICAL"
    }

    //Findbugs general
    if(category in ["CORRECTNESS", "PERFORMANCE", "SECURITY","MULTI-THREADING","BAD_PRACTICE"]) return "MAJOR";
    if(category in ["STYLE", "MALICIOUS_CODE", "I18N"]) return "INFO"

    println("Unknown priority for "+type+" ("+category+")")
    return "INFO";
}

//Plugin definition
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
        homeDir + "/.groovy/grapes/"+groupId+"/"+artifactId+"/jars/"+artifactId+"-"+version+".jar"
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

FB = new Plugin(groupId: 'com.google.code.findbugs', artifactId: 'findbugs', version: '3.0.1')
CONTRIB = new Plugin(groupId: 'com.mebigfatguy.fb-contrib', artifactId: 'fb-contrib', version: '6.8.0')
FSB = new Plugin(groupId: 'com.h3xstream.findsecbugs', artifactId: 'findsecbugs-plugin', version: '1.5.0')

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

            if(category == "EXPERIMENTAL" || category == "NOISE") return;
            if(category == "MT_CORRECTNESS") category = "MULTI-THREADING"

            //if(rulesSetName == 'jsp') println pattern.attribute("type")

            if((includedBugs.isEmpty() || includedBugs.contains(pattern.attribute("type"))) && !excludedBugs.contains(pattern.attribute("type"))) {
                //if(rulesSetName == 'jsp') println "-INCLUDED"

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

                    if(deprecatedRules.contains(pattern.attribute("type"))) {
                        status("DEPRECATED")
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

def excludedJspRules = ["XSS_REQUEST_PARAMETER_TO_JSP_WRITER"];

//FindBugs
writeRules("findbugs", [FB], [], excludedJspRules)
//Find Security Bugs
writeRules("findsecbugs", [FSB], informationnalPatterns + cryptoBugs + majorBugs + criticalBugs)
writeRules("jsp", [FSB,FB], majorJspBugs + criticalJspBugs)
//FB-contrib
writeRules("fbcontrib", [CONTRIB], [])

////////////// Generate the profile files

def writeProfile(String profileName,List<String> includedBugs,List<String> excludedBugs = []) {

    File f = new File("out_sonar","profile-"+profileName+".xml")
    printf("Building profile %s (%s)%n",profileName,f.getCanonicalPath())



    def xml = new MarkupBuilder(new PrintWriter(f))
    xml.FindBugsFilter {
        mkp.comment "This file is auto-generated."


        includedBugs.forEach { patternName ->

            if(!excludedBugs.contains(patternName)) {
                Match {
                    Bug(pattern: patternName)
                }
            }
        }

    }
}


def getAllPatternsFromPlugin(Plugin plugin) {
    def patterns = [];

    patternsXml = new XmlParser().parse(plugin.getMessages())
    patternsXml.BugPattern.each { pattern ->

        category = getFindBugsCategory([plugin], pattern.attribute("type"))

        if (category == "EXPERIMENTAL" || category == "NOISE") return;
        //if (category == "MT_CORRECTNESS") category = "MULTI-THREADING";

        patterns << pattern.attribute("type");
    }

    return patterns;
}


writeProfile("findbugs-only", getAllPatternsFromPlugin(FB), excludedJspRules);
writeProfile("findbugs-and-fb-contrib", getAllPatternsFromPlugin(FB) + getAllPatternsFromPlugin(CONTRIB), excludedJspRules);
writeProfile("findbugs-security-audit", informationnalPatterns + cryptoBugs + majorBugs + majorBugsAuditOnly + criticalBugs + findBugsPatterns)
writeProfile("findbugs-security-minimal", cryptoBugs + majorBugs + criticalBugs + findBugsPatterns)
writeProfile("findbugs-security-jsp", majorJspBugs + criticalJspBugs)
