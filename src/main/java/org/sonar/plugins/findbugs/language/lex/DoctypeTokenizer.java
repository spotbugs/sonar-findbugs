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

import org.sonar.plugins.findbugs.language.node.Attribute;
import org.sonar.plugins.findbugs.language.node.DirectiveNode;
import org.sonar.plugins.findbugs.language.node.Node;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.List;

class DoctypeTokenizer extends AbstractTokenizer<List<Node>> {

  public DoctypeTokenizer(String startToken, String endToken) {
    super(startToken, endToken);
  }

  @Override
  protected void addNode(List<Node> nodeList, Node node) {
    super.addNode(nodeList, node);

    parseToken((DirectiveNode) node);
  }

  private static void parseToken(DirectiveNode node) {
    String code = node.getCode();
    StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(code));
    tokenizer.quoteChar('"');
    try {
      while (tokenizer.nextToken() != StreamTokenizer.TT_EOF) {
        if (tokenizer.sval != null) {
          if (node.getNodeName() == null) {
            node.setNodeName(tokenizer.sval);
          } else {
            node.getAttributes().add(new Attribute(tokenizer.sval));
          }
        }
      }
    } catch (IOException e) {
      // ignore
    }
  }

  @Override
  Node createNode() {
    return new DirectiveNode();
  }
}
