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
package org.sonar.plugins.findbugs.language;

import com.sonar.sslr.api.TokenType;
import com.sonar.sslr.impl.Lexer;
import com.sonar.sslr.impl.channel.BlackHoleChannel;
import com.sonar.sslr.impl.channel.BomCharacterChannel;
import com.sonar.sslr.impl.channel.IdentifierAndKeywordChannel;
import com.sonar.sslr.impl.channel.UnknownCharacterChannel;

import java.nio.charset.Charset;

import static com.sonar.sslr.impl.channel.RegexpChannelBuilder.commentRegexp;
import static com.sonar.sslr.impl.channel.RegexpChannelBuilder.regexp;

final class JspLexer {

  private JspLexer() {
  }

  public static Lexer create(Charset charset) {
    return Lexer.builder()
      .withCharset(charset)

      .withFailIfNoChannelToConsumeOneCharacter(true)

      .withChannel(new BomCharacterChannel())
      .withChannel(new BlackHoleChannel("\\s++"))

      .withChannel(regexp(JspTokenType.DOCTYPE, "<!DOCTYPE.*>"))

      .withChannel(regexp(JspTokenType.TAG, "</?[:\\w]+>?"))
      .withChannel(regexp(JspTokenType.TAG, "/?>"))

      // JSP comment
      .withChannel(commentRegexp("<%--[\\w\\W]*?%>"))
      // HTML comment
      .withChannel(commentRegexp("<!--[\\w\\W]*?-->"))
      // C comment
      .withChannel(commentRegexp("/\\*[\\w\\W]*?\\*/"))
      // CPP comment
      .withChannel(commentRegexp("//[^\n\r]*"))

      //JSP Scriptlet
      .withChannel(regexp(JspTokenType.EXPRESSION, "<%[\\w\\W]*?%>"))
      //EL Expression
      .withChannel(regexp(JspTokenType.EXPRESSION, "\\$\\{[\\w\\W]*?\\}"))

      .withChannel(regexp(JspTokenType.ATTRIBUTE, "=[\"']{1}[\\w\\W]*?[\"']{1}"))
      .withChannel(regexp(JspTokenType.ATTRIBUTE, "=[^\\s'\"=<>`]++"))

      .withChannel(new IdentifierAndKeywordChannel("\\w++", true, new TokenType[]{}))

      .withChannel(new UnknownCharacterChannel())

      .build();
  }

}
