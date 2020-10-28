package com.dotcms.mock;

import static org.hamcrest.MatcherAssert.assertThat;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

import com.dotcms.UnitTestBase;
import com.dotcms.mock.request.BaseRequest;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockInternalRequest;
import com.dotcms.mock.request.MockSessionRequest;
/**
 * 
 * Unit test for request and resonse mock
 *
 */
public class MockRequestTest extends UnitTestBase {
	
	
	
	final String TEST="test";
	final String HOST="demo.dotcms.com";
	final String URI="/index";
	final String PROTOCOL="http://";
	final String IP="127.0.0.1";
	
	@Test
	public void testBaseRequest() {
		assertThat("request is not null", new BaseRequest().request() !=null);
		assertThat("request is a request ", new BaseRequest().request() instanceof ServletRequest);
	}

	
	@Test
	public void testMockAttributeRequest() {
		HttpServletRequest request = 	new MockAttributeRequest(new BaseRequest().request());
		assertThat("request is not null", request !=null);
		assertThat("request attribute is not null", request.getAttributeNames() != null);
		request.setAttribute(TEST, TEST);
		assertThat("request attribute sets an attribute", TEST.equals(request.getAttribute(TEST)));
	}
	
	@Test
	public void testMockHeaderRequest() {
		HttpServletRequest request = 	new MockHeaderRequest(new BaseRequest().request(), TEST,TEST);
		assertThat("request is not null", request !=null);
		assertThat("request headers is not null", request.getHeaderNames()!= null);
		assertThat("request header is not null", request.getHeader(TEST).equals(TEST));
	}
	
	@Test
	public void testMockSessionRequest() {
		HttpServletRequest request = 	new MockSessionRequest(new BaseRequest().request());
		assertThat("request is not null", request !=null);
		assertThat("request.getSession(false) is null", request.getSession(false) == null);
		assertThat("request.getSession(true) is not null", request.getSession(true) != null);
		request.getSession().setAttribute(TEST, TEST);
		assertThat("request session gets an attribute", TEST.equals(request.getSession().getAttribute(TEST)));		
	}
	
	@Test
	public void testMockHttpRequest() {
		HttpServletRequest request = 	new MockHttpRequest(HOST,URI).request();
		assertThat("request ip is not null", IP.equals(request.getRemoteAddr()));
		assertThat("request url works", (PROTOCOL + HOST + URI).equals(request.getRequestURL().toString()));
		assertThat("request uri works", URI.equals(request.getRequestURI()));
		assertThat("request isSecure is false", !request.isSecure());
		assertThat("request is a httprequest ", request instanceof HttpServletRequest);
	}
	
	@Test
	public void testMockInternalRequest() {
		HttpServletRequest request = 	new MockInternalRequest().request();
		assertThat("request ip is not null", IP.equals(request.getRemoteAddr()));
		assertThat("request url works", (PROTOCOL  + "127.0.0.1/").equals(request.getRequestURL().toString()));
		assertThat("request uri works", "/".equals(request.getRequestURI()));
		assertThat("request isSecure is false", !request.isSecure());
		assertThat("request is a httprequest ", request instanceof HttpServletRequest);
	}
	
	
    @Test
    public void testMockHttpRequestWithNullValues() {
        final String expectedHostname="localhost";
        final String expectedUri="/";
        HttpServletRequest request =    new MockHttpRequest(null,null).request();
        assertThat("request ip is not null", IP.equals(request.getRemoteAddr()));

        assertThat("request url works", (PROTOCOL + expectedHostname + expectedUri).equals(request.getRequestURL().toString()));
        assertThat("request uri works", expectedUri.equals(request.getRequestURI()));
        assertThat("request host works", expectedHostname.equals(request.getServerName()));

        assertThat("request is a httprequest ", request instanceof HttpServletRequest);
    }
	    
	
}
