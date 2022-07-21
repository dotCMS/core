package com.dotcms.api;


import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.authentication.APITokenRequest;
import com.dotcms.model.authentication.TokenEntity;
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

    @Inject
    @RestClient
    ContentTypeAPI client;

    @Test
    public void Test_Content_Type() {

        final String user = "admin@dotcms.com";
        final String passwd= "admin";
        authenticationContext.login(user, passwd);

        final ResponseEntityView<List<ContentType>> response = client.getContentTypes(null, null, null, null, null, null, null );
        Assertions.assertNotNull(response);
    }

}
