package com.dotmarketing.portlets.rules.conditionlet;

import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.CONTAINS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.ENDS_WITH;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS_NOT;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.REGEX;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.STARTS_WITH;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.dotcms.repackage.org.junit.Assert;
import com.dotcms.repackage.org.junit.Before;
import com.dotcms.repackage.org.junit.Rule;
import com.dotcms.repackage.org.junit.Test;
import com.dotcms.repackage.org.junit.rules.ExpectedException;
import com.dotcms.repackage.org.mockito.Mockito;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.rules.exception.ComparisonNotPresentException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.util.WebKeys;

public class UsersVisitedUrlConditionletTest {

    private static final String HOST_IDENTIFIER = "48190c8c-42c4-46af-8d1a-0cd5db894797";
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private UsersVisitedUrlConditionlet visitedConditionlet = new UsersVisitedUrlConditionlet();
    private static Map<String, Set<String>> visitedUrls;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void before() {
        // Mock request
        request = Mockito.mock(HttpServletRequest.class);
        Host host = new Host();
        host.setHostname("demo.dotcms.com");
        host.setIdentifier(HOST_IDENTIFIER);
        Mockito.when(request.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(host);

        // Mock response
        response = Mockito.mock(HttpServletResponse.class);

        // Load visited urls for an specific host
        visitedUrls = new HashMap<>();
        Set<String> urls = new LinkedHashSet<>();
        urls.add("/products");
        urls.add("/contact-us");
        urls.add("/about-us/index");
        visitedUrls.put(HOST_IDENTIFIER, urls);

        // Mock session
        session = Mockito.mock(HttpSession.class);
        Mockito.when(session.getAttribute(UsersVisitedUrlConditionlet.RULES_CONDITIONLET_VISITEDURLS)).thenReturn(
                visitedUrls);
        Mockito.when(request.getSession(true)).thenReturn(session);
    }

    @Test
    public void testUsersVisitedUrlConditionletConfiguration() {
        Map<String, ParameterDefinition> parameters = visitedConditionlet.getParameterDefinitions();

        // Conditionlet has the input field
        ParameterDefinition input = ((ParameterDefinition) parameters
                .get(UsersVisitedUrlConditionlet.PATTERN_URL_INPUT_KEY));
        Assert.assertNotNull(input);

        // Conditionlet has comparisons
        ComparisonParameterDefinition comparisonDefinition = ((ComparisonParameterDefinition) parameters
                .get(Conditionlet.COMPARISON_KEY));
        Assert.assertNotNull(comparisonDefinition);
        Assert.assertNotNull(comparisonDefinition.comparisonFrom(CONTAINS.getId()));
        Assert.assertNotNull(comparisonDefinition.comparisonFrom(ENDS_WITH.getId()));
        Assert.assertNotNull(comparisonDefinition.comparisonFrom(IS.getId()));
        Assert.assertNotNull(comparisonDefinition.comparisonFrom(IS_NOT.getId()));
        Assert.assertNotNull(comparisonDefinition.comparisonFrom(REGEX.getId()));
        Assert.assertNotNull(comparisonDefinition.comparisonFrom(STARTS_WITH.getId()));

        exception.expect(ComparisonNotPresentException.class);
        Assert.assertNull(comparisonDefinition.comparisonFrom("dummyId"));
    }

    @Test
    public void testIsComparison() {
        final String currentUrl = "/indexIs";

        HttpSession session = Mockito.mock(HttpSession.class);
        Mockito.when(session.getAttribute(UsersVisitedUrlConditionlet.RULES_CONDITIONLET_VISITEDURLS)).thenReturn(
                visitedUrls);
        Mockito.when(request.getSession(true)).thenReturn(session);
        Mockito.when(request.getRequestURI()).thenReturn(currentUrl);

        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS.getId()));
        parameters.put(UsersVisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, new ParameterModel(
                UsersVisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, "/products"));

        final boolean result = visitedConditionlet.evaluate(request, response,
                visitedConditionlet.instanceFrom(parameters));
        // Correct, a visited URL is "/products"
        Assert.assertTrue(result);

