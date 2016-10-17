package com.dotmarketing.portlets.rules.conditionlet;

import com.dotmarketing.portlets.rules.model.ParameterModel;

import eu.bitwalker.useragentutils.Browser;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS_NOT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class UsersBrowserConditionletTest {
	
	private HttpServletRequest request;
    private HttpServletResponse response;
    private UsersBrowserConditionlet conditionlet = new UsersBrowserConditionlet();
    private Collection<String> browsers;
    
    @Before
    public void before () {
        // Mock the request
        request = Mockito.mock(HttpServletRequest.class);
        // Mock the response
        response = Mockito.mock(HttpServletResponse.class);

        //Mock the Browsers
        browsers = new ArrayList<>();
        browsers.add(Browser.CHROME.getName());
        browsers.add(Browser.FIREFOX.getName());
        browsers.add(Browser.SAFARI.getName());
        browsers.add(Browser.IE.getName());
    }
    
    @Test
    public void testIsComparison () {


        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS.getId()));
        parameters.put(UsersBrowserConditionlet.BROWSER_KEY, new ParameterModel(UsersBrowserConditionlet.BROWSER_KEY, "Chrome"));
        UsersBrowserConditionlet.Instance instance = conditionlet.instanceFrom(parameters);

        Mockito.when(request.getHeader("User-Agent")).thenReturn(Browser.CHROME.getName());
        assertThat("The user-agent string IS for the 'Chrome' browser.", conditionlet.evaluate(request, response, instance), is(true));


        parameters.put(UsersBrowserConditionlet.BROWSER_KEY, new ParameterModel(UsersBrowserConditionlet.BROWSER_KEY, "Edge"));
        instance = conditionlet.instanceFrom(parameters);

        Mockito.when(request.getHeader("User-Agent")).thenReturn(Browser.EDGE.getName());
        assertThat("The user-agent string IS for the 'Edge' browser.", conditionlet.evaluate(request, response, instance), is(true));
    }

    @Test
    public void testIsNotComparison () {

        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS_NOT.getId()));
        parameters.put(UsersBrowserConditionlet.BROWSER_KEY, new ParameterModel(UsersBrowserConditionlet.BROWSER_KEY, "Opera"));
        UsersBrowserConditionlet.Instance instance = conditionlet.instanceFrom(parameters);

        Mockito.when(request.getHeader("User-Agent")).thenReturn(Browser.EDGE.toString());
        assertThat("The user-agent string IS NOT for the 'Opera' browser.", conditionlet.evaluate(request, response, instance), is(true));

        Mockito.when(request.getHeader("User-Agent")).thenReturn(Browser.OPERA.toString());
        assertThat("Should be false for IS NOT and UA-string matches the condition input value", conditionlet.evaluate(request, response, instance), is(false));
    }


}
