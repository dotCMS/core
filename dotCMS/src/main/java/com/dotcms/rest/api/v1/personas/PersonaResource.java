package com.dotcms.rest.api.v1.personas;

import static com.dotcms.util.DotPreconditions.checkNotEmpty;
import static com.dotcms.util.DotPreconditions.checkNotNull;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.server.JSONP;

@Path("/v1/personas")
public class PersonaResource {

    private final PersonaAPI personaAPI;
    private final WebResource webResource;

    @SuppressWarnings("unused")
    public PersonaResource() {
        this(APILocator.getPersonaAPI(), new WebResource(new ApiProvider()));
    }

    @VisibleForTesting
    protected PersonaResource(PersonaAPI personaAPI, WebResource webResource) {
        this.personaAPI = personaAPI;
        this.webResource = webResource;
    }

    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Map<String, RestPersona> list(@Context HttpServletRequest request, @Context final HttpServletResponse response) {
        Host host = (Host)request.getSession().getAttribute(WebKeys.CURRENT_HOST);
        if(host == null){
            String siteId = request.getHeader(WebKeys.CURRENT_HOST);
            siteId = checkNotEmpty(siteId, BadRequestException.class, "Site Id is required.");
            host = getHost(siteId);
        }
        host = checkNotNull(host, BadRequestException.class, "Current Site Host could not be determined.");

        PersonaTransform transform = new PersonaTransform();
        User user = getUser(request, response);
        List<Persona> personas = getPersonasInternal(user, host);
        Map<String, RestPersona> hash = Maps.newHashMapWithExpectedSize(personas.size());
        for (Persona persona : personas) {
            hash.put(persona.getIdentifier(), transform.appToRest(persona));
        }
        return hash;
    }

    @GET
    @JSONP
    @Path("{id}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public RestPersona self(@Context HttpServletRequest request,
                            @Context final HttpServletResponse response,
                            @PathParam("siteId") String siteId, @PathParam("id") String personaId) {
        checkNotEmpty(siteId, BadRequestException.class, "Site Id is required.");
        User user = getUser(request, response);
        personaId = checkNotEmpty(personaId, BadRequestException.class, "Persona Id is required.");
        return new PersonaTransform().appToRest(this.getPersonaInternal(personaId, user));
    }

    private Host getHost(String siteId) {
        Host proxy = new Host();
        proxy.setIdentifier(siteId);
        return proxy;
    }

    private User getUser(HttpServletRequest request, final HttpServletResponse response) {
        return webResource.init(request, response, true).getUser();
    }

    private Persona getPersonaInternal(String personaId, User user) {
        try {
            return personaAPI.find(personaId, user, true);
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e);
        }
    }

    private List<Persona> getPersonasInternal(User user, Host host) {
        try {
            return personaAPI.getPersonas(host, true, false, user, true);
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e);
        }
    }
}
