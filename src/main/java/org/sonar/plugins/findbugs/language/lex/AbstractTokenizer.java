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

import org.apache.commons.lang.ArrayUtils;
import org.sonar.channel.Channel;
import org.sonar.channel.CodeReader;
import org.sonar.channel.EndMatcher;
import org.sonar.plugins.findbugs.language.node.Node;

import java.util.List;

/**
 * AbstractTokenizer provides basic token parsing.
 *
 * @author Matthijs Galesloot
 * @since 1.0
 */
abstract class AbstractTokenizer<T extends List<Node>> extends Channel<T> {

  private final class EndTokenMatcher implements EndMatcher {

    private final CodeReader codeReader;
    private boolean quoting;
    private int nesting;

    private EndTokenMatcher(CodeReader codeReader) {
      this.codeReader = codeReader;
    }

    @Override
    public boolean match(int endFlag) {
      if (endFlag == '"') {
        quoting = !quoting;
      }
      if (!quoting) {
        boolean started = equalsIgnoreCase(codeReader.peek(startChars.length), startChars);
        if (started) {
          nesting++;
        } else {
          boolean ended = ArrayUtils.isEquals(codeReader.peek(endChars.length), endChars);
          if (ended) {
            nesting--;
            return nesting < 0;
          }
        }
      }
      return false;
    }
  }

  private final char[] endChars;

  private final char[] startChars;

  public AbstractTokenizer(String startChars, String endChars) {
    this.startChars = startChars.toCharArray();
    this.endChars = endChars.toCharArray();
  }

  protected void addNode(List<Node> nodeList, Node node) {
    nodeList.add(node);
  }

  @Override
  public boolean consume(CodeReader codeReader, T nodeList) {
    if (equalsIgnoreCase(codeReader.peek(startChars.length), startChars)) {
      Node node = createNode();
      setStartPosition(codeReader, node);

      StringBuilder stringBuilder = new StringBuilder();
      codeReader.popTo(getEndMatcher(codeReader), stringBuilder);
      for (int i = 0; i < endChars.length; i++) {
        codeReader.pop(stringBuilder);
      }
      node.setCode(stringBuilder.toString());
      setEndPosition(codeReader, node);

      addNode(nodeList, node);

      return true;
    } else {
      return false;
    }
  }

  abstract Node createNode();

  protected final void setEndPosition(CodeReader code, Node node) {
    node.setEndLinePosition(code.getLinePosition());
    node.setEndColumnPosition(code.getColumnPosition());
  }

  protected final void setStartPosition(CodeReader code, Node node) {
    node.setStartLinePosition(code.getLinePosition());
    node.setStartColumnPosition(code.getColumnPosition());
  }

  private static boolean equalsIgnoreCase(char[] a, char[] b) {
    if (a.length != b.length) {
      return false;
    }

    for (int i = 0; i < a.length; i++) {
      if (Character.toLowerCase(a[i]) != Character.toLowerCase(b[i])) {
        return false;
      }
    }

    return true;
  }

  protected EndMatcher getEndMatcher(CodeReader codeReader) {
    return new EndTokenMatcher(codeReader);
  }

}
