package com.dotcms.rest.api.v1.temp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;
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
    final TempResource resource = new TempResource();

    final HttpServletRequest request = mockRequest();

    final String fileName = "test.file";
    final HttpServletResponse response = new MockHttpResponse();


    final Date date = new Date();
    final FormDataContentDisposition fileMetaData = FormDataContentDisposition.name("testData").fileName(fileName).creationDate(date)
        .modificationDate(date).readDate(date).size(1222).build();

    final Response jsonResponse = resource.uploadTempResource(request, response, inputStream(), fileMetaData);

    final DotTempFile dotTempFile = (DotTempFile) jsonResponse.getEntity();

    final String tempFileId = dotTempFile.id;
    // its not an image because we set the filename to "test.file"
    assertFalse((Boolean) dotTempFile.image);

    assert (tempFileId.startsWith(TempResourceAPI.TEMP_RESOURCE_PREFIX));

    assert (dotTempFile.file.getName().equals(fileName));
    assert (dotTempFile.length() > 0);

  }

  @Test
  public void test_tempResourceAPI_who_can_use_via_session() throws Exception {
    final TempResource resource = new TempResource();

    final HttpServletRequest request = mockRequest();

    final String fileName = "test.png";
    final HttpServletResponse response = new MockHttpResponse();


    final Date date = new Date();
    final FormDataContentDisposition fileMetaData = FormDataContentDisposition.name("testData").fileName(fileName).creationDate(date)
        .modificationDate(date).readDate(date).size(1222).build();

    final Response jsonResponse = resource.uploadTempResource(request, response, inputStream(), fileMetaData);

    final DotTempFile dotTempFile = (DotTempFile) jsonResponse.getEntity();

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
    final TempResource resource = new TempResource();

    final HttpServletRequest request = mockRequest();
    final User user = new UserDataGen().nextPersisted();

    final String fileName = "test.png";
    final HttpServletResponse response = new MockHttpResponse();
    request.setAttribute(WebKeys.USER, user);

    final Date date = new Date();
    final FormDataContentDisposition fileMetaData = FormDataContentDisposition.name("testData").fileName(fileName).creationDate(date)
        .modificationDate(date).readDate(date).size(1222).build();

    final Response jsonResponse = resource.uploadTempResource(request, response, inputStream(), fileMetaData);

    final DotTempFile dotTempFile = (DotTempFile) jsonResponse.getEntity();

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
    final TempResource resource = new TempResource();

    final HttpServletRequest request = mockRequest();


    final String fileName = "test.png";
    final HttpServletResponse response = new MockHttpResponse();


    final Date date = new Date();
    final FormDataContentDisposition fileMetaData = FormDataContentDisposition.name("testData").fileName(fileName).creationDate(date)
        .modificationDate(date).readDate(date).size(1222).build();

    final Response jsonResponse = resource.uploadTempResource(request, response, inputStream(), fileMetaData);

    final DotTempFile dotTempFile = (DotTempFile) jsonResponse.getEntity();

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
    final TempResource resource = new TempResource();

    final HttpServletRequest request = mockRequest();

    final String fileName = "test.png";
    final HttpServletResponse response = new MockHttpResponse();

    Config.setProperty(TempResourceAPI.TEMP_RESOURCE_ALLOW_ANONYMOUS, false);
    final Date date = new Date();
    final FormDataContentDisposition fileMetaData = FormDataContentDisposition.name("testData").fileName(fileName).creationDate(date)
        .modificationDate(date).readDate(date).size(1222).build();

    try {
      resource.uploadTempResource(request, response, inputStream(), fileMetaData);
      assertTrue("We should have throw a resource unavailable exception", false);
    } catch (SecurityException se) {
      assertTrue("We  throw a resource unavailable exception", true);
    } catch (Exception e) {
      assertTrue("We should have thrown a SecurityException", false);
    }
    Config.setProperty(TempResourceAPI.TEMP_RESOURCE_ALLOW_ANONYMOUS, true);
    final Response jsonResponse = resource.uploadTempResource(request, response, inputStream(), fileMetaData);
    final DotTempFile dotTempFile = (DotTempFile) jsonResponse.getEntity();
    assert(UtilMethods.isSet(dotTempFile.id));
  }
  
  
  @Test
  public void temp_resource_makes_it_into_checked_in_content() throws Exception {

    final TempResource resource = new TempResource();
    final HttpServletRequest request = mockRequest();

    final HttpServletResponse response = new MockHttpResponse();
    final User user = APILocator.systemUser();
    final Host host = APILocator.getHostAPI().findDefaultHost(user, true);

    // set user to system user
    request.setAttribute(WebKeys.USER, user);

    // create a file asset type

    ContentType contentType = ContentTypeBuilder.builder(BaseContentType.FILEASSET.immutableClass()).description("description")
        .folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("ContentTypeTesting" + System.currentTimeMillis()).owner("owner")
        .variable("velocityVarNameTesting" + System.currentTimeMillis()).build();
    contentType = APILocator.getContentTypeAPI(APILocator.systemUser()).save(contentType);

    // Add another binary field
    final List<Field> fields = new ArrayList<>(contentType.fields());

    final Field fieldToSave =
        FieldBuilder.builder(BinaryField.class)
        .name("testBinary")
        .variable("testBinary")
        .contentTypeId(contentType.id())
            .build();

    fields.add(fieldToSave);

    contentType = APILocator.getContentTypeAPI(user).save(contentType, fields);

    final String fileName1 = "testFileName1" + UUIDGenerator.shorty() + ".png";
    final String fileName2 = "testFileName2" + UUIDGenerator.shorty() + ".gif";
    final Date date = new Date();
    final FormDataContentDisposition fileMetaData = FormDataContentDisposition.name("testData").fileName(fileName1).creationDate(date)
        .modificationDate(date).readDate(date).size(1222).build();

    Response jsonResponse = resource.uploadTempResource(request, response, inputStream(), fileMetaData);

    final DotTempFile dotTempFile1 = (DotTempFile) jsonResponse.getEntity();

    final RemoteUrlForm form = new RemoteUrlForm(
        "https://raw.githubusercontent.com/dotCMS/core/master/dotCMS/src/main/webapp/html/images/skin/logo.gif", fileName2, null);

    jsonResponse = resource.copyTempFromUrl(request, form);
    DotTempFile dotTempFile2 = (DotTempFile) jsonResponse.getEntity();

    final Map<String, Object> m = new HashMap<String, Object>();
    m.put("stInode", contentType.id());
    m.put("hostFolder", host.getIdentifier());
    m.put("languageId", host.getLanguageId());
    m.put("title", dotTempFile1.fileName);
    m.put("fileAsset", dotTempFile1.id);
    m.put("showOnMenu", "false");
    m.put("sortOrder", 1);
    m.put("description", "description");
    m.put("testBinary", dotTempFile2.id);
    Contentlet contentlet = new Contentlet(m);
    contentlet = APILocator.getContentletAPI().checkin(contentlet, user, true);

    assert (contentlet.getBinary("fileAsset").exists());
    assert (contentlet.getBinary("fileAsset").getName().equals(fileName1));
    assert (contentlet.getBinary("fileAsset").length() == 381411);
    assert (contentlet.getBinary("testBinary").exists());
    assert (contentlet.getBinary("testBinary").getName().equals(fileName2));
    assert (contentlet.getBinary("testBinary").length() == 13695);

  }
  
  
  

}
