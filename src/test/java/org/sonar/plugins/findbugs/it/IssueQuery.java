/**
 * 
 */
package org.sonar.plugins.findbugs.it;

import org.sonarqube.ws.client.issues.SearchRequest;

import java.util.Arrays;

/**
 * @author gtoison
 *
 */
public class IssueQuery {

  public static IssueQuery create() {
    return new IssueQuery();
  }
  
  public SearchRequest components(String ... keys) {
    SearchRequest request = new SearchRequest();
    request.setComponentKeys(Arrays.asList(keys));
    
    return request;
  }
  
  public SearchRequest projects(String ... keys) {
    SearchRequest request = new SearchRequest();
    request.setProjects(Arrays.asList(keys));
    
    return request;
  }
  
  public SearchRequest rules(String ... keys) {
    SearchRequest request = new SearchRequest();
    request.setRules(Arrays.asList(keys));
    
    return request;
  }
}
