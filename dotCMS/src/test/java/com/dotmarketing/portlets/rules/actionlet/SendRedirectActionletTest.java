package com.dotmarketing.portlets.rules.actionlet;

import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

@RunWith(DataProviderRunner.class)
public class SendRedirectActionletTest {

    private static final String URL_KEY = "URL";

    @Test
    public void testGeneralConfiguration() throws Exception {
        SendRedirectActionlet actionlet = new SendRedirectActionlet();
        assertThat(actionlet.getI18nKey(), is("api.system.ruleengine.actionlet.send_redirect"));
        assertThat("There is only one parameter.", actionlet.getParameterDefinitions().size(), is(1));
        assertThat(actionlet.getId(), is("SendRedirectActionlet"));
    }

    @Test
    @UseDataProvider("urlCases")
    public void testValidateParameters(SimpleUrlCase theCase) throws Exception {
        SendRedirectActionlet actionlet = new SendRedirectActionlet();
        ParameterModel param = new ParameterModel(URL_KEY, theCase.url);
        List<ParameterModel> list = new ArrayList<>();
        list.add(param);
        RuleAction actionInstance = new RuleAction();
        actionInstance.setParameters(list);
        Exception exception = null;
        try {
            actionlet.instanceFrom(actionInstance.getParameters());
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

        String url = "//foo";
        ParameterModel param = new ParameterModel(URL_KEY, url);
        Map<String, ParameterModel> params = new HashMap<>();
        params.put(URL_KEY, param);

        SendRedirectActionlet actionlet = new SendRedirectActionlet();
        SendRedirectActionlet.Instance instance = actionlet.instanceFrom(params);
        actionlet.evaluate(null, response, instance);

        Mockito.verify(response).setStatus(301);
        Mockito.verify(response).setHeader("Location",url);

    }

    /**
     * Define some test cases for validating the URL. JUnit will run each of these cases as a separate test.
     * This is a great way to test a large number of allowed inputs... and also helps makes your test count look amazing.
     */
    @DataProvider
    public static Object[][] urlCases() {

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
