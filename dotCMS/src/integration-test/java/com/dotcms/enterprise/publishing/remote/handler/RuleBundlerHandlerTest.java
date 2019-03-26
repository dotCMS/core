package com.dotcms.enterprise.publishing.remote.handler;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.LogicalOperator;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.liferay.portal.model.User;
import java.io.File;
import java.net.URL;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/** Integration for Rule Handler */
public class RuleBundlerHandlerTest extends IntegrationTestBase {

  private static User user;

  @BeforeClass
  public static void prepare() throws Exception {

    // Setting web app environment
    IntegrationTestInitService.getInstance().init();
    LicenseTestUtil.getLicense();

    user = APILocator.getUserAPI().getSystemUser();
  }

  @Test
  public void testBundlerAndHandler_success_newCategories() throws Exception {

    final PublisherConfig publisherConfig = new PublisherConfig();
    final RuleHandler ruleHandler = new RuleHandler(publisherConfig);
    final URL fileUrl = getClass().getResource("/bundle/rules/");
    final File file = new File(fileUrl.getFile());

    LicenseTestUtil.getLicense();
    ruleHandler.handle(file);

    final Rule rule =
        APILocator.getRulesAPI().getRuleById("189eac50-4997-48ab-8250-8dac0d30adf4", user, false);
    Assert.assertNotNull(rule);
    Assert.assertEquals("Identify user from M param", rule.getName());
    Assert.assertEquals(Rule.FireOn.EVERY_PAGE, rule.getFireOn());
    Assert.assertEquals(1, rule.getPriority());
    Assert.assertTrue(rule.isEnabled());

    final List<ConditionGroup> conditionGroups = rule.getGroups();
    Assert.assertNotNull(conditionGroups);
    Assert.assertEquals(1, conditionGroups.size());

    final ConditionGroup conditionGroup = conditionGroups.get(0);
    Assert.assertNotNull(conditionGroup);

    Assert.assertEquals("310c6e20-6b2f-49ec-b81b-d0b3c9cc0bed", conditionGroup.getId());
    Assert.assertEquals(LogicalOperator.AND, conditionGroup.getOperator());
    Assert.assertEquals(1, conditionGroup.getPriority());

    final List<Condition> conditions = conditionGroup.getConditions();

    Assert.assertNotNull(conditions);
    Assert.assertEquals(1, conditions.size());

    final Condition condition = conditions.get(0);

    Assert.assertNotNull(condition);
    Assert.assertEquals("105708c0-b6a9-467b-922e-6ae4eeac5662", condition.getId());
    Assert.assertEquals("CountRulesActionlet", condition.getConditionletId());

    final List<RuleAction> ruleActions = rule.getRuleActions();
    Assert.assertNotNull(ruleActions);
    Assert.assertEquals(2, ruleActions.size());

    final RuleAction setRequestAttributeActionlet = ruleActions.get(0);
    Assert.assertEquals(
        "49e13c3c-3a00-4a6b-bae8-6f6097ed5f58", setRequestAttributeActionlet.getId());
    Assert.assertEquals(
        "SetRequestAttributeActionlet", setRequestAttributeActionlet.getActionlet());
    Assert.assertEquals(2, setRequestAttributeActionlet.getPriority());

    final RuleAction setResponseHeaderActionlet = ruleActions.get(1);
    Assert.assertEquals("a35c8dcc-b43c-46b9-8e94-391be229b6de", setResponseHeaderActionlet.getId());
    Assert.assertEquals("SetResponseHeaderActionlet", setResponseHeaderActionlet.getActionlet());
    Assert.assertEquals(1, setResponseHeaderActionlet.getPriority());
  }
}
