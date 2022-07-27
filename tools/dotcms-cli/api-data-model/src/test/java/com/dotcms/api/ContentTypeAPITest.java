package com.dotcms.api;


import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.contenttype.ContentType;
import io.quarkus.test.junit.QuarkusTest;
import java.util.List;
import javax.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ContentTypeAPITest {

    @Inject
    AuthenticationContext authenticationContext;



    @Test
    public void Test_Content_Type() {

        final String user = "admin@dotcms.com";
        final char[] passwd= "admin".toCharArray();
        authenticationContext.login(user, passwd);

        //final ResponseEntityView<List<ContentType>> response = client.getContentTypes(null, null, null, null, null, null, null );
        //Assertions.assertNotNull(response);
    }

}
