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
package org.sonar.plugins.findbugs.language.node;

import org.apache.commons.lang.StringUtils;

/**
 * Defines a node.
 *
 * @author Matthijs Galesloot
 * @since 1.0
 */
public abstract class Node {

  private String code;
  private int endColumnPosition;
  private int endLinePosition;
  private final NodeType nodeType;
  private int startColumnPosition;
  private int startLinePosition;

  public Node(NodeType nodeType) {
    this.nodeType = nodeType;
  }

  public String getCode() {
    return code;
  }

  public int getEndColumnPosition() {
    return endColumnPosition;
  }

  public int getEndLinePosition() {
    return endLinePosition;
  }

  public int getLinesOfCode() {
    return StringUtils.countMatches(code, "\n");
  }

  public NodeType getNodeType() {
    return nodeType;
  }

  public int getStartColumnPosition() {
    return startColumnPosition;
  }

  public int getStartLinePosition() {
    return startLinePosition;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public void setEndColumnPosition(int endColumnPosition) {
    this.endColumnPosition = endColumnPosition;
  }

  public void setEndLinePosition(int endLinePosition) {
    this.endLinePosition = endLinePosition;
  }

  public void setStartColumnPosition(int startColumnPosition) {
    this.startColumnPosition = startColumnPosition;
  }

  public void setStartLinePosition(int startLinePosition) {
    this.startLinePosition = startLinePosition;
  }

  @Override
  public String toString() {
    return code;
  }

}
