package com.dotmarketing.portlets.rules.conditionlet;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.dotcms.TestBase;
import com.dotcms.repackage.com.maxmind.geoip2.exception.GeoIp2Exception;
import com.dotcms.repackage.org.junit.Assert;
import com.dotcms.repackage.org.junit.Test;
import com.dotcms.repackage.org.mockito.Mockito;
import com.dotcms.util.GeoIp2CityDbUtil;
import com.dotmarketing.beans.Clickstream;
import com.dotmarketing.beans.ClickstreamRequest;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.rules.model.ConditionValue;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

/**
 * jUnit test used to verify the results of calling the conditionlets provided
 * out of the box in dotCMS.
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 05-14-2015
 *
 */
public class ConditionletTest extends TestBase {

	/**
	 * Test IP address. Points to our <a
	 * href="http://www.dotcms.com">www.dotcms.com</a> domain in Ashburn,
	 * Virginia.
	 */
	private static final String IP_ADDRESS_1 = "54.209.28.36";
	/**
	 * Test IP address. This is a public address located in Albany, NY, USA.
	 */
	private static final String IP_ADDRESS_2 = "170.123.234.133";

	@Test
	public void testUsersStateConditionlet() throws Exception {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getRemoteAddr()).thenReturn(IP_ADDRESS_1);
		Conditionlet stateConditionlet = new UsersStateConditionlet();
		List<ConditionValue> conditionletValues = new ArrayList<ConditionValue>();
		// Conditionlet has the input field
		Collection<ConditionletInput> inputCollection = stateConditionlet
				.getInputs(null);
		Assert.assertTrue(inputCollection.size() > 0);
		// Conditionlet has comparisons
		Set<Comparison> comparisonSet = stateConditionlet.getComparisons();
		Assert.assertTrue(comparisonSet.size() > 0);
		List<ConditionletInput> inputs = new ArrayList<ConditionletInput>(
				inputCollection);
		List<Comparison> comparisons = new ArrayList<Comparison>(comparisonSet);
		ConditionValue value1 = new ConditionValue();
		value1.setId(inputs.get(0).getId());
        value1.setKey(inputs.get(0).getId());
		value1.setPriority(1);
		value1.setValue("VA");
		ConditionValue value2 = new ConditionValue();
		value2.setId(inputs.get(0).getId());
        value2.setKey(inputs.get(0).getId());
		value2.setPriority(2);
		value2.setValue("FL");
		conditionletValues.add(value1);
		conditionletValues.add(value2);
		boolean result = stateConditionlet.evaluate(request, null, comparisons
				.get(0).getId(), conditionletValues);
		// Correct, the client is from Virginia
		Assert.assertTrue(result);

