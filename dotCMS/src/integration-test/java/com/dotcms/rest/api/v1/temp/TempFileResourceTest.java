package com.dotcms.rest.api.v1.temp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
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

import com.dotcms.api.web.HttpServletRequestThreadLocal;
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
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class TempFileResourceTest {


  static HttpServletResponse response;
  static TempFileResource resource;

  @BeforeClass
  public static void prepare() throws Exception {
    // Setting web app environment
    IntegrationTestInitService.getInstance().init();

    resource = new TempFileResource();

    response = new MockHttpResponse();
  }

  private static HttpServletRequest mockRequest() {
    return new MockSessionRequest(
        new MockHeaderRequest(new MockHttpRequest("localhost", "/api/v1/tempResource").request(), "Origin", "localhost").request());
  }
  private InputStream inputStream() {
    return this.getClass().getResourceAsStream("/images/SqcP9KgFqruagXJfe7CES.png");
  }

  private DotTempFile saveTempFile_usingTempResource(final String fileName, final HttpServletRequest request){
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
    final DotTempFile dotTempFile = saveTempFile_usingTempResource(fileName, mockRequest());
  // its not an image because we set the filename to "test.file"
    assertFalse((Boolean) dotTempFile.image);

    assertTrue(dotTempFile.id.startsWith(TempFileAPI.TEMP_RESOURCE_PREFIX));

    assertTrue(dotTempFile.file.getName().equals(fileName));
    assertTrue(dotTempFile.length() > 0);
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
    
    final Response jsonResponse = resource.uploadTempResourceMulti(mockRequest(), response, (FormDataMultiPart) multipartEntity);
    assertNotNull(jsonResponse);

    final Map<String,List<DotTempFile>> dotTempFile = (Map) jsonResponse.getEntity();
    assertFalse(dotTempFile.isEmpty());
    
    assertEquals(2,dotTempFile.get("tempFiles").size());
    DotTempFile file= (DotTempFile) dotTempFile.get("tempFiles").get(0);
    assertEquals(fileName1,file.fileName);
    assertNotNull(file.image);
    assertTrue(file.length()>1000);
    
    file= (DotTempFile) dotTempFile.get("tempFiles").get(1);
    assertEquals(fileName2,file.fileName);
    assertNotNull(file.image);
    assertTrue(file.length()>1000);

  }

  @Test
  public void test_tempResourceAPI_who_can_use_via_userID(){

    final User user = new UserDataGen().nextPersisted();
    HttpServletRequest request = mockRequest();
    request.setAttribute(WebKeys.USER, user);
    request.setAttribute(WebKeys.USER_ID, user.getUserId());

    final String fileName = "test.png";
    final DotTempFile dotTempFile = saveTempFile_usingTempResource(fileName,request);

    // CANNOT get the file again because we have a new session ID in the new mock request
    Optional<DotTempFile> file = new TempFileAPI().getTempFile(mockRequest(), dotTempFile.id);
    assertFalse(file.isPresent());

    request.setAttribute(WebKeys.USER, user);
    request.setAttribute(WebKeys.USER_ID, user.getUserId());
    // CAN get the file again because we are the user who uploaded it
    file = new TempFileAPI().getTempFile(request, dotTempFile.id);
    assertTrue(file.isPresent() && !file.get().file.isDirectory());
  }

  @Test
  public void test_tempResourceapi_max_age(){
    HttpServletRequest request = mockRequest();
    final String fileName = "test.png";

    final DotTempFile dotTempFile = saveTempFile_usingTempResource(fileName,request);

    Optional<DotTempFile> file = new TempFileAPI().getTempFile(request, dotTempFile.id);

    assertTrue(file.isPresent());

    int tempResourceMaxAgeSeconds = Config.getIntProperty("TEMP_RESOURCE_MAX_AGE_SECONDS", 1800);

    // this works becuase we set the file age to newer than max age
    file.get().file.setLastModified(System.currentTimeMillis() - 60 * 10 * 1000);
    file = new TempFileAPI().getTempFile(request, dotTempFile.id);
    assertTrue(file.isPresent());

    // Setting the file to older than max age makes the file inaccessable
    file.get().file.setLastModified(System.currentTimeMillis() - (tempResourceMaxAgeSeconds * 1000) - 1);
    file = new TempFileAPI().getTempFile(request, dotTempFile.id);
    assertFalse(file.isPresent());
  }
  

  @Test
  public void test_tempResourceapi_test_anonymous_access(){

    final String fileName = "test.png";
    HttpServletRequest request = mockRequest();
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
    assertNotNull(dotTempFile.id);
  }
  
  
  @Test
  public void temp_resource_makes_it_into_checked_in_content() throws Exception {

    final User user = APILocator.systemUser();
    final Host host = APILocator.getHostAPI().findDefaultHost(user, true);
    HttpServletRequest request = mockRequest();
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
    final DotTempFile dotTempFile1 = saveTempFile_usingTempResource(fileName1, request);
    
    final RemoteUrlForm form = new RemoteUrlForm(
        "https://raw.githubusercontent.com/dotCMS/core/master/dotCMS/src/main/webapp/html/images/skin/logo.gif", fileName2, null);

    final Response jsonResponse = resource.copyTempFromUrl(request, form);
    final Map<String,List<DotTempFile>> dotTempFiles = (Map) jsonResponse.getEntity();
    final DotTempFile dotTempFile2 = dotTempFiles.get("tempFiles").get(0);

    
    HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
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
    
    

    assertNotNull(contentlet.getBinary("fileAsset"));
    assertEquals(fileName1,contentlet.getBinary("fileAsset").getName());
    assertTrue(contentlet.getBinary("fileAsset").length() > 0);
    assertNotNull(contentlet.getBinary("testBinary").exists());
    assertEquals(fileName2,contentlet.getBinary("testBinary").getName());
    assertTrue(contentlet.getBinary("testBinary").length() > 0);

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
      HttpServletRequest request = mockRequest();
      final String fileName = "test.png";

      Config.setProperty(TempFileAPI.TEMP_RESOURCE_ENABLED, false);
      final BodyPart filePart1 = new StreamDataBodyPart(fileName, inputStream());

      final MultiPart multipartEntity = new FormDataMultiPart()
              .bodyPart(filePart1);

      final Response jsonResponse = resource.uploadTempResourceMulti(request, response, (FormDataMultiPart) multipartEntity);

      assertEquals(Status.NOT_FOUND.getStatusCode(), jsonResponse.getStatus());
    }finally {
      Config.setProperty(TempFileAPI.TEMP_RESOURCE_ENABLED, tempResourceEnabledOriginalValue);
    }
  }



  @Test
  @UseDataProvider("testCasesChangeFingerPrint")
  public void testGetTempFile_fileIsNotReturned_fingerprintIsDifferent(final testCaseChangeFingerPrint testCase){

    final User user = new UserDataGen().nextPersisted();
    HttpServletRequest request = mockRequest();
    request.setAttribute(WebKeys.USER, user);

    final String fileName = "test.png";
    final DotTempFile dotTempFile = saveTempFile_usingTempResource(fileName,request);

    // CAN get the file again because it is the same request
    Optional<DotTempFile> file = new TempFileAPI().getTempFile(request, dotTempFile.id);
    assertTrue(file.isPresent() && !file.get().file.isDirectory());

    // CANNOT get the file again because the request header changed
    HttpServletRequest newRequest = new MockSessionRequest(
            new MockHeaderRequest(new MockHttpRequest("localhost", "/api/v1/tempResource").request(), testCase.headerName, "newValue")
                    .request());
    file = new TempFileAPI().getTempFile(newRequest, dotTempFile.id);
    assertFalse(file.isPresent());
  }

  private static class testCaseChangeFingerPrint{
    String headerName;

    testCaseChangeFingerPrint(final String headerName){
      this.headerName = headerName;
    }
  }

  @DataProvider
  public static Object[] testCasesChangeFingerPrint(){
    return new Object[] {
            new testCaseChangeFingerPrint("User-Agent"),
            new testCaseChangeFingerPrint("Host"),
            new testCaseChangeFingerPrint("Accept-Language"),
            new testCaseChangeFingerPrint("Accept-Encoding"),
            new testCaseChangeFingerPrint("X-Forwarded-For"),
            new testCaseChangeFingerPrint("referer")
    };
  }

}
