package com.dotcms.api;

import com.dotcms.api.provider.DefaultResponseExceptionMapper;
import com.dotcms.api.provider.DotCMSClientHeaders;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.language.Language;
import com.dotcms.model.views.CommonViews;
import com.dotcms.model.views.CommonViews.LanguageFileView;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.List;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;

@Path("/v2/languages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tags(
        value = {@Tag(name = "Language")}
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
    @Path("/id/{languageid}")
    @Operation(
            summary = " Returns the Language that matches the specified id but using a different "
                    + "view, a reduced one, for pull operations and the generation of the language "
                    + "files."
    )
    @JsonView(LanguageFileView.class)
    ResponseEntityView<Language> findByIdForPull(@PathParam("languageid") String languageId);

    @GET
    @Path("/{languageTag}")
    @Operation(
            summary = " Returns the Language that matches the specified ISO code"
    )
    ResponseEntityView<Language> getFromLanguageIsoCode(
            @PathParam("languageTag") String languageIsoCode);

    @GET
    @Path("/{languageTag}")
    @Operation(
            summary = " Returns the Language that matches the specified ISO code but using a "
                    + "different view, a reduced one, for pull operations and the generation of the "
                    + "language files."
    )
    @JsonView(LanguageFileView.class)
    ResponseEntityView<Language> getFromLanguageIsoCodeForPull(
            @PathParam("languageTag") String languageIsoCode);

    @GET
    @Operation(
            summary = " Returns all the languages in the system but using a different view, a "
                    + "reduced one, for pull operations and the generation of the language files."
    )
    @JsonView(LanguageFileView.class)
    ResponseEntityView<List<Language>> listForPull();

    @GET
    @Operation(
            summary = " Returns all the languages in the system"
    )
    ResponseEntityView<List<Language>> list();

    @POST
    @Operation(
            summary = " Creates a new language in the system"
    )
    @JsonView(CommonViews.LanguageFileView.class)
    ResponseEntityView<Language> create(
            @JsonView(CommonViews.LanguageWriteView.class) Language language);

    @POST
    @Path("/{languageTag}")
    @Operation(
            summary = " Creates a new language in the system given its ISO code"
    )
    @JsonView(CommonViews.LanguageFileView.class)
    ResponseEntityView<Language> create(
            @JsonView(CommonViews.LanguageWriteView.class)
            @PathParam("languageTag") String languageIsoCode);

    @PUT
    @Path("/{languageId}")
    @Operation(
            summary = " Updates an existing language in the system"
    )
    @JsonView(CommonViews.LanguageFileView.class)
    ResponseEntityView<Language> update(@PathParam("languageId") String languageId,
            @JsonView(CommonViews.LanguageWriteView.class) Language language);

    @DELETE
    @Path("/{languageId}")
    @Operation(
            summary = " Deletes an existing language from the system"
    )
    ResponseEntityView<String> delete(@PathParam("languageId") String languageId);
}
