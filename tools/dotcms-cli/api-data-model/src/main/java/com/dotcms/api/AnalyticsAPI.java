package com.dotcms.api;

import com.dotcms.api.provider.DefaultResponseExceptionMapper;
import com.dotcms.api.provider.DotCMSClientHeaders;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.analytics.AnalyticsEvent;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;

/**
 * Entry point to dotCMS Analytics Rest API
 */
@Path("/v1/analytics/content")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tags(
        value = {@Tag(name = "Analytics")}
)
@RegisterClientHeaders(DotCMSClientHeaders.class)
@RegisterProvider(DefaultResponseExceptionMapper.class)
public interface AnalyticsAPI {

    @POST
    @Path("/event")
    @Operation(
            summary = "Fire a custom user event"
    )
    ResponseEntityView<String> fireEvent(final AnalyticsEvent eventPayload);
}
