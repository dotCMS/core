package com.dotcms.contenttype.test;

import static org.hamcrest.MatcherAssert.assertThat;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.business.CopyContentTypeBean;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.JsonContentTypeTransformer;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.api.v1.contenttype.ContentTypeForm;
import com.dotcms.rest.api.v1.contenttype.ContentTypeResource;
import com.dotcms.util.ConfigTestHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
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
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import java.util.Base64;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class ContentTypeResourceTest extends ContentTypeBaseTest {

	final String base = "/com/dotcms/contenttype/test/";

	private static final String TO_REPLACE_HOST_ID = "REPLACE_WITH_HOST_ID";
	private static Host site;

	public static void prepare() {

		if (null == site) {
			site = new SiteDataGen().nextPersisted();
		}
	}

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

	/**
	 * Method to test: {@link ContentTypeResource#setHostAndFolderAsIdentifer(String, String, User, CopyContentTypeBean.Builder)}
	 * Given Scenario: Sends the host and folder null
	 * ExpectedResult: null would be expected
	 */
	@Test
	public void test_setHostAndFolderAsIdentifer_with_null() throws Exception {

		final CopyContentTypeBean.Builder builder = new CopyContentTypeBean.Builder();
		ContentTypeResource.setHostAndFolderAsIdentifer(null, null, APILocator.systemUser(), builder);
		final CopyContentTypeBean copyContentTypeBean = builder.build();
		Assert.assertEquals(null, copyContentTypeBean.getHost());
	}

	/**
	 * Method to test: {@link ContentTypeResource#setHostAndFolderAsIdentifer(String, String, User, CopyContentTypeBean.Builder)}
	 * Given Scenario: Sends the host as a SYSTEM_HOST
	 * ExpectedResult: The system host is expected as a result
	 */
	@Test
	public void test_setHostAndFolderAsIdentifer_with_System_Host() throws Exception {

		final String folderPathOrIdentifier = "";
		final String hostOrId = "SYSTEM_HOST";
		final CopyContentTypeBean.Builder builder = new CopyContentTypeBean.Builder();
		ContentTypeResource.setHostAndFolderAsIdentifer(folderPathOrIdentifier, hostOrId, APILocator.systemUser(), builder);
		final CopyContentTypeBean copyContentTypeBean = builder.build();
		Assert.assertEquals(Host.SYSTEM_HOST, copyContentTypeBean.getHost());
	}

	@Test
	@UseDataProvider("testCases")
	@WrapInTransaction
	public void testJson(String jsonFile) throws Exception {

		//Preparing test data
		prepare();

		Logger.info(this.getClass(), "testing:" + jsonFile);
		Logger.info(this.getClass(), "testing:" + base + jsonFile);

		InputStream stream = this.getClass().getResourceAsStream(base + jsonFile);
		String json = IOUtils.toString(stream);
		json = json.replaceAll(TO_REPLACE_HOST_ID, site.getIdentifier());

		stream.close();
		List<ContentType> delTypes = new JsonContentTypeTransformer(json).asList();

		final ContentTypeResource resource = new ContentTypeResource();

		final ContentTypeForm.ContentTypeFormDeserialize contentTypeFormDeserialize = new ContentTypeForm.ContentTypeFormDeserialize();
		ContentTypeForm contentTypeForm = contentTypeFormDeserialize.buildForm(json);
		try{
			Response response = resource.createType(getHttpRequest(),  new EmptyHttpResponse(), contentTypeForm);

			int x = response.getStatus();
			assertThat("result:200 with json " + jsonFile + "got :" + x, x == 200);
		}finally{
			for(ContentType delType:delTypes){
				try {
					if (UtilMethods.isSet(delType) && !UtilMethods.isSet(delType.id())){
						contentTypeApi.delete(contentTypeApi.find(delType.variable()));
					}
				} catch (NotFoundInDbException e) {

				}
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
				new MockSessionRequest(new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
				.request());

		request.setHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString("admin@dotcms.com:admin".getBytes()));

		return request;
	}
}
