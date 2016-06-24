package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.LicenseTestUtil;
import com.dotmarketing.business.web.LanguageWebAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.util.WebKeys;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.*;

/**
 * @author Jonathan Gamba
 *         Date: 1/14/16
 */
public class CurrentSessionLanguageConditionletTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private LanguageWebAPI languageAPI;
    private CurrentSessionLanguageConditionlet conditionlet;

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
        //Mock the session
        session = Mockito.mock(HttpSession.class);
        // Mock Language API
        languageAPI = Mockito.mock(LanguageWebAPI.class);

        conditionlet = new CurrentSessionLanguageConditionlet(languageAPI);
    }

    @Test
    public void testIsComparison () {

        Language english = new Language();
        english.setLanguageCode("en");
        Mockito.when(languageAPI.getLanguage(request)).thenReturn(english);

        //++++++++++++++++++++++++++++++
        //Correct case
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS.getId()));
        parameters.put(CurrentSessionLanguageConditionlet.LANGUAGE_KEY,
                new ParameterModel(CurrentSessionLanguageConditionlet.LANGUAGE_KEY, "en"));

        CurrentSessionLanguageConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        // Correct, the language in session is equals to the selected language
        Assert.assertTrue(conditionlet.evaluate(request, response, instance));

        //++++++++++++++++++++++++++++++
        //Wrong case
        parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS.getId()));
        parameters.put(CurrentSessionLanguageConditionlet.LANGUAGE_KEY,
                new ParameterModel(CurrentSessionLanguageConditionlet.LANGUAGE_KEY, "nso"));

        instance = conditionlet.instanceFrom(parameters);
        // Correct, the language in session is equals to the selected language
        Assert.assertFalse(conditionlet.evaluate(request, response, instance));

        //++++++++++++++++++++++++++++++
        //Wrong case
        parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS.getId()));
        parameters.put(CurrentSessionLanguageConditionlet.LANGUAGE_KEY,
                new ParameterModel(CurrentSessionLanguageConditionlet.LANGUAGE_KEY, "xh"));

        instance = conditionlet.instanceFrom(parameters);
        // Correct, the language in session is equals to the selected language
        Assert.assertFalse(conditionlet.evaluate(request, response, instance));

        //++++++++++++++++++++++++++++++
        //Wrong case
        parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS.getId()));
        parameters.put(CurrentSessionLanguageConditionlet.LANGUAGE_KEY,
                new ParameterModel(CurrentSessionLanguageConditionlet.LANGUAGE_KEY, "es"));

        instance = conditionlet.instanceFrom(parameters);
        // Incorrect, the language in session is not equals to the selected language
        Assert.assertFalse(conditionlet.evaluate(request, response, instance));

        //++++++++++++++++++++++++++++++
        //Test case
        parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS.getId()));
        parameters.put(CurrentSessionLanguageConditionlet.LANGUAGE_KEY,
                new ParameterModel(CurrentSessionLanguageConditionlet.LANGUAGE_KEY, "EN"));

        instance = conditionlet.instanceFrom(parameters);
        // Correct, the language in session is equals to the selected language
        Assert.assertTrue(conditionlet.evaluate(request, response, instance));
    }

    @Test
    public void testIsNotComparison () {

        Language spanish = new Language();
        spanish.setLanguageCode("es");
        Mockito.when(languageAPI.getLanguage(request)).thenReturn(spanish);

        //++++++++++++++++++++++++++++++
        //Correct case
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS_NOT.getId()));
        parameters.put(CurrentSessionLanguageConditionlet.LANGUAGE_KEY,
                new ParameterModel(CurrentSessionLanguageConditionlet.LANGUAGE_KEY, "en"));

        CurrentSessionLanguageConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        // Correct, the language in session is not equals to the selected language
        Assert.assertTrue(conditionlet.evaluate(request, response, instance));

        //++++++++++++++++++++++++++++++
        //Wrong case
        parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS_NOT.getId()));
        parameters.put(CurrentSessionLanguageConditionlet.LANGUAGE_KEY,
                new ParameterModel(CurrentSessionLanguageConditionlet.LANGUAGE_KEY, "es"));

        instance = conditionlet.instanceFrom(parameters);
        // Incorrect, the language in session is not equals to the selected language
        Assert.assertFalse(conditionlet.evaluate(request, response, instance));
    }

    @Test ( expected = ComparisonNotSupportedException.class )
    public void testExistComparison () {

        //Mock the request language id
        Mockito.when(request.getParameter("language_id")).thenReturn("2");
        Mockito.when(request.getAttribute(WebKeys.HTMLPAGE_LANGUAGE)).thenReturn("2");
        Mockito.when(request.getSession(true)).thenReturn(session);

        //Correct case
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, EXISTS.getId()));
        parameters.put(CurrentSessionLanguageConditionlet.LANGUAGE_KEY,
                new ParameterModel(CurrentSessionLanguageConditionlet.LANGUAGE_KEY, "en"));

        CurrentSessionLanguageConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        conditionlet.evaluate(request, response, instance);
    }

    @Test ( expected = ComparisonNotSupportedException.class )
    public void testStartsWithComparison () {

        //Mock the request language id
        Mockito.when(request.getParameter("language_id")).thenReturn("2");
        Mockito.when(request.getAttribute(WebKeys.HTMLPAGE_LANGUAGE)).thenReturn("2");
        Mockito.when(request.getSession(true)).thenReturn(session);

        //Correct case
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, STARTS_WITH.getId()));
        parameters.put(CurrentSessionLanguageConditionlet.LANGUAGE_KEY,
                new ParameterModel(CurrentSessionLanguageConditionlet.LANGUAGE_KEY, "en"));

        CurrentSessionLanguageConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        conditionlet.evaluate(request, response, instance);
    }

    @Test ( expected = ComparisonNotSupportedException.class )
    public void testEndsWithComparison () {

        //Mock the request language id
        Mockito.when(request.getParameter("language_id")).thenReturn("2");
        Mockito.when(request.getAttribute(WebKeys.HTMLPAGE_LANGUAGE)).thenReturn("2");
        Mockito.when(request.getSession(true)).thenReturn(session);

        //Correct case
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, ENDS_WITH.getId()));
        parameters.put(CurrentSessionLanguageConditionlet.LANGUAGE_KEY,
                new ParameterModel(CurrentSessionLanguageConditionlet.LANGUAGE_KEY, "en"));

        CurrentSessionLanguageConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        conditionlet.evaluate(request, response, instance);
    }

    @Test ( expected = ComparisonNotSupportedException.class )
    public void testContainsComparison () {

        //Mock the request language id
        Mockito.when(request.getParameter("language_id")).thenReturn("2");
        Mockito.when(request.getAttribute(WebKeys.HTMLPAGE_LANGUAGE)).thenReturn("2");
        Mockito.when(request.getSession(true)).thenReturn(session);

        //Correct case
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, CONTAINS.getId()));
        parameters.put(CurrentSessionLanguageConditionlet.LANGUAGE_KEY,
                new ParameterModel(CurrentSessionLanguageConditionlet.LANGUAGE_KEY, "en"));

        CurrentSessionLanguageConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        conditionlet.evaluate(request, response, instance);
    }

    @Test ( expected = ComparisonNotSupportedException.class )
    public void testRegexComparison () {

        //Mock the request language id
        Mockito.when(request.getParameter("language_id")).thenReturn("2");
        Mockito.when(request.getAttribute(WebKeys.HTMLPAGE_LANGUAGE)).thenReturn("2");
        Mockito.when(request.getSession(true)).thenReturn(session);

        //Correct case
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, REGEX.getId()));
        parameters.put(CurrentSessionLanguageConditionlet.LANGUAGE_KEY,
                new ParameterModel(CurrentSessionLanguageConditionlet.LANGUAGE_KEY, "en"));

        CurrentSessionLanguageConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        conditionlet.evaluate(request, response, instance);
    }

    @Test ( expected = ComparisonNotSupportedException.class )
    public void testBetweenComparison () {

        //Mock the request language id
        Mockito.when(request.getParameter("language_id")).thenReturn("2");
        Mockito.when(request.getAttribute(WebKeys.HTMLPAGE_LANGUAGE)).thenReturn("2");
        Mockito.when(request.getSession(true)).thenReturn(session);

        //Correct case
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, BETWEEN.getId()));
        parameters.put(CurrentSessionLanguageConditionlet.LANGUAGE_KEY,
                new ParameterModel(CurrentSessionLanguageConditionlet.LANGUAGE_KEY, "en"));

        CurrentSessionLanguageConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        conditionlet.evaluate(request, response, instance);
    }

    @Test ( expected = ComparisonNotSupportedException.class )
    public void testEqualComparison () {

        //Mock the request language id
        Mockito.when(request.getParameter("language_id")).thenReturn("2");
        Mockito.when(request.getAttribute(WebKeys.HTMLPAGE_LANGUAGE)).thenReturn("2");
        Mockito.when(request.getSession(true)).thenReturn(session);

        //Correct case
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, EQUAL.getId()));
        parameters.put(CurrentSessionLanguageConditionlet.LANGUAGE_KEY,
                new ParameterModel(CurrentSessionLanguageConditionlet.LANGUAGE_KEY, "en"));

        CurrentSessionLanguageConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        conditionlet.evaluate(request, response, instance);
    }

    @Test ( expected = ComparisonNotSupportedException.class )
    public void testLessThanComparison () {

        //Mock the request language id
        Mockito.when(request.getParameter("language_id")).thenReturn("2");
        Mockito.when(request.getAttribute(WebKeys.HTMLPAGE_LANGUAGE)).thenReturn("2");
        Mockito.when(request.getSession(true)).thenReturn(session);

        //Correct case
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, LESS_THAN.getId()));
        parameters.put(CurrentSessionLanguageConditionlet.LANGUAGE_KEY,
                new ParameterModel(CurrentSessionLanguageConditionlet.LANGUAGE_KEY, "en"));

        CurrentSessionLanguageConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        conditionlet.evaluate(request, response, instance);
    }

    @Test ( expected = ComparisonNotSupportedException.class )
    public void testGreaterThanComparison () {

        //Mock the request language id
        Mockito.when(request.getParameter("language_id")).thenReturn("2");
        Mockito.when(request.getAttribute(WebKeys.HTMLPAGE_LANGUAGE)).thenReturn("2");
        Mockito.when(request.getSession(true)).thenReturn(session);

        //Correct case
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, GREATER_THAN.getId()));
        parameters.put(CurrentSessionLanguageConditionlet.LANGUAGE_KEY,
                new ParameterModel(CurrentSessionLanguageConditionlet.LANGUAGE_KEY, "en"));

        CurrentSessionLanguageConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        conditionlet.evaluate(request, response, instance);
    }

    @Test ( expected = ComparisonNotSupportedException.class )
    public void testLessThanOrEqualComparison () {

        //Mock the request language id
        Mockito.when(request.getParameter("language_id")).thenReturn("2");
        Mockito.when(request.getAttribute(WebKeys.HTMLPAGE_LANGUAGE)).thenReturn("2");
        Mockito.when(request.getSession(true)).thenReturn(session);

        //Correct case
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, LESS_THAN_OR_EQUAL.getId()));
        parameters.put(CurrentSessionLanguageConditionlet.LANGUAGE_KEY,
                new ParameterModel(CurrentSessionLanguageConditionlet.LANGUAGE_KEY, "en"));

        CurrentSessionLanguageConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        conditionlet.evaluate(request, response, instance);
    }

    @Test ( expected = ComparisonNotSupportedException.class )
    public void testGreaterThanOrEqualComparison () {

        //Mock the request language id
        Mockito.when(request.getParameter("language_id")).thenReturn("2");
        Mockito.when(request.getAttribute(WebKeys.HTMLPAGE_LANGUAGE)).thenReturn("2");
        Mockito.when(request.getSession(true)).thenReturn(session);

        //Correct case
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, GREATER_THAN_OR_EQUAL.getId()));
        parameters.put(CurrentSessionLanguageConditionlet.LANGUAGE_KEY,
                new ParameterModel(CurrentSessionLanguageConditionlet.LANGUAGE_KEY, "en"));

        CurrentSessionLanguageConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        conditionlet.evaluate(request, response, instance);
    }

}