import groovy.transform.Field

class FsbClassifier {

  //Includes all the bugs that are bundle with FindBugs by default

  static findBugsPatterns = ["XSS_REQUEST_PARAMETER_TO_SEND_ERROR",
                      "XSS_REQUEST_PARAMETER_TO_SERVLET_WRITER",
                      "HRS_REQUEST_PARAMETER_TO_HTTP_HEADER",
                      "HRS_REQUEST_PARAMETER_TO_COOKIE",
                      "DMI_CONSTANT_DB_PASSWORD",
                      "DMI_EMPTY_DB_PASSWORD",
                      "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE",
                      "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING"
  ]

  //Informational stuff that will interest Security Reviewer but will annoys the developers.
  static informationnalPatterns = ["SERVLET_PARAMETER",
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
                            "ANDROID_WEB_VIEW_JAVASCRIPT_INTERFACE",
                            "FORMAT_STRING_MANIPULATION",
                            "DESERIALIZATION_GADGET", //Prone to false positive.. therefore only in audit profile
                            "INFORMATION_EXPOSURE_THROUGH_AN_ERROR_MESSAGE",
  ]

  //All the cryptography related bugs. Usually with issues related to confidentiality or integrity of data in transit.
  static cryptoBugs = [
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
          "CIPHER_INTEGRITY",
          "SSL_CONTEXT",
          "UNENCRYPTED_SERVER_SOCKET",
          "DEFAULT_HTTP_CLIENT", //TLS 1.2 vs SSL
          "INSECURE_SMTP_SSL",
          "UNSAFE_HASH_EQUALS",
          "TDES_USAGE",
  ]

  static majorBugsAuditOnly = [ //Mostly due to their high false-positive rate
          "TRUST_BOUNDARY_VIOLATION"
  ]

  //Important bugs but that have lower chance to get full compromise of system (see critical).
  static majorBugs = [
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
          "SPRING_CSRF_PROTECTION_DISABLED",
          "SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING",
          "COOKIE_PERSISTENT",
          "PLAY_UNVALIDATED_REDIRECT",
          "SPRING_UNVALIDATED_REDIRECT",
          "PERMISSIVE_CORS",
          "LDAP_ANONYMOUS",
          "URL_REWRITING",
          "STRUTS_FILE_DISCLOSURE",
          "SPRING_FILE_DISCLOSURE",
          "HTTP_PARAMETER_POLLUTION",
          "SMTP_HEADER_INJECTION",
          "REQUESTDISPATCHER_FILE_DISCLOSURE",
          "URLCONNECTION_SSRF_FD",
          "SPRING_ENTITY_LEAK",
          "WICKET_XSS1",
          "ENTITY_LEAK",
          "ENTITY_MASS_ASSIGNMENT",
          "OVERLY_PERMISSIVE_FILE_PERMISSION",
          "MODIFICATION_AFTER_VALIDATION",
          "NORMALIZATION_AFTER_VALIDATION",
  ]

  static criticalBugs = [ //RCE or powerful function
          "COMMAND_INJECTION",
          "XXE_SAXPARSER",
          "XXE_XMLREADER",
          "XXE_DOCUMENT",
          "XXE_XMLSTREAMREADER",
          "XXE_XPATH",
          "XXE_XSLT_TRANSFORM_FACTORY",
          "XXE_DTD_TRANSFORM_FACTORY",
          "XXE_SCHEMA_FACTORY",
          "XXE_VALIDATOR",
          "SQL_INJECTION_HIBERNATE",
          "SQL_INJECTION_JDO",
          "SQL_INJECTION_JPA",
          "SQL_INJECTION",
          "SQL_INJECTION_TURBINE",
          "SQL_INJECTION_ANDROID",
          "OGNL_INJECTION",
          "LDAP_INJECTION",
          "XPATH_INJECTION",
          "XML_DECODER",
          "SCALA_SENSITIVE_DATA_EXPOSURE",
          "SCALA_PLAY_SSRF",
          "SCALA_XSS_TWIRL",
          "SCALA_XSS_MVC_API",
          "SCALA_PATH_TRAVERSAL_IN",
          "SCALA_COMMAND_INJECTION",
          "SCALA_SQL_INJECTION_SLICK",
          "SCALA_SQL_INJECTION_ANORM",
          "PREDICTABLE_RANDOM_SCALA",
          "SCRIPT_ENGINE_INJECTION",
          "SPEL_INJECTION",
          "SQL_INJECTION_SPRING_JDBC",
          "SQL_INJECTION_JDBC",
          "EL_INJECTION",
          "SEAM_LOG_INJECTION",
          "OBJECT_DESERIALIZATION",
          "MALICIOUS_XSLT",
          "JACKSON_UNSAFE_DESERIALIZATION",
          "TEMPLATE_INJECTION_VELOCITY",
          "TEMPLATE_INJECTION_FREEMARKER",
          "AWS_QUERY_INJECTION",
          "LDAP_ENTRY_POISONING",
          "BEAN_PROPERTY_INJECTION",
          "RPC_ENABLED_EXTENSIONS",
          "GROOVY_SHELL",
          "TEMPLATE_INJECTION_PEBBLE",
          "SQL_INJECTION_VERTX",
          "IMPROPER_UNICODE",
          "SAML_IGNORE_COMMENTS",
          "DANGEROUS_PERMISSION_COMBINATION",
          "POTENTIAL_XML_INJECTION",
  ]

  static majorJspBugs = ["XSS_REQUEST_PARAMETER_TO_JSP_WRITER",
          "XSS_JSP_PRINT", "JSP_JSTL_OUT"]

  static //RCE from JSP specific functions (taglibs)
  criticalJspBugs = ["JSP_INCLUDE","JSP_SPRING_EVAL","JSP_XSLT"]

  static
  criticalScalaBugs = ["SCALA_SENSITIVE_DATA_EXPOSURE",
                       "SCALA_PLAY_SSRF",
                       "SCALA_XSS_TWIRL",
                       "SCALA_XSS_MVC_API",
                       "SCALA_PATH_TRAVERSAL_IN",
                       "SCALA_COMMAND_INJECTION",
                       "SCALA_SQL_INJECTION_SLICK",
                       "SCALA_SQL_INJECTION_ANORM",
                       "PREDICTABLE_RANDOM_SCALA"]

  static exclusions = ['CUSTOM_INJECTION']

  static deprecatedRules = []

  static String getPriorityFromType(String type,String category) {
    //FSB Specific
    if (type in criticalBugs || type in criticalJspBugs || type in criticalScalaBugs) return "CRITICAL";
    if (type in majorBugs || type in cryptoBugs || type in majorJspBugs) return "MAJOR";
    if (type in informationnalPatterns) return "INFO"

    if(category.equals("SECURITY")) {
      //println("Unknown priority for "+type)
      return "MAJOR"
    }

    return null
  }

}
