package com.dotcms.rest;

import static org.junit.Assert.assertNotNull;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;

import org.codehaus.cargo.util.Base64;
import org.junit.Before;
import org.junit.Test;

import com.dotcms.TestBase;
import com.dotcms.rest.config.RestServiceUtil;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class WebResourceTest extends TestBase  {

	private Client client;
	private WebResource webResource;
	private HttpServletRequest request;
	private String serverName;
	private Integer serverPort;

	@Before
	public void init() {
		client = Client.create();
		request = ServletTestRunner.localRequest.get();
		serverName = request.getServerName();
		serverPort = request.getServerPort();
		webResource = client.resource("http://"+serverName+":"+serverPort+"/api/role");
		RestServiceUtil.addResource(DummyResource.class);
	}

	@Test(expected=UniformInterfaceException.class)
	public void testAuthenticateNoUser() {
		webResource.path("/loadchildren/").get(String.class);
	}

	@Test(expected=UniformInterfaceException.class)
	public void testAuthenticateInvalidUserInURL() {
		webResource.path("/loadchildren/user/wrong@user.com/password/123456").get(String.class);
	}

	@Test
	public void testAuthenticateValidUserInURL() {
		String response = webResource.path("/loadchildren/user/admin@dotcms.com/password/admin").get(String.class);
		assertNotNull(response);
	}

	@Test(expected=UniformInterfaceException.class)
	public void testAuthenticateInvalidUserPost() {
		MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
		formData.add("user", "wrong@user.com");
		formData.add("password", "123456");
		webResource = client.resource("http://"+serverName+":"+serverPort+"/api/dummy");
		webResource.path("/postauth").type("application/x-www-form-urlencoded").post(String.class, formData);
	}

	@Test
	public void testAuthenticateValidUserPost() {
		MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
		formData.add("user", "admin@dotcms.com");
		formData.add("password", "admin");
		webResource = client.resource("http://"+serverName+":"+serverPort+"/api/dummy");
		String response = webResource.path("/postauth").type("application/x-www-form-urlencoded").post(String.class, formData);
		assertNotNull(response);
	}

	@Test(expected=UniformInterfaceException.class)
	public void testAuthenticateInvalidUserBasicAuth() {
		client.addFilter(new HTTPBasicAuthFilter("wrong@user.com", "123456"));
		webResource.path("/loadchildren/").get(String.class);
	}

	@Test
	public void testAuthenticateValidUserBasicAuth() {
		client.addFilter(new HTTPBasicAuthFilter("admin@dotcms.com", "admin"));
		String response = webResource.path("/loadchildren/").get(String.class);
		assertNotNull(response);
	}

	@Test(expected=UniformInterfaceException.class)
	public void testAuthenticateInvalidUserHeaderAuth() {
		webResource.path("/loadchildren/").header("DOTAUTH", Base64.encode("wrong@user.com:123456")).get(String.class);
	}

	@Test
	public void testAuthenticateValidUserHeaderAuth() {
		String response = webResource.path("/loadchildren/").header("DOTAUTH", Base64.encode("admin@dotcms.com:admin")).get(String.class);
		assertNotNull(response);
	}


}
