package com.dotmarketing.portlets.rules.conditionlet;

import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.CONTAINS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.ENDS_WITH;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS_NOT;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.REGEX;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.STARTS_WITH;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.dotcms.LicenseTestUtil;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Clickstream;
import com.dotmarketing.beans.Host;
import com.dotmarketing.factories.ClickstreamFactory;
import com.dotmarketing.portlets.rules.conditionlet.VisitedUrlConditionlet.Instance;
import com.dotmarketing.portlets.rules.exception.ComparisonNotPresentException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.util.WebKeys;

public class VisitedUrlConditionletTest {

  private VisitedUrlConditionlet visitedConditionlet = new VisitedUrlConditionlet();
  private static final String INDEX = "index";
  private static Host host = null;
  private static HttpServletResponse response = new MockHttpResponse();
  
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  private static Clickstream clickStream;
  
  @BeforeClass
  public static void prepare() throws Exception {
    // Setting web app environment
    IntegrationTestInitService.getInstance().init();
    LicenseTestUtil.getLicense();
    host = new SiteDataGen().nextPersisted();
    clickStream = new Clickstream(); 
    request("/products");
    request("/contact-us");
    request("/about-us/index");


  }


  static HttpServletRequest request(String url) throws Exception {
    HttpServletRequest request = new MockHttpRequest(host.getHostname(),url).request();
    request.getSession().setAttribute("clickstream", clickStream);
    request.setAttribute(WebKeys.CLICKSTREAM_IDENTIFIER_OVERRIDE, url);
    request.removeAttribute("CLICKSTREAM_RECORDED");
    ClickstreamFactory.addRequest(request, response, host);
    return request;
  }



  @Test
  public void testVisitedUrlConditionletConfiguration() {
    Map<String, ParameterDefinition> parameters = visitedConditionlet.getParameterDefinitions();

    // Conditionlet has the input field
    ParameterDefinition input = ((ParameterDefinition) parameters.get(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY));
    Assert.assertNotNull(input);

    // Conditionlet has comparisons
    ComparisonParameterDefinition comparisonDefinition = ((ComparisonParameterDefinition) parameters.get(Conditionlet.COMPARISON_KEY));
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
  public void testIsComparison() throws Exception {
    final String currentUrl = "/indexIs";
    HttpServletRequest request = request(currentUrl);

    Map<String, ParameterModel> parameters = new HashMap<>();
    parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS.getId()));
    parameters.put(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY,
        new ParameterModel(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, "/products"));

    Instance instance = visitedConditionlet.instanceFrom(parameters);
    
    Assert.assertTrue("has visited /products", visitedConditionlet.evaluate(request, response, INDEX, instance));

    

    
    // Verify is the new url was added to the visitedUrls
    Assert.assertTrue(ClickstreamFactory.addRequest(request, response, host).getClickstreamRequests().stream()
        .anyMatch(click -> click.getRequestURI().equals(currentUrl)));

