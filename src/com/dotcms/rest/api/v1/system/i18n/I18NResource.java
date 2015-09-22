package com.dotcms.rest.api.v1.system.i18n;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.config.AuthenticationProvider;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotcms.rest.validation.Preconditions;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import java.util.Locale;
import java.util.Optional;

/**
 * @author Geoff M. Granum
 */
@Path("/v1/system/i18n")
public class I18NResource {

  private static final long serialVersionUID = 1L;
  private final AuthenticationProvider authProxy;

  public I18NResource() {
    this(new ApiProvider());
  }

  private I18NResource(ApiProvider apiProvider) {
    this(apiProvider, new AuthenticationProvider(apiProvider));
  }

  @VisibleForTesting
  protected I18NResource(ApiProvider apiProvider, AuthenticationProvider authProxy) {
    this.authProxy = authProxy;
  }

  /**
   * <p>Returns a JSON with all the RuleActionlet Objects defined.
   * <p>
   * Usage: /ruleactionlets/
   */
  @GET
  @Path("/{lang:[\\w]{2}(?:-?[\\w]{2})?}/{rsrc:.*}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response list(@PathParam("lang") String lang, @PathParam("rsrc") String rsrc) {
    RestResourceLookup lookup = new RestResourceLookup(lang, rsrc == null ? "" : rsrc);
    Optional<String> result;

    try {
      result = LanguageUtil.getOpt(PublicCompanyFactory.getDefaultCompanyId(), lookup.locale, lookup.key);
    } catch (LanguageException e) {
      throw new BadRequestException(e, "Could not process requested language '%s'", lookup.lang);
    }
    Response response;
    if(result.isPresent()) {
      response = Response.ok("{ \"" + lookup.leafName + "\": \"" + result + "\"}").build();
    } else {
      throw new NotFoundException("Message not found for resource %s", lookup.ref);
    }
    return response;
  }


  private static class RestResourceLookup {

    private final String lang;
    private final Locale locale;
    private final String ref;
    private final String key;
    private final String leafName;

    public RestResourceLookup(String lang, String childRef) {
      Preconditions.checkNotEmpty(lang, BadRequestException.class, "Language is required.");
      Preconditions.checkNotNull(childRef, BadRequestException.class, "Resource path is required.");

      this.lang = lang;
      this.ref = lang + '/' + childRef;
      this.key = childRef.replace('/', '.');
      int idx = key.lastIndexOf('.') + 1;
      this.leafName = (idx != 0 && idx < key.length())  ? key.substring(idx) : key;
      locale = Locale.forLanguageTag(lang);

      if( locale == null) {
        throw new BadRequestException("Could not process requested language '%s'", lang);
      }
    }



  }
}
 
