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

import com.google.common.collect.Lists;
import org.sonar.plugins.findbugs.language.node.CommentNode;
import org.sonar.plugins.findbugs.language.node.DirectiveNode;
import org.sonar.plugins.findbugs.language.node.ExpressionNode;
import org.sonar.plugins.findbugs.language.node.Node;
import org.sonar.plugins.findbugs.language.node.TagNode;
import org.sonar.plugins.findbugs.language.node.TextNode;

import java.nio.charset.Charset;
import java.util.List;

/**
 * Scans the nodes of a page and send events to the visitors.
 *
 * @author Matthijs Galesloot
 * @since 1.0
 */
public class HtmlAstScanner {

  private final List<DefaultNodeVisitor> metricVisitors;
  private final List<DefaultNodeVisitor> checkVisitors = Lists.newArrayList();

  public HtmlAstScanner(List<DefaultNodeVisitor> metricVisitors) {
    this.metricVisitors = metricVisitors;
  }

  /**
   * Add a visitor to the list of visitors.
   */
  public void addVisitor(DefaultNodeVisitor visitor) {
    checkVisitors.add(visitor);
    visitor.init();
  }

  /**
   * Scan a list of Nodes and send events to the visitors.
   */
  public void scan(List<Node> nodeList, WebSourceCode webSourceCode, Charset charset) {
    scan(nodeList, webSourceCode, charset, metricVisitors);
    scan(nodeList, webSourceCode, charset, checkVisitors);
  }

  private void scan(List<Node> nodeList, WebSourceCode webSourceCode, Charset charset, List<DefaultNodeVisitor> visitors) {
    // prepare the visitors
    for (DefaultNodeVisitor visitor : visitors) {
      visitor.setSourceCode(webSourceCode);

      if (visitor instanceof CharsetAwareVisitor) {
        ((CharsetAwareVisitor) visitor).setCharset(charset);
      }
    }

    // notify visitors for a new document
    for (DefaultNodeVisitor visitor : visitors) {
      visitor.startDocument(nodeList);
    }

    // notify the visitors for start and end of element
    for (Node node : nodeList) {
      for (DefaultNodeVisitor visitor : visitors) {
        scanElement(visitor, node);
      }
    }

    // notify visitors for end of document
    for (DefaultNodeVisitor visitor : visitors) {
      visitor.endDocument();
    }
  }

  /**
   * Scan a single element and send appropriate event: start element, end element, characters, comment, expression or directive.
   */
  private void scanElement(DefaultNodeVisitor visitor, Node node) {
    switch (node.getNodeType()) {
      case TAG:
        scanElementTag(visitor, (TagNode) node);
        break;
      case TEXT:
        visitor.characters((TextNode) node);
        break;
      case COMMENT:
        visitor.comment((CommentNode) node);
        break;
      case EXPRESSION:
        visitor.expression((ExpressionNode) node);
        break;
      case DIRECTIVE:
        visitor.directive((DirectiveNode) node);
        break;
      default:
        break;
    }
  }

  private static void scanElementTag(DefaultNodeVisitor visitor, TagNode node) {
    if (!node.isEndElement()) {
      visitor.startElement(node);
    }
    if (node.isEndElement() || node.hasEnd()) {
      visitor.endElement(node);
    }
  }

}
