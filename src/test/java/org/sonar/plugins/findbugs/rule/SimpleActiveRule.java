package org.sonar.plugins.findbugs.rule;

import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleParam;

import java.util.HashMap;
import java.util.Map;

/**
 * Use for testing purpose only. It wrap a {@link Rule} instance and expose the {@link ActiveRule} interface.
 */
public class SimpleActiveRule implements ActiveRule {

  private final RuleKey ruleKey;
  private final String severity;
  private final String internalKey;
  private final String language;
  private final String templateRuleKey;
  private final Map<String, String> params = new HashMap<>();

  public SimpleActiveRule(Rule rule) {

    this.severity = rule.getSeverity() != null ? rule.getSeverity().name() : Severity.INFO;
    this.internalKey = rule.getRepositoryKey();
    this.templateRuleKey = rule.getTemplate() != null ? rule.getTemplate().getKey() : "";
    this.ruleKey = RuleKey.of(rule.getRepositoryKey(), rule.getKey());
    this.language = rule.getLanguage();
    if (rule.getParams() != null) {
      for (RuleParam param : rule.getParams()) {
        params.put(param.getKey(), param.getDescription());
      }
    }
  }

  @Override
  public RuleKey ruleKey() {
    return ruleKey;
  }

  @Override
  public String severity() {
    return severity;
  }

  @Override
  public String language() {
    return language;
  }

  @Override
  public String param(String key) {
    return key;
  }

  @Override
  public Map<String, String> params() {
    return params;
  }

  @Override
  public String internalKey() {
    return internalKey;
  }

  @Override
  public String templateRuleKey() {
    return templateRuleKey;
  }
}
