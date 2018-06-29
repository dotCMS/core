package v2.languages;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v2.languages.LanguagesResource;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.portal.model.User;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @test LanguageResource
 */
public class LanguagesResourceTest {

    @Test
    public void list() throws DotSecurityException, DotDataException {
        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final WebResource webResource = mock(WebResource.class);
        final LanguageAPI languageAPI = mock(LanguageAPI.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User user = new User();
        final List<Language> languages = CollectionsUtils.list(mock(Language.class));

        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(null, true, request, true, null)).thenReturn(initDataObject);
        when(languageAPI.getLanguages()).thenReturn(languages);

        final LanguagesResource languagesResource = new LanguagesResource(languageAPI, webResource);
        final Response response = languagesResource.list(request, null);

        assertEquals(languages, ((ResponseEntityView) response.getEntity()).getEntity());
    }

    @Test
    public void test_when_contentInodeIsNotNull_should_listLanguageByInode() throws DotSecurityException, DotDataException {
        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final WebResource webResource = mock(WebResource.class);
        final LanguageAPI languageAPI = mock(LanguageAPI.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User user = new User();
        final List<Language> languages = CollectionsUtils.list(mock(Language.class));

        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(null, true, request, true, null)).thenReturn(initDataObject);
        when(languageAPI.getAvailableContentLanguages("2", user)).thenReturn(languages);

        final LanguagesResource languagesResource = new LanguagesResource(languageAPI, webResource);
        final Response response = languagesResource.list(request, "2");

        assertEquals(languages, ((ResponseEntityView) response.getEntity()).getEntity());
    }

    @Test(expected = DotDataException.class)
    public void test_when_contentInodeIsNotNull_and_throwDotDataException() throws DotSecurityException, DotDataException {
        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final WebResource webResource = mock(WebResource.class);
        final LanguageAPI languageAPI = mock(LanguageAPI.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User user = new User();
        final DotDataException exception = mock(DotDataException.class);

        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(null, true, request, true, null)).thenReturn(initDataObject);
        when(languageAPI.getAvailableContentLanguages("2", user)).thenThrow(exception);

        final LanguagesResource languagesResource = new LanguagesResource(languageAPI, webResource);


        languagesResource.list(request, "2");
    }

    @Test(expected = DotSecurityException.class)
    public void test_when_contentInodeIsNotNull_and_throwDotSecurityException() throws DotSecurityException, DotDataException {
        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final WebResource webResource = mock(WebResource.class);
        final LanguageAPI languageAPI = mock(LanguageAPI.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User user = new User();
        final DotSecurityException exception = mock(DotSecurityException.class);

        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(null, true, request, true, null)).thenReturn(initDataObject);
        when(languageAPI.getAvailableContentLanguages("2", user)).thenThrow(exception);

        final LanguagesResource languagesResource = new LanguagesResource(languageAPI, webResource);

        languagesResource.list(request, "2");
    }
}
