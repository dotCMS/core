package com.dotcms.rest;

import static com.dotcms.repackage.org.junit.Assert.assertNotNull;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.repackage.javax.ws.rs.client.Client;
import com.dotcms.repackage.javax.ws.rs.client.Entity;
import com.dotcms.repackage.javax.ws.rs.client.WebTarget;
import com.dotcms.repackage.javax.ws.rs.core.MultivaluedHashMap;
import com.dotcms.repackage.javax.ws.rs.core.MultivaluedMap;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.codehaus.cargo.util.Base64;
import com.dotcms.repackage.org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import com.dotcms.repackage.org.junit.Before;
import com.dotcms.repackage.org.junit.Test;

import com.dotcms.TestBase;
import com.dotcms.rest.config.RestServiceUtil;
import com.dotmarketing.servlets.test.ServletTestRunner;

public class WebResourceTest extends TestBase  {

	private Client client;
	private WebTarget webTarget;
	private HttpServletRequest request;
	private String serverName;
	private Integer serverPort;

	@Before
	public void init() {
		client = RestClientBuilder.newClient();
        request = ServletTestRunner.localRequest.get();
        serverName = request.getServerName();
        serverPort = request.getServerPort();
        webTarget = client.target("http://"+serverName+":"+serverPort+"/api/role");
		RestServiceUtil.addResource(DummyResource.class);
	}

	@Test//(expected=UniformInterfaceException.class)
	public void testAuthenticateNoUser() {
        webTarget.path("/loadchildren/").request().get(String.class);
	}

	@Test//(expected=UniformInterfaceException.class)
	public void testAuthenticateInvalidUserInURL() {
        webTarget.path("/loadchildren/user/wrong@user.com/password/123456").request().get(String.class);
	}

	@Test
	public void testAuthenticateValidUserInURL() {
		String response = webTarget.path("/loadchildren/user/admin@dotcms.com/password/admin").request().get(String.class);
		assertNotNull(response);
	}

	@Test//(expected=UniformInterfaceException.class)
	public void testAuthenticateInvalidUserPost() {
		MultivaluedMap<String, String> formData = new MultivaluedHashMap();
		formData.add("user", "wrong@user.com");
		formData.add("password", "123456");
		webTarget = client.target("http://" + serverName + ":" + serverPort + "/api/dummy");
        webTarget.path("/postauth").request().post(Entity.form(formData));
	}

	@Test
	public void testAuthenticateValidUserPost() {
		MultivaluedMap<String, String> formData = new MultivaluedHashMap();
		formData.add("user", "admin@dotcms.com");
		formData.add("password", "admin");
        webTarget = client.target("http://" + serverName + ":" + serverPort + "/api/dummy");
        Response response = webTarget.path("/postauth").request().post(Entity.form(formData));
		assertNotNull(response);
	}

	@Test//(expected=UniformInterfaceException.class)
	public void testAuthenticateInvalidUserBasicAuth() {
        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("wrong@user.com", "123456");
		client.register(feature);
		webTarget.path("/loadchildren/").request().get(String.class);
	}

	@Test
	public void testAuthenticateValidUserBasicAuth() {
        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("admin@dotcms.com", "admin");
		client.register(feature);
		String response = webTarget.path("/loadchildren/").request().get(String.class);
		assertNotNull(response);
	}

	@Test//(expected=UniformInterfaceException.class)
	public void testAuthenticateInvalidUserHeaderAuth() {
        webTarget.path("/loadchildren/").request().header("DOTAUTH", Base64.encode("wrong@user.com:123456")).get(String.class);
	}

	@Test
	public void testAuthenticateValidUserHeaderAuth() {
        String response = webTarget.path("/loadchildren/").request().header("DOTAUTH", Base64.encode("admin@dotcms.com:admin")).get(String.class);
		assertNotNull(response);
	}


}
