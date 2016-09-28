package com.dotcms.rest.api.v1.personas;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

import static com.dotcms.util.DotPreconditions.checkNotEmpty;
import static com.dotcms.util.DotPreconditions.checkNotNull;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

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
    public Map<String, RestPersona> list(@Context HttpServletRequest request) {
        Host host = (Host)request.getSession().getAttribute(WebKeys.CURRENT_HOST);
        if(host == null){
            String siteId = request.getHeader(WebKeys.CURRENT_HOST);
            siteId = checkNotEmpty(siteId, BadRequestException.class, "Site Id is required.");
            host = getHost(siteId);
        }
        host = checkNotNull(host, BadRequestException.class, "Current Site Host could not be determined.");

        PersonaTransform transform = new PersonaTransform();
        User user = getUser(request);
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
    public RestPersona self(@Context HttpServletRequest request, @PathParam("siteId") String siteId, @PathParam("id") String personaId) {
        checkNotEmpty(siteId, BadRequestException.class, "Site Id is required.");
        User user = getUser(request);
        personaId = checkNotEmpty(personaId, BadRequestException.class, "Persona Id is required.");
        return new PersonaTransform().appToRest(this.getPersonaInternal(personaId, user));
    }

    private Host getHost(String siteId) {
        Host proxy = new Host();
        proxy.setIdentifier(siteId);
        return proxy;
    }

    private User getUser(@Context HttpServletRequest request) {
        return webResource.init(true, request, true).getUser();
    }

    private Persona getPersonaInternal(String personaId, User user) {
        try {
            return personaAPI.find(personaId, user, true);
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException | InvalidLicenseException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }

    private List<Persona> getPersonasInternal(User user, Host host) {
        try {
            return personaAPI.getPersonas(host, true, false, user, true);
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException | InvalidLicenseException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }
}
