package com.dotcms.rest.api.v1.temp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import javax.ws.rs.core.Response.Status;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;
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

public class TempFileResourceTest {

  static HttpServletRequest request;
  static HttpServletResponse response;
  static TempFileResource resource;

  @BeforeClass
  public static void prepare() throws Exception {
    // Setting web app environment
    IntegrationTestInitService.getInstance().init();

    resource = new TempFileResource();
    request = mockRequest();
    response = new MockHttpResponse();
  }

  private static HttpServletRequest mockRequest() {
    return new MockSessionRequest(
        new MockHeaderRequest(new MockHttpRequest("localhost", "/api/v1/tempResource").request(), "Origin", "localhost").request());
  }
  private final InputStream inputStream() {
    return this.getClass().getResourceAsStream("/images/SqcP9KgFqruagXJfe7CES.png");
  }

  private DotTempFile saveTempFile_usingTempResource(final String fileName){
    final BodyPart filePart1 = new StreamDataBodyPart(fileName, inputStream());

    final MultiPart multipartEntity = new FormDataMultiPart()
            .bodyPart(filePart1);

    final Response jsonResponse = resource.uploadTempResourceMulti(request, response, (FormDataMultiPart) multipartEntity);

    final Map<String,List<DotTempFile>> dotTempFiles = (Map) jsonResponse.getEntity();
    return dotTempFiles.get("tempFiles").get(0);
  }
  
  @Test
  public void test_temp_resource_upload(){
    final String fileName = "test.file";
    final DotTempFile dotTempFile = saveTempFile_usingTempResource(fileName);
  // its not an image because we set the filename to "test.file"
    assertFalse((Boolean) dotTempFile.image);

    assert (dotTempFile.id.startsWith(TempFileAPI.TEMP_RESOURCE_PREFIX));

    assert (dotTempFile.file.getName().equals(fileName));
    assert (dotTempFile.length() > 0);
  }

  @Test
  public void test_temp_resource_multifile_upload(){
    final String fileName1 ="here-is-my-file.png";
    final BodyPart filePart1 = new StreamDataBodyPart(fileName1, inputStream());

    final String fileName2 ="here-is-my-file2.png";
    final BodyPart filePart2 = new StreamDataBodyPart(fileName2, inputStream());

    //uploading 2 files
    final MultiPart multipartEntity = new FormDataMultiPart()
            .bodyPart(filePart1)
            .bodyPart(filePart2);
    
    final Response jsonResponse = resource.uploadTempResourceMulti(request, response, (FormDataMultiPart) multipartEntity);
    assert (jsonResponse!=null);

    final Map<String,List<DotTempFile>> dotTempFile = (Map) jsonResponse.getEntity();
    assert (dotTempFile.size() > 0);
    
    assert (dotTempFile.get("tempFiles").size()==2);
    DotTempFile file= (DotTempFile) dotTempFile.get("tempFiles").get(0);
    assert(file.fileName.equals(fileName1));
    assert(file.image);
    assert(file.length()>1000);
    
    file= (DotTempFile) dotTempFile.get("tempFiles").get(1);
    assert(file.fileName.equals(fileName2));
    assert(file.image);
    assert(file.length()>1000);

  }

  @Test
  public void test_tempResourceAPI_who_can_use_via_session(){

    final String fileName = "test.file";
    final DotTempFile dotTempFile = saveTempFile_usingTempResource(fileName);

    // we can get the file because we have the same sessionId as the request
    Optional<DotTempFile> file = new TempFileAPI().getTempFile(null, request.getSession().getId(), dotTempFile.id);
    assert (file.isPresent() && !file.get().file.isDirectory());

    // we CANNOT get the file again because we have a new session ID in the request
    file = new TempFileAPI().getTempFile(null, mockRequest().getSession().getId(), dotTempFile.id);
    assert (!file.isPresent());

  }

  @Test
  public void test_tempResourceAPI_who_can_use_via_userID(){

    final User user = new UserDataGen().nextPersisted();
    request.setAttribute(WebKeys.USER, user);

    final String fileName = "test.png";
    final DotTempFile dotTempFile = saveTempFile_usingTempResource(fileName);

    // CANNOT get the file again because we have a new session ID in the new mock request
    Optional<DotTempFile> file = new TempFileAPI().getTempFile(null, mockRequest().getSession().getId(), dotTempFile.id);
    assert (!file.isPresent());

    // CAN get the file again because we are the user who uploaded it
    file = new TempFileAPI().getTempFile(user, mockRequest().getSession().getId(), dotTempFile.id);
    assert (file.isPresent() && !file.get().file.isDirectory());
  }