    // Verify use case with empty visited url collections
    verifyEmptyVisitedUrlsUseCase(instance, IS.getId());
  }

  @Test
  public void testIsNotComparison() throws Exception {
    final String currentUrl = "/indexIsNot";

    Map<String, ParameterModel> parameters = new HashMap<>();
    parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS_NOT.getId()));
    parameters.put(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY,
        new ParameterModel(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, "/news-events/news"));

    Instance instance = visitedConditionlet.instanceFrom(parameters);
    // Correct, a visited URL IS_NOT "/news-events/news"
    Assert.assertTrue(visitedConditionlet.evaluate(request(currentUrl), response, INDEX, instance));

    // Verify use case with empty visited url collections
    verifyEmptyVisitedUrlsUseCase(instance, IS_NOT.getId());
  }

  @Test
  public void testWrongIsNotComparison() throws Exception {
    final String currentUrl = "/indexIsNot";

    Map<String, ParameterModel> parameters = new HashMap<>();
    parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, IS_NOT.getId()));
    parameters.put(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY,
        new ParameterModel(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, "/about-us/index"));

    Instance instance = visitedConditionlet.instanceFrom(parameters);
    // Incorrect, a visited URL IS_NOT "/about-us/index"
    Assert.assertFalse(visitedConditionlet.evaluate(request(currentUrl), response, INDEX, instance));

    // Verify is the new url was added to the visitedUrls
    Assert.assertTrue(ClickstreamFactory.addRequest(request(currentUrl), response, host).getClickstreamRequests().stream()
        .anyMatch(click -> click.getRequestURI().equals(currentUrl)));
  }

  @Test
  public void testStartWithComparison() throws Exception {
    final String currentUrl = "/indexStartsWith";

    Map<String, ParameterModel> parameters = new HashMap<>();
    parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, STARTS_WITH.getId()));
    parameters.put(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY,
        new ParameterModel(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, "/about"));

    Instance instance = visitedConditionlet.instanceFrom(parameters);
    // Correct, a visited URL STARTS_WITH "/about"
    Assert.assertTrue(visitedConditionlet.evaluate(request(currentUrl), response, INDEX, instance));

    // Verify is the new url was added to the visitedUrls
    Assert.assertTrue(ClickstreamFactory.addRequest(request(currentUrl), response, host).getClickstreamRequests().stream()
        .anyMatch(click -> click.getRequestURI().equals(currentUrl)));

    // Verify use case with empty visited url collections
    verifyEmptyVisitedUrlsUseCase(instance, STARTS_WITH.getId());
  }

  @Test
  public void testEndsWithComparison() throws Exception {
    final String currentUrl = "/indexEndsWith";

    Map<String, ParameterModel> parameters = new HashMap<>();
    parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, ENDS_WITH.getId()));
    parameters.put(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, new ParameterModel(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, "-us"));

    Instance instance = visitedConditionlet.instanceFrom(parameters);
    // Correct, a visited URL ENDS_WITH "-us"
    Assert.assertTrue(visitedConditionlet.evaluate(request(currentUrl), response, INDEX, instance));

    // Verify is the new url was added to the visitedUrls
    Assert.assertTrue(ClickstreamFactory.addRequest(request(currentUrl), response, host).getClickstreamRequests().stream()
        .anyMatch(click -> click.getRequestURI().equals(currentUrl)));

    // Verify use case with empty visited url collections
    verifyEmptyVisitedUrlsUseCase(instance, ENDS_WITH.getId());
  }

  @Test
  public void testContainsComparison() throws Exception {
    final String currentUrl = "/indexContains";

    Map<String, ParameterModel> parameters = new HashMap<>();
    parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, CONTAINS.getId()));
    parameters.put(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, new ParameterModel(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, "tact"));

    Instance instance = visitedConditionlet.instanceFrom(parameters);
    // Correct, a visited URL CONTAINS "tact"
    Assert.assertTrue(visitedConditionlet.evaluate(request(currentUrl), response, INDEX, instance));

    // Verify is the new url was added to the visitedUrls
    Assert.assertTrue(ClickstreamFactory.addRequest(request(currentUrl), response, host).getClickstreamRequests().stream()
        .anyMatch(click -> click.getRequestURI().equals(currentUrl)));

    // Verify use case with empty visited url collections
    verifyEmptyVisitedUrlsUseCase(instance, CONTAINS.getId());
  }

  @Test
  public void testRegexComparison() throws Exception {
    final String currentUrl = "/indexRegex";

    Map<String, ParameterModel> parameters = new HashMap<>();
    parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, REGEX.getId()));
    parameters.put(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, new ParameterModel(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, "/.*us"));

    Instance instance = visitedConditionlet.instanceFrom(parameters);
    // Correct, a visited URL REGEX "/.*us"
    Assert.assertTrue(visitedConditionlet.evaluate(request(currentUrl), response, INDEX, instance));

    // Verify is the new url was added to the visitedUrls
    Assert.assertTrue(ClickstreamFactory.addRequest(request(currentUrl), response, host).getClickstreamRequests().stream()
        .anyMatch(click -> click.getRequestURI().equals(currentUrl)));

    // Verify use case with empty visited url collections
    verifyEmptyVisitedUrlsUseCase(instance, REGEX.getId());
  }

  @Test
  public void testWrongRegexComparison() throws Exception {
    final String currentUrl = "/indexRegex";

    Map<String, ParameterModel> parameters = new HashMap<>();
    parameters.put(Conditionlet.COMPARISON_KEY, new ParameterModel(Conditionlet.COMPARISON_KEY, REGEX.getId()));
    parameters.put(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY,
        new ParameterModel(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, "/.*yyy"));

    Instance instance = visitedConditionlet.instanceFrom(parameters);
    final boolean result = visitedConditionlet.evaluate(request(currentUrl), response, INDEX, instance);
    // Incorrect, a visited URL REGEX "/.*yyy"
    Assert.assertFalse(result);

    // Verify is the new url was added to the visitedUrls
    Assert.assertTrue(ClickstreamFactory.addRequest(request(currentUrl), response, host).getClickstreamRequests().stream()
        .anyMatch(click -> click.getRequestURI().equals(currentUrl)));
  }

  private void verifyEmptyVisitedUrlsUseCase(Instance instance, final String comparison) throws Exception {
    // Test when visited urls are empty

    if (comparison.equalsIgnoreCase(IS_NOT.getId())) {
      Assert.assertTrue(visitedConditionlet.evaluate(new MockHttpRequest(host.getHostname(), "test").request(), response, INDEX, instance));
    } else {
      Assert
          .assertFalse(visitedConditionlet.evaluate(new MockHttpRequest(host.getHostname(), "test").request(), response, INDEX, instance));
    }
  }
}
