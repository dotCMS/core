package com.dotcms.api;

import com.dotcms.api.provider.DefaultResponseExceptionMapper;
import com.dotcms.api.provider.DotCMSClientHeaders;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.folder.SearchByPathRequest;
import java.util.List;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;

@Path("/v1/folder")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tags(
        value = { @Tag(name = "Site Folders")}
)
@RegisterClientHeaders(DotCMSClientHeaders.class)
@RegisterProvider(DefaultResponseExceptionMapper.class)
public interface FolderAPI {

    @POST
    @Path("/createfolders/{siteName}")
    ResponseEntityView<List<Map<String, Object>>> makeFolders(final List<String> paths, @PathParam("siteName") final String siteName);


    @POST
    @Path("/byPath")
    ResponseEntityView<List<Map<String, Object>>> findByPath(SearchByPathRequest request);

}
