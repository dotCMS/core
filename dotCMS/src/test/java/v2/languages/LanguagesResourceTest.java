package v2.languages;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v2.languages.LanguagesResource;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.portal.model.User;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @test LanguageResource
 */
public class LanguagesResourceTest {

    @Test
    public void list(){
        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final WebResource webResource = mock(WebResource.class);
        LanguageAPI languageAPI = mock(LanguageAPI.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User user = new User();
        final UserAPI userAPI = mock(UserAPI.class);
        final List<Language> languages = CollectionsUtils.list(mock(Language.class));

        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(null, true, request, true, null)).thenReturn(initDataObject);
        when(languageAPI.getLanguages()).thenReturn(languages);

        LanguagesResource languagesResource = new LanguagesResource(languageAPI, webResource);

        Response response = languagesResource.list(request);

        assertEquals(languages, ((ResponseEntityView) response.getEntity()).getEntity());
    }
}
