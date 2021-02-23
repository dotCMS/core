package com.dotcms.translate;

import static com.dotcms.translate.TranslateTestUtil.TEXT_AREA_VN;
import static com.dotcms.translate.TranslateTestUtil.TEXT_FIELD_VN;
import static com.dotcms.translate.TranslateTestUtil.WYSIWYG_VN;
import static com.dotcms.translate.TranslateTestUtil.english;
import static com.dotcms.translate.TranslateTestUtil.french;
import static com.dotcms.translate.TranslateTestUtil.getEnglishContent;
import static com.dotcms.translate.TranslateTestUtil.spanish;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.rendering.velocity.viewtools.JSONTool;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.mockito.Mockito;

public class GoogleTranslationServiceTest extends UnitTestBase {
    private User testUser = new User();

    @Test(expected = NullPointerException.class)
    public void translateContent_NullContent() throws TranslationException {
        new GoogleTranslationService("key", new JSONTool(), new ApiProvider())
            .translateContent(null, new Language(), null, testUser);
    }

    @Test
    public void translateContent() throws TranslationException, DotDataException, DotSecurityException {

        Contentlet toTranslate = getEnglishContent();
        toTranslate = Mockito.spy(toTranslate);
        Structure st = new Structure();
        Mockito.doReturn(st).when(toTranslate).getStructure();

        // mock language api to return language id
        LanguageAPI languageAPI = Mockito.mock(LanguageAPI.class);
        when(languageAPI.getLanguage(english.getId())).thenReturn(english);

        UserAPI userAPI = Mockito.mock(UserAPI.class);
        when(userAPI.getSystemUser()).thenReturn(testUser);

        ContentletAPI contentAPI = Mockito.mock(ContentletAPI.class);
        when(contentAPI.checkout(toTranslate.getInode(), testUser, false)).thenReturn(new Contentlet());

        // mock api provider to return mocked language api
        ApiProvider apiProvider = Mockito.mock(ApiProvider.class);
        when(apiProvider.languageAPI()).thenReturn(languageAPI);
        when(apiProvider.userAPI()).thenReturn(userAPI);
        when(apiProvider.contentletAPI()).thenReturn(contentAPI);

        List<String> valuesToTranslate = Arrays.asList("English Value 1", "English Value 2", "English Value 3");
        List<String> translatedValues = Arrays.asList("Valor Español 1", "Valor Español 2", "Valor Español 3");

        TranslationService service = Mockito.spy(new GoogleTranslationService("key", null, apiProvider));

        Mockito.doReturn(translatedValues).when(service).translateStrings(valuesToTranslate, english, spanish);

        Contentlet translatedContent = service
            .translateContent(toTranslate, spanish, (List<Field>) toTranslate.get("fieldsToTranslate"), testUser);

        assertEquals(translatedContent.getLanguageId(), spanish.getId());
        assertEquals(translatedContent.getStringProperty("textFieldVN"), "Valor Español 1");
        assertEquals(translatedContent.getStringProperty("wVN"), "Valor Español 2");
        assertEquals(translatedContent.getStringProperty("textAreaVN"), "Valor Español 3");
    }

    @Test
    public void translateContent_multiLanguages() throws TranslationException, DotDataException, DotSecurityException {

        Contentlet toTranslate = getEnglishContent();
        toTranslate = Mockito.spy(toTranslate);
        Structure st = new Structure();
        Mockito.doReturn(st).when(toTranslate).getStructure();
        // mock language api to return language id
        LanguageAPI languageAPI = Mockito.mock(LanguageAPI.class);
        when(languageAPI.getLanguage(english.getId())).thenReturn(english);

        UserAPI userAPI = Mockito.mock(UserAPI.class);
        when(userAPI.getSystemUser()).thenReturn(testUser);

        ContentletAPI contentAPI = Mockito.mock(ContentletAPI.class);

        when(contentAPI.checkout(toTranslate.getInode(), testUser, false))
                .thenAnswer(invocation -> new Contentlet());

        // mock api provider to return mocked language api
        ApiProvider apiProvider = Mockito.mock(ApiProvider.class);
        when(apiProvider.languageAPI()).thenReturn(languageAPI);
        when(apiProvider.userAPI()).thenReturn(userAPI);
        when(apiProvider.contentletAPI()).thenReturn(contentAPI);

        List<String> valuesToTranslate = Arrays.asList("English Value 1", "English Value 2", "English Value 3");
        List<String> translatedSpanishValues = Arrays.asList("Valor Español 1", "Valor Español 2", "Valor Español 3");
        List<String> translatedFrenchValues = Arrays.asList("French 1", "French 2", "French 3");

        TranslationService service = Mockito.spy(new GoogleTranslationService("key", null, apiProvider));

        Mockito.doReturn(translatedSpanishValues).when(service).translateStrings(valuesToTranslate, english, spanish);
        Mockito.doReturn(translatedFrenchValues).when(service).translateStrings(valuesToTranslate, english, french);

        List<Contentlet> translatedContents = service
            .translateContent(toTranslate,
                Arrays.asList(spanish, french), (List<Field>) toTranslate.get("fieldsToTranslate"), testUser);

        Contentlet spanishVersion = translatedContents.get(0);
        Contentlet frenchVersion = translatedContents.get(1);

        assertEquals(spanishVersion.getLanguageId(), spanish.getId());
        assertEquals(spanishVersion.getStringProperty(TEXT_FIELD_VN), "Valor Español 1");
        assertEquals(spanishVersion.getStringProperty(WYSIWYG_VN), "Valor Español 2");
        assertEquals(spanishVersion.getStringProperty(TEXT_AREA_VN), "Valor Español 3");

        assertEquals(frenchVersion.getLanguageId(), french.getId());
        assertEquals(frenchVersion.getStringProperty(TEXT_FIELD_VN), "French 1");
        assertEquals(frenchVersion.getStringProperty(WYSIWYG_VN), "French 2");
        assertEquals(frenchVersion.getStringProperty(TEXT_AREA_VN), "French 3");
    }

