package com.dotmarketing.portlets.rules.actionlet;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.portlets.rules.model.RuleActionParameter;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.mockito.Mockito;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.dotmarketing.portlets.rules.actionlet.SetRequestAttributeActionlet.REQUEST_KEY;
import static com.dotmarketing.portlets.rules.actionlet.SetRequestAttributeActionlet.REQUEST_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;

public class SetRequestAttributeActionletTest {

    @Test
    public void testGeneralConfiguration() throws Exception {
        SetRequestAttributeActionlet actionlet = new SetRequestAttributeActionlet();
        assertThat(actionlet.getI18nKey(), is("api.system.ruleengine.actionlet.SetRequestAttribute"));
        assertThat("It has two parameters.", actionlet.getParameters().size(), is(2));
        assertThat(actionlet.getId(), is("SetRequestAttributeActionlet"));
    }

    @Test(dataProvider = "cases")
    public void testValidateParameters(TestCase theCase) throws Exception {
        SetRequestAttributeActionlet actionlet = new SetRequestAttributeActionlet();
        List<RuleActionParameter> list = ImmutableList.of(
            new RuleActionParameter(REQUEST_KEY, theCase.key),
            new RuleActionParameter(REQUEST_VALUE, theCase.value)
        );
        RuleAction actionInstance = new RuleAction();
        actionInstance.setParameters(list);
        Exception exception = null;
        try {
            actionlet.validateActionInstance(actionInstance);
        } catch (Exception e) {
            exception = e;
        }
        if(theCase.valid && exception != null) {
            exception.printStackTrace();
        }
        assertThat(theCase.msg, exception, theCase.valid ? nullValue() : notNullValue());
    }

    @Test
    public void testExecuteActionDoesNotValidate() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);

        String msg = "For performance reasons validation is not performed on execute. Using a null key verifies this, as null key is not allowed.";
        String keyValue = null;
        String valueValue = "Anything at all is allowed";
        Map<String, RuleActionParameter> params = ImmutableMap.of(
            REQUEST_KEY, new RuleActionParameter(REQUEST_KEY, keyValue),
            REQUEST_VALUE, new RuleActionParameter(REQUEST_VALUE, valueValue)
        );

        SetRequestAttributeActionlet actionlet = new SetRequestAttributeActionlet();
        actionlet.executeAction(request, null, params);

        Mockito.verify(request).setAttribute(keyValue, valueValue);
    }

    @Test
    public void testExecuteActionClearsWhenNullValueSent() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);

        String keyValue = "Foo";
        String valueValue = null;
        Map<String, RuleActionParameter> params = ImmutableMap.of(
            REQUEST_KEY, new RuleActionParameter(REQUEST_KEY, keyValue),
            REQUEST_VALUE, new RuleActionParameter(REQUEST_VALUE, valueValue)
        );

        SetRequestAttributeActionlet actionlet = new SetRequestAttributeActionlet();
        actionlet.executeAction(request, null, params);

        Mockito.verify(request).removeAttribute(keyValue);
    }

    /**
     * Define some test cases for validating the URL. TestNG will run each of these cases as a separate test.
     * This is a great way to test a large number of allowed inputs... and also helps makes your test count look amazing.
     */
    @DataProvider(name = "cases")
    public Object[][] noConditionCases() {

        return new TestCase[][]{
            {new TestCase("Null key is not valid", null, "anything", false)},
            {new TestCase("A single character key is valid", "a", "anything", true)},
            {new TestCase("A null value is valid", "foo", null, true)},
        };
    }

    public static class TestCase {

        String msg;
        String key;
        String value;
        boolean valid;

        public TestCase(String msg, String key, String value, boolean valid) {
            this.msg = msg;
            this.key = key;
            this.value = value;
            this.valid = valid;
        }
    }
}