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

import com.sonar.sslr.api.Token;

class TokenLocation {

  private final int startLine;
  private final int startCharacter;
  private final int endLine;
  private final int endCharacter;

  TokenLocation(Token token) {
    this.startLine = token.getLine();
    this.startCharacter = token.getColumn();
    final String[] lines = token.getOriginalValue().split("\r\n|\n|\r", -1);
    if (lines.length > 1) {
      this.endLine = token.getLine() + lines.length - 1;
      this.endCharacter = lines[lines.length - 1].length();
    } else {
      this.endLine = startLine;
      this.endCharacter = startCharacter + token.getOriginalValue().length();
    }
  }

  int startLine() {
    return startLine;
  }

  int startCharacter() {
    return startCharacter;
  }

  int endLine() {
    return endLine;
  }

  int endCharacter() {
    return endCharacter;
  }

}