		value1.setValue("NY");
		value1.setValue("AZ");
		result = stateConditionlet.evaluate(request, null, comparisons.get(1)
				.getId(), conditionletValues);
		// Correct, the client IS NOT from New York
		Assert.assertTrue(result);
	}

	@Test
	public void testUsersCountryConditionlet() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getRemoteAddr()).thenReturn(IP_ADDRESS_1);
		Conditionlet countryConditionlet = new UsersCountryConditionlet();
		List<ConditionValue> conditionletValues = new ArrayList<ConditionValue>();
		// Conditionlet has the input field
		Collection<ConditionletInput> inputCollection = countryConditionlet
				.getInputs(null);
		Assert.assertTrue(inputCollection.size() > 0);
		// Conditionlet has comparisons
		Set<Comparison> comparisonSet = countryConditionlet.getComparisons();
		Assert.assertTrue(comparisonSet.size() > 0);
		List<ConditionletInput> inputs = new ArrayList<ConditionletInput>(
				inputCollection);
		List<Comparison> comparisons = new ArrayList<Comparison>(comparisonSet);
		ConditionValue value1 = new ConditionValue();
		value1.setId(inputs.get(0).getId());
		value1.setKey("isoCode");
        value1.setPriority(1);
		value1.setValue("US");
		ConditionValue value2 = new ConditionValue();
		value2.setId(inputs.get(0).getId());
        value2.setKey("isoCode");
		value2.setPriority(2);
		value2.setValue("CA");
		ConditionValue value3 = new ConditionValue();
		value3.setId(inputs.get(0).getId());
        value3.setKey("isoCode");
		value3.setPriority(3);
		value3.setValue("CO");
		conditionletValues.add(value1);
		conditionletValues.add(value2);
		conditionletValues.add(value3);
		boolean result = countryConditionlet.evaluate(request, null,
				comparisons.get(0).getId(), conditionletValues);
		// Correct, the client is from USA
		Assert.assertTrue(result);

		value1.setValue("CR");
		value2.setValue("CO");
		value3.setValue("VE");
		result = countryConditionlet.evaluate(request, null, comparisons.get(1)
				.getId(), conditionletValues);
		// Correct, the client IS NOT from Costa Rica, Colombia or Venezuela
		Assert.assertTrue(result);
	}

	@Test
	public void testUsersCity() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getRemoteAddr()).thenReturn(IP_ADDRESS_2);
		Conditionlet cityConditionlet = new UsersCityConditionlet();
		// Conditionlet has the input field
		Collection<ConditionletInput> inputCollection = cityConditionlet
				.getInputs(null);
		Assert.assertTrue(inputCollection.size() > 0);
		// Conditionlet has comparisons
		Set<Comparison> comparisonSet = cityConditionlet.getComparisons();
		Assert.assertTrue(comparisonSet.size() > 0);
		List<ConditionletInput> inputs = new ArrayList<ConditionletInput>(
				inputCollection);
		List<Comparison> comparisons = new ArrayList<Comparison>(comparisonSet);
		List<ConditionValue> conditionletValues = new ArrayList<ConditionValue>();
		ConditionValue value1 = new ConditionValue();
		value1.setId(inputs.get(0).getId());
        value1.setKey("isoCode");
		value1.setPriority(1);
		value1.setValue("Albany");
		conditionletValues.add(value1);
		boolean result = cityConditionlet.evaluate(request, null, comparisons
				.get(0).getId(), conditionletValues);
		// Correct, the client is from USA
		Assert.assertTrue(result);

		value1.setValue("Salt Lake City");
		ConditionValue value2 = new ConditionValue();
		value2.setId(inputs.get(0).getId());
        value2.setKey("isoCode");
		value2.setPriority(2);
		value2.setValue("Springfield");
		conditionletValues.add(value2);
		result = cityConditionlet.evaluate(request, null, comparisons.get(1)
				.getId(), conditionletValues);
		// Correct, the client is NOT from Salt Lake City or Springfield
		Assert.assertTrue(result);
	}

	@Test
	public void testUsersLanguage() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getRemoteAddr()).thenReturn(IP_ADDRESS_1);
		Mockito.when(request.getAttribute(WebKeys.HTMLPAGE_LANGUAGE))
				.thenReturn("1");
		Conditionlet langConditionlet = new UsersLanguageConditionlet();
		// Conditionlet has the input field
		Collection<ConditionletInput> inputCollection = langConditionlet
				.getInputs(null);
		Assert.assertTrue(inputCollection.size() > 0);
		// Conditionlet has comparisons
		Set<Comparison> comparisonSet = langConditionlet.getComparisons();
		Assert.assertTrue(comparisonSet.size() > 0);
		List<ConditionletInput> inputs = new ArrayList<ConditionletInput>(
				inputCollection);
		List<Comparison> comparisons = new ArrayList<Comparison>(comparisonSet);
		List<ConditionValue> conditionletValues = new ArrayList<ConditionValue>();
		ConditionValue value1 = new ConditionValue();
		value1.setId(inputs.get(0).getId());
        value1.setKey("isoCode");
		value1.setPriority(1);
		value1.setValue("en");
		ConditionValue value2 = new ConditionValue();
		value2.setId(inputs.get(0).getId());
        value2.setKey("isoCode");
		value2.setPriority(2);
		value2.setValue("de");
		ConditionValue value3 = new ConditionValue();
		value3.setId(inputs.get(0).getId());
        value3.setKey("isoCode");
		value3.setPriority(3);
		value3.setValue("ja");
		conditionletValues.add(value1);
		conditionletValues.add(value2);
		conditionletValues.add(value3);
		boolean result = langConditionlet.evaluate(request, null, comparisons
				.get(0).getId(), conditionletValues);
		// Correct, the client's language is English
		Assert.assertTrue(result);

		value1.setValue("es");
		result = langConditionlet.evaluate(request, null, comparisons.get(1)
				.getId(), conditionletValues);
		// Correct, the client's language IS NOT Spanish, German, or Japanese
		Assert.assertTrue(result);
	}

	@Test
	public void testUsersHost() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Host host = new Host();
		host.setHostname("demo.dotcms.com");
		Mockito.when(request.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(
				host);
		Conditionlet hostConditionlet = new UsersHostConditionlet();
		// Conditionlet has the input field
		Collection<ConditionletInput> inputCollection = hostConditionlet
				.getInputs(null);
		Assert.assertTrue(inputCollection.size() > 0);
		// Conditionlet has comparisons
		Set<Comparison> comparisonSet = hostConditionlet.getComparisons();
		Assert.assertTrue(comparisonSet.size() > 0);
		List<ConditionletInput> inputs = new ArrayList<ConditionletInput>(
				inputCollection);
		List<Comparison> comparisons = new ArrayList<Comparison>(comparisonSet);
		List<ConditionValue> conditionletValues = new ArrayList<ConditionValue>();
		ConditionValue value1 = new ConditionValue();
		value1.setId(inputs.get(0).getId());
        value1.setKey("isoCode");
		value1.setPriority(1);
		value1.setValue("demo.dotcms.com");
		conditionletValues.add(value1);
		boolean result = hostConditionlet.evaluate(request, null, comparisons
				.get(0).getId(), conditionletValues);
		// Correct, the current host is "demo.dotcms.com"
		Assert.assertTrue(result);

		value1.setValue("m.dotcms.com");
		result = hostConditionlet.evaluate(request, null, comparisons.get(1)
				.getId(), conditionletValues);
		// Correct, the current host IS NOT "m.dotcms.com"
		Assert.assertTrue(result);

		value1.setValue("demo");
		result = hostConditionlet.evaluate(request, null, comparisons.get(2)
				.getId(), conditionletValues);
		// Correct, the current host starts with "demo"
		Assert.assertTrue(result);

		value1.setValue(".com");
		result = hostConditionlet.evaluate(request, null, comparisons.get(3)
				.getId(), conditionletValues);
		// Correct, the current host ends with ".com"
		Assert.assertTrue(result);

		value1.setValue(".com");
		result = hostConditionlet.evaluate(request, null, comparisons.get(4)
				.getId(), conditionletValues);
		// Correct, the current host contains ".com"
		Assert.assertTrue(result);

		value1.setValue(".*cms.com");
		result = hostConditionlet.evaluate(request, null, comparisons.get(5)
				.getId(), conditionletValues);
		// Correct, the current host contains matches the RegEx
		Assert.assertTrue(result);
	}

	/**
	 * Bear in mind that the names of the browsers returned by the User Agent
	 * Utils API might include the version numbers too. For example, Safari and
	 * Firefox might show up as "Safari 7" and "Firefox 37" respectively.
	 */
	@Test
	public void testUsersBrowser() {
		Conditionlet browserConditionlet = new UsersBrowserConditionlet();
		// Conditionlet has the input field
		Collection<ConditionletInput> inputCollection = browserConditionlet
				.getInputs(null);
		Assert.assertTrue(inputCollection.size() > 0);
		// Conditionlet has comparisons
		Set<Comparison> comparisonSet = browserConditionlet.getComparisons();
		Assert.assertTrue(comparisonSet.size() > 0);
		List<ConditionletInput> inputs = new ArrayList<ConditionletInput>(
				inputCollection);
		List<Comparison> comparisons = new ArrayList<Comparison>(comparisonSet);
		List<ConditionValue> conditionletValues = new ArrayList<ConditionValue>();
		ConditionValue value1 = new ConditionValue();
		value1.setId(inputs.get(0).getId());
        value1.setKey("isoCode");
		value1.setPriority(1);
		value1.setValue("Chrome");
		conditionletValues.add(value1);
		String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.65 Safari/537.36";
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getHeader("User-Agent")).thenReturn(userAgent);
		boolean result = browserConditionlet.evaluate(request, null,
				comparisons.get(0).getId(), conditionletValues);
		// Correct, the current browser is "Chrome"
		Assert.assertTrue(result);

		value1.setValue("Chrome");
		userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/600.6.3 (KHTML, like Gecko) Version/7.1.6 Safari/537.85.15";
		Mockito.when(request.getHeader("User-Agent")).thenReturn(userAgent);
		result = browserConditionlet.evaluate(request, null, comparisons.get(1)
				.getId(), conditionletValues);
		// Correct, the current browser IS NOT "Chrome", it's "Safari"
		Assert.assertTrue(result);

		value1.setValue("Fire");
		userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:37.0) Gecko/20100101 Firefox/37.0";
		Mockito.when(request.getHeader("User-Agent")).thenReturn(userAgent);
		result = browserConditionlet.evaluate(request, null, comparisons.get(2)
				.getId(), conditionletValues);
		// Correct, the current browser starts with "Fire"
		Assert.assertTrue(result);

		value1.setValue("ome");
		userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.65 Safari/537.36";
		Mockito.when(request.getHeader("User-Agent")).thenReturn(userAgent);
		result = browserConditionlet.evaluate(request, null, comparisons.get(3)
				.getId(), conditionletValues);
		// Correct, the current browser ends with "ome"
		Assert.assertTrue(result);

		value1.setValue("Firefox");
		userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:37.0) Gecko/20100101 Firefox/37.0";
		Mockito.when(request.getHeader("User-Agent")).thenReturn(userAgent);
		result = browserConditionlet.evaluate(request, null, comparisons.get(4)
				.getId(), conditionletValues);
		// Correct, the current browser contains "Fire"
		Assert.assertTrue(result);

		value1.setValue("Intern.*");
		userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; rv:11.0) like Gecko";
		Mockito.when(request.getHeader("User-Agent")).thenReturn(userAgent);
		result = browserConditionlet.evaluate(request, null, comparisons.get(5)
				.getId(), conditionletValues);
		// Correct, the current browser contains matches the RegEx
		Assert.assertTrue(result);
	}

	@Test
	public void testUsersOperatingSystem() {
		Conditionlet osConditionlet = new UsersOperatingSystemConditionlet();
		// Conditionlet has the input field
		Collection<ConditionletInput> inputCollection = osConditionlet
				.getInputs(null);
		Assert.assertTrue(inputCollection.size() > 0);
		// Conditionlet has comparisons
		Set<Comparison> comparisonSet = osConditionlet.getComparisons();
		Assert.assertTrue(comparisonSet.size() > 0);
		List<ConditionletInput> inputs = new ArrayList<ConditionletInput>(
				inputCollection);
		List<Comparison> comparisons = new ArrayList<Comparison>(comparisonSet);
		List<ConditionValue> conditionletValues = new ArrayList<ConditionValue>();
		ConditionValue value1 = new ConditionValue();
		value1.setId(inputs.get(0).getId());
        value1.setKey("isoCode");
		value1.setPriority(1);
		value1.setValue("Mac OS X");
		conditionletValues.add(value1);
		String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.65 Safari/537.36";
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getHeader("User-Agent")).thenReturn(userAgent);
		boolean result = osConditionlet.evaluate(request, null, comparisons
				.get(0).getId(), conditionletValues);
		// Correct, the current operating system is "Mac"
		Assert.assertTrue(result);

		value1.setValue("Mac OS X");
		userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; rv:11.0) like Gecko";
		Mockito.when(request.getHeader("User-Agent")).thenReturn(userAgent);
		result = osConditionlet.evaluate(request, null, comparisons.get(1)
				.getId(), conditionletValues);
		// Correct, the current operating system IS NOT "Mac", it's "Windows"
		Assert.assertTrue(result);

		value1.setValue("Mac");
		userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.65 Safari/537.36";
		Mockito.when(request.getHeader("User-Agent")).thenReturn(userAgent);
		result = osConditionlet.evaluate(request, null, comparisons.get(2)
				.getId(), conditionletValues);
		// Correct, the current operating system starts with "Mac"
		Assert.assertTrue(result);

		value1.setValue("OS X");
		userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.65 Safari/537.36";
		Mockito.when(request.getHeader("User-Agent")).thenReturn(userAgent);
		result = osConditionlet.evaluate(request, null, comparisons.get(3)
				.getId(), conditionletValues);
		// Correct, the current operating system ends with "OS X"
		Assert.assertTrue(result);

		value1.setValue("Windows");
		userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; rv:11.0) like Gecko";
		Mockito.when(request.getHeader("User-Agent")).thenReturn(userAgent);
		result = osConditionlet.evaluate(request, null, comparisons.get(4)
				.getId(), conditionletValues);
		// Correct, the current operating system contains "Windows"
		Assert.assertTrue(result);

		value1.setValue("Mac.*");
		userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.65 Safari/537.36";
		Mockito.when(request.getHeader("User-Agent")).thenReturn(userAgent);
		result = osConditionlet.evaluate(request, null, comparisons.get(5)
				.getId(), conditionletValues);
		// Correct, the current operating system matches the RegEx
		Assert.assertTrue(result);
	}

	@Test
	public void testUsersIp() {
		Conditionlet ipConditionlet = new UsersIpAddressConditionlet();
		// Conditionlet has the input field
		Collection<ConditionletInput> inputCollection = ipConditionlet
				.getInputs(null);
		Assert.assertTrue(inputCollection.size() > 0);
		// Conditionlet has comparisons
		Set<Comparison> comparisonSet = ipConditionlet.getComparisons();
		Assert.assertTrue(comparisonSet.size() > 0);
		List<ConditionletInput> inputs = new ArrayList<ConditionletInput>(
				inputCollection);
		List<Comparison> comparisons = new ArrayList<Comparison>(comparisonSet);
		List<ConditionValue> conditionletValues = new ArrayList<ConditionValue>();
		ConditionValue value1 = new ConditionValue();
		value1.setId(inputs.get(0).getId());
        value1.setKey("isoCode");
		value1.setPriority(1);
		value1.setValue("192.168.0.3");
		conditionletValues.add(value1);
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getRemoteAddr()).thenReturn("192.168.0.3");
		boolean result = ipConditionlet.evaluate(request, null, comparisons
				.get(0).getId(), conditionletValues);
		// Correct, the current IP is "192.168.0.3"
		Assert.assertTrue(result);

		value1.setValue("192.168.0.255");
		result = ipConditionlet.evaluate(request, null, comparisons.get(1)
				.getId(), conditionletValues);
		// Correct, the current IP address IS NOT "192.168.0.3"
		Assert.assertTrue(result);

		value1.setValue("192.168");
		result = ipConditionlet.evaluate(request, null, comparisons.get(2)
				.getId(), conditionletValues);
		// Correct, the current IP starts with "192.168"
		Assert.assertTrue(result);

		// Using CIDR notation
		value1.setValue("192.168.0.3/24");
		result = ipConditionlet.evaluate(request, null, comparisons.get(3)
				.getId(), conditionletValues);
		// Correct, the current IP starts within the submask "255.255.255.0"
		Assert.assertTrue(result);

		// Using classic netmask
		value1.setValue("192.168.0.3/255.255.255.0");
		result = ipConditionlet.evaluate(request, null, comparisons.get(3)
				.getId(), conditionletValues);
		// Correct, the current IP starts within the submask "255.255.255.0"
		Assert.assertTrue(result);

		value1.setValue("192.\\d{3}.0.*");
		result = ipConditionlet.evaluate(request, null, comparisons.get(4)
				.getId(), conditionletValues);
		// Correct, the current IP matches the RegEx
		Assert.assertTrue(result);
	}

	@Test
	public void testUsersReferringUrl() {
		Conditionlet referringConditionlet = new UsersReferringUrlConditionlet();
		// Conditionlet has the input field
		Collection<ConditionletInput> inputCollection = referringConditionlet
				.getInputs(null);
		Assert.assertTrue(inputCollection.size() > 0);
		// Conditionlet has comparisons
		Set<Comparison> comparisonSet = referringConditionlet.getComparisons();
		Assert.assertTrue(comparisonSet.size() > 0);
		List<ConditionletInput> inputs = new ArrayList<ConditionletInput>(
				inputCollection);
		List<Comparison> comparisons = new ArrayList<Comparison>(comparisonSet);
		List<ConditionValue> conditionletValues = new ArrayList<ConditionValue>();
		ConditionValue value1 = new ConditionValue();
		value1.setId(inputs.get(0).getId());
        value1.setKey("isoCode");
		value1.setPriority(1);
		value1.setValue("http://demo.dotcms.com/services/investment-banking");
		conditionletValues.add(value1);
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getHeader("referer")).thenReturn(
				"http://demo.dotcms.com/services/investment-banking");
		boolean result = referringConditionlet.evaluate(request, null,
				comparisons.get(0).getId(), conditionletValues);
		// Correct, the referring URL is
		// "http://demo.dotcms.com/services/investment-banking"
		Assert.assertTrue(result);

		value1.setValue("http://demo.dotcms.com/products");
		result = referringConditionlet.evaluate(request, null,
				comparisons.get(1).getId(), conditionletValues);
		// Correct, the referring URL IS NOT
		// "http://demo.dotcms.com/products"
		Assert.assertTrue(result);

		value1.setValue("http://demo.dotcms.com/services");
		result = referringConditionlet.evaluate(request, null,
				comparisons.get(2).getId(), conditionletValues);
		// Correct, the referring URL starts with
		// "http://demo.dotcms.com/services"
		Assert.assertTrue(result);

		value1.setValue("investment-banking");
		result = referringConditionlet.evaluate(request, null,
				comparisons.get(3).getId(), conditionletValues);
		// Correct, the referring URL ends with "investment-banking"
		Assert.assertTrue(result);

		value1.setValue("/services/investment-banking");
		result = referringConditionlet.evaluate(request, null,
				comparisons.get(4).getId(), conditionletValues);
		// Correct, the referring URL contains "/services/investment-banking"
		Assert.assertTrue(result);

		value1.setValue("https?://.*.com/services.*");
		result = referringConditionlet.evaluate(request, null,
				comparisons.get(5).getId(), conditionletValues);
		// Correct, the referring URL matches the RegEx
		Assert.assertTrue(result);
	}

	@Test
	public void testUsersVisitedUrl() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Host host = new Host();
		host.setHostname("demo.dotcms.com");
		host.setIdentifier("48190c8c-42c4-46af-8d1a-0cd5db894797");
		Mockito.when(request.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(
				host);
		Conditionlet visitedConditionlet = new UsersVisitedUrlConditionlet();
		// Conditionlet has the input field
		Collection<ConditionletInput> inputCollection = visitedConditionlet
				.getInputs(null);
		Assert.assertTrue(inputCollection.size() > 0);
		// Conditionlet has comparisons
		Set<Comparison> comparisonSet = visitedConditionlet.getComparisons();
		Assert.assertTrue(comparisonSet.size() > 0);
		List<ConditionletInput> inputs = new ArrayList<ConditionletInput>(
				inputCollection);
		List<Comparison> comparisons = new ArrayList<Comparison>(comparisonSet);
		List<ConditionValue> conditionletValues = new ArrayList<ConditionValue>();
		ConditionValue value1 = new ConditionValue();
		value1.setId(inputs.get(0).getId());
        value1.setKey("isoCode");
		value1.setPriority(1);
		value1.setValue("/products");
		conditionletValues.add(value1);
		Map<String, Set<String>> visitedUrls = new HashMap<String, Set<String>>();
		Set<String> urls = new LinkedHashSet<String>();
		urls.add("/products");
		urls.add("/contact-us");
		urls.add("/about-us/index");
		visitedUrls.put("48190c8c-42c4-46af-8d1a-0cd5db894797", urls);
		HttpSession session = Mockito.mock(HttpSession.class);
		Mockito.when(
				session.getAttribute(WebKeys.RULES_CONDITIONLET_VISITEDURLS))
				.thenReturn(visitedUrls);
		Mockito.when(request.getSession(true)).thenReturn(session);
		Mockito.when(request.getRequestURI()).thenReturn("/index");
		boolean result = visitedConditionlet.evaluate(request, null,
				comparisons.get(0).getId(), conditionletValues);
		// Correct, a visited URL is "/products"
		Assert.assertTrue(result);

		value1.setValue("/news-events/news");
		result = visitedConditionlet.evaluate(request, null, comparisons.get(1)
				.getId(), conditionletValues);
		// Correct, a visited URL IS NOT "/news-events/news"
		Assert.assertTrue(result);

		value1.setValue("/about-us");
		result = visitedConditionlet.evaluate(request, null, comparisons.get(2)
				.getId(), conditionletValues);
		// Correct, a visited URL starts with "/about-us"
		Assert.assertTrue(result);

		value1.setValue("-us");
		result = visitedConditionlet.evaluate(request, null, comparisons.get(3)
				.getId(), conditionletValues);
		// Correct, a visited URL ends with "-us"
		Assert.assertTrue(result);

		value1.setValue("contact");
		result = visitedConditionlet.evaluate(request, null, comparisons.get(4)
				.getId(), conditionletValues);
		// Correct, a visited URL contains "contact"
		Assert.assertTrue(result);

		value1.setValue("/.*-us");
		result = visitedConditionlet.evaluate(request, null, comparisons.get(5)
				.getId(), conditionletValues);
		// Correct, a visited URL matches the RegEx
		Assert.assertTrue(result);
	}

	@Test
	public void testUsersCurrentUrl() {
		Conditionlet currentConditionlet = new UsersCurrentUrlConditionlet();
		// Conditionlet has the input field
		Collection<ConditionletInput> inputCollection = currentConditionlet
				.getInputs(null);
		Assert.assertTrue(inputCollection.size() > 0);
		// Conditionlet has comparisons
		Set<Comparison> comparisonSet = currentConditionlet.getComparisons();
		Assert.assertTrue(comparisonSet.size() > 0);
		List<ConditionletInput> inputs = new ArrayList<ConditionletInput>(
				inputCollection);
		List<Comparison> comparisons = new ArrayList<Comparison>(comparisonSet);
		List<ConditionValue> conditionletValues = new ArrayList<ConditionValue>();
		ConditionValue value1 = new ConditionValue();
		value1.setId(inputs.get(0).getId());
        value1.setKey("isoCode");
		value1.setPriority(1);
		value1.setValue("/services/investment-banking");
		conditionletValues.add(value1);
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getRequestURI()).thenReturn(
				"/services/investment-banking");
		boolean result = currentConditionlet.evaluate(request, null,
				comparisons.get(0).getId(), conditionletValues);
		// Correct, the current URL is "/index"
		Assert.assertTrue(result);

		value1.setValue("/news-events/news");
		result = currentConditionlet.evaluate(request, null, comparisons.get(1)
				.getId(), conditionletValues);
		// Correct, the current URL IS NOT "/news-events/news"
		Assert.assertTrue(result);

		value1.setValue("/services/");
		result = currentConditionlet.evaluate(request, null, comparisons.get(2)
				.getId(), conditionletValues);
		// Correct, the current URL starts with "/services/"
		Assert.assertTrue(result);

		value1.setValue("banking");
		result = currentConditionlet.evaluate(request, null, comparisons.get(3)
				.getId(), conditionletValues);
		// Correct, the current URL ends with "banking"
		Assert.assertTrue(result);

		value1.setValue("investment");
		result = currentConditionlet.evaluate(request, null, comparisons.get(4)
				.getId(), conditionletValues);
		// Correct, the current URL contains "investment"
		Assert.assertTrue(result);

		value1.setValue("/ser.*/investment-.*");
		result = currentConditionlet.evaluate(request, null, comparisons.get(5)
				.getId(), conditionletValues);
		// Correct, the current URL matches the RegEx
		Assert.assertTrue(result);
	}

	@Test
	public void testUsersSiteVisits() {
		Conditionlet visitsConditionlet = new UsersSiteVisitsConditionlet();
		// Conditionlet has the input field
		Collection<ConditionletInput> inputCollection = visitsConditionlet
				.getInputs(null);
		Assert.assertTrue(inputCollection != null);
		// Conditionlet has comparisons
		Set<Comparison> comparisonSet = visitsConditionlet.getComparisons();
		Assert.assertTrue(comparisonSet.size() > 0);
		List<ConditionletInput> inputs = new ArrayList<ConditionletInput>(
				inputCollection);
		List<Comparison> comparisons = new ArrayList<Comparison>(comparisonSet);
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Host host = new Host();
		host.setHostname("demo.dotcms.com");
		host.setIdentifier("48190c8c-42c4-46af-8d1a-0cd5db894797");
		Mockito.when(request.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(
				host);
		HttpSession session = Mockito.mock(HttpSession.class);
		Mockito.when(
				session.getAttribute(com.liferay.portal.util.WebKeys.USER_ID))
				.thenReturn("dotcms.org.1");
		User user = new User();
		user.setUserId("dotcms.org.1");
		Mockito.when(request.getAttribute(com.liferay.portal.util.WebKeys.USER))
				.thenReturn(user);
		Mockito.when(request.getSession(false)).thenReturn(session);
		List<ConditionValue> conditionletValues = new ArrayList<ConditionValue>();
		ConditionValue value1 = new ConditionValue();
		value1.setId(inputs.get(0).getId());
        value1.setKey("isoCode");
		value1.setPriority(1);
		value1.setValue("2");
		conditionletValues.add(value1);
		UsersSiteVisitsConditionlet mockedConditionlet = Mockito
				.mock(UsersSiteVisitsConditionlet.class);
		Mockito.when(mockedConditionlet.getInputs(Mockito.anyString()))
				.thenCallRealMethod();
		Mockito.when(mockedConditionlet.getComparisons()).thenCallRealMethod();
		Mockito.when(mockedConditionlet.getComparisonById(Mockito.anyString()))
				.thenCallRealMethod();
		ValidationResults results = new ValidationResults();
		results.setErrors(false);
		Mockito.when(
				mockedConditionlet.validate(Mockito.any(Comparison.class),
						Mockito.anySetOf(ConditionletInputValue.class)))
				.thenReturn(results);
		Mockito.when(
				mockedConditionlet.evaluate(request, null, comparisons.get(0)
						.getId(), conditionletValues)).thenCallRealMethod();
		Mockito.when(
				mockedConditionlet.getSiteVisits(Mockito.anyString(),
						Mockito.anyString())).thenReturn(5);
		boolean result = mockedConditionlet.evaluate(request, null, comparisons
				.get(0).getId(), conditionletValues);
		// Correct, the number of visits is greater than 2
		Assert.assertTrue(result);

		Mockito.when(
				mockedConditionlet.evaluate(request, null, comparisons.get(1)
						.getId(), conditionletValues)).thenCallRealMethod();
		value1.setValue("3");
		result = mockedConditionlet.evaluate(request, null, comparisons.get(1)
				.getId(), conditionletValues);
		// Correct, the number of visits is greater or equal than 3
		Assert.assertTrue(result);

		value1.setValue("5");
		Mockito.when(
				mockedConditionlet.evaluate(request, null, comparisons.get(2)
						.getId(), conditionletValues)).thenCallRealMethod();
		result = mockedConditionlet.evaluate(request, null, comparisons.get(2)
				.getId(), conditionletValues);
		// Correct, the number of visits is equal to 5
		Assert.assertTrue(result);

		value1.setValue("6");
		Mockito.when(
				mockedConditionlet.evaluate(request, null, comparisons.get(3)
						.getId(), conditionletValues)).thenCallRealMethod();
		result = mockedConditionlet.evaluate(request, null, comparisons.get(3)
				.getId(), conditionletValues);
		// Correct, the number of visits is lower or equal than 6
		Assert.assertTrue(result);

		value1.setValue("10");
		Mockito.when(
				mockedConditionlet.evaluate(request, null, comparisons.get(4)
						.getId(), conditionletValues)).thenCallRealMethod();
		result = mockedConditionlet.evaluate(request, null, comparisons.get(4)
				.getId(), conditionletValues);
		// Correct, the number of visits is lower than 10
		Assert.assertTrue(result);
	}

	@Test
	public void testUsersPageVisits() {
		Conditionlet pagesConditionlet = new UsersPageVisitsConditionlet();
		// Conditionlet has the input field
		Collection<ConditionletInput> inputCollection = pagesConditionlet
				.getInputs(null);
		Assert.assertTrue(inputCollection.size() > 0);
		// Conditionlet has comparisons
		Set<Comparison> comparisonSet = pagesConditionlet.getComparisons();
		Assert.assertTrue(comparisonSet.size() > 0);
		List<ConditionletInput> inputs = new ArrayList<ConditionletInput>(
				inputCollection);
		List<Comparison> comparisons = new ArrayList<Comparison>(comparisonSet);
		List<ConditionValue> conditionletValues = new ArrayList<ConditionValue>();
		ConditionValue value1 = new ConditionValue();
		value1.setId(inputs.get(0).getId());
        value1.setKey("isoCode");
		value1.setPriority(1);
		value1.setValue("1");
		conditionletValues.add(value1);
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		HttpSession session = Mockito.mock(HttpSession.class);
		Mockito.when(request.getSession(true)).thenReturn(session);
		Mockito.when(request.getRequestURI()).thenReturn(
				"/services/investment-banking");
		Host host = new Host();
		host.setHostname("demo.dotcms.com");
		host.setIdentifier("48190c8c-42c4-46af-8d1a-0cd5db894797");
		Mockito.when(request.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(
				host);
		Map<String, Set<String>> visitedUrls = new HashMap<String, Set<String>>();
		Set<String> urls = new LinkedHashSet<String>();
		urls.add("/products");
		visitedUrls.put("48190c8c-42c4-46af-8d1a-0cd5db894797", urls);
		Mockito.when(
				session.getAttribute(WebKeys.RULES_CONDITIONLET_VISITEDURLS))
				.thenReturn(visitedUrls);
		boolean result = pagesConditionlet.evaluate(request, null, comparisons
				.get(0).getId(), conditionletValues);
		// Correct, the number of visited pages is greater than 1
		Assert.assertTrue(result);

		value1.setValue("2");
		result = pagesConditionlet.evaluate(request, null, comparisons.get(1)
				.getId(), conditionletValues);
		// Correct, the number of visited pages is greater then or equal to 2
		Assert.assertTrue(result);

		urls.add("/contact-us");
		value1.setValue("3");
		result = pagesConditionlet.evaluate(request, null, comparisons.get(2)
				.getId(), conditionletValues);
		// Correct, the number of visited pages is equal to 3
		Assert.assertTrue(result);

		value1.setValue("3");
		result = pagesConditionlet.evaluate(request, null, comparisons.get(3)
				.getId(), conditionletValues);
		// Correct, the number of visited pages is lower than or equal to 3
		Assert.assertTrue(result);

		value1.setValue("10");
		result = pagesConditionlet.evaluate(request, null, comparisons.get(4)
				.getId(), conditionletValues);
		// Correct, the number of visited pages is lower than 10
		Assert.assertTrue(result);
	}

	@Test
	public void testUsersPlatform() {
		Conditionlet platformConditionlet = new UsersPlatformConditionlet();
		// Conditionlet has the input field
		Collection<ConditionletInput> inputCollection = platformConditionlet
				.getInputs(null);
		Assert.assertTrue(inputCollection.size() > 0);
		// Conditionlet has comparisons
		Set<Comparison> comparisonSet = platformConditionlet.getComparisons();
		Assert.assertTrue(comparisonSet.size() > 0);
		List<ConditionletInput> inputs = new ArrayList<ConditionletInput>(
				inputCollection);
		List<Comparison> comparisons = new ArrayList<Comparison>(comparisonSet);
		List<ConditionValue> conditionletValues = new ArrayList<ConditionValue>();
		ConditionValue value1 = new ConditionValue();
		value1.setId(inputs.get(0).getId());
        value1.setKey("isoCode");
		value1.setPriority(1);
		value1.setValue("Computer");
		conditionletValues.add(value1);
		String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.65 Safari/537.36";
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getHeader("User-Agent")).thenReturn(userAgent);
		boolean result = platformConditionlet.evaluate(request, null,
				comparisons.get(0).getId(), conditionletValues);
		// Correct, the current platform is "computer"
		Assert.assertTrue(result);

		value1.setValue("Mobile");
		userAgent = "Mozilla/5.0 (Linux; U; Android 4.0.3; ko-kr; LG-L160L Build/IML74K) AppleWebkit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30";
		Mockito.when(request.getHeader("User-Agent")).thenReturn(userAgent);
		result = platformConditionlet.evaluate(request, null, comparisons
				.get(0).getId(), conditionletValues);
		// Correct, the current platform is "mobile"
		Assert.assertTrue(result);

		value1.setValue("Tablet");
		userAgent = "Mozilla/5.0 (iPad; CPU OS 7_0 like Mac OS X) AppleWebKit/537.51.1 (KHTML, like Gecko) Version/7.0 Mobile/11A465 Safari/9537.53";
		Mockito.when(request.getHeader("User-Agent")).thenReturn(userAgent);
		result = platformConditionlet.evaluate(request, null, comparisons
				.get(0).getId(), conditionletValues);
		// Correct, the current platform is "mobile"
		Assert.assertTrue(result);

		value1.setValue("Game console");
		userAgent = "Mozilla/5.0 (PLAYSTATION 3; 3.55)";
		Mockito.when(request.getHeader("User-Agent")).thenReturn(userAgent);
		result = platformConditionlet.evaluate(request, null, comparisons
				.get(0).getId(), conditionletValues);
		// Correct, the current platform is "gaming console"
		Assert.assertTrue(result);

		value1.setValue("Digital media receiver");
		userAgent = "Mozilla/5.0 (X11; U; Linux i686; en-US) AppleWebKit/533.4 (KHTML, like Gecko) Chrome/5.0.375.127 Large Screen Safari/533.4 GoogleTV/ 162671";
		Mockito.when(request.getHeader("User-Agent")).thenReturn(userAgent);
		result = platformConditionlet.evaluate(request, null, comparisons
				.get(0).getId(), conditionletValues);
		// Correct, the current platform is "digital medio receiver"
		Assert.assertTrue(result);
	}

	@Test
	public void testUsersDateTime() throws UnknownHostException, IOException,
			GeoIp2Exception {
		// Get client's time to add/subtract time and compare
		Calendar baseClientDate = GeoIp2CityDbUtil.getInstance().getDateTime(
				IP_ADDRESS_1);
		Conditionlet datetimeConditionlet = new UsersDateTimeConditionlet();
		// Conditionlet has the input field
		Collection<ConditionletInput> inputCollection = datetimeConditionlet
				.getInputs(null);
		Assert.assertTrue(inputCollection.size() > 0);
		// Conditionlet has comparisons
		Set<Comparison> comparisonSet = datetimeConditionlet.getComparisons();
		Assert.assertTrue(comparisonSet.size() > 0);
		List<ConditionletInput> inputs = new ArrayList<ConditionletInput>(
				inputCollection);
		List<Comparison> comparisons = new ArrayList<Comparison>(comparisonSet);
		List<ConditionValue> conditionletValues = new ArrayList<ConditionValue>();
		ConditionValue value1 = new ConditionValue();
		value1.setId(inputs.get(0).getId());
        value1.setKey("isoCode");
		value1.setPriority(1);
		baseClientDate.add(Calendar.MINUTE, -5);
		value1.setValue("" + baseClientDate.getTime().getTime());
		conditionletValues.add(value1);
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getRemoteAddr()).thenReturn(IP_ADDRESS_1);
		boolean result = datetimeConditionlet.evaluate(request, null,
				comparisons.get(0).getId(), conditionletValues);
		// Correct, the client's time is greater than the specified date
		Assert.assertTrue(result);

		result = datetimeConditionlet.evaluate(request, null, comparisons
				.get(1).getId(), conditionletValues);
		// Correct, the client's time is greater than or equal to the specified
		// date
		Assert.assertTrue(result);

		baseClientDate.add(Calendar.MINUTE, 15);
		value1.setValue("" + baseClientDate.getTime().getTime());
		result = datetimeConditionlet.evaluate(request, null, comparisons
				.get(3).getId(), conditionletValues);
		// Correct, the client's time is lower than or equal to the specified
		// date
		Assert.assertTrue(result);

		value1.setValue("" + baseClientDate.getTime().getTime());
		result = datetimeConditionlet.evaluate(request, null, comparisons
				.get(4).getId(), conditionletValues);
		// Correct, the client's time is lower than or equal to the specified
		// date
		Assert.assertTrue(result);
	}

	@Test
	public void testUsersUrlParameter() {
		Conditionlet urlConditionlet = new UsersUrlParameterConditionlet();
		// Conditionlet has the input field
		Collection<ConditionletInput> inputCollection = urlConditionlet
				.getInputs(null);
		Assert.assertTrue(inputCollection.size() > 0);
		// Conditionlet has comparisons
		Set<Comparison> comparisonSet = urlConditionlet.getComparisons();
		Assert.assertTrue(comparisonSet.size() > 0);
		List<ConditionletInput> inputs = new ArrayList<ConditionletInput>(
				inputCollection);
		List<Comparison> comparisons = new ArrayList<Comparison>(comparisonSet);
		List<ConditionValue> conditionletValues = new ArrayList<ConditionValue>();
		ConditionValue value1 = new ConditionValue();
		value1.setId(inputs.get(0).getId());
        value1.setKey("isoCode");
		value1.setPriority(1);
		value1.setValue("query");
		conditionletValues.add(value1);
		ConditionValue value2 = new ConditionValue();
		value2.setId(inputs.get(1).getId());
        value2.setKey("isoCode");
		value2.setPriority(2);
		value2.setValue("testparametervalue");
		conditionletValues.add(value2);
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getParameter("query")).thenReturn(
				"testparametervalue");
		boolean result = urlConditionlet.evaluate(request, null, comparisons
				.get(0).getId(), conditionletValues);
		// Correct, the URL parameter is called "query" and is
		// "testparametervalue"
		Assert.assertTrue(result);

		value2.setValue("other-testparametervalue");
		result = urlConditionlet.evaluate(request, null, comparisons.get(1)
				.getId(), conditionletValues);
		// Correct, the URL parameter is called "query" and IS NOT
		// "testparametervalue"
		Assert.assertTrue(result);

		value2.setValue("test");
		result = urlConditionlet.evaluate(request, null, comparisons.get(2)
				.getId(), conditionletValues);
		// Correct, the URL parameter is called "query" and starts with
		// "test"
		Assert.assertTrue(result);

		value2.setValue("value");
		result = urlConditionlet.evaluate(request, null, comparisons.get(3)
				.getId(), conditionletValues);
		// Correct, the URL parameter is called "query" and ends with
		// "test"
		Assert.assertTrue(result);

		value2.setValue("parameter");
		result = urlConditionlet.evaluate(request, null, comparisons.get(4)
				.getId(), conditionletValues);
		// Correct, the URL parameter is called "query" and contains
		// "parameter"
		Assert.assertTrue(result);

		value2.setValue("test.*value");
		result = urlConditionlet.evaluate(request, null, comparisons.get(5)
				.getId(), conditionletValues);
		// Correct, the URL parameter is called "query" and matches the RegEx
		Assert.assertTrue(result);
	}

	@Test
	public void testUsersLandingPage() {
		Conditionlet landingConditionlet = new UsersLandingPageUrlConditionlet();
		// Conditionlet has the input field
		Collection<ConditionletInput> inputCollection = landingConditionlet
				.getInputs(null);
		Assert.assertTrue(inputCollection.size() > 0);
		// Conditionlet has comparisons
		Set<Comparison> comparisonSet = landingConditionlet.getComparisons();
		Assert.assertTrue(comparisonSet.size() > 0);
		List<ConditionletInput> inputs = new ArrayList<ConditionletInput>(
				inputCollection);
		List<Comparison> comparisons = new ArrayList<Comparison>(comparisonSet);
		List<ConditionValue> conditionletValues = new ArrayList<ConditionValue>();
		ConditionValue value1 = new ConditionValue();
		value1.setId(inputs.get(0).getId());
        value1.setKey("isoCode");
		value1.setPriority(1);
		value1.setValue("/contact-us");
		conditionletValues.add(value1);
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		HttpSession session = Mockito.mock(HttpSession.class);
		Clickstream clickstream = new Clickstream();
		List<ClickstreamRequest> clickstreamRequests = new ArrayList<ClickstreamRequest>();
		ClickstreamRequest clickReq1 = new ClickstreamRequest();
		clickReq1.setRequestURI("/contact-us");
		ClickstreamRequest clickReq2 = new ClickstreamRequest();
		clickReq2.setRequestURI("/products");
		ClickstreamRequest clickReq3 = new ClickstreamRequest();
		clickReq3.setRequestURI("/about-us/index");
		clickstreamRequests.add(clickReq1);
		clickstreamRequests.add(clickReq2);
		clickstreamRequests.add(clickReq3);
		clickstream.setClickstreamRequests(clickstreamRequests);
		Mockito.when(request.getSession(true)).thenReturn(session);
		Mockito.when(session.getAttribute("clickstream")).thenReturn(
				clickstream);
		boolean result = landingConditionlet.evaluate(request, null,
				comparisons.get(0).getId(), conditionletValues);
		// Correct, the landing page is "/contact-us"
		Assert.assertTrue(result);

		value1.setValue("/products");
		result = landingConditionlet.evaluate(request, null, comparisons.get(1)
				.getId(), conditionletValues);
		// Correct, the landing page IS NOT "/products"
		Assert.assertTrue(result);

		value1.setValue("/contact");
		result = landingConditionlet.evaluate(request, null, comparisons.get(2)
				.getId(), conditionletValues);
		// Correct, the landing page starts with "/contact"
		Assert.assertTrue(result);

		value1.setValue("-us");
		result = landingConditionlet.evaluate(request, null, comparisons.get(3)
				.getId(), conditionletValues);
		// Correct, the landing page ends with "/-us"
		Assert.assertTrue(result);

		value1.setValue("contact");
		result = landingConditionlet.evaluate(request, null, comparisons.get(4)
				.getId(), conditionletValues);
		// Correct, the landing page contains "contact"
		Assert.assertTrue(result);

		value1.setValue("/.*-us");
		result = landingConditionlet.evaluate(request, null, comparisons.get(5)
				.getId(), conditionletValues);
		// Correct, the landing page matches the RegEx
		Assert.assertTrue(result);
	}

	@Test
	public void testUsersTime() throws UnknownHostException, IOException,
			GeoIp2Exception {
		// Get client's time to add/subtract time and compare
		Calendar baseClientDate = GeoIp2CityDbUtil.getInstance().getDateTime(
				IP_ADDRESS_1);
		Conditionlet timeConditionlet = new UsersTimeConditionlet();
		// Conditionlet has comparisons
		Set<Comparison> comparisonSet = timeConditionlet.getComparisons();
		Assert.assertTrue(comparisonSet.size() > 0);
		List<Comparison> comparisons = new ArrayList<Comparison>(comparisonSet);
		// Conditionlet has the input field
		Collection<ConditionletInput> inputCollection = timeConditionlet
				.getInputs(comparisons.get(0).getId());
		List<ConditionletInput> inputs = new ArrayList<ConditionletInput>(
				inputCollection);
		Assert.assertTrue(inputCollection.size() > 0);
		List<ConditionValue> conditionletValues = new ArrayList<ConditionValue>();
		ConditionValue value1 = new ConditionValue();
		value1.setId(inputs.get(0).getId());
        value1.setKey("isoCode");
		value1.setPriority(1);
		baseClientDate.add(Calendar.MINUTE, -5);
		value1.setValue("" + baseClientDate.getTime().getTime());
		conditionletValues.add(value1);
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getRemoteAddr()).thenReturn(IP_ADDRESS_1);
		boolean result = timeConditionlet.evaluate(request, null, comparisons
				.get(0).getId(), conditionletValues);
		// Correct, the client's time is greater than the specified date
		Assert.assertTrue(result);

		result = timeConditionlet.evaluate(request, null, comparisons.get(1)
				.getId(), conditionletValues);
		// Correct, the client's time is greater than or equal to the specified
		// date
		Assert.assertTrue(result);

		baseClientDate.add(Calendar.MINUTE, 15);
		value1.setValue("" + baseClientDate.getTime().getTime());
		result = timeConditionlet.evaluate(request, null, comparisons.get(3)
				.getId(), conditionletValues);
		// Correct, the client's time is lower than or equal to the specified
		// date
		Assert.assertTrue(result);

		value1.setValue("" + baseClientDate.getTime().getTime());
		result = timeConditionlet.evaluate(request, null, comparisons.get(4)
				.getId(), conditionletValues);
		// Correct, the client's time is lower than or equal to the specified
		// date
		Assert.assertTrue(result);

		inputCollection = timeConditionlet
				.getInputs(comparisons.get(5).getId());
		inputs = new ArrayList<ConditionletInput>(inputCollection);
		baseClientDate.add(Calendar.MINUTE, -15);
		value1.setValue("" + baseClientDate.getTime().getTime());
		ConditionValue value2 = new ConditionValue();
		value2.setId(inputs.get(1).getId());
        value2.setKey("isoCode");
		value2.setPriority(2);
		baseClientDate.add(Calendar.MINUTE, 15);
		value2.setValue("" + baseClientDate.getTime().getTime());
		conditionletValues.add(value2);
		result = timeConditionlet.evaluate(request, null, comparisons.get(5)
				.getId(), conditionletValues);
		// Correct, the client's time is between the specified dates
		Assert.assertTrue(result);
	}

	@Test
	public void testRequestHeader() {
		Conditionlet headerConditionlet = new RequestHeaderConditionlet();
		// Conditionlet has the input field
		Collection<ConditionletInput> inputCollection = headerConditionlet
				.getInputs(null);
		Assert.assertTrue(inputCollection.size() > 0);
		// Conditionlet has comparisons
		Set<Comparison> comparisonSet = headerConditionlet.getComparisons();
		Assert.assertTrue(comparisonSet.size() > 0);
		List<ConditionletInput> inputs = new ArrayList<ConditionletInput>(
				inputCollection);
		List<Comparison> comparisons = new ArrayList<Comparison>(comparisonSet);
		List<ConditionValue> conditionletValues = new ArrayList<ConditionValue>();
		ConditionValue value1 = new ConditionValue();
		value1.setId(inputs.get(0).getId());
        value1.setKey("headerKeyValue");
		value1.setPriority(1);
		value1.setValue("User-Agent");
		conditionletValues.add(value1);
		ConditionValue value2 = new ConditionValue();
		value2.setId(inputs.get(1).getId());
        value2.setKey("compareTo");
		value2.setPriority(2);
		value2.setValue("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.65 Safari/537.36");
		conditionletValues.add(value2);
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getHeader("User-Agent"))
				.thenReturn(
						"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.65 Safari/537.36");
		boolean result = headerConditionlet.evaluate(request, null, comparisons
				.get(0).getId(), conditionletValues);
		// Correct, the browser header "User-Agent" is the specified one
		Assert.assertTrue(result);

		value2.setValue("Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; rv:11.0) like Gecko");
		result = headerConditionlet.evaluate(request, null, comparisons.get(1)
				.getId(), conditionletValues);
		// Correct, the browser header "User-Agent" IS NOT the specified one
		Assert.assertTrue(result);

		value2.setValue("Mozilla/5.0 (Macintosh");
		result = headerConditionlet.evaluate(request, null, comparisons.get(2)
				.getId(), conditionletValues);
		// Correct, the browser header "User-Agent" starts with the specified
		// one
		Assert.assertTrue(result);

		value2.setValue("Chrome/43.0.2357.65 Safari/537.36");
		result = headerConditionlet.evaluate(request, null, comparisons.get(3)
				.getId(), conditionletValues);
		// Correct, the browser header "User-Agent" ends with the specified one
		Assert.assertTrue(result);

		value2.setValue("Chrome");
		result = headerConditionlet.evaluate(request, null, comparisons.get(4)
				.getId(), conditionletValues);
		// Correct, the browser header "User-Agent" contains the specified one
		Assert.assertTrue(result);

		value2.setValue("Mozilla/5.0 \\([Macintosh;|Windows.*;|Linux.*;].* Chrome/.* Safari/.*");
		result = headerConditionlet.evaluate(request, null, comparisons.get(5)
				.getId(), conditionletValues);
		// Correct, the browser header "User-Agent" matches the RegEx
		Assert.assertTrue(result);
	}

	@Test
	public void testUsersLogIn() {
		Conditionlet loginConditionlet = new UsersLogInConditionlet();
		// Conditionlet has the input field
		Collection<ConditionletInput> inputCollection = loginConditionlet
				.getInputs(null);
		Assert.assertTrue(inputCollection == null);
		// Conditionlet has comparisons
		Set<Comparison> comparisonSet = loginConditionlet.getComparisons();
		Assert.assertTrue(comparisonSet.size() > 0);
		List<Comparison> comparisons = new ArrayList<Comparison>(comparisonSet);
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		HttpSession session = Mockito.mock(HttpSession.class);
		Mockito.when(
				session.getAttribute(com.liferay.portal.util.WebKeys.USER_ID))
				.thenReturn("dotcms.org.1");
		User user = new User();
		user.setUserId("dotcms.org.1");
		Mockito.when(request.getAttribute(com.liferay.portal.util.WebKeys.USER))
				.thenReturn(user);
		Mockito.when(request.getSession(false)).thenReturn(session);
		boolean result = loginConditionlet.evaluate(request, null, comparisons
				.get(0).getId(), null);
		// Correct, the specified user is logged in
		Assert.assertTrue(result);

		Mockito.when(
				session.getAttribute(com.liferay.portal.util.WebKeys.USER_ID))
				.thenReturn(null);
		result = loginConditionlet.evaluate(request, null, comparisons.get(1)
				.getId(), null);
		// Correct, the specified user IS NOT logged in
		Assert.assertTrue(result);
	}

}