    @Test(expected = NullPointerException.class)
    public void translateContent_nullFields() throws TranslationException, DotDataException, DotSecurityException {
        ApiProvider apiProvider = Mockito.mock(ApiProvider.class);
        TranslationService service = Mockito.spy(new GoogleTranslationService("key", null, apiProvider));
        service.translateContent(new Contentlet(), spanish, null, testUser);
    }

    @Test(expected = IllegalArgumentException.class)
    public void translateContent_emtpyFields() throws TranslationException, DotDataException, DotSecurityException {
        ApiProvider apiProvider = Mockito.mock(ApiProvider.class);
        TranslationService service = Mockito.spy(new GoogleTranslationService("", null, apiProvider));
        service.translateContent(new Contentlet(), spanish, new ArrayList<>(), testUser);
    }

    @Test
    public void testTranslateStrings() throws Exception {
        String jsonStr = read(getClass().getResourceAsStream("/spanishMultiTranslation.json"));
        JSONObject jsonObject = new JSONObject(jsonStr);

        JSONTool jsonTool = Mockito.mock(JSONTool.class);
        when(jsonTool
                .post(Mockito.anyString(), Mockito.anyInt(), Mockito.anyMap(), Mockito.anyString()))
                .thenReturn(jsonObject);

        TranslationService service = new GoogleTranslationService("key", jsonTool, new ApiProvider());
        List<String> toTranslate = Arrays.asList("This is a test in English", "Latest News",
            "Why is your cable TV bill is going up?");

        List<String> translated = service.translateStrings(toTranslate, english, spanish);

        assertEquals(translated.get(0), "Esta es una prueba en Inglés");
        assertEquals(translated.get(1), "Últimas noticias");
        assertEquals(translated.get(2), "¿Por qué es su factura de televisión por cable está subiendo?");
    }

    @Test
    public void testTranslateStrings_NullItems() throws Exception {
        String jsonStr = read(getClass().getResourceAsStream("/spanishMultiTranslation.json"));
        JSONObject jsonObject = new JSONObject(jsonStr);

        JSONTool jsonTool = Mockito.mock(JSONTool.class);
        when(jsonTool
                .post(Mockito.anyString(), Mockito.anyInt(), Mockito.anyMap(), Mockito.anyString()))
                .thenReturn(jsonObject);

        TranslationService service = new GoogleTranslationService("key", jsonTool, new ApiProvider());
        List<String> toTranslate = Arrays.asList("This is a test in English", null,
            null);

        List<String> translated = service.translateStrings(toTranslate, english, spanish);

        assertEquals(translated.get(0), "Esta es una prueba en Inglés");
    }

    @Test
    public void testTranslateString() throws Exception {
        String jsonStr = read(getClass().getResourceAsStream("/spanishSingleTranslation.json"));
        JSONObject jsonObject = new JSONObject(jsonStr);

        JSONTool jsonTool = Mockito.mock(JSONTool.class);
        when(jsonTool
                .post(Mockito.anyString(), Mockito.anyInt(), Mockito.anyMap(), Mockito.anyString()))
                .thenReturn(jsonObject);

        GoogleTranslationService service = new GoogleTranslationService("key", jsonTool, new ApiProvider());
        String translated = service.translateString("This is a test in English", english, spanish);

        assertEquals(translated, "Esta es una prueba en Inglés");
    }

    @Test(expected = TranslationException.class)
    public void testTranslateString_BadJSON() throws Exception {
        JSONObject jsonObject = new JSONObject("{invalid:invalid}");

        JSONTool jsonTool = Mockito.mock(JSONTool.class);
        when(jsonTool.fetch(Mockito.anyString())).thenReturn(jsonObject);

        GoogleTranslationService service = new GoogleTranslationService("key", null, new ApiProvider());
        service.translateString("whatever", english, spanish);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTranslateString_SameLanguage() throws Exception {
        GoogleTranslationService service = new GoogleTranslationService("", null, null);
        service.translateString("whatever", english, english);
    }

    private static String read(InputStream input) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
            return buffer.lines().collect(Collectors.joining("\n"));
        }
    }
}