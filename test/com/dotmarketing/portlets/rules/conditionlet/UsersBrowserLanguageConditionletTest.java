package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.LicenseTestUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.*;

/**
 * @author Jonathan Gamba
 *         Date: 1/12/16
 */
public class UsersBrowserLanguageConditionletTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private UsersBrowserLanguageConditionlet conditionlet = new UsersBrowserLanguageConditionlet();
    private Collection<Locale> localeEnCollection;//English
    private Collection<Locale> localeEsCollection;//Spanish

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

        //Mock the getLocales
        localeEnCollection = new ArrayList<>();
        Locale localeNso = new Locale("nso");
        Locale localeXh = new Locale("xh");
        Locale localeEn_US = new Locale("en", "US");
        Locale localeEn = new Locale("en");
        localeEnCollection.add(localeNso);
        localeEnCollection.add(localeXh);
        localeEnCollection.add(localeEn_US);
        localeEnCollection.add(localeEn);

        //Mock the getLocales
        localeEsCollection = new ArrayList<>();
        Locale localeEs_CR = new Locale("es", "CR");
        Locale localeEs = new Locale("es");
        localeEsCollection.add(localeNso);
        localeEsCollection.add(localeXh);
        localeEsCollection.add(localeEs_CR);
        localeEsCollection.add(localeEs);
    }

    @Test
    public void testIsComparison () {

        //++++++++++++++++++++++++++++++
        //Correct case

        //Mock the getLocales
        Mockito.when(request.getLocales()).thenReturn(Collections.enumeration(localeEnCollection));

        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS.getId()));
        parameters.put(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY,
                new ParameterModel(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY, "en"));

        UsersBrowserLanguageConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        // Correct, the Accept-Language language is equals to the selected language
        Assert.assertTrue(conditionlet.evaluate(request, response, instance));

        //++++++++++++++++++++++++++++++
        //Correct case

        //Mock the getLocales
        Mockito.when(request.getLocales()).thenReturn(Collections.enumeration(localeEnCollection));

        parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS.getId()));
        parameters.put(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY,
                new ParameterModel(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY, "nso"));

        instance = conditionlet.instanceFrom(parameters);
        // Correct, the Accept-Language language is equals to the selected language
        Assert.assertTrue(conditionlet.evaluate(request, response, instance));

        //++++++++++++++++++++++++++++++
        //Correct case

        //Mock the getLocales
        Mockito.when(request.getLocales()).thenReturn(Collections.enumeration(localeEnCollection));

        parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS.getId()));
        parameters.put(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY,
                new ParameterModel(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY, "xh"));

        instance = conditionlet.instanceFrom(parameters);
        // Correct, the Accept-Language language is equals to the selected language
        Assert.assertTrue(conditionlet.evaluate(request, response, instance));

        //++++++++++++++++++++++++++++++
        //Wrong case

        //Mock the getLocales
        Mockito.when(request.getLocales()).thenReturn(Collections.enumeration(localeEnCollection));

        parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS.getId()));
        parameters.put(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY,
                new ParameterModel(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY, "es"));

        instance = conditionlet.instanceFrom(parameters);
        // Incorrect, the Accept-Language language is not equals to the selected language
        Assert.assertFalse(conditionlet.evaluate(request, response, instance));

        //++++++++++++++++++++++++++++++
        //Test case

        //Mock the getLocales
        Mockito.when(request.getLocales()).thenReturn(Collections.enumeration(localeEnCollection));

        parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS.getId()));
        parameters.put(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY,
                new ParameterModel(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY, "EN"));

        instance = conditionlet.instanceFrom(parameters);
        // Correct, the Accept-Language language is equals to the selected language
        Assert.assertTrue(conditionlet.evaluate(request, response, instance));

        //++++++++++++++++++++++++++++++
        //Mock the getLocales
        Collection<Locale> localeCollection = new ArrayList<>();
        Locale localeNso = new Locale("NSO");
        Locale localeXh = new Locale("XH");
        Locale localeEn_Us = new Locale("EN", "US");
        Locale localeEn = new Locale("EN");
        localeCollection.add(localeNso);
        localeCollection.add(localeXh);
        localeCollection.add(localeEn_Us);
        localeCollection.add(localeEn);

        Mockito.when(request.getLocales()).thenReturn(Collections.enumeration(localeCollection));

        //++++++++++++++++++++++++++++++
        //Test case
        parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS.getId()));
        parameters.put(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY,
                new ParameterModel(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY, "en"));

        instance = conditionlet.instanceFrom(parameters);
        // Correct, the Accept-Language language is equals to the selected language
        Assert.assertTrue(conditionlet.evaluate(request, response, instance));
    }

    @Test
    public void testIsNotComparison () {

        //++++++++++++++++++++++++++++++
        //Correct case

        //Mock the getLocales
        Mockito.when(request.getLocales()).thenReturn(Collections.enumeration(localeEsCollection));

        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS_NOT.getId()));
        parameters.put(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY,
                new ParameterModel(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY, "en"));

        UsersBrowserLanguageConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        // Correct, the Accept-Language language is not equals to the selected language
        Assert.assertTrue(conditionlet.evaluate(request, response, instance));

        //++++++++++++++++++++++++++++++
        //Wrong case

        //Mock the getLocales
        Mockito.when(request.getLocales()).thenReturn(Collections.enumeration(localeEsCollection));

        parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS_NOT.getId()));
        parameters.put(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY,
                new ParameterModel(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY, "es"));

        instance = conditionlet.instanceFrom(parameters);
        // Incorrect, the Accept-Language language is not equals to the selected language
        Assert.assertFalse(conditionlet.evaluate(request, response, instance));
    }

    @Test ( expected = ComparisonNotSupportedException.class )
    public void testExistComparison () {

        //Mock the getLocales
        Mockito.when(request.getLocales()).thenReturn(Collections.enumeration(localeEsCollection));

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

        //Mock the getLocales
        Mockito.when(request.getLocales()).thenReturn(Collections.enumeration(localeEsCollection));

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

        //Mock the getLocales
        Mockito.when(request.getLocales()).thenReturn(Collections.enumeration(localeEsCollection));

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

        //Mock the getLocales
        Mockito.when(request.getLocales()).thenReturn(Collections.enumeration(localeEsCollection));

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

        //Mock the getLocales
        Mockito.when(request.getLocales()).thenReturn(Collections.enumeration(localeEsCollection));

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

        //Mock the getLocales
        Mockito.when(request.getLocales()).thenReturn(Collections.enumeration(localeEsCollection));

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

        //Mock the getLocales
        Mockito.when(request.getLocales()).thenReturn(Collections.enumeration(localeEsCollection));

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

        //Mock the getLocales
        Mockito.when(request.getLocales()).thenReturn(Collections.enumeration(localeEsCollection));

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

        //Mock the getLocales
        Mockito.when(request.getLocales()).thenReturn(Collections.enumeration(localeEsCollection));

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

        //Mock the getLocales
        Mockito.when(request.getLocales()).thenReturn(Collections.enumeration(localeEsCollection));

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

        //Mock the getLocales
        Mockito.when(request.getLocales()).thenReturn(Collections.enumeration(localeEsCollection));

        //Correct case
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, GREATER_THAN_OR_EQUAL.getId()));
        parameters.put(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY,
                new ParameterModel(UsersBrowserLanguageConditionlet.LANGUAGE_INPUT_KEY, "en"));

        UsersBrowserLanguageConditionlet.Instance instance = conditionlet.instanceFrom(parameters);
        conditionlet.evaluate(request, response, instance);
    }

}