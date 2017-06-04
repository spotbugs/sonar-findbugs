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
package org.sonar.plugins.findbugs.language.lex;

import org.sonar.channel.Channel;
import org.sonar.channel.ChannelDispatcher;
import org.sonar.channel.CodeReader;
import org.sonar.plugins.findbugs.language.node.Node;
import org.sonar.plugins.findbugs.language.node.NodeType;
import org.sonar.plugins.findbugs.language.node.TagNode;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Lexical analysis of a web page.
 *
 * @author Matthijs Galesloot
 */
@SuppressWarnings("unchecked")
public final class PageLexer {

  /**
   * The order of the tokenizers is significant, as they are processed in this order.
   *
   * TextTokenizer must be last, it will always consume the characters until the next token arrives.
   */
  @SuppressWarnings("rawtypes")
  private static List tokenizers = Arrays.asList(
      /* HTML Comments */
      new CommentTokenizer("<!--", "-->", Boolean.TRUE),
      /* JSP Comments */
      new CommentTokenizer("<%--", "--%>", Boolean.FALSE),
      /* HTML Directive */
      new DoctypeTokenizer("<!DOCTYPE", ">"),
      /* XML Directives */
      new DirectiveTokenizer("<?", "?>"),
      /* JSP Directives */
      new DirectiveTokenizer("<%@", "%>"),
      /* JSP Expressions */
      new ExpressionTokenizer("<%", "%>"),
      /* XML and HTML Tags */
      new ElementTokenizer("<", ">"),
      /* Text (for everything else) */
      new TextTokenizer());

  /**
   * Parse a nested node.
   */
  @SuppressWarnings("rawtypes")
  public List<Node> nestedParse(CodeReader reader) {
    List<Node> nodeList = new ArrayList<>();
    for (AbstractTokenizer tokenizer : (List<AbstractTokenizer>) tokenizers) {
      if (tokenizer.consume(reader, nodeList)) {
        break;
      }
    }
    return nodeList;
  }

  /**
   * Parse the input into a list of tokens, with parent/child relations between the tokens.
   */
  public List<Node> parse(Reader reader) {

    // CodeReader reads the file stream
    CodeReader codeReader = new CodeReader(reader);

    // ArrayList collects the nodes
    List<Node> nodeList = new ArrayList<>();

    // ChannelDispatcher manages the tokenizers
    ChannelDispatcher<List<Node>> channelDispatcher = ChannelDispatcher.builder().addChannels((Channel[]) tokenizers.toArray(new Channel[tokenizers.size()])).build();
    channelDispatcher.consume(codeReader, nodeList);

    createNodeHierarchy(nodeList);

    return nodeList;
  }

  /**
   * Scan the nodes and build the hierarchy of parent and child nodes.
   */
  private static void createNodeHierarchy(List<Node> nodeList) {
    TagNode current = null;
    for (Node node : nodeList) {
      if (node.getNodeType() == NodeType.TAG) {
        TagNode element = (TagNode) node;

        // start element
        if (!element.isEndElement()) {
          element.setParent(current);
          current = element;
        }

        // end element
        if ((element.isEndElement() || element.hasEnd()) && current != null) {
          current = current.getParent();
        }
      }
    }
  }
}
