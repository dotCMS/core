package com.dotcms.api;

import com.dotcms.DotCMSITProfile;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.common.SiteTestHelperService;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.config.ServiceBean;
import com.dotcms.model.site.CopySiteRequest;
import com.dotcms.model.site.CreateUpdateSiteRequest;
import com.dotcms.model.site.GetSiteByNameRequest;
import com.dotcms.model.site.Site;
import com.dotcms.model.site.SiteView;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

@QuarkusTest
@TestProfile(DotCMSITProfile.class)
class SiteAPIIT {

    @ConfigProperty(name = "com.dotcms.starter.site", defaultValue = "default")
    String siteName;

    @Inject
    AuthenticationContext authenticationContext;

    @Inject
    RestClientFactory clientFactory;

    @Inject
    ServiceManager serviceManager;

    @Inject
    SiteTestHelperService siteTestHelper;

    @BeforeEach
    public void setupTest() throws IOException {
        serviceManager.removeAll().persist(ServiceBean.builder().name("default").url(new URL("http://localhost:8080")).active(true).build());

        final String user = "admin@dotcms.com";
        final char[] passwd = "admin".toCharArray();
        authenticationContext.login(user, passwd);
    }

    @Test
    void Test_Get_Sites() {

        final ResponseEntityView<List<Site>> sitesResponse = clientFactory.getClient(SiteAPI.class).getSites(null, false, true, true, 1, 10);
        Assertions.assertNotNull(sitesResponse);
    }

    @Test
    void Test_Find_Host_By_Name() {
        final ResponseEntityView<SiteView> sitesResponse = clientFactory.getClient(SiteAPI.class)
                .findByName(
                        GetSiteByNameRequest.builder().siteName(siteName).build());
        Assertions.assertNotNull(sitesResponse);
        Assertions.assertEquals(siteName,sitesResponse.entity().hostName());
    }

    @Test
    void Test_Find_Non_Existing_Host_By_Name() {
        try {
            final ResponseEntityView<SiteView> sitesResponse = clientFactory.getClient(SiteAPI.class)
                    .findByName(
                            GetSiteByNameRequest.builder().siteName("myRandomSite" + System.currentTimeMillis()).build());
            Assertions.fail(" 404 Exception should have been thrown here.");
        }catch (Exception e){
            Assertions.assertTrue(e instanceof NotFoundException);
        }
    }

    @Test
    void Test_Default_Site() {
        ResponseEntityView<Site> defaultSiteResponse = clientFactory.getClient(SiteAPI.class).defaultSite();
        Assertions.assertNotNull(defaultSiteResponse);
        Assertions.assertTrue(defaultSiteResponse.entity().isDefault());
    }

    @Test
    void Test_Create_New_Site_Then_Update_Then_Delete() {

        final String newSiteName = String.format("newSiteName-%d",System.currentTimeMillis());
        CreateUpdateSiteRequest newSiteRequest = CreateUpdateSiteRequest.builder().siteName(newSiteName).build();
        ResponseEntityView<SiteView> createSiteResponse = clientFactory.getClient(SiteAPI.class).create(newSiteRequest);
        Assertions.assertNotNull(createSiteResponse);
        Assertions.assertFalse(createSiteResponse.entity().isDefault());
        String identifier = createSiteResponse.entity().identifier();
        Assertions.assertNotNull(identifier);
        Assertions.assertEquals(newSiteName,createSiteResponse.entity().hostName());

        final String updateSiteName = String.format("updatedSiteName-%d",System.currentTimeMillis());
        CreateUpdateSiteRequest updateSiteRequest = CreateUpdateSiteRequest.builder().siteName(updateSiteName).forceExecution(true).build();
        ResponseEntityView<SiteView> updateSiteResponse = clientFactory.getClient(SiteAPI.class).update(identifier,updateSiteRequest);
        Assertions.assertNotNull(updateSiteResponse);
        Assertions.assertFalse(updateSiteResponse.entity().isDefault());
        String returnedIdentifier = updateSiteResponse.entity().identifier();
        Assertions.assertNotNull(returnedIdentifier);
        Assertions.assertEquals(updateSiteName,updateSiteResponse.entity().hostName());

        ResponseEntityView<SiteView> archiveSite = clientFactory.getClient(SiteAPI.class).archive(returnedIdentifier);
        Assertions.assertNotNull(archiveSite.entity());
        Assertions.assertEquals(updateSiteName,archiveSite.entity().hostName());

        ResponseEntityView<Boolean> deleteSite = clientFactory.getClient(SiteAPI.class).delete(returnedIdentifier);
        Assertions.assertTrue(deleteSite.entity());

    }

