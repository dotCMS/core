package com.dotcms.rest.api.v1.personalization;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.dotcms.util.DotPreconditions.checkNotEmpty;
import static com.dotcms.util.DotPreconditions.checkNotNull;

/**
 * Resource to provide personalization stuff on dotCMS
 */
@Path("/v1/personalization")
public class PersonalizationResource {

    private final PersonaAPI   personaAPI;
    private final WebResource  webResource;
    private final MultiTreeAPI multiTreeAPI;

    @SuppressWarnings("unused")
    public PersonalizationResource() {
        this(APILocator.getPersonaAPI(), APILocator.getMultiTreeAPI(), new WebResource(new ApiProvider()));
    }

    @VisibleForTesting
    protected PersonalizationResource(final PersonaAPI personaAPI,
                                      final MultiTreeAPI multiTreeAPI,
                                      final WebResource webResource) {
        this.personaAPI   = personaAPI;
        this.multiTreeAPI = multiTreeAPI;
        this.webResource  = webResource;
    }


    /**
     * Returns the list of personas with a flag that determine if the persona has been customized on a page or not.
     * { persona:Persona, personalized:boolean }
     * @param request
     * @param response
     * @param pageId
     * @return Response, pair with
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @GET
    @Path("/pagepersonas/{pageId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getPersonasOnPage (@Context final HttpServletRequest  request,
                                       @Context final HttpServletResponse response,
                                       @PathParam("pageId") final String  pageId) throws DotDataException, DotSecurityException {

        final User user = webResource.init(true, request, true).getUser();
        Host host = (Host)request.getSession().getAttribute(WebKeys.CURRENT_HOST);
        if(host == null){
            String siteId = request.getHeader(WebKeys.CURRENT_HOST);
            siteId = checkNotEmpty(siteId, BadRequestException.class, "Site Id is required.");
            host   = getHost(siteId);
        }

        host = checkNotNull(host, BadRequestException.class, "Current Site Host could not be determined.");

        Logger.debug(this, ()-> "Getting page personas per page: " + pageId);

        // todo: eventually do pagination here.
        final List<Persona> personas        = this.personaAPI.getPersonas(host, true, false, user, true);
        final Set<String> personaTagPerPage = this.multiTreeAPI.getPersonalizationsForPage (pageId);
        final List<PersonalizationPersonaPageView> personalizationPersonaPageViews =
                            new ArrayList<>(personas.size());

        for (final Persona persona : personas) {

            personalizationPersonaPageViews.add(new PersonalizationPersonaPageView(pageId,
                    personaTagPerPage.contains(persona.getKeyTag()), persona));
        }

        return Response.ok(new ResponseEntityView(personas)).build();
    }

    private Host getHost(final String siteId) {
        final Host proxy = new Host();
        proxy.setIdentifier(siteId);
        return proxy;
    }



}
