package v2.languages;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v2.languages.LanguageView;
import com.dotcms.rest.api.v2.languages.LanguagesResource;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @test LanguageResource
 */
public class LanguagesResourceTest {

    @Test
    public void list() throws DotSecurityException, DotDataException {
        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        final WebResource webResource = mock(WebResource.class);
        final LanguageAPI languageAPI = mock(LanguageAPI.class);
        final LanguageVariableAPI languageVariableAPI = mock(LanguageVariableAPI.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User user = new User();
        final List<Language> languages = CollectionsUtils.list(mock(Language.class));

        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(request, httpServletResponse,true)).thenReturn(initDataObject);
        when(languageAPI.getLanguages()).thenReturn(languages);
        final LanguageView languageView = mock(LanguageView.class);

        final LanguagesResource languagesResource = spy(new LanguagesResource(languageAPI, languageVariableAPI, webResource));
        Mockito.doReturn((Function<Language, LanguageView>) language -> languageView)
                .when(languagesResource)
                .instanceLanguageView();
        final Response response = languagesResource.list(request, httpServletResponse, null, false);

        assertEquals(languages.size(), ((List) ((ResponseEntityView) response.getEntity()).getEntity()).size());
    }

    @Test
    public void test_when_contentInodeIsNotNull_should_listLanguageByInode() throws DotSecurityException, DotDataException {
        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        final WebResource webResource = mock(WebResource.class);
        final LanguageAPI languageAPI = mock(LanguageAPI.class);
        final LanguageVariableAPI languageVariableAPI = mock(LanguageVariableAPI.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User user = new User();
        final List<Language> languages = CollectionsUtils.list(mock(Language.class));
        final LanguageView languageView = mock(LanguageView.class);

        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(request, httpServletResponse,true)).thenReturn(initDataObject);
        when(languageAPI.getAvailableContentLanguages("2", user)).thenReturn(languages);
        when(languageAPI.getDefaultLanguage()).thenReturn(mock(Language.class));

        final LanguagesResource languagesResource = spy(new LanguagesResource(languageAPI, languageVariableAPI, webResource));
        Mockito.doReturn((Function<Language, LanguageView>) language -> languageView)
                .when(languagesResource)
                .instanceLanguageView();

        final Response response = languagesResource.list(request, httpServletResponse, "2", false);

        assertEquals(languages.size(), ((List) ((ResponseEntityView) response.getEntity()).getEntity()).size());
    }

    @Test(expected = DotDataException.class)
    public void test_when_contentInodeIsNotNull_and_throwDotDataException() throws DotSecurityException, DotDataException {
        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        final WebResource webResource = mock(WebResource.class);
        final LanguageAPI languageAPI = mock(LanguageAPI.class);
        final LanguageVariableAPI languageVariableAPI = mock(LanguageVariableAPI.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User user = new User();
        final DotDataException exception = mock(DotDataException.class);

        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(request, httpServletResponse, true)).thenReturn(initDataObject);
        when(languageAPI.getAvailableContentLanguages("2", user)).thenThrow(exception);
        when(languageAPI.getDefaultLanguage()).thenReturn(mock(Language.class));
        final LanguagesResource languagesResource = new LanguagesResource(languageAPI, languageVariableAPI, webResource);

        languagesResource.list(request, httpServletResponse, "2", false);
    }

    @Test(expected = DotSecurityException.class)
    public void test_when_contentInodeIsNotNull_and_throwDotSecurityException() throws DotSecurityException, DotDataException {
        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        final WebResource webResource = mock(WebResource.class);
        final LanguageAPI languageAPI = mock(LanguageAPI.class);
        final LanguageVariableAPI languageVariableAPI = mock(LanguageVariableAPI.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User user = new User();
        final DotSecurityException exception = mock(DotSecurityException.class);

        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(request, httpServletResponse, true)).thenReturn(initDataObject);
        when(languageAPI.getAvailableContentLanguages("2", user)).thenThrow(exception);
        when(languageAPI.getDefaultLanguage()).thenReturn(mock(Language.class));
        final LanguagesResource languagesResource = new LanguagesResource(languageAPI, languageVariableAPI, webResource);

        languagesResource.list(request, httpServletResponse, "2", false);
    }