    @Test
    void Test_Archive_Unarchive() {

        final String newSiteName = String.format("newSiteName-%d",System.currentTimeMillis());
        CreateUpdateSiteRequest newSiteRequest = CreateUpdateSiteRequest.builder().siteName(newSiteName).build();
        ResponseEntityView<SiteView> createSiteResponse = clientFactory.getClient(SiteAPI.class).create(newSiteRequest);
        Assertions.assertNotNull(createSiteResponse);
        Assertions.assertFalse(createSiteResponse.entity().isDefault());
        final String identifier = createSiteResponse.entity().identifier();
        Assertions.assertNotNull(identifier);
        Assertions.assertEquals(newSiteName,createSiteResponse.entity().hostName());

        ResponseEntityView<SiteView> archiveSite = clientFactory.getClient(SiteAPI.class).archive(identifier);
        Assertions.assertNotNull(archiveSite.entity());
        Assertions.assertTrue(siteTestHelper.checkValidSiteStatus(newSiteName, false, true));

        ResponseEntityView<SiteView> unarchiveSite = clientFactory.getClient(SiteAPI.class).unarchive(identifier);
        Assertions.assertNotNull(unarchiveSite.entity());
        Assertions.assertTrue(
                siteTestHelper.checkValidSiteStatus(newSiteName, false, false));
    }

    @Test
    void Test_Publish_UnPublish_Site() {

        final String newSiteName = String.format("newSiteName-%d",System.currentTimeMillis());
        CreateUpdateSiteRequest newSiteRequest = CreateUpdateSiteRequest.builder().siteName(newSiteName).build();
        ResponseEntityView<SiteView> createSiteResponse = clientFactory.getClient(SiteAPI.class).create(newSiteRequest);
        Assertions.assertNotNull(createSiteResponse);
        Assertions.assertFalse(createSiteResponse.entity().isDefault());
        final String identifier = createSiteResponse.entity().identifier();
        Assertions.assertNotNull(identifier);
        Assertions.assertEquals(newSiteName,createSiteResponse.entity().hostName());
        Assert.assertFalse(createSiteResponse.entity().isLive());

        ResponseEntityView<SiteView> publishedSite = clientFactory.getClient(SiteAPI.class).publish(identifier);
        Assertions.assertNotNull(publishedSite.entity());
        Assertions.assertTrue(siteTestHelper.checkValidSiteStatus(newSiteName, true, false));

        ResponseEntityView<SiteView> unPublishedSite = clientFactory.getClient(SiteAPI.class).unpublish(identifier);
        Assertions.assertNotNull(unPublishedSite.entity());
        Assertions.assertTrue(
                siteTestHelper.checkValidSiteStatus(newSiteName, false, false));
    }

    @Test
    void Test_Copy_Site() {
        final String newSiteName = String.format("newSiteName-%d",System.currentTimeMillis());
        CreateUpdateSiteRequest newSiteRequest = CreateUpdateSiteRequest.builder().siteName(newSiteName).build();
        ResponseEntityView<SiteView> createSiteResponse = clientFactory.getClient(SiteAPI.class).create(newSiteRequest);
        Assertions.assertNotNull(createSiteResponse);

        final String copySiteName = String.format("newSiteName-%d",System.currentTimeMillis());

        CopySiteRequest copySiteRequest = CopySiteRequest.builder().copyFromSiteId(
                createSiteResponse.entity().identifier()).
                copyContentOnSite(true).
                copyAll(true).
                copyContentOnPages(true).
                copyFolders(true).
                copyTemplatesContainers(true).
                copyLinks(true).
                copySiteVariables(true).
                site(CreateUpdateSiteRequest.builder().siteName(copySiteName).build()).build();

        ResponseEntityView<SiteView> copy = clientFactory.getClient(SiteAPI.class).copy(copySiteRequest);
        Assertions.assertNotNull(copy.entity());

    }

}
