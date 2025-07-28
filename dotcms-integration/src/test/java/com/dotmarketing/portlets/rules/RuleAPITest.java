package com.dotmarketing.portlets.rules;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.rules.actionlet.RuleActionlet;
import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;

public class RuleAPITest {


  @BeforeClass
  public static void prepare() throws Exception {
    // Setting web app environment
    IntegrationTestInitService.getInstance().init();


  }


  @Test
  public void ensureConditionletsLoad() throws Exception {

    List<Conditionlet<?>> conditionlets = APILocator.getRulesAPI().findConditionlets();

    assert conditionlets.size() > 5;
    assert conditionlets.stream().anyMatch(
        c -> c.getClass().getName().equals("com.dotmarketing.portlets.rules.conditionlet.ReferringURLConditionlet"));


  }

  @Test
  public void ensureActionletsLoad() throws Exception {

    List<RuleActionlet> actionlets = APILocator.getRulesAPI().findActionlets();

    assert actionlets.size() > 0;

    assert actionlets.stream().anyMatch(
        c -> c.getClass().getName().equals("com.dotmarketing.portlets.rules.actionlet.SendRedirectActionlet"));


  }


}
