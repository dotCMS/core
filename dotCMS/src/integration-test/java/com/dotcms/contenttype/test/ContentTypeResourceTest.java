package com.dotcms.contenttype.test;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.contenttype.business.ContentTypeApi;
import com.dotcms.contenttype.business.ContentTypeFactory;
import com.dotcms.contenttype.business.ContentTypeFactoryImpl;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.repackage.javax.ws.rs.core.Application;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.tika.io.IOUtils;
import com.dotcms.rest.api.v1.contenttype.ContentTypeResource;
import com.dotmarketing.business.APILocator;

public class ContentTypeResourceTest {

  final ContentTypeFactory factory = new ContentTypeFactoryImpl();
  static ContentTypeApi api;

  @BeforeClass
  public static void SetUpTests() throws FileNotFoundException, Exception {
    SuperContentTypeTest.SetUpTests();
    api = APILocator.getContentTypeAPI2(APILocator.systemUser());
  }

  /**
   * BasicAuth
   * @return
   */
  private HttpServletRequest getHttpRequest() {
    HttpServletRequest request = new MockHeaderRequest(
        new MockSessionRequest(
            new MockAttributeRequest(
                new MockHttpRequest("localhost", "/").request()).request())
            .request(),"Authorization", "Basic YWRtaW5AZG90Y21zLmNvbTphZG1pbg=="
         ).request();


    return request;
  }

  public class MyApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> s = new HashSet<Class<?>>();
        s.add(ContentTypeResource.class);
        return s;
    }
}
  
  
  
  
  
  
  @Test
  public void testJsonToContentTypeArray() throws Exception {

    try {
      ContentType type = api.find("banner");
      api.delete(type);
    } catch (NotFoundInDbException e) {

    }

    MyApplication app = new MyApplication();
    for(Object j : app.getSingletons()){
      
    }

    InputStream stream = this.getClass().getResourceAsStream("/com/dotcms/contenttype/test/content-type-array.json");
    String json = IOUtils.toString(stream);
    stream.close();

    HttpServletRequest req = getHttpRequest();

    HttpServletRequest req2 = new MockHeaderRequest(req,"wetrew","qwerewrew");
    
    ContentTypeResource resource = new ContentTypeResource();

    Response response = resource.saveType(getHttpRequest(), json);

    int x = response.getStatus();
    assertThat("we get a 200 back when saving content type via json", x == 200);
  }


}
