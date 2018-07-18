package com.dotcms.contenttype.test;

import static org.hamcrest.MatcherAssert.assertThat;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.util.UtilMethods;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.rest.api.v1.contenttype.ContentTypeForm;
import com.dotcms.util.ConfigTestHelper;
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
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class ContentTypeResourceTest extends ContentTypeBaseTest {
	final String base = "/com/dotcms/contenttype/test/";

	@DataProvider
	public static Object[] testCases() throws URISyntaxException {

		List testCases = new ArrayList();
		URL resource = ConfigTestHelper.getUrlToTestResource("com/dotcms/contenttype/test/file-asset.json");
		File pivotResource = new File(resource.toURI());
		File directory = pivotResource.getParentFile();
		assertThat("we have a testing directory with json in it", directory != null);

		for (String file : directory.list()) {
			if (file.endsWith(".json")) {
				testCases.add(file);
			}
		}

		return testCases.toArray();
	}

	@Test
	@UseDataProvider("testCases")
	@WrapInTransaction
	public void testJson(String jsonFile) throws Exception {
		Logger.info(this.getClass(), "testing:" + jsonFile);


		Logger.info(this.getClass(), "testing:" + base + jsonFile);
		InputStream stream = this.getClass().getResourceAsStream(base + jsonFile);
		String json = IOUtils.toString(stream);
		stream.close();
		List<ContentType> delTypes = new JsonContentTypeTransformer(json).asList();
		for(ContentType delType:delTypes){
			try {
				if (UtilMethods.isSet(delType.id())){
					contentTypeApi.delete(contentTypeApi.find(delType.id()));
				}
			} catch (NotFoundInDbException e) {

			}
			try {
				if (UtilMethods.isSet(delType.variable())){
					contentTypeApi.delete(contentTypeApi.find(delType.variable()));
				}
			} catch (NotFoundInDbException ee) {

			}
		}

		final ContentTypeResource resource = new ContentTypeResource();

		final ContentTypeForm.ContentTypeFormDeserialize contentTypeFormDeserialize = new ContentTypeForm.ContentTypeFormDeserialize();
		ContentTypeForm contentTypeForm = contentTypeFormDeserialize.buildForm(json);
		Response response = resource.createType(getHttpRequest(), contentTypeForm);

		int x = response.getStatus();
		assertThat("result:200 with json " + jsonFile + "got :" + x, x == 200);
		for(ContentType delType:delTypes){
			if(delType.variable().contains("test")) {
				contentTypeApi.delete(contentTypeApi.find(delType.variable()));
			}
		}
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
