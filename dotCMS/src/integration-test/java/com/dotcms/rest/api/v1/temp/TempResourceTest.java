package com.dotcms.rest.api.v1.temp;

import static org.junit.Assert.assertFalse;

import java.io.InputStream;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.util.IntegrationTestInitService;

public class TempResourceTest {
  @BeforeClass
  public static void prepare() throws Exception {
    // Setting web app environment
    IntegrationTestInitService.getInstance().init();

  }

  HttpServletRequest mockRequest() {
    return new MockSessionRequest(new MockHeaderRequest(new MockHttpRequest("localhost", "/api/v1/tempResource").request(), "Origin", "localhost").request());
  }
  
  
  @Test
  public void test_temp_resource_upload()  {
   TempResource resource = new TempResource();
   
   HttpServletRequest request =mockRequest();

   final String sessionID = request.getSession().getId();
   final String fieldVar = "image";
   final String fileName="test.file";
   HttpServletResponse response = new MockHttpResponse();
   
   InputStream inputStream = this.getClass().getResourceAsStream("/images/SqcP9KgFqruagXJfe7CES.png");
   Date date = new Date();
   final FormDataContentDisposition fileMetaData = FormDataContentDisposition.name("testData").fileName(fileName).creationDate(date)
       .modificationDate(date).readDate(date).size(1222).build();



   Response jsonResponse = resource.uploadTempResource(request, response, fieldVar, inputStream, fileMetaData);


    Map<String,Object> map = (Map) jsonResponse.getEntity();
    System.err.println("got temp resource:" + map);
    final String tempFileUrl=map.get("tempFile").toString();
    // its not an image because we set the filename to "test.file"
    assertFalse((Boolean)map.get("tempFileIsImage"));
    
    assert(tempFileUrl.startsWith("/tmp"));
    assert(tempFileUrl.endsWith(fileName));
    assert(tempFileUrl.split("/")[2].equals(fieldVar));
    assert(map.get("tempFileName").toString().equals(fileName));
    assert(((Long) map.get("tempFileSize"))>0);

  }

}
