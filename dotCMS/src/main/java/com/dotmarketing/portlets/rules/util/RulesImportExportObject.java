package com.dotmarketing.portlets.rules.util;

import com.dotmarketing.portlets.rules.model.Rule;
import java.io.Serializable;
import java.util.List;

public class RulesImportExportObject implements Serializable {

  private static final long serialVersionUID = -7961507120261310327L;
  List<Rule> rules;

  public List<Rule> getRules() {
    return rules;
  }

  public void setRules(List<Rule> rules) {
    this.rules = rules;
  }
}
