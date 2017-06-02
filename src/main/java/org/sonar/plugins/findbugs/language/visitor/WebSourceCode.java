/*
 * SonarSource :: Web :: Sonar Plugin
 * Copyright (c) 2010-2017 SonarSource SA and Matthijs Galesloot
 * sonarqube@googlegroups.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sonar.plugins.findbugs.language.visitor;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.measures.Metric;
//import org.sonar.plugins.findbugs.language.checks.WebIssue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WebSourceCode {

  private final InputFile inputFile;
  private final Map<Metric<Integer>, Integer> measures = new HashMap<>();
//  private final List<WebIssue> issues = new ArrayList<>();
  private Set<Integer> detailedLinesOfCode;
  private Set<Integer> detailedLinesOfComments;

  public WebSourceCode(InputFile inputFile) {
    this.inputFile = inputFile;
  }

  public InputFile inputFile() {
    return inputFile;
  }

  public void addMeasure(Metric<Integer> metric, int value) {
    measures.put(metric, value);
  }

//  public void addIssue(WebIssue issue) {
//    this.issues.add(issue);
//  }

  public Integer getMeasure(Metric metric) {
    return measures.get(metric);
  }

  public Map<Metric<Integer>, Integer> getMeasures() {
    return measures;
  }

//  public List<WebIssue> getIssues() {
//    return issues;
//  }

  @Override
  public String toString() {
    return inputFile().absolutePath();
  }

  public Set<Integer> getDetailedLinesOfCode() {
    return detailedLinesOfCode;
  }

  public void setDetailedLinesOfCode(Set<Integer> detailedLinesOfCode) {
    this.detailedLinesOfCode = detailedLinesOfCode;
  }

  public Set<Integer> getDetailedLinesOfComments() {
    return detailedLinesOfComments;
  }

  public void setDetailedLinesOfComments(Set<Integer> detailedLinesOfComments) {
    this.detailedLinesOfComments = detailedLinesOfComments;
  }
}
