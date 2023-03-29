package com.dotcms.api;

import com.dotcms.api.provider.DefaultResponseExceptionMapper;
import com.dotcms.api.provider.DotCMSClientHeaders;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.language.Language;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;

@Path("/v2/languages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tags(
        value = { @Tag(name = "Language")}
)
@RegisterClientHeaders(DotCMSClientHeaders.class)
@RegisterProvider(DefaultResponseExceptionMapper.class)
/**
 * Interface that defines all the operations that can be performed over Languages
 * @author nollymar
 */
public interface LanguageAPI {

    @GET
    @Path("/id/{languageid}")
    @Operation(
            summary = " Returns the Language that matches the specified id"
    )
    ResponseEntityView<Language> findById(@PathParam("languageid") String languageId);


    @GET
    @Path("/{languageTag}")
    @Operation(
            summary = " Returns the Language that matches the specified tag"
    )
    ResponseEntityView<Language> getFromLanguageTag(@PathParam("languageTag") String languageTag);

    @GET
    @Operation(
            summary = " Returns all the languages in the system"
    )
    ResponseEntityView<List<Language>> list();
}
