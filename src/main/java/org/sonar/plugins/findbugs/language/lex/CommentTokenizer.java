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
import org.sonar.channel.CodeReader;
import org.sonar.channel.EndMatcher;
import org.sonar.plugins.findbugs.language.node.CommentNode;
import org.sonar.plugins.findbugs.language.node.Node;

import java.util.List;

/**
 * Tokenizer for a HTML or JSP comment.
 *
 * @author Matthijs Galesloot
 * @since 1.0
 */
class CommentTokenizer<T extends List<Node>> extends AbstractTokenizer<T> {

  private final class EndTokenMatcher implements EndMatcher {

    private final CodeReader codeReader;

    private EndTokenMatcher(CodeReader codeReader) {
      this.codeReader = codeReader;
    }

    @Override
    public boolean match(int endFlag) {
      return ArrayUtils.isEquals(codeReader.peek(endChars.length), endChars);
    }

  }

  private final Boolean html;
  private final char[] endChars;

  public CommentTokenizer(String startToken, String endToken, Boolean html) {
    super(startToken, endToken);

    this.html = html;
    this.endChars = endToken.toCharArray();
  }

  @Override
  protected EndMatcher getEndMatcher(CodeReader codeReader) {
    return new EndTokenMatcher(codeReader);
  }

  @Override
  Node createNode() {

    CommentNode node = new CommentNode();
    node.setHtml(html);
    return node;
  }
}