        // Verify is the new url was added to the visitedUrls
        Assert.assertTrue(visitedUrls.get(HOST_IDENTIFIER).contains(currentUrl));
    }

    @Test
    public void testIsNotComparison() {
        final String currentUrl = "/indexIsNot";

        Mockito.when(request.getRequestURI()).thenReturn(currentUrl);

        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS_NOT.getId()));
        parameters.put(UsersVisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, new ParameterModel(
                UsersVisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, "/news-events/news"));

        final boolean result = visitedConditionlet.evaluate(request, response,
                visitedConditionlet.instanceFrom(parameters));
        // Correct, a visited URL IS_NOT "/news-events/news"
        Assert.assertTrue(result);

        // Verify is the new url was added to the visitedUrls
        Assert.assertTrue(visitedUrls.get(HOST_IDENTIFIER).contains(currentUrl));
    }

    @Test
    public void testStartWithComparison() {
        final String currentUrl = "/indexStartsWith";

        Mockito.when(request.getRequestURI()).thenReturn(currentUrl);

        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY,
                new ParameterModel(Conditionlet.COMPARISON_KEY, STARTS_WITH.getId()));
        parameters.put(UsersVisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, new ParameterModel(
                UsersVisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, "/about"));

        final boolean result = visitedConditionlet.evaluate(request, response,
                visitedConditionlet.instanceFrom(parameters));
        // Correct, a visited URL STARTS_WITH "/about"
        Assert.assertTrue(result);

        // Verify is the new url was added to the visitedUrls
        Assert.assertTrue(visitedUrls.get(HOST_IDENTIFIER).contains(currentUrl));
    }

    @Test
    public void testEndsWithComparison() {
        final String currentUrl = "/indexEndsWith";

        Mockito.when(request.getRequestURI()).thenReturn(currentUrl);

        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, ENDS_WITH.getId()));
        parameters.put(UsersVisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, new ParameterModel(
                UsersVisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, "-us"));

        final boolean result = visitedConditionlet.evaluate(request, response,
                visitedConditionlet.instanceFrom(parameters));
        // Correct, a visited URL ENDS_WITH "-us"
        Assert.assertTrue(result);

        // Verify is the new url was added to the visitedUrls
        Assert.assertTrue(visitedUrls.get(HOST_IDENTIFIER).contains(currentUrl));
    }

    @Test
    public void testContainsComparison() {
        final String currentUrl = "/indexContains";

        Mockito.when(request.getRequestURI()).thenReturn(currentUrl);

        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, CONTAINS.getId()));
        parameters.put(UsersVisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, new ParameterModel(
                UsersVisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, "tact"));

        final boolean result = visitedConditionlet.evaluate(request, response,
                visitedConditionlet.instanceFrom(parameters));
        // Correct, a visited URL CONTAINS "tact"
        Assert.assertTrue(result);

        // Verify is the new url was added to the visitedUrls
        Assert.assertTrue(visitedUrls.get(HOST_IDENTIFIER).contains(currentUrl));
    }

    @Test
    public void testRegexComparison() {
        final String currentUrl = "/indexRegex";

        Mockito.when(request.getRequestURI()).thenReturn(currentUrl);

        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, REGEX.getId()));
        parameters.put(UsersVisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, new ParameterModel(
                UsersVisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, "/.*us"));

        final boolean result = visitedConditionlet.evaluate(request, response,
                visitedConditionlet.instanceFrom(parameters));
        // Correct, a visited URL REGEX "/.*us"
        Assert.assertTrue(result);

        // Verify is the new url was added to the visitedUrls
        Assert.assertTrue(visitedUrls.get(HOST_IDENTIFIER).contains(currentUrl));
    }

    @Test
    public void testWrongRegexComparison() {
        final String currentUrl = "/indexRegex";

        Mockito.when(request.getRequestURI()).thenReturn(currentUrl);

        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, REGEX.getId()));
        parameters.put(UsersVisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, new ParameterModel(
                UsersVisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, "/.*yyy"));

        final boolean result = visitedConditionlet.evaluate(request, response,
                visitedConditionlet.instanceFrom(parameters));
        // Incorrect, a visited URL REGEX "/.*yyy"
        Assert.assertFalse(result);

        // Verify is the new url was added to the visitedUrls
        Assert.assertTrue(visitedUrls.get(HOST_IDENTIFIER).contains(currentUrl));
    }
}
