package com.dotmarketing.portlets.rules.conditionlet;

import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS_NOT;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
import static com.dotcms.repackage.eu.bitwalker.useragentutils.DeviceType.COMPUTER;
import static com.dotcms.repackage.eu.bitwalker.useragentutils.DeviceType.MOBILE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.dotcms.repackage.eu.bitwalker.useragentutils.Browser;
import com.dotmarketing.portlets.rules.model.ParameterModel;

public class UsersBrowserConditionletTest {
	
	private HttpServletRequest request;
    private HttpServletResponse response;
    private UsersBrowserConditionlet conditionlet = new UsersBrowserConditionlet();
    private Collection<Browser> browsers;
    
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

        Mockito.when(request.getHeader("User-Agent")).thenReturn(Collections.enumeration(browsers));
        Mockito.when(request.getHeader("User-Agent")).thenReturn(COMPUTER);

        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS.getId()));
        parameters.put(UsersBrowserConditionlet.BROWSER_KEY,
                new ParameterModel(UsersBrowserConditionlet.BROWSER_KEY, "Chrome"));

        UsersBrowserConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        // Correct, the User-Agent Browser is not the selected one
        Assert.assertTrue(conditionlet.evaluate(request, response, instance));
    }
    
    @Test
    public void testIsNotComparison () {

        Mockito.when(request.getHeader("User-Agent")).thenReturn(Collections.enumeration(browsers));
        Mockito.when(request.getHeader("User-Agent")).thenReturn(MOBILE);

        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS_NOT.getId()));
        parameters.put(UsersBrowserConditionlet.BROWSER_KEY,
                new ParameterModel(UsersBrowserConditionlet.BROWSER_KEY, "Opera"));

        UsersBrowserConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        // Correct, the User-Agent Browser is not the selected one
        Assert.assertTrue(conditionlet.evaluate(request, response, instance));
    }
	

}
