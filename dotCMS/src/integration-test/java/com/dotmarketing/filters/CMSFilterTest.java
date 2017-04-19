package com.dotmarketing.filters;

import static org.mockito.Matchers.startsWith;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.dotcms.LicenseTestUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.VirtualLinksCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.virtuallinks.business.VirtualLinkAPI;
import com.dotmarketing.portlets.virtuallinks.model.VirtualLink;
import com.dotmarketing.servlets.SpeedyAssetServlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.velocity.ClientVelocityServlet;
import com.dotmarketing.velocity.VelocityServlet;

public class CMSFilterTest {
	
	@BeforeClass
    public static void prepare () throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();
        Mockito.when(Config.CONTEXT.getRealPath(startsWith("/"))).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return (String) invocation.getArguments()[0];
            }
        });
        
        Mockito.when(Config.CONTEXT.getResourceAsStream(startsWith("/"))).thenAnswer(new Answer<FileInputStream>() {
            @Override
            public FileInputStream answer(InvocationOnMock invocation) throws Throwable {
                return new FileInputStream((String) invocation.getArguments()[0]);
            }
        });
        IntegrationTestInitService.getInstance().mockStrutsActionModule();
    }

	@Test
	public void shouldWorkVirtualLink() throws IOException {
		VirtualLinkAPI virtualLinkAPI = APILocator.getVirtualLinkAPI();
		VirtualLink link1 = new VirtualLink();
		VirtualLink link2 = new VirtualLink();
		VirtualLink link3 = new VirtualLink();
		VirtualLink link4 = new VirtualLink();
		VirtualLink link5 = new VirtualLink();

		// build them up
		try {
			link1 = virtualLinkAPI.getVirtualLinkByURL("/testLink1");
			link1 = new VirtualLink();
			link1.setActive(true);
			link1.setTitle("test link1");
			link1.setUri("/about-us/" +CMSFilter.CMS_INDEX_PAGE);
			link1.setUrl("/testLink1");
			HibernateUtil.save(link1);


			link2 = virtualLinkAPI.getVirtualLinkByURL("demo.dotcms.com:/testLink2");

			link2 = new VirtualLink();
			link2.setActive(true);
			link2.setTitle("test link2");
			link2.setUri("/about-us/"+CMSFilter.CMS_INDEX_PAGE);
			link2.setUrl("demo.dotcms.com:/testLink2");
			HibernateUtil.save(link2);


			link3 = virtualLinkAPI.getVirtualLinkByURL("/testLink3");

			link3 = new VirtualLink();
			link3.setActive(true);
			link3.setTitle("test link3");
			link3.setUri("http://demo.dotcms.com/about-us/"+CMSFilter.CMS_INDEX_PAGE);
			link3.setUrl("/testLink3");
			HibernateUtil.save(link3);


			link4 = virtualLinkAPI.getVirtualLinkByURL("demo.dotcms.com:/testLink4");

			link4 = new VirtualLink();
			link4.setActive(true);
			link4.setTitle("test link4");
			link4.setUri("http://demo.dotcms.com/about-us/"+CMSFilter.CMS_INDEX_PAGE);
			link4.setUrl("demo.dotcms.com:/testLink4");
			HibernateUtil.save(link4);

			
			link5 = virtualLinkAPI.getVirtualLinkByURL("demo.dotcms.com:/testLink5");

			link5 = new VirtualLink();
			link5.setActive(true);
			link5.setTitle("test link5");
			link5.setUri("/products/");
			link5.setUrl("/testLink5");
			HibernateUtil.save(link5);
			
			

			
			
			
			CMSFilter cmsFilter = new CMSFilter();
			HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
			MockResponseWrapper response = new MockResponseWrapper(res);
			FilterChain chain = Mockito.mock(FilterChain.class);
			Logger.info(this.getClass(), "/testLink1 should forward to /about-us/"+CMSFilter.CMS_INDEX_PAGE);
			HttpServletRequest request = getMockRequest("demo.dotcms.com", "/testLink1");
			cmsFilter.doFilter(request, response, chain);
			Logger.info(this.getClass(), "looking for 200, got;" + response.getStatus());
			Assert.assertEquals(200, response.getStatus());
			Logger.info(this.getClass(), "looking for /about-us/"+CMSFilter.CMS_INDEX_PAGE+", got;" + request.getAttribute(CMSFilter.CMS_FILTER_URI_OVERRIDE));
			Assert.assertEquals("/about-us/"+CMSFilter.CMS_INDEX_PAGE, request.getAttribute(CMSFilter.CMS_FILTER_URI_OVERRIDE));

			
			
			Logger.info(this.getClass(), "/testLink2 should forward to /about-us/"+CMSFilter.CMS_INDEX_PAGE);
			request = getMockRequest("demo.dotcms.com", "/testLink2");
			response = new MockResponseWrapper(Mockito.mock(HttpServletResponse.class));
			cmsFilter.doFilter(request, response, chain);
			Logger.info(this.getClass(), "looking for 200, got;" + response.getStatus());
			Assert.assertEquals(200, response.getStatus());
			Logger.info(this.getClass(), "looking for /about-us"+CMSFilter.CMS_INDEX_PAGE+", got;" + request.getAttribute(CMSFilter.CMS_FILTER_URI_OVERRIDE));
			Assert.assertEquals("/about-us/"+CMSFilter.CMS_INDEX_PAGE, request.getAttribute(CMSFilter.CMS_FILTER_URI_OVERRIDE));

			
			
			Logger.info(this.getClass(), "/testLink3 should redirect to http://demo.dotcms.com/about-us/"+CMSFilter.CMS_INDEX_PAGE);
			request = getMockRequest("demo.dotcms.com", "/testLink3");
			response = new MockResponseWrapper(Mockito.mock(HttpServletResponse.class));
			cmsFilter.doFilter(request, response, chain);
			Logger.info(this.getClass(), "looking for 301, got;" + response.getStatus());
			Assert.assertEquals(301, response.getStatus());
			Logger.info(this.getClass(), "looking for http://demo.dotcms.com/about-us"+CMSFilter.CMS_INDEX_PAGE+", got;" + response.getRedirectLocation());
			Assert.assertEquals("http://demo.dotcms.com/about-us/"+CMSFilter.CMS_INDEX_PAGE, response.getRedirectLocation());

			
			
			Logger.info(this.getClass(), "/testLink4 should redirect to http://demo.dotcms.com/about-us/"+CMSFilter.CMS_INDEX_PAGE);
			request = getMockRequest("demo.dotcms.com", "/testLink4");
			response = new MockResponseWrapper(Mockito.mock(HttpServletResponse.class));
			cmsFilter.doFilter(request, response, chain);
			
			Logger.info(this.getClass(), "looking for 301, got;" + response.getStatus());
			Assert.assertEquals(301, response.getStatus());
			Logger.info(this.getClass(), "looking for http://demo.dotcms.com/about-us"+CMSFilter.CMS_INDEX_PAGE+", got;" + response.getRedirectLocation());
			Assert.assertEquals("http://demo.dotcms.com/about-us/"+CMSFilter.CMS_INDEX_PAGE, response.getRedirectLocation());
			
			
			Logger.info(this.getClass(), "/testLink5 should forward to /products/"+CMSFilter.CMS_INDEX_PAGE);
			request = getMockRequest("demo.dotcms.com", "/testLink5/");
			cmsFilter.doFilter(request, response, chain);
			response = new MockResponseWrapper(Mockito.mock(HttpServletResponse.class));
			Logger.info(this.getClass(), "looking for 200, got;" + response.getStatus());
			Assert.assertEquals(200, response.getStatus());
			Logger.info(this.getClass(), "looking for /products/"+CMSFilter.CMS_INDEX_PAGE+", got;" + request.getAttribute(CMSFilter.CMS_FILTER_URI_OVERRIDE));
			Assert.assertEquals("/products/"+CMSFilter.CMS_INDEX_PAGE, request.getAttribute(CMSFilter.CMS_FILTER_URI_OVERRIDE));
			
		
		
			
			



			
			
			
			
		
		
		} catch (Exception e) {

			e.printStackTrace();
			Assert.fail();

		} finally {
			// cleanup
			try {
				HibernateUtil.delete(link1);
				HibernateUtil.delete(link2);
				HibernateUtil.delete(link3);
				HibernateUtil.delete(link4);
				HibernateUtil.delete(link5);
				VirtualLinksCache.removePathFromCache(link1.getUrl());
				VirtualLinksCache.removePathFromCache(link2.getUrl());
				VirtualLinksCache.removePathFromCache(link3.getUrl());
				VirtualLinksCache.removePathFromCache(link4.getUrl());
				VirtualLinksCache.removePathFromCache(link5.getUrl());
				
			} catch (DotHibernateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	@Test
	public void shouldWorkVirtualLinkCMSHomePage() throws IOException {
		VirtualLinkAPI virtualLinkAPI = APILocator.getVirtualLinkAPI();
		VirtualLink cmsHomePage = new VirtualLink();

		// build them up
		try {

			cmsHomePage = virtualLinkAPI.getVirtualLinkByURL("/cmsHomePage");

			cmsHomePage = new VirtualLink();
			cmsHomePage.setActive(true);
			cmsHomePage.setTitle("cmsHomePage");
			cmsHomePage.setUri("/about-us/"+CMSFilter.CMS_INDEX_PAGE);
			cmsHomePage.setUrl("/cmsHomePage");
			HibernateUtil.save(cmsHomePage);

			CMSFilter cmsFilter = new CMSFilter();
			HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
			MockResponseWrapper response = new MockResponseWrapper(res);
			FilterChain chain = Mockito.mock(FilterChain.class);
			HttpServletRequest request = null;
			
			Logger.info(this.getClass(), "/cmsHomePage should forward to /about-us/"+CMSFilter.CMS_INDEX_PAGE);
			request = getMockRequest("demo.dotcms.com", "/");
			response = new MockResponseWrapper(Mockito.mock(HttpServletResponse.class));
			cmsFilter.doFilter(request, response, chain);
			Logger.info(this.getClass(), "looking for 200, got;" + response.getStatus());
			Assert.assertEquals(200, response.getStatus());
			Logger.info(this.getClass(), "looking for /about-us/"+CMSFilter.CMS_INDEX_PAGE+", got;" + request.getAttribute(CMSFilter.CMS_FILTER_URI_OVERRIDE));
			Assert.assertEquals("/about-us/"+CMSFilter.CMS_INDEX_PAGE, request.getAttribute(CMSFilter.CMS_FILTER_URI_OVERRIDE));
			HibernateUtil.delete(cmsHomePage);
			VirtualLinksCache.removePathFromCache(cmsHomePage.getUrl());

			cmsHomePage = virtualLinkAPI.getVirtualLinkByURL("demo.dotcms.com:/cmsHomePage");

			cmsHomePage = new VirtualLink();
			cmsHomePage.setActive(true);
			cmsHomePage.setTitle("cmsHomePage Host");
			cmsHomePage.setUri("/about-us/"+CMSFilter.CMS_INDEX_PAGE);
			cmsHomePage.setUrl("demo.dotcms.com:/cmsHomePage");
			HibernateUtil.save(cmsHomePage);
			// need to remove the _NOT_FOUND_ entry for this uri created in the previous test
			VirtualLinksCache.removePathFromCache(cmsHomePage.getUrl());
			
			
			Logger.info(this.getClass(), "demo.dotcms.com:/cmsHomePage should forward to /about-us/"+CMSFilter.CMS_INDEX_PAGE);
			request = getMockRequest("demo.dotcms.com", "/");
			response = new MockResponseWrapper(Mockito.mock(HttpServletResponse.class));
			cmsFilter.doFilter(request, response, chain);
			Logger.info(this.getClass(), "looking for 200, got;" + response.getStatus());
			Assert.assertEquals(200, response.getStatus());
			Logger.info(this.getClass(), "looking for /about-us"+CMSFilter.CMS_INDEX_PAGE+", got;" + request.getAttribute(CMSFilter.CMS_FILTER_URI_OVERRIDE));
			Assert.assertEquals("/about-us/"+CMSFilter.CMS_INDEX_PAGE, request.getAttribute(CMSFilter.CMS_FILTER_URI_OVERRIDE));

		} catch (Exception e) {

			e.printStackTrace();
			Assert.fail();

		} finally {
			try {
				HibernateUtil.delete(cmsHomePage);
				VirtualLinksCache.removePathFromCache(cmsHomePage.getUrl());
			} catch (DotHibernateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}
	
	
	
	
	
	private HttpServletRequest getMockRequest(String hostname, String uri) {

		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getRequestURI()).thenReturn(uri);
		Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer("http://" + hostname + uri));
		Mockito.when(request.getCookies()).thenReturn(new Cookie[] {});
		Mockito.when(request.getServerName()).thenReturn(hostname);
		Mockito.when(request.getSession()).thenReturn(new MockSession());
		Mockito.when(request.getSession(Mockito.anyBoolean())).thenReturn(new MockSession());
		Mockito.when(request.getRequestDispatcher("/servlets/VelocityServlet")).thenReturn(new RequestDispatcher() {

			@Override
			public void include(ServletRequest arg0, ServletResponse arg1) throws ServletException, IOException {

			}

			@Override
			public void forward(ServletRequest arg0, ServletResponse arg1) throws ServletException, IOException {
				
				VelocityServlet servlet = new ClientVelocityServlet() ;
				servlet.init(null);
				servlet.service(arg0, arg1);
			}
		});
		Mockito.when(request.getRequestDispatcher(Mockito.startsWith("/dotAsset/"))).thenReturn(new RequestDispatcher() {

			@Override
			public void include(ServletRequest arg0, ServletResponse arg1) throws ServletException, IOException {

			}

			@Override
			public void forward(ServletRequest arg0, ServletResponse arg1) throws ServletException, IOException {
				
				SpeedyAssetServlet servlet = new SpeedyAssetServlet() ;
				servlet.init(null);
				servlet.service(arg0, arg1);
			}
		});

		MockRequestWrapper reqWrap = new MockRequestWrapper(request);
		return reqWrap;

	}

	
	public void runTests() throws IOException {

		shouldRedirectToFolderIndex();
		shouldWorkVirtualLink();
		shouldForwardToImage();
		shouldRedirect401() ;
		shouldWorkVirtualLinkCMSHomePage();
	}

	@Test
	public void shouldReturnStrutsPage() throws IOException {


		Logger.info(this.getClass(), "/dotCMS/login should forward to /application/login/"+CMSFilter.CMS_INDEX_PAGE);
		MockResponseWrapper response = new MockResponseWrapper(Mockito.mock(HttpServletResponse.class));
		FilterChain chain = Mockito.mock(FilterChain.class);
		HttpServletRequest request = getMockRequest("demo.dotcms.com", "/dotCMS/login");


		try {
			new CMSFilter().doFilter(request, response, chain);
			Logger.info(this.getClass(), "looking for 200, got;" + response.getStatus());
			Assert.assertEquals(200, response.getStatus());
			Mockito.verify(chain).doFilter(request, response);
		} catch (ServletException e) {
			Assert.fail();
			e.printStackTrace();
		}

	}
	/**
	 * This tests the demo site for its 404 image
	 * @throws IOException
	 */
	@Test
	public void shouldForwardToImage() throws IOException {

		
		ServletOutputStream sos =  Mockito.mock(ServletOutputStream.class);

		Logger.info(this.getClass(), "/images/404.jpg should give us a 200");
		HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
		Mockito.when(res.getOutputStream()).thenReturn(sos);
		MockResponseWrapper response = new MockResponseWrapper(res);
		
		FilterChain chain = Mockito.mock(FilterChain.class);
		HttpServletRequest request = getMockRequest("demo.dotcms.com", "/images/404.jpg");


		try {
			new CMSFilter().doFilter(request, response, chain);
			Logger.info(this.getClass(), "looking for 200, got;" + response.getStatus());
			Assert.assertEquals(200, response.getStatus());


		} catch (ServletException e) {
			Assert.fail();
			e.printStackTrace();
		}

	}
	
	/**
	 * This tests the demo site for its 404 image
	 * @throws IOException
	 */
	@Test
	public void shouldRedirect401() throws IOException {


		HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
		MockResponseWrapper response = new MockResponseWrapper(res);
		
		FilterChain chain = Mockito.mock(FilterChain.class);
		HttpServletRequest request = getMockRequest("demo.dotcms.com", "/intranet/");


		try {
			new CMSFilter().doFilter(request, response, chain);
			Logger.info(this.getClass(), "looking for 401, got;" + response.getStatus());
			Assert.assertEquals(401, response.getStatus());


		} catch (ServletException e) {
			Assert.fail();
			e.printStackTrace();
		}

	}

	/*
	 * This tests if the cms filter correctly redirects a user from /products to
	 * /products/
	 */
	@Test
	public void shouldRedirectToFolderIndex() throws IOException {
		Logger.info(this.getClass(), "/products should redirect to /products/");

		MockResponseWrapper response = new MockResponseWrapper(Mockito.mock(HttpServletResponse.class));
		FilterChain chain = Mockito.mock(FilterChain.class);
		HttpServletRequest request = getMockRequest("demo.dotcms.com", "/products");

		try {
			new CMSFilter().doFilter(request, response, chain);
			
			Assert.assertEquals("/products/", ((MockResponseWrapper) response).getRedirectLocation());
			Assert.assertEquals(301, response.getStatus());
		} catch (ServletException e) {
			Assert.fail();
		}
		
		
		Logger.info(this.getClass(), "/home/ should forward to /products/"+CMSFilter.CMS_INDEX_PAGE);
		request = getMockRequest("demo.dotcms.com", "/products/");
		response = new MockResponseWrapper(Mockito.mock(HttpServletResponse.class));
		try {
			new CMSFilter().doFilter(request, response, chain);
			Logger.info(this.getClass(), "looking for /products/" +CMSFilter.CMS_INDEX_PAGE+" , got;" + request.getAttribute(CMSFilter.CMS_FILTER_URI_OVERRIDE));
			Assert.assertEquals("/products/" + CMSFilter.CMS_INDEX_PAGE, request.getAttribute(CMSFilter.CMS_FILTER_URI_OVERRIDE));
			Logger.info(this.getClass(), "looking for 200, got;" + response.getStatus());
			Assert.assertEquals(200, response.getStatus());
		} catch (ServletException e) {
			Assert.fail();
		}
		

	}

	class MockRequestWrapper extends HttpServletRequestWrapper {
		Map<String, Object> valmap = new HashMap<>();

		public MockRequestWrapper(HttpServletRequest request) {
			super(request);

		}

		@Override
		public void setAttribute(String arg0, Object arg1) {
			valmap.put(arg0, arg1);

		}

		@Override
		public Object getAttribute(String arg0) {
			return valmap.get(arg0);
		}

	}

	class MockResponseWrapper extends HttpServletResponseWrapper {

		StringWriter stringWriter = new StringWriter();
		PrintWriter writer = new PrintWriter(stringWriter);
		int status = 200;
		String location = null;
		Map<String,String> headers = new HashMap<String,String>();

		@Override
		public PrintWriter getWriter() throws IOException {
			return writer;
		}

		public MockResponseWrapper(HttpServletResponse response) {
			super(response);

		}

		@Override
		public int getStatus() {
			// TODO Auto-generated method stub
			return status;
		}

		@Override
		public void sendError(int sc, String msg) throws IOException {
			status = sc;
			Logger.info(this.getClass(), msg);
		}

		@Override
		public void sendError(int sc) throws IOException {
			status = sc;
		}

		@Override
		public void sendRedirect(String location) throws IOException {
			this.location = location;
			Logger.info(this.getClass(), "redirecting;" + location);
			status = 301;
		}

		@Override
		public void setStatus(int sc, String sm) {
			Logger.info(this.getClass(), sm);
			status = sc;
		}

		@Override
		public void setStatus(int sc) {
			status = sc;
		}

		public String getRedirectLocation() {
			if(location != null && !location.isEmpty()){
				return location;
			}	
			return headers.get("Location");
		}
		
		@Override
		public void setHeader(String key, String value){
			headers.put(key, value);
		}
	}

	class MockSession implements HttpSession {
		Map<String, Object> valmap = new HashMap<>();

		@Override
		public void setMaxInactiveInterval(int arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void setAttribute(String arg0, Object arg1) {
			valmap.put(arg0, arg1);

		}

		@Override
		public void removeValue(String arg0) {
			valmap.remove(arg0);

		}

		@Override
		public void removeAttribute(String arg0) {
			valmap.remove(arg0);

		}

		@Override
		public void putValue(String arg0, Object arg1) {
			valmap.put(arg0, arg1);

		}

		@Override
		public boolean isNew() {

			return true;
		}

		@Override
		public void invalidate() {
			valmap = new HashMap<>();

		}

		@Override
		public String[] getValueNames() {

			return valmap.keySet().toArray(new String[valmap.size()]);
		}

		@Override
		public Object getValue(String arg0) {
			return valmap.get(arg0);
		}

		@Override
		public ServletContext getServletContext() {

			return null;
		}

		@Override
		public int getMaxInactiveInterval() {

			return 0;
		}

		@Override
		public long getLastAccessedTime() {

			return 0;
		}

		@Override
		public String getId() {

			return null;
		}

		@Override
		public long getCreationTime() {

			return 0;
		}

		@Override
		public Enumeration<String> getAttributeNames() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object getAttribute(String arg0) {
			return valmap.get(arg0);
		}

		@Override
		public HttpSessionContext getSessionContext() {
			// TODO Auto-generated method stub
			return null;
		}
	}

}