  @Test
  public void test_tempResourceapi_max_age(){

    final String fileName = "test.png";

    final DotTempFile dotTempFile = saveTempFile_usingTempResource(fileName);

    Optional<DotTempFile> file = new TempFileAPI().getTempFile(null, request.getSession().getId(), dotTempFile.id);

    assert (file.isPresent());

    int tempResourceMaxAgeSeconds = Config.getIntProperty("TEMP_RESOURCE_MAX_AGE_SECONDS", 1800);

    // this works becuase we set the file age to newer than max age
    file.get().file.setLastModified(System.currentTimeMillis() - 60 * 10 * 1000);
    file = new TempFileAPI().getTempFile(null, request.getSession().getId(), dotTempFile.id);
    assert (file.isPresent());

    // Setting the file to older than max age makes the file inaccessable
    file.get().file.setLastModified(System.currentTimeMillis() - (tempResourceMaxAgeSeconds * 1000) - 1);
    file = new TempFileAPI().getTempFile(null, request.getSession().getId(), dotTempFile.id);
    assertFalse(file.isPresent());
  }
  

  @Test
  public void test_tempResourceapi_test_anonymous_access(){

    final String fileName = "test.png";

    Config.setProperty(TempFileAPI.TEMP_RESOURCE_ALLOW_ANONYMOUS, false);
    final BodyPart filePart1 = new StreamDataBodyPart(fileName, inputStream());

    final MultiPart multipartEntity = new FormDataMultiPart()
            .bodyPart(filePart1);

    Response jsonResponse = resource.uploadTempResourceMulti(request, response, (FormDataMultiPart) multipartEntity);
    assertEquals(Status.UNAUTHORIZED.getStatusCode(), jsonResponse.getStatus());

    Config.setProperty(TempFileAPI.TEMP_RESOURCE_ALLOW_ANONYMOUS, true);
    jsonResponse = resource.uploadTempResourceMulti(request, response, (FormDataMultiPart) multipartEntity);
    final Map<String,List<DotTempFile>> dotTempFiles = (Map) jsonResponse.getEntity();
    final DotTempFile dotTempFile = dotTempFiles.get("tempFiles").get(0);
    assert(UtilMethods.isSet(dotTempFile.id));
  }
  
  
  @Test
  public void temp_resource_makes_it_into_checked_in_content() throws Exception {

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
    final DotTempFile dotTempFile1 = saveTempFile_usingTempResource(fileName1);
    
    final RemoteUrlForm form = new RemoteUrlForm(
        "https://raw.githubusercontent.com/dotCMS/core/master/dotCMS/src/main/webapp/html/images/skin/logo.gif", fileName2, null);

    Response jsonResponse = resource.copyTempFromUrl(request, form);
    final Map<String,List<DotTempFile>> dotTempFiles = (Map) jsonResponse.getEntity();
    final DotTempFile dotTempFile2 = dotTempFiles.get("tempFiles").get(0);

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
    
    contentlet = APILocator.getContentletAPI().find(contentlet.getInode(), user, true);
    
    

    assert (contentlet.getBinary("fileAsset").exists());
    assert (contentlet.getBinary("fileAsset").getName().equals(fileName1));
    assert (contentlet.getBinary("fileAsset").length() > 0);
    assert (contentlet.getBinary("testBinary").exists());
    assert (contentlet.getBinary("testBinary").getName().equals(fileName2));
    assert (contentlet.getBinary("testBinary").length() > 0);

  }


  /**
   * This test is for the TEMP_RESOURCE_ENABLED property
   * If is set to false the temp resource is disabled and a 403 should be thrown.
   *
   * @throws Exception
   */
  @Test
  public void test_TempResourceAPI_TempResourceEnabledProperty(){
    final boolean tempResourceEnabledOriginalValue = Config.getBooleanProperty(TempFileAPI.TEMP_RESOURCE_ENABLED, true);
    try {

      final String fileName = "test.png";

      Config.setProperty(TempFileAPI.TEMP_RESOURCE_ENABLED, false);
      final BodyPart filePart1 = new StreamDataBodyPart(fileName, inputStream());

      final MultiPart multipartEntity = new FormDataMultiPart()
              .bodyPart(filePart1);

      final Response jsonResponse = resource.uploadTempResourceMulti(request, response, (FormDataMultiPart) multipartEntity);

      assertEquals(Status.FORBIDDEN.getStatusCode(), jsonResponse.getStatus());
    }finally {
      Config.setProperty(TempFileAPI.TEMP_RESOURCE_ENABLED, tempResourceEnabledOriginalValue);
    }
  }

}