    /**
     * Test that the language resource returns the language list with the count of language variables
     * Given scenario: The language resource is requested with the count of language variables
     * Expected result: The language list should contain the count of language variables for each language
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void test_when_countLangVarsIsNotNull() throws DotSecurityException, DotDataException {
        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        final WebResource webResource = mock(WebResource.class);
        final LanguageAPI languageAPI = mock(LanguageAPI.class);
        final LanguageVariableAPI languageVariableAPI = mock(LanguageVariableAPI.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User user = new User();
        //Mock some languages
        final List<Language> languages = CollectionsUtils.list(
                new Language(1L, "en", "US", "English", "United States"),
                new Language(2L, "es", "ES", "Spanish", "Spain")
        );
        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(request, httpServletResponse,true)).thenReturn(initDataObject);
        when(languageAPI.getAvailableContentLanguages("2", user)).thenReturn(languages);
        //Mock the default language
        when(languageAPI.getDefaultLanguage()).thenReturn(languages.get(0));

        final LanguagesResource languagesResource = new LanguagesResource(languageAPI, languageVariableAPI, webResource);
        //Request the languages with the count of language variables
        final Response response = languagesResource.list(request, httpServletResponse, "2", true);

        final Object entity = ((ResponseEntityView) response.getEntity()).getEntity();
        List<LanguageView>  languageViews = (List<LanguageView>) entity;
        Assert.assertEquals(languages.size(), languageViews.size());
        Assert.assertTrue(languageViews.stream().allMatch(languageView -> languageView.getVariables() != null));
        //Verify that there's a default language
        final Optional<LanguageView> defaultLang = languageViews.stream()
                .filter(LanguageView::isDefaultLanguage).findFirst();
        Assert.assertTrue(defaultLang.isPresent());
        Assert.assertEquals(1L, defaultLang.get().getId());
        //Finally test that all the languages have a language variable count info
        Assert.assertTrue(languageViews.stream().allMatch(Objects::nonNull));

    }

    /**
     * Test that the language resource returns the language and country list
     * Given scenario: Simply call the language resource
     * Expected result: The language and country list We test that a few counties and languages are returned
     */
    @Test
    public void testISO() {
        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        final WebResource webResource = mock(WebResource.class);
        final LanguageAPI languageAPI = mock(LanguageAPI.class);
        final LanguageVariableAPI languageVariableAPI = mock(LanguageVariableAPI.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User user = new User();
        when(initDataObject.getUser()).thenReturn(user);

        final LanguagesResource languagesResource = new LanguagesResource(languageAPI, languageVariableAPI, webResource);
        final Response response = languagesResource.getIsoLanguagesAndCountries(
                request, httpServletResponse);

        final Object entity = ((ResponseEntityView) response.getEntity()).getEntity();
        Map<String,List<Map<String, String>>> map = (Map<String,List<Map<String, String>>>) entity;
        final List<Map<String, String>> languages = map.get("languages");
        final List<Map<String, String>> countries = map.get("countries");
        Assert.assertNotNull(languages);

        final Set<String> langCodes = languages.stream()
                .map(stringStringMap -> stringStringMap.get("code")).collect(Collectors.toSet());

        Assert.assertNotNull(countries);

        final Set<String> countryCodes = countries.stream()
                .map(stringStringMap -> stringStringMap.get("code")).collect(Collectors.toSet());

        Assert.assertTrue(langCodes.contains("en"));
        Assert.assertTrue(langCodes.contains("es"));

        Assert.assertTrue(countryCodes.contains("US"));
        Assert.assertTrue(countryCodes.contains("CR"));

    }

}
