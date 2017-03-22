package com.dotcms.contenttype.test;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.JsonContentTypeTransformer;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.internal.util.Base64;
import com.dotcms.rest.api.v1.contenttype.ContentTypeResource;
import com.dotmarketing.util.Logger;

public class ContentTypeResourceTest extends ContentTypeBaseTest {
	final String base = "/com/dotcms/contenttype/test/";

	@Test
	public void jsonTests() throws Exception {

		URL resource = this.getClass().getResource(base);
		File directory = new File(resource.toURI());
		assertThat("we have a testing directory with json in it", directory != null);

		for (String file : directory.list()) {
			if (file.endsWith(".json")) {
				testJson(file);
			}
		}
	}

	public void testJson(String jsonFile) throws Exception {
		Logger.info(this.getClass(), "testing:" + jsonFile);


		Logger.info(this.getClass(), "testing:" + base + jsonFile);
		InputStream stream = this.getClass().getResourceAsStream(base + jsonFile);
		String json = IOUtils.toString(stream);
		stream.close();
		List<ContentType> delTypes = new JsonContentTypeTransformer(json).asList();
		for(ContentType delType:delTypes){
			try {
				contentTypeApi.delete(contentTypeApi.find(delType.id()));
			} catch (NotFoundInDbException e) {

			}
			try {
				contentTypeApi.delete(contentTypeApi.find(delType.variable()));
			} catch (NotFoundInDbException ee) {

			}
		}

		ContentTypeResource resource = new ContentTypeResource();

		Response response = resource.saveType(getHttpRequest(), json);

		int x = response.getStatus();
		assertThat("result:200 with json " + jsonFile, x == 200);
	}

	/**
	 * BasicAuth
	 * 
	 * @return
	 */
	private HttpServletRequest getHttpRequest() {
		MockHeaderRequest request = new MockHeaderRequest(
				new MockSessionRequest(new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
				.request());

		request.setHeader("Authorization", "Basic " + new String(Base64.encode("admin@dotcms.com:admin".getBytes())));

		return request;
	}
}
