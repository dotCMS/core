package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.LicenseTestUtil;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import eu.bitwalker.useragentutils.OperatingSystem;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.*;


public class VisitorOperatingSystemConditionletTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private VisitorOperatingSystemConditionlet conditionlet = new VisitorOperatingSystemConditionlet();

    @BeforeClass
    public static void prepare () throws Exception {
        LicenseTestUtil.getLicense();
    }

    @Before
    public void before () {
        // Mock the request
        request = Mockito.mock(HttpServletRequest.class);


        // Mock the response
        response = Mockito.mock(HttpServletResponse.class);
    }

    @Test
    public void testEvaluateIs() {
        VisitorOperatingSystemConditionlet.Instance instance = test(
                "Mozilla/5.0 (Windows NT 6.3; Trident/7.0; rv:11.0) like Gecko",
                OperatingSystem.WINDOWS_81, IS.getId());
        Assert.assertTrue(conditionlet.evaluate(request, response, instance));

        //----------------------------
        instance = test(
                "Mozilla/5.0 (Windows NT 6.1; Trident/7.0; rv:11.0) like Gecko",
                OperatingSystem.WINDOWS_7, IS.getId());

        Assert.assertTrue(conditionlet.evaluate(request, response, instance));

        //----------------------------
        instance = test(
                "Mozilla/5.0 (Windows NT 5.1; Trident/7.0; rv:11.0) like Gecko",
                OperatingSystem.WINDOWS_XP, IS.getId());
        Assert.assertTrue(conditionlet.evaluate(request, response, instance));

        //----------------------------
        instance = test(
                "Mozilla/5.0 (X11; Linux i686; rv:10.0) Gecko/20100101 Firefox/33.0",
                OperatingSystem.LINUX, IS.getId());
        Assert.assertTrue(conditionlet.evaluate(request, response, instance));

        //----------------------------
        instance = test(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:10.0) Gecko/20100101 Firefox/33.0",
                OperatingSystem.MAC_OS_X, IS.getId());
        Assert.assertTrue(conditionlet.evaluate(request, response, instance));

    }

    private VisitorOperatingSystemConditionlet.Instance test(String agentUserHeaderValue, OperatingSystem toCompare,
                                                             String comparationID) {

        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, comparationID));
        parameters.put(VisitorOperatingSystemConditionlet.OS_NAME__KEY,
                new ParameterModel(VisitorOperatingSystemConditionlet.OS_NAME__KEY, toCompare.getName()));

        Mockito.when(request.getHeader("User-Agent")).thenReturn(agentUserHeaderValue);
        return conditionlet.instanceFrom(parameters);
    }

    @Test
    public void testEvaluateIsNot() {
        VisitorOperatingSystemConditionlet.Instance instance = test(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:10.0) Gecko/20100101 Firefox/33.0",
                OperatingSystem.WINDOWS_XP, IS_NOT.getId());
        Assert.assertTrue(conditionlet.evaluate(request, response, instance));
    }
}