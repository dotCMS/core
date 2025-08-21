package com.dotcms.rest.api.v1.personas;

import static com.dotcms.util.DotPreconditions.checkNotEmpty;
import static com.dotcms.util.DotPreconditions.checkNotNull;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@SwaggerCompliant(value = "Core authentication and user management APIs", batch = 1)
@Tag(name = "Personas")
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

    @Operation(
        summary = "List personas",
        description = "Returns all personas for the current site. Site can be determined from session or header."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Personas retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = MapStringRestPersonaView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - site ID required or invalid",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
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

    @Operation(
        summary = "Get persona by ID",
        description = "Returns a specific persona by its identifier"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Persona retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = RestPersona.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - persona ID or site ID required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Persona not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @Path("{id}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public RestPersona self(@Context HttpServletRequest request,
                            @Context final HttpServletResponse response,
                            @Parameter(description = "Site identifier", required = true) @PathParam("siteId") String siteId, 
                            @Parameter(description = "Persona identifier", required = true) @PathParam("id") String personaId) {
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
