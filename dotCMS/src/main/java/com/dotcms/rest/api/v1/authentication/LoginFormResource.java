package com.dotcms.rest.api.v1.authentication;

import com.dotcms.company.CompanyAPI;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.InitRequestRequired;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;

import com.dotcms.rest.api.LanguageView;
import com.dotcms.rest.api.v1.I18NForm;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.I18NUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.util.ReleaseInfo;
import com.liferay.util.LocaleUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.ws.rs.Consumes;


/**
 * Encapsulates the necessary info to show the login page.
 * @author jsanca
 */
@Path("/v1/loginform")
@SwaggerCompliant(value = "Core authentication and user management APIs", batch = 1)
@Tag(name = "Authentication")
public class LoginFormResource implements Serializable {

    private final LanguageAPI languageAPI;
    private final CompanyAPI  companyAPI;
    private final WebResource webResource;
    private final ConversionUtils conversionUtils;
    private final I18NUtil i18NUtil;

    @SuppressWarnings("unused")
    public LoginFormResource() {
        this(I18NUtil.INSTANCE,
                APILocator.getLanguageAPI(),
                ConversionUtils.INSTANCE,
                APILocator.getCompanyAPI(),
                new WebResource(new ApiProvider()));
    }

    @VisibleForTesting
    protected LoginFormResource(final I18NUtil i18NUtil, final LanguageAPI languageAPI,
                                     final ConversionUtils conversionUtils,
                                     final CompanyAPI  companyAPI,
                                     final WebResource webResource) {

        this.i18NUtil        = i18NUtil;
        this.conversionUtils = conversionUtils;
        this.languageAPI     = languageAPI;
        this.companyAPI      = companyAPI;
        this.webResource     = webResource;
    }

    @Operation(
        summary = "Get login form configuration",
        description = "Retrieves login form configuration including company details, available languages, and localized messages"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Login form configuration retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityLoginFormView.class))),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - security exception",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    // todo: add the https annotation
    @POST
    @JSONP
    @NoCache
    @InitRequestRequired
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public final Response loginForm(@Context final HttpServletRequest request,
                                         @RequestBody(description = "Internationalization form containing language and country preferences", 
                                                    required = true,
                                                    content = @Content(schema = @Schema(implementation = I18NForm.class)))
                                         final I18NForm i18nForm) {

        Response res = null;

        try {

            final Company defaultCompany =
                    this.companyAPI.getDefaultCompany();

            final LoginFormResultView.Builder builder =
                    new LoginFormResultView.Builder();

            final HttpSession session =
                    request.getSession();

            //Trying to find out and process the locale to use
            LocaleUtil.processCustomLocale(request, session);

            final Map<String, String> messagesMap =
                    this.i18NUtil.getMessagesMap(
                            // if the user set's a switch, it overrides the session too.
                            i18nForm.getCountry(), i18nForm.getLanguage(),
                            i18nForm.getMessagesKey(), request,
                            true); // want to create a session to store the locale.

            final Locale userLocale = LocaleUtil.getLocale(request,
                    i18nForm.getCountry(), i18nForm.getLanguage());

            builder.serverId(LicenseUtil.getDisplayServerId())
                .levelName(LicenseUtil.getLevelName())
                .version(ReleaseInfo.getVersion())
                .buildDateString(ReleaseInfo.getBuildDateString())
                .languages(this.conversionUtils.convert(LanguageUtil.getAvailableLocales(),
                        (final Locale locale) -> {

                            return new LanguageView(locale.getLanguage(), locale.getCountry(),
                                    locale.getDisplayName(locale));
                        }))
                .backgroundColor(defaultCompany.getSize())
                .backgroundPicture(defaultCompany.getHomeURL())
                .logo(this.companyAPI.getLogoPath(defaultCompany))
                .authorizationType(defaultCompany.getAuthType())
                .currentLanguage(new LanguageView(userLocale.getLanguage(), userLocale.getCountry(),
                            userLocale.getDisplayName(userLocale)))
                .companyEmail("@" + defaultCompany.getMx());

            res = Response.ok(new ResponseEntityView<>(builder.build(), messagesMap)).build(); // 200

        } catch (DotSecurityException e) {
            throw new ForbiddenException(e);

        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            res = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }

        return res;
    } // authentication
} // E:O:F:LoginFormResource.
