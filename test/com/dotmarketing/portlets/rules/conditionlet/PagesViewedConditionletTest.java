package com.dotmarketing.portlets.rules.conditionlet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.mockito.Mockito;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.util.WebKeys;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import com.dotcms.visitor.domain.Visitor;

import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.*;

/**
 * Created by freddy on 26/01/16.
 */

public class PagesViewedConditionletTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession httpSessionMock;
    private PagesViewedConditionlet conditionlet = new PagesViewedConditionlet();

    @Before
    public void before () {
        // Mock the request
        request = Mockito.mock(HttpServletRequest.class);

        // Mock the response
        response = Mockito.mock(HttpServletResponse.class);

        //Mock the session
        httpSessionMock = Mockito.mock(HttpSession.class);
        Mockito.when(request.getSession()).thenReturn(httpSessionMock);

        //Mock Visitor
        Visitor visitor = Mockito.mock(Visitor.class);
        Mockito.when(visitor.getNumberPagesViewed()).thenReturn(3);
        Mockito.when(httpSessionMock.getAttribute(WebKeys.VISITOR)).thenReturn(visitor);

    }

    @Test
    public void testEvaluateEquals() {
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, EQUAL.getId()));
        parameters.put(PagesViewedConditionlet.NUMBER_PAGES_VIEWED_INPUT_KEY,
                new ParameterModel(PagesViewedConditionlet.NUMBER_PAGES_VIEWED_INPUT_KEY, "3"));

        PagesViewedConditionlet.Instance instance = conditionlet.instanceFrom(parameters);

        Assert.assertTrue(conditionlet.evaluate(request, response, instance));

        //---------------------------------------------------------------------------------------------------------

        parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, EQUAL.getId()));
        parameters.put(PagesViewedConditionlet.NUMBER_PAGES_VIEWED_INPUT_KEY,
                new ParameterModel(PagesViewedConditionlet.NUMBER_PAGES_VIEWED_INPUT_KEY, "2"));

        instance = conditionlet.instanceFrom(parameters);

        Assert.assertFalse(conditionlet.evaluate(request, response, instance));
    }

    @Test
    public void testEvaluateNotEquals() {
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, NOT_EQUAL.getId()));
        parameters.put(PagesViewedConditionlet.NUMBER_PAGES_VIEWED_INPUT_KEY,
                new ParameterModel(PagesViewedConditionlet.NUMBER_PAGES_VIEWED_INPUT_KEY, "2"));

        PagesViewedConditionlet.Instance instance = conditionlet.instanceFrom(parameters);

        Assert.assertTrue(conditionlet.evaluate(request, response, instance));

        //------------------------------------------------------------------------------------------------------------
        parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, NOT_EQUAL.getId()));
        parameters.put(PagesViewedConditionlet.NUMBER_PAGES_VIEWED_INPUT_KEY,
                new ParameterModel(PagesViewedConditionlet.NUMBER_PAGES_VIEWED_INPUT_KEY, "3"));

        instance = conditionlet.instanceFrom(parameters);

        Assert.assertFalse(conditionlet.evaluate(request, response, instance));
    }

    @Test
    public void testEvaluateLessThan() {
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, LESS_THAN.getId()));
        parameters.put(PagesViewedConditionlet.NUMBER_PAGES_VIEWED_INPUT_KEY,
                new ParameterModel(PagesViewedConditionlet.NUMBER_PAGES_VIEWED_INPUT_KEY, "4"));

        PagesViewedConditionlet.Instance instance = conditionlet.instanceFrom(parameters);

        Assert.assertTrue(conditionlet.evaluate(request, response, instance));

        //-------------------------------------------------------------------------------------------------------------
        parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, LESS_THAN.getId()));
        parameters.put(PagesViewedConditionlet.NUMBER_PAGES_VIEWED_INPUT_KEY,
                new ParameterModel(PagesViewedConditionlet.NUMBER_PAGES_VIEWED_INPUT_KEY, "2"));

        instance = conditionlet.instanceFrom(parameters);

        Assert.assertFalse(conditionlet.evaluate(request, response, instance));
    }

    @Test
    public void testEvaluateGreaterThan() {
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, GREATER_THAN.getId()));
        parameters.put(PagesViewedConditionlet.NUMBER_PAGES_VIEWED_INPUT_KEY,
                new ParameterModel(PagesViewedConditionlet.NUMBER_PAGES_VIEWED_INPUT_KEY, "2"));

        PagesViewedConditionlet.Instance instance = conditionlet.instanceFrom(parameters);

        Assert.assertTrue(conditionlet.evaluate(request, response, instance));

        //------------------------------------------------------------------------------------------------------------
        parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, GREATER_THAN.getId()));
        parameters.put(PagesViewedConditionlet.NUMBER_PAGES_VIEWED_INPUT_KEY,
                new ParameterModel(PagesViewedConditionlet.NUMBER_PAGES_VIEWED_INPUT_KEY, "5"));

        instance = conditionlet.instanceFrom(parameters);

        Assert.assertFalse(conditionlet.evaluate(request, response, instance));
    }

    @Test
    public void testEvaluateLessThanOrEquals() {
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, LESS_THAN_OR_EQUAL.getId()));
        parameters.put(PagesViewedConditionlet.NUMBER_PAGES_VIEWED_INPUT_KEY,
                new ParameterModel(PagesViewedConditionlet.NUMBER_PAGES_VIEWED_INPUT_KEY, "4"));

        PagesViewedConditionlet.Instance instance = conditionlet.instanceFrom(parameters);

        Assert.assertTrue(conditionlet.evaluate(request, response, instance));

        //------------------------------------------------------------------------------------------------------------
        parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, LESS_THAN_OR_EQUAL.getId()));
        parameters.put(PagesViewedConditionlet.NUMBER_PAGES_VIEWED_INPUT_KEY,
                new ParameterModel(PagesViewedConditionlet.NUMBER_PAGES_VIEWED_INPUT_KEY, "3"));

        instance = conditionlet.instanceFrom(parameters);

        Assert.assertTrue(conditionlet.evaluate(request, response, instance));

        //------------------------------------------------------------------------------------------------------------
        parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, LESS_THAN_OR_EQUAL.getId()));
        parameters.put(PagesViewedConditionlet.NUMBER_PAGES_VIEWED_INPUT_KEY,
                new ParameterModel(PagesViewedConditionlet.NUMBER_PAGES_VIEWED_INPUT_KEY, "2"));

        instance = conditionlet.instanceFrom(parameters);

        Assert.assertFalse(conditionlet.evaluate(request, response, instance));
    }

    @Test
    public void testEvaluateGreaterThanOrEquals() {
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, GREATER_THAN_OR_EQUAL.getId()));
        parameters.put(PagesViewedConditionlet.NUMBER_PAGES_VIEWED_INPUT_KEY,
                new ParameterModel(PagesViewedConditionlet.NUMBER_PAGES_VIEWED_INPUT_KEY, "2"));

        PagesViewedConditionlet.Instance instance = conditionlet.instanceFrom(parameters);

        Assert.assertTrue(conditionlet.evaluate(request, response, instance));

        //------------------------------------------------------------------------------------------------------------
        parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, GREATER_THAN_OR_EQUAL.getId()));
        parameters.put(PagesViewedConditionlet.NUMBER_PAGES_VIEWED_INPUT_KEY,
                new ParameterModel(PagesViewedConditionlet.NUMBER_PAGES_VIEWED_INPUT_KEY, "3"));

        instance = conditionlet.instanceFrom(parameters);

        Assert.assertTrue(conditionlet.evaluate(request, response, instance));

        //------------------------------------------------------------------------------------------------------------
        parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, GREATER_THAN_OR_EQUAL.getId()));
        parameters.put(PagesViewedConditionlet.NUMBER_PAGES_VIEWED_INPUT_KEY,
                new ParameterModel(PagesViewedConditionlet.NUMBER_PAGES_VIEWED_INPUT_KEY, "4"));

        instance = conditionlet.instanceFrom(parameters);

        Assert.assertFalse(conditionlet.evaluate(request, response, instance));
    }
}
