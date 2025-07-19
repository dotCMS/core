package com.dotcms.contenttype.model.field;


import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import static com.dotcms.util.CollectionsUtils.toImmutableList;

/**
 * This end-point provides access to information associated to dotCMS FieldType.
 */
@SwaggerCompliant(value = "Content management and workflow APIs", batch = 2)
@Path("/v1/fieldTypes")
@Tag(name = "Content Type Field")
public class FieldTypeResource {

    private final WebResource webResource;
    private FieldTypeAPI fieldTypeAPI;

    public FieldTypeResource() {
        this(new WebResource(), FieldTypeAPI.getInstance());
    }

    @VisibleForTesting
    public FieldTypeResource(final WebResource webresource, FieldTypeAPI fieldTypeAPI) {
        this.webResource = webresource;
        this.fieldTypeAPI = fieldTypeAPI;
    }

    @Operation(
        summary = "Get field types",
        description = "Retrieves all available field types in dotCMS for content type configuration"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Field types retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityFieldTypeListView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @NoCache
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getFieldTypes(@Context final HttpServletRequest req) {

        final InitDataObject initData = this.webResource.init(null, true, req, true, null);
        final User user = initData.getUser();

        final ImmutableList<Map<String, Object>> fieldTypesMap = fieldTypeAPI.getFieldTypes(user).stream()
                .map(FieldType::toMap)
                .collect(toImmutableList());

        return Response.ok( new ResponseEntityFieldTypeListView( fieldTypesMap ) ).build();
    }
}
