package com.dotcms.enterprise.priv;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.content.elasticsearch.business.ESSearchResults;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ESSearchProxyTest extends IntegrationTestBase {

    private static User user;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();

        long defLangId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        user = APILocator.getUserAPI().getSystemUser();

        final Host defaultHost = APILocator.getHostAPI()
                .findDefaultHost(APILocator.systemUser(), false);
        final User systemUser = APILocator.systemUser();
        final Folder aboutUs =
         APILocator.getFolderAPI()
                .createFolders("/about-us" + System.currentTimeMillis() + "/",
                        defaultHost, systemUser, false);

        for(int i=0; i<=10; i++ ){
            final Contentlet page = TestDataUtils.getPageContent(true, defLangId, aboutUs);
            ContentletDataGen.publish(page);
        }

    }

    @Test
    public void test_esSearch_WithoutLicense_Success() throws Exception {
        runNoLicense(()-> {
                final String query = "{\"query\":{\"query_string\":{\"query\":\"+basetype:5 +parentpath:*\\\\\\/abou*\"}}}";
                final List<ESSearchResults> resultsList = getEsSearchResults(query);
                Assert.assertFalse(resultsList.isEmpty());
        });
    }

    @Test
    public void test_esSearch_WithLicense_Success() throws Exception {
        final String query = "{\"query\":{\"query_string\":{\"query\":\"+basetype:5 +parentpath:*\\\\\\/abou*\"}}}";
        final List<ESSearchResults> resultsList = getEsSearchResults(query);
        Assert.assertFalse(resultsList.isEmpty());
    }

    private List<ESSearchResults> getEsSearchResults(final String query)
            throws DotSecurityException, DotDataException {
        final ESSearchProxy esSearchProxy = new ESSearchProxy();
        return (List<ESSearchResults>) esSearchProxy.esSearch(query,true, user,false);
    }

}
