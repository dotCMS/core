package com.dotmarketing.osgi.ruleengine.actionlet;

import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.portlets.rules.model.RuleActionParameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.mockito.Mockito;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;

public class SendRedirectActionletTest {

    private static final String URL_KEY = "URL";

    @Test
    public void testGeneralConfiguration() throws Exception {
        SendRedirectActionlet actionlet = new SendRedirectActionlet();
        assertThat(actionlet.getI18nKey(), is("com.dotmarketing.osgi.ruleengine.actionlet.send_redirect"));
        assertThat("There is only one parameter.", actionlet.getParameters().size(), is(1));
        assertThat(actionlet.getId(), is("SendRedirectActionlet"));
    }

    @Test(dataProvider = "urlCases")
    public void testValidateParameters(SimpleUrlCase theCase) throws Exception {
        SendRedirectActionlet actionlet = new SendRedirectActionlet();
        RuleActionParameter param = new RuleActionParameter(URL_KEY, theCase.url);
        List<RuleActionParameter> list = new ArrayList<>();
        list.add(param);
        RuleAction actionInstance = new RuleAction();
        actionInstance.setParameters(list);
        Exception exception = null;
        try {
            actionlet.validateActionInstance(actionInstance);
        } catch (Exception e) {
            exception = e;
        }
        if(theCase.valid && exception != null){
            exception.printStackTrace();
        }
        assertThat(theCase.msg, exception, theCase.valid ? nullValue() : notNullValue());
    }

    @Test
    public void testExecuteAction() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);

        String url = "For performance reasons validation is not performed on execute. Any value is fine for testing.";
        RuleActionParameter param = new RuleActionParameter(URL_KEY, url);
        Map<String, RuleActionParameter> params = new HashMap<>();
        params.put(URL_KEY, param);

        SendRedirectActionlet actionlet = new SendRedirectActionlet();
        actionlet.executeAction(null, response, params);

        Mockito.verify(response).setStatus(301);
        Mockito.verify(response).setHeader("Location",url);

    }

    /**
     * Define some test cases for validating the URL. TestNG will run each of these cases as a separate test.
     * This is a great way to test a large number of allowed inputs... and also helps makes your test count look amazing.
     */
    @DataProvider(name = "urlCases")
    public Object[][] noConditionCases() {

        return new SimpleUrlCase[][]{
            {new SimpleUrlCase("Absolute url is valid", "https://www.google.com", true)},
            {new SimpleUrlCase("URL Relative to root is valid", "/some/path", true)},
            {new SimpleUrlCase("Relative URL using ../ notation is valid", "../../foo", true)},
//            {new SimpleUrlCase("Empty string is not valid", "", false)},
            {new SimpleUrlCase("'.' is not valid", ".", false)},
            {new SimpleUrlCase("Trailing \\ is not valid", "https://www.google.com\\", false)},
        };
    }

    public static class SimpleUrlCase {

        String msg;
        String url;
        boolean valid;

        public SimpleUrlCase(String msg, String url, boolean valid) {
            this.msg = msg;
            this.url = url;
            this.valid = valid;
        }
    }
}