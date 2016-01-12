package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.repackage.org.junit.Assert;
import com.dotcms.repackage.org.junit.Before;
import com.dotcms.repackage.org.junit.Test;
import com.dotcms.repackage.org.mockito.Mockito;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.*;

/**
 * @author Jonathan Gamba
 *         Date: 1/12/16
 */
public class UsersBrowserLanguageConditionletTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private UsersBrowserLanguageConditionlet conditionlet = new UsersBrowserLanguageConditionlet();

    @Before
    public void before () {
        // Mock the request
        request = Mockito.mock(HttpServletRequest.class);
        // Mock the response
        response = Mockito.mock(HttpServletResponse.class);
    }

    @Test
    public void testIsComparison () {

        Mockito.when(request.getHeader(UsersBrowserLanguageConditionlet.BROWSER_LANGUAGE_HEADER)).thenReturn("en-us,en;q=0.5");

        //Correct case
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS.getId()));
        parameters.put(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY,
                new ParameterModel(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY, "en"));

        UsersBrowserLanguageConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        // Correct, the Accept-Language language is equals (Actually behaves as a Contains) to the selected language
        Assert.assertTrue(conditionlet.evaluate(request, response, instance));

        //Wrong case
        parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS.getId()));
        parameters.put(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY,
                new ParameterModel(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY, "es"));

        instance = conditionlet.instanceFrom(parameters);
        // Incorrect, the Accept-Language language is not equals (Actually behaves as a Contains) to the selected language
        Assert.assertFalse(conditionlet.evaluate(request, response, instance));

        //Test case
        parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS.getId()));
        parameters.put(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY,
                new ParameterModel(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY, "EN"));

        instance = conditionlet.instanceFrom(parameters);
        // Correct, the Accept-Language language is equals (Actually behaves as a Contains) to the selected language
        Assert.assertTrue(conditionlet.evaluate(request, response, instance));

        //Test case
        parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS.getId()));
        parameters.put(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY,
                new ParameterModel(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY, "en"));

        Mockito.when(request.getHeader(UsersBrowserLanguageConditionlet.BROWSER_LANGUAGE_HEADER)).thenReturn("EN-US,EN;Q=0.5");

        instance = conditionlet.instanceFrom(parameters);
        // Correct, the Accept-Language language is equals (Actually behaves as a Contains) to the selected language
        Assert.assertTrue(conditionlet.evaluate(request, response, instance));
    }

    @Test
    public void testIsNotComparison () {

        Mockito.when(request.getHeader(UsersBrowserLanguageConditionlet.BROWSER_LANGUAGE_HEADER)).thenReturn("es-CR,es;q=0.5");

        //Correct case
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS_NOT.getId()));
        parameters.put(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY,
                new ParameterModel(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY, "en"));

        UsersBrowserLanguageConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        // Correct, the Accept-Language language is not equals (Actually behaves as a NOT Contains) to the selected language
        Assert.assertTrue(conditionlet.evaluate(request, response, instance));

        //Wrong case
        parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS_NOT.getId()));
        parameters.put(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY,
                new ParameterModel(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY, "es"));

        instance = conditionlet.instanceFrom(parameters);
        // Incorrect, the Accept-Language language is not equals (Actually behaves as a NOT Contains) to the selected language
        Assert.assertFalse(conditionlet.evaluate(request, response, instance));
    }

    @Test ( expected = ComparisonNotSupportedException.class )
    public void testExistComparison () {

        Mockito.when(request.getHeader(UsersBrowserLanguageConditionlet.BROWSER_LANGUAGE_HEADER)).thenReturn("es-CR,es;q=0.5");

        //Correct case
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, EXISTS.getId()));
        parameters.put(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY,
                new ParameterModel(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY, "en"));

        UsersBrowserLanguageConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        conditionlet.evaluate(request, response, instance);
    }

    @Test ( expected = ComparisonNotSupportedException.class )
    public void testStartsWithComparison () {

        Mockito.when(request.getHeader(UsersBrowserLanguageConditionlet.BROWSER_LANGUAGE_HEADER)).thenReturn("es-CR,es;q=0.5");

        //Correct case
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, STARTS_WITH.getId()));
        parameters.put(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY,
                new ParameterModel(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY, "en"));

        UsersBrowserLanguageConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        conditionlet.evaluate(request, response, instance);
    }

    @Test ( expected = ComparisonNotSupportedException.class )
    public void testEndsWithComparison () {

        Mockito.when(request.getHeader(UsersBrowserLanguageConditionlet.BROWSER_LANGUAGE_HEADER)).thenReturn("es-CR,es;q=0.5");

        //Correct case
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, ENDS_WITH.getId()));
        parameters.put(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY,
                new ParameterModel(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY, "en"));

        UsersBrowserLanguageConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        conditionlet.evaluate(request, response, instance);
    }

    @Test ( expected = ComparisonNotSupportedException.class )
    public void testContainsComparison () {

        Mockito.when(request.getHeader(UsersBrowserLanguageConditionlet.BROWSER_LANGUAGE_HEADER)).thenReturn("es-CR,es;q=0.5");

        //Correct case
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, CONTAINS.getId()));
        parameters.put(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY,
                new ParameterModel(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY, "en"));

        UsersBrowserLanguageConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        conditionlet.evaluate(request, response, instance);
    }

    @Test ( expected = ComparisonNotSupportedException.class )
    public void testRegexComparison () {

        Mockito.when(request.getHeader(UsersBrowserLanguageConditionlet.BROWSER_LANGUAGE_HEADER)).thenReturn("es-CR,es;q=0.5");

        //Correct case
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, REGEX.getId()));
        parameters.put(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY,
                new ParameterModel(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY, "en"));

        UsersBrowserLanguageConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        conditionlet.evaluate(request, response, instance);
    }

    @Test ( expected = ComparisonNotSupportedException.class )
    public void testBetweenComparison () {

        Mockito.when(request.getHeader(UsersBrowserLanguageConditionlet.BROWSER_LANGUAGE_HEADER)).thenReturn("es-CR,es;q=0.5");

        //Correct case
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, BETWEEN.getId()));
        parameters.put(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY,
                new ParameterModel(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY, "en"));

        UsersBrowserLanguageConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        conditionlet.evaluate(request, response, instance);
    }

    @Test ( expected = ComparisonNotSupportedException.class )
    public void testEqualComparison () {

        Mockito.when(request.getHeader(UsersBrowserLanguageConditionlet.BROWSER_LANGUAGE_HEADER)).thenReturn("es-CR,es;q=0.5");

        //Correct case
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, EQUAL.getId()));
        parameters.put(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY,
                new ParameterModel(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY, "en"));

        UsersBrowserLanguageConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        conditionlet.evaluate(request, response, instance);
    }

    @Test ( expected = ComparisonNotSupportedException.class )
    public void testLessThanComparison () {

        Mockito.when(request.getHeader(UsersBrowserLanguageConditionlet.BROWSER_LANGUAGE_HEADER)).thenReturn("es-CR,es;q=0.5");

        //Correct case
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, LESS_THAN.getId()));
        parameters.put(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY,
                new ParameterModel(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY, "en"));

        UsersBrowserLanguageConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        conditionlet.evaluate(request, response, instance);
    }

    @Test ( expected = ComparisonNotSupportedException.class )
    public void testGreaterThanComparison () {

        Mockito.when(request.getHeader(UsersBrowserLanguageConditionlet.BROWSER_LANGUAGE_HEADER)).thenReturn("es-CR,es;q=0.5");

        //Correct case
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, GREATER_THAN.getId()));
        parameters.put(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY,
                new ParameterModel(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY, "en"));

        UsersBrowserLanguageConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        conditionlet.evaluate(request, response, instance);
    }

    @Test ( expected = ComparisonNotSupportedException.class )
    public void testLessThanOrEqualComparison () {

        Mockito.when(request.getHeader(UsersBrowserLanguageConditionlet.BROWSER_LANGUAGE_HEADER)).thenReturn("es-CR,es;q=0.5");

        //Correct case
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, LESS_THAN_OR_EQUAL.getId()));
        parameters.put(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY,
                new ParameterModel(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY, "en"));

        UsersBrowserLanguageConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        conditionlet.evaluate(request, response, instance);
    }

    @Test ( expected = ComparisonNotSupportedException.class )
    public void testGreaterThanOrEqualComparison () {

        Mockito.when(request.getHeader(UsersBrowserLanguageConditionlet.BROWSER_LANGUAGE_HEADER)).thenReturn("es-CR,es;q=0.5");

        //Correct case
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, GREATER_THAN_OR_EQUAL.getId()));
        parameters.put(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY,
                new ParameterModel(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY, "en"));

        UsersBrowserLanguageConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        conditionlet.evaluate(request, response, instance);
    }

}