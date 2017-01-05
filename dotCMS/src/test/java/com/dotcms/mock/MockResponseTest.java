package com.dotcms.mock;

import static org.hamcrest.MatcherAssert.assertThat;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import com.dotcms.UnitTestBase;
import com.dotcms.mock.response.BaseResponse;
import com.dotcms.mock.response.MockHttpResponse;
/**
 * 
 * Unit test for request and resonse mock
 *
 */
public class MockResponseTest extends UnitTestBase {
	
	
	
	final String TEST="test";
	final String HOST="demo.dotcms.com";
	final String URI="/index";
	final String PROTOCOL="http://";
	final String IP="127.0.0.1";
	
	@Test
	public void testBaseRequest() {
		assertThat("response is not null", new BaseResponse().response() !=null);
		assertThat("response is a response ", new BaseResponse().response() instanceof ServletResponse);
	}

	@Test
	public void testHttpRequest() {
		
		assertThat("response is not null", new MockHttpResponse(new BaseResponse().response()).response() !=null);
		assertThat("response is a response ", new MockHttpResponse(new BaseResponse().response()).response() instanceof HttpServletResponse);
	}
	
}
