package com.dotmarketing.cms.urlmap;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.rest.api.v1.page.PageResource;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.SimpleStructureURLMap;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;

import static org.jgroups.util.Util.*;

/**
 * Test for {@link URLMapAPIImpl} but removing all the Content Type with Url Mapper from database
 */
public class URLMapAPIImplNotMapperTest {
    private static User systemUser;
    private static Host host;
    private static  URLMapAPIImpl urlMapAPI;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        systemUser = APILocator.getUserAPI().getSystemUser();

        deleteAllUrlMapperContentType();

        host = new SiteDataGen().nextPersisted();
        urlMapAPI = new URLMapAPIImpl();
    }

    /**
     * methodToTest {@link URLMapAPIImpl#processURLMap(UrlMapContext)}
     * Given Scenario: Procces dorAdmin URL when not any URL Mapper exists
     * ExpectedResult: Should return a {@link Optional#empty()}
     */
    @Test
    public void processURLMap() throws DotDataException, DotSecurityException {

        final UrlMapContext context = getUrlMapContext(systemUser, host, "/dotAdmin");

        final Optional<URLMapInfo> urlMapInfoOptional = urlMapAPI.processURLMap(context);

        assertFalse(urlMapInfoOptional.isPresent());
    }

    /**
     * methodToTest {@link URLMapAPIImpl#isUrlPattern(UrlMapContext)}}
     * Given Scenario: Call  isUrlPattern with dorAdmin URL when not any URL Mapper exists
     * ExpectedResult: Should return false
     */
    @Test
    public void isUrlPattern() throws DotDataException {
        final UrlMapContext context = getUrlMapContext(systemUser, host, "/dotAdmin");
        assertFalse(urlMapAPI.isUrlPattern(context));
    }

    private static void deleteAllUrlMapperContentType() throws DotDataException {
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(systemUser);
        final List<SimpleStructureURLMap> structureURLMapPatterns =
                contentTypeAPI.findStructureURLMapPatterns();

        structureURLMapPatterns
                .stream()
                .map((SimpleStructureURLMap simpleStructureURLMap) -> simpleStructureURLMap.getInode())
                .forEach(contenTypeInode -> {
                    try {
                        contentTypeAPI.delete(contentTypeAPI.find(contenTypeInode));
                    } catch (DotSecurityException | DotDataException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private UrlMapContext getUrlMapContext(final User systemUser, final Host host, final String uri) {
        return UrlMapContextBuilder.builder()
                .setHost(host)
                .setLanguageId(1L)
                .setMode(PageMode.PREVIEW_MODE)
                .setUri(uri)
                .setUser(systemUser)
                .build();
    }
}
