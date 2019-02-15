package com.dotmarketing.cms.urlmap;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static org.jgroups.util.Util.assertEquals;
import static org.jgroups.util.Util.assertFalse;
import static org.mockito.Mockito.mock;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class URLMapAPIImplTest {
    private static User systemUser;
    private static Host host;

    private URLMapAPIImpl urlMapAPI;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        systemUser = APILocator.getUserAPI().getSystemUser();
        host = APILocator.getHostAPI().findByName("demo.dotcms.com", systemUser, false);
    }

    @Before
    public void prepareTest() {
        urlMapAPI = new URLMapAPIImpl();
    }

    @Test
    public void shouldReturnContentletWhenTheContentExists()
            throws DotDataException, DotSecurityException {

        final UrlMapContext context = getUrlMapContext(systemUser, host,
                "/news/u-s-labor-department-moves-forward-on-retirement-advice-proposal");

        final Optional<URLMapInfo> urlMapInfoOptional = urlMapAPI.processURLMap(context);

        final URLMapInfo urlMapInfo = urlMapInfoOptional.get();
        assertEquals("U.S. Labor Department moves forward on retirement advice proposal", urlMapInfo.getContentlet().getName());
        assertEquals("/news-events/news/news-detail", urlMapInfo.getIdentifier().getURI());
    }

    @Test
    public void shouldReturnNullWhenTheContentNotExists()
            throws DotDataException, DotSecurityException {

        final UrlMapContext context = getUrlMapContext(systemUser, host,
                "/news/u-s-labor-department-moves-forward-on-retirement-advice-proposal-esp");

        final Optional<URLMapInfo> urlMapInfoOptional = urlMapAPI.processURLMap(context);

        assertFalse(urlMapInfoOptional.isPresent());
    }

    @Test
    public void shouldReturnNullWhenThePageUriIsNotDetailPage()
            throws DotDataException, DotSecurityException {

        final UrlMapContext context = getUrlMapContext(systemUser, host, "/about-us/index");

        final Optional<URLMapInfo> urlMapInfoOptional = urlMapAPI.processURLMap(context);

        assertFalse(urlMapInfoOptional.isPresent());
    }

    @Test(expected = DotSecurityException.class)
    public void shouldThrowDotSecurityExceptionWhenUserDontHavePermission()
            throws DotDataException, DotSecurityException {

        final List<User> users = APILocator.getUserAPI().getUsersByNameOrEmail("chris@dotcms.com", 1, 1);
        final UrlMapContext context = getUrlMapContext(users.get(0), host,
                "/news/u-s-labor-department-moves-forward-on-retirement-advice-proposal");

        urlMapAPI.processURLMap(context);
    }


    private UrlMapContext getUrlMapContext(final User systemUser, final Host host, final String uri) {
        return UrlMapContextBuilder.builder()
                .setHost(host)
                .setLanguageId(1l)
                .setMode(PageMode.PREVIEW_MODE)
                .setUri(uri)
                .setUser(systemUser)
                .build();
    }
}
