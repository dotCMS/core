package com.dotcms.api;


import com.dotcms.api.client.RestClientFactory;
import com.dotcms.api.client.ServiceManager;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.config.ServiceBean;
import com.dotcms.model.contenttype.ContentType;
import com.google.common.collect.ImmutableList;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ContentTypeAPITest {

    @Inject
    AuthenticationContext authenticationContext;

    @Inject
    RestClientFactory apiClientFactory;

    @Inject
    ServiceManager serviceManager;

    @BeforeEach
    public void setupTest() throws IOException {
        serviceManager.removeAll().persist(ServiceBean.builder().name("default").active(true).build());
    }

    @Test
    public void Test_Content_Types() {

        final ContentTypeAPI client = apiClientFactory.getClient(ContentTypeAPI.class);

        final String user = "admin@dotcms.com";
        final char[] passwd= "admin".toCharArray();
        authenticationContext.login(user, passwd);

        final ResponseEntityView<List<ContentType>> response = client.getContentTypes(null, null, null, null, null, null, null );
        Assertions.assertNotNull(response);
    }

    @Test
    public void Test_Content_Type() {

        final ContentTypeAPI client = apiClientFactory.getClient(ContentTypeAPI.class);

        final String user = "admin@dotcms.com";
        final char[] passwd= "admin".toCharArray();
        authenticationContext.login(user, passwd);

        final ResponseEntityView<ContentType> response = client.getContentType("Blog", 1L, true);
        Assertions.assertNotNull(response);
    }


    @Test
    public void Test_Save_Content_Type() {

        final ContentTypeAPI client = apiClientFactory.getClient(ContentTypeAPI.class);

        final String user = "admin@dotcms.com";
        final char[] passwd= "admin".toCharArray();
        authenticationContext.login(user, passwd);

        final ResponseEntityView<ContentType> response = client.getContentType("Blog", 1L, true);
        final ContentType ct = response.entity();

        final ResponseEntityView<List<ContentType>> response2 = client.createContentTypes(ImmutableList.of(ct));
        Assertions.assertNotNull(response2);
    }

}
