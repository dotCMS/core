package com.dotmarketing.viewtools;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.KeyValueContentType;
import com.dotcms.integrationtestutil.content.ContentUtils;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link UtilMethods}
 */
public class LanguageWebAPITest extends IntegrationTestBase {

	private static final int GENERAL_ENGLISH = 3;

	private static long englishLanguageId;
	private static long spanishLanguageId;
	private static long spanishNoCountryLanguageId;
	private static long spanishCostaRicaLanguageId;

	@BeforeClass
	public static void prepare() throws Exception {
		//Setting web app environment
		IntegrationTestInitService.getInstance().init();
		final User systemUser = APILocator.systemUser();
		final String contentTypeVelocityVarName = LanguageVariableAPI.LANGUAGEVARIABLE;
		ContentType languageVariableContentType = null;

		try {
			// Using the provided Language Variable Content Type
			languageVariableContentType = APILocator.getContentTypeAPI(systemUser).find(contentTypeVelocityVarName);
		} catch (Exception e) {
			// Content Type not found, then create it
			languageVariableContentType = createLanguageVariable(systemUser, contentTypeVelocityVarName);
		}
		Assert.assertNotNull("The Language Variable Content Type MUST EXIST in order to run this Integration Test.",
				languageVariableContentType);
		englishLanguageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
		spanishLanguageId = APILocator.getLanguageAPI().getLanguage("es", "ES").getId();

		createSpanishNoCountryLanguage ();
		createSpanishCostaRicaLanguage ();
		if (null == APILocator.getLanguageVariableAPI().get("text.example", spanishNoCountryLanguageId, systemUser, false)) {
			ContentUtils.createTestLanguageVariableContent("test.example", "Esto es una prueba general", spanishNoCountryLanguageId,
					languageVariableContentType, systemUser);
		}
		if (null == APILocator.getLanguageVariableAPI().get("text.example", spanishCostaRicaLanguageId, systemUser, false)) {
			ContentUtils.createTestLanguageVariableContent("test.example", "Esto es una prueba puravida", spanishCostaRicaLanguageId,
					languageVariableContentType, systemUser);
		}
	}

	private static ContentType createLanguageVariable (final User systemUser,
													   final String contentTypeVelocityVarName) throws Exception {

		final String contentTypeName = "Language Variable";
		final Host site = APILocator.getHostAPI().findDefaultHost(systemUser, Boolean.FALSE);
		final ContentTypeAPI contentTypeApi = APILocator.getContentTypeAPI(systemUser);
		ContentType languageVariableContentType = ContentTypeBuilder.builder(KeyValueContentType.class).host(site.getIdentifier())
				.description("Testing the Language Variable API.").name(contentTypeName)
				.variable(contentTypeVelocityVarName).fixed(Boolean.FALSE).owner(systemUser.getUserId()).build();
		return  contentTypeApi.save(languageVariableContentType);
	}

	private static void createSpanishNoCountryLanguage() {

		Language language = null;

		try {
			language =
					APILocator.getLanguageAPI().getLanguage("es", "");
		} catch (Exception e) {

			language = null;
		}

		if (null == language) {

			language = new Language();
			language.setLanguageCode("es");
			APILocator.getLanguageAPI().saveLanguage(language);
		}
		spanishNoCountryLanguageId =
				APILocator.getLanguageAPI().getLanguage("es", "").getId();
	}

	private static void createSpanishCostaRicaLanguage() {

		Language language = null;

		try {
			language =
					APILocator.getLanguageAPI().getLanguage("es", "CR");
		} catch (Exception e) {

			language = null;
		}

		if (null == language) {

			language = new Language();
			language.setLanguageCode("es");
			APILocator.getLanguageAPI().saveLanguage(language);
		}
		spanishCostaRicaLanguageId =
				APILocator.getLanguageAPI().getLanguage("es", "CR").getId();
	}

	@Test
    public void testLangNoCountryDefaultRequestLocaleGet() {

		final LanguageWebAPI languageWebAPI = new LanguageWebAPI();
		final HttpServletRequest request    = mock(HttpServletRequest.class);
		final HttpSession session = mock(HttpSession.class);
		final ViewContext    viewContext    = mock(ViewContext.class);
		when(viewContext.getRequest()).thenReturn(request);
		when(request.getSession()).thenReturn(session);
		when(request.getLocale()).thenReturn(new Locale("es", "CR"));
		when(session.getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE)).thenReturn(String.valueOf(spanishNoCountryLanguageId));

		languageWebAPI.init(viewContext);

		final String text = languageWebAPI.get("test.example");


        assertEquals("Esto es una prueba puravida", text);
    }
}
