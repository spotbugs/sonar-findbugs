/*
 * SonarQube Findbugs Plugin
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.findbugs;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.platform.ServerFileSystem;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.XMLRuleParser;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FindbugsRuleRepositoryTest {
  private FindbugsRuleRepository repository;

  @Before
  public void setUpRuleRepository() throws URISyntaxException {
    URL extraRules = getClass().getResource("/org/sonar/plugins/findbugs/extra-rules.xml");
    ServerFileSystem serverFileSystem = mock(ServerFileSystem.class);
    when(serverFileSystem.getExtensions(eq(FindbugsRuleRepository.REPOSITORY_KEY), eq("xml"))).thenReturn(Lists.newArrayList(new File(extraRules.getPath())));
    repository = new FindbugsRuleRepository(serverFileSystem, new XMLRuleParser());
  }

  @Test
  public void testLoadRepositoryFromXml() {
    List<Rule> rules = repository.createRules();

    assertThat(rules.size()).isEqualTo(424);
    for (Rule rule : rules) {
      assertThat(rule.getKey()).isNotNull();
      assertThat(rule.getConfigKey()).isNotNull();
      assertThat(rule.getName()).isNotNull();
    }
  }

}
