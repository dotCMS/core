package com.dotcms.rest.api.v1.temp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;

public class TempResourceTest {
  @BeforeClass
  public static void prepare() throws Exception {
    // Setting web app environment
    IntegrationTestInitService.getInstance().init();

  }

  private HttpServletRequest mockRequest() {
    return new MockSessionRequest(
        new MockHeaderRequest(new MockHttpRequest("localhost", "/api/v1/tempResource").request(), "Origin", "localhost").request());
  }
  private final InputStream inputStream() {
    return this.getClass().getResourceAsStream("/images/SqcP9KgFqruagXJfe7CES.png");
  }
  
  @Test
  public void test_temp_resource_upload() throws Exception {
    TempResource resource = new TempResource();

    HttpServletRequest request = mockRequest();

    final String fileName = "test.file";
    HttpServletResponse response = new MockHttpResponse();


    Date date = new Date();
    final FormDataContentDisposition fileMetaData = FormDataContentDisposition.name("testData").fileName(fileName).creationDate(date)
        .modificationDate(date).readDate(date).size(1222).build();

    Response jsonResponse = resource.uploadTempResource(request, response, inputStream(), fileMetaData);

    DotTempFile dotTempFile = (DotTempFile) jsonResponse.getEntity();

    final String tempFileId = dotTempFile.id;
    // its not an image because we set the filename to "test.file"
    assertFalse((Boolean) dotTempFile.image);

    assert (tempFileId.startsWith(TempResourceAPI.TEMP_RESOURCE_PREFIX));

    assert (dotTempFile.file.getName().equals(fileName));
    assert (dotTempFile.length() > 0);

  }

  @Test
  public void test_tempResourceAPI_who_can_use_via_session() throws Exception {
    TempResource resource = new TempResource();

    HttpServletRequest request = mockRequest();

    final String fileName = "test.png";
    HttpServletResponse response = new MockHttpResponse();


    Date date = new Date();
    final FormDataContentDisposition fileMetaData = FormDataContentDisposition.name("testData").fileName(fileName).creationDate(date)
        .modificationDate(date).readDate(date).size(1222).build();

    Response jsonResponse = resource.uploadTempResource(request, response, inputStream(), fileMetaData);

    DotTempFile dotTempFile = (DotTempFile) jsonResponse.getEntity();

    final String tempFileId = dotTempFile.id;

    // we can get the file because we have the same sessionId as the request
    Optional<File> file = new TempResourceAPI().getTempFile(null, request.getSession().getId(), tempFileId);
    assert (file.isPresent() && !file.get().isDirectory());

    // we can get the file again because we have the same sessionId as the request
    file = new TempResourceAPI().getTempFile(null, request.getSession().getId(), tempFileId);
    assert (file.isPresent() && !file.get().isDirectory());

    // we CANNOT get the file again because we have a new session ID in the request
    file = new TempResourceAPI().getTempFile(null, mockRequest().getSession().getId(), tempFileId);
    assert (!file.isPresent());

  }

  @Test
  public void test_tempResourceAPI_who_can_use_via_userID() throws Exception {
    TempResource resource = new TempResource();

    HttpServletRequest request = mockRequest();
    User user = new UserDataGen().nextPersisted();

    final String fileName = "test.png";
    HttpServletResponse response = new MockHttpResponse();
    request.setAttribute(WebKeys.USER, user);

    Date date = new Date();
    final FormDataContentDisposition fileMetaData = FormDataContentDisposition.name("testData").fileName(fileName).creationDate(date)
        .modificationDate(date).readDate(date).size(1222).build();

    Response jsonResponse = resource.uploadTempResource(request, response, inputStream(), fileMetaData);

    DotTempFile dotTempFile = (DotTempFile) jsonResponse.getEntity();

    final String tempFileId = dotTempFile.id;

    // CANNOT get the file again because we have a new session ID in the new mock request
    Optional<File> file = new TempResourceAPI().getTempFile(null, mockRequest().getSession().getId(), tempFileId);
    assert (!file.isPresent());

    // CAN get the file again because we are the user who uploaded it
    file = new TempResourceAPI().getTempFile(user, mockRequest().getSession().getId(), tempFileId);
    assert (file.isPresent() && !file.get().isDirectory());
  }

  @Test
  public void test_tempResourceapi_max_age() throws Exception {
    TempResource resource = new TempResource();

    HttpServletRequest request = mockRequest();

    final String fieldVar = "image";
    final String fileName = "test.png";
    HttpServletResponse response = new MockHttpResponse();


    Date date = new Date();
    final FormDataContentDisposition fileMetaData = FormDataContentDisposition.name("testData").fileName(fileName).creationDate(date)
        .modificationDate(date).readDate(date).size(1222).build();

    Response jsonResponse = resource.uploadTempResource(request, response, inputStream(), fileMetaData);

    DotTempFile dotTempFile = (DotTempFile) jsonResponse.getEntity();

    final String tempFileId = dotTempFile.id;

    Optional<File> file = new TempResourceAPI().getTempFile(null, request.getSession().getId(), tempFileId);

    assert (file.isPresent());

    int tempResourceMaxAgeSeconds = Config.getIntProperty("TEMP_RESOURCE_MAX_AGE_SECONDS", 1800);

    // this works becuase we set the file age to newer than max age
    file.get().setLastModified(System.currentTimeMillis() - 60 * 10 * 1000);
    file = new TempResourceAPI().getTempFile(null, request.getSession().getId(), tempFileId);
    assert (file.isPresent());

    // Setting the file to older than max age makes the file inaccessable
    file.get().setLastModified(System.currentTimeMillis() - (tempResourceMaxAgeSeconds * 1000) - 1);
    file = new TempResourceAPI().getTempFile(null, request.getSession().getId(), tempFileId);
    assertFalse(file.isPresent());
  }
  

  @Test
  public void test_tempResourceapi_test_anonymous_access() throws Exception {
    TempResource resource = new TempResource();

    HttpServletRequest request = mockRequest();

    final String fieldVar = "image";
    final String fileName = "test.png";
    HttpServletResponse response = new MockHttpResponse();

    Config.setProperty(TempResourceAPI.TEMP_RESOURCE_ALLOW_ANONYMOUS, false);
    Date date = new Date();
    final FormDataContentDisposition fileMetaData = FormDataContentDisposition.name("testData").fileName(fileName).creationDate(date)
        .modificationDate(date).readDate(date).size(1222).build();

    try {
      Response jsonResponse = resource.uploadTempResource(request, response, inputStream(), fileMetaData);
      assertTrue("We should have throw a resource unavailable exception", false);
    } catch (SecurityException se) {
      assertTrue("We  throw a resource unavailable exception", true);
    } catch (Exception e) {
      assertTrue("We should have thrown a SecurityException", false);
    }
    Config.setProperty(TempResourceAPI.TEMP_RESOURCE_ALLOW_ANONYMOUS, true);
    Response jsonResponse = resource.uploadTempResource(request, response, inputStream(), fileMetaData);
    DotTempFile dotTempFile = (DotTempFile) jsonResponse.getEntity();
    assert(UtilMethods.isSet(dotTempFile.id));
  }

}
