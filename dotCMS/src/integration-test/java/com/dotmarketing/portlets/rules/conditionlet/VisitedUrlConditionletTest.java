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

import com.dotcms.LicenseTestUtil;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.rules.conditionlet.VisitedUrlConditionlet.Instance;
import com.dotmarketing.portlets.rules.exception.ComparisonNotPresentException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.util.IntegrationTestInitService;
import com.dotmarketing.util.WebKeys;

public class VisitedUrlConditionletTest {

    private static final String HOST_IDENTIFIER = "48190c8c-42c4-46af-8d1a-0cd5db894797";
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private VisitedUrlConditionlet visitedConditionlet = new VisitedUrlConditionlet();
    private static Map<String, Set<String>> visitedUrls;
    private static final String INDEX = "index";

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @BeforeClass
    public static void prepare () throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();
    }

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
        Mockito.when(session.getAttribute(VisitedUrlConditionlet.RULES_CONDITIONLET_VISITEDURLS)).thenReturn(
                visitedUrls);
        Mockito.when(request.getSession(true)).thenReturn(session);
    }

    @Test
    public void testVisitedUrlConditionletConfiguration() {
        Map<String, ParameterDefinition> parameters = visitedConditionlet.getParameterDefinitions();

        // Conditionlet has the input field
        ParameterDefinition input = ((ParameterDefinition) parameters.get(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY));
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

        Mockito.when(request.getRequestURI()).thenReturn(currentUrl);

        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS.getId()));
        parameters.put(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, new ParameterModel(
                VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, "/products"));

        Instance instance = visitedConditionlet.instanceFrom(parameters);
        // Correct, a visited URL is "/products"
        Assert.assertTrue(visitedConditionlet.evaluate(request, response, INDEX, instance));

        // Verify is the new url was added to the visitedUrls
        Assert.assertTrue(visitedUrls.get(HOST_IDENTIFIER).contains(currentUrl));

        // Verify use case with empty visited url collections
        verifyEmptyVisitedUrlsUseCase(instance, IS.getId());
    }

    @Test
    public void testIsNotComparison() {
        final String currentUrl = "/indexIsNot";

        Mockito.when(request.getRequestURI()).thenReturn(currentUrl);

        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS_NOT.getId()));
        parameters.put(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, new ParameterModel(
                VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, "/news-events/news"));

        Instance instance = visitedConditionlet.instanceFrom(parameters);
        // Correct, a visited URL IS_NOT "/news-events/news"
        Assert.assertTrue(visitedConditionlet.evaluate(request, response, INDEX, instance));

        // Verify is the new url was added to the visitedUrls
        Assert.assertTrue(visitedUrls.get(HOST_IDENTIFIER).contains(currentUrl));

        // Verify use case with empty visited url collections
        verifyEmptyVisitedUrlsUseCase(instance, IS_NOT.getId());
    }

    @Test
    public void testWrongIsNotComparison() {
        final String currentUrl = "/indexIsNot";

        Mockito.when(request.getRequestURI()).thenReturn(currentUrl);

        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS_NOT.getId()));
        parameters.put(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, new ParameterModel(
                VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, "/about-us/index"));

        Instance instance = visitedConditionlet.instanceFrom(parameters);
        // Incorrect, a visited URL IS_NOT "/about-us/index"
        Assert.assertFalse(visitedConditionlet.evaluate(request, response, INDEX,  instance));

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
        parameters.put(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, new ParameterModel(
                VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, "/about"));

        Instance instance = visitedConditionlet.instanceFrom(parameters);
        // Correct, a visited URL STARTS_WITH "/about"
        Assert.assertTrue(visitedConditionlet.evaluate(request, response, INDEX, instance));

        // Verify is the new url was added to the visitedUrls
        Assert.assertTrue(visitedUrls.get(HOST_IDENTIFIER).contains(currentUrl));

        // Verify use case with empty visited url collections
        verifyEmptyVisitedUrlsUseCase(instance, STARTS_WITH.getId());
    }

    @Test
    public void testEndsWithComparison() {
        final String currentUrl = "/indexEndsWith";

        Mockito.when(request.getRequestURI()).thenReturn(currentUrl);

        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, ENDS_WITH.getId()));
        parameters.put(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, new ParameterModel(
                VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, "-us"));

        Instance instance = visitedConditionlet.instanceFrom(parameters);
        // Correct, a visited URL ENDS_WITH "-us"
        Assert.assertTrue(visitedConditionlet.evaluate(request, response, INDEX, instance));

        // Verify is the new url was added to the visitedUrls
        Assert.assertTrue(visitedUrls.get(HOST_IDENTIFIER).contains(currentUrl));

        // Verify use case with empty visited url collections
        verifyEmptyVisitedUrlsUseCase(instance, ENDS_WITH.getId());
    }

    @Test
    public void testContainsComparison() {
        final String currentUrl = "/indexContains";

        Mockito.when(request.getRequestURI()).thenReturn(currentUrl);

        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, CONTAINS.getId()));
        parameters.put(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, new ParameterModel(
                VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, "tact"));

        Instance instance = visitedConditionlet.instanceFrom(parameters);
        // Correct, a visited URL CONTAINS "tact"
        Assert.assertTrue(visitedConditionlet.evaluate(request, response, INDEX, instance));

        // Verify is the new url was added to the visitedUrls
        Assert.assertTrue(visitedUrls.get(HOST_IDENTIFIER).contains(currentUrl));

        // Verify use case with empty visited url collections
        verifyEmptyVisitedUrlsUseCase(instance, CONTAINS.getId());
    }

    @Test
    public void testRegexComparison() {
        final String currentUrl = "/indexRegex";

        Mockito.when(request.getRequestURI()).thenReturn(currentUrl);

        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, REGEX.getId()));
        parameters.put(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, new ParameterModel(
                VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, "/.*us"));

        Instance instance = visitedConditionlet.instanceFrom(parameters);
        // Correct, a visited URL REGEX "/.*us"
        Assert.assertTrue(visitedConditionlet.evaluate(request, response, INDEX, instance));

        // Verify is the new url was added to the visitedUrls
        Assert.assertTrue(visitedUrls.get(HOST_IDENTIFIER).contains(currentUrl));

        // Verify use case with empty visited url collections
        verifyEmptyVisitedUrlsUseCase(instance, REGEX.getId());
    }

    @Test
    public void testWrongRegexComparison() {
        final String currentUrl = "/indexRegex";

        Mockito.when(request.getRequestURI()).thenReturn(currentUrl);

        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, REGEX.getId()));
        parameters.put(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, new ParameterModel(
                VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, "/.*yyy"));

        Instance instance = visitedConditionlet.instanceFrom(parameters);
        final boolean result = visitedConditionlet.evaluate(request, response, INDEX, instance);
        // Incorrect, a visited URL REGEX "/.*yyy"
        Assert.assertFalse(result);

        // Verify is the new url was added to the visitedUrls
        Assert.assertTrue(visitedUrls.get(HOST_IDENTIFIER).contains(currentUrl));
    }

    private void verifyEmptyVisitedUrlsUseCase(Instance instance, final String comparison) {
        // Test when visited urls are empty
        visitedUrls = new HashMap<>();
        Mockito.when(session.getAttribute(VisitedUrlConditionlet.RULES_CONDITIONLET_VISITEDURLS)).thenReturn(
                visitedUrls);

        if (comparison.equalsIgnoreCase(IS_NOT.getId())) {
            Assert.assertTrue(visitedConditionlet.evaluate(request, response, INDEX, instance));
        } else {
            Assert.assertFalse(visitedConditionlet.evaluate(request, response, INDEX, instance));
        }
    }
}
