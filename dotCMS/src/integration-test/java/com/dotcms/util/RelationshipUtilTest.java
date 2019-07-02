package com.dotcms.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.liferay.portal.model.User;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author nollymar
 */
public class RelationshipUtilTest {

    private static User user;
    private static ContentletAPI contentletAPI;
    private static ContentTypeAPI contentTypeAPI;
    private static Host host;
    private static HostAPI hostAPI;
    private static LanguageAPI languageAPI;
    private static long languageID;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        user = APILocator.getUserAPI().getSystemUser();
        contentletAPI  = APILocator.getContentletAPI();
        contentTypeAPI = APILocator.getContentTypeAPI(user);
        hostAPI        = APILocator.getHostAPI();
        languageAPI    = APILocator.getLanguageAPI();
        languageID     = languageAPI.getDefaultLanguage().getId();
        host = hostAPI.findDefaultHost(user, false);
    }

    @Test
    public void testFilterContentletWithLuceneQuery()
            throws DotSecurityException, DotDataException {

        final String filter = "+contentType:Youtube +(conhost:" + host.getIdentifier() + " conhost:SYSTEM_HOST) +catchall:how*";
        final List<Contentlet> testResults = RelationshipUtil
                .filterContentlet(languageID, filter, user, false);

        final List<Contentlet> expectedResults = contentletAPI
                .search(filter + " +languageId:" + languageID, 0, 0, null, user, false);

        assertNotNull(testResults);
        assertNotNull(expectedResults);
        assertEquals(expectedResults.size(), testResults.size());
        assertTrue(testResults.stream().allMatch(contentlet -> expectedResults.contains(contentlet)));
    }

    @Test
    public void testFilterContentletWithIdentifier()
            throws DotSecurityException, DotDataException {

        final ContentType widgetContentType = TestDataUtils.getWidgetLikeContentType();
        final Contentlet contentlet = TestDataUtils
                .getWidgetContent(true, languageID, widgetContentType.id());

        try {
            final String query = "+identifier:" + contentlet.getIdentifier() + " +languageId:" + languageID;
            final List<Contentlet> testResults = RelationshipUtil
                    .filterContentlet(languageID, contentlet.getIdentifier(), user, false);

            final List<Contentlet> expectedResults = contentletAPI
                    .search(query, 0, 0, null, user, false);

            assertNotNull(testResults);
            assertNotNull(expectedResults);
            assertEquals(expectedResults.size(), testResults.size());
            assertTrue(testResults.stream().allMatch(elem -> expectedResults.contains(elem)));
        } finally {
            if(contentlet != null && contentlet.getInode() != null){
                ContentletDataGen.remove(contentlet);
            }
        }
    }

    @Test
    public void testFilterContentletWithIdentifierAndLuceneQuery()
            throws DotSecurityException, DotDataException {

        final ContentType widgetContentType = TestDataUtils.getWidgetLikeContentType();
        final Contentlet contentlet = TestDataUtils
                .getWidgetContent(true, languageID, widgetContentType.id());

        try {
            final String filter = "+contentType:" + widgetContentType.variable();
            final List<Contentlet> testResults = RelationshipUtil
                    .filterContentlet(languageID,  contentlet.getIdentifier() + "," + filter , user, false);

            final List<Contentlet> expectedResults = contentletAPI
                    .search(filter + " +languageId:" + languageID, 0, 0, null, user, false);

            assertNotNull(testResults);
            assertNotNull(expectedResults);
            assertEquals(expectedResults.size(), testResults.size());
            assertTrue(testResults.stream().allMatch(elem -> expectedResults.contains(elem)));
        } finally {
            if (contentlet != null && contentlet.getInode() != null) {
                ContentletDataGen.remove(contentlet);
            }
        }
    }

}
