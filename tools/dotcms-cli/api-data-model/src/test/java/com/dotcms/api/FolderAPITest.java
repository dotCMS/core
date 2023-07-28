package com.dotcms.api;

import com.dotcms.api.client.RestClientFactory;
import com.dotcms.api.client.ServiceManager;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.config.ServiceBean;
import com.dotcms.model.folder.SearchByPathRequest;
import com.google.common.collect.ImmutableList;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.Ignore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Ignore
@QuarkusTest
class FolderAPITest {

    @ConfigProperty(name = "com.dotcms.starter.site", defaultValue = "default")
    String siteName;

    @Inject
    AuthenticationContext authenticationContext;

    @Inject
    RestClientFactory clientFactory;

    @Inject
    ServiceManager serviceManager;

    @BeforeEach
    public void setupTest() throws IOException {
        serviceManager.removeAll().persist(ServiceBean.builder().name("default").active(true).build());

        final String user = "admin@dotcms.com";
        final char[] passwd = "admin".toCharArray();
        authenticationContext.login(user, passwd);
    }

    @Test
    void Test_Make_Folder() {

        final FolderAPI folderAPI = clientFactory.getClient(FolderAPI.class);
        final ResponseEntityView<List<Map<String, Object>>> makeFoldersResponse = folderAPI.makeFolders(
                ImmutableList.of("/f1", "/f1/f2", "/f1/f2/f3"), siteName);
        Assertions.assertNotNull(makeFoldersResponse.entity());

        final ResponseEntityView<List<Map<String, Object>>> byPathResponse = folderAPI.findByPath(
                SearchByPathRequest.builder().path(String.format("//%s/",siteName)).build());
        Assertions.assertNotNull(byPathResponse.entity());

    }

}
