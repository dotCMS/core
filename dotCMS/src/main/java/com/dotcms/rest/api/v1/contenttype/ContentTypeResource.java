package com.dotcms.rest.api.v1.contenttype;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.JsonContentTypeTransformer;
import com.dotcms.contenttype.transform.field.JsonFieldTransformer;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.gson.JsonObject;
import com.dotcms.repackage.edu.emory.mathcs.backport.java.util.Arrays;
import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.AccessControlAllowOrigin;
import com.dotcms.rest.annotation.InitRequestRequired;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.I18NUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.model.User;
import com.liferay.util.LocaleUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.map;

/**
 * Created by jasontesser on 9/28/16.
 */
@Path("/v1/contenttype")
public class ContentTypeResource implements Serializable {
    private final WebResource webResource;

    /**
     * 
     */
    private static final long serialVersionUID = 1L;


    @VisibleForTesting
    public ContentTypeResource(){
        this.webResource =new WebResource();
    };

    @POST
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response saveType(@Context final HttpServletRequest req,
                                        final String json) throws DotDataException, DotSecurityException {
        Response response = null;
        final InitDataObject initData = this.webResource.init(null, true, req, true, null);
        final User user = initData.getUser();

        
        
        
        ContentType type = new JsonContentTypeTransformer(json).from();
        APILocator.getContentTypeAPI2().save(type, user);
        
        List<Field> fields = new JsonFieldTransformer(json).asList();
        if(fields.size()>0){
            
        }

        for(Field field : fields){
            APILocator.getFieldAPI2().save(field, user);    
        }

        return Response.ok(new ResponseEntityView(new JsonContentTypeTransformer(type).json())).build();

    }
    
    @GET
    @Path ("/{id}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getType(@PathParam("id") final String id,
                             @Context final HttpServletRequest req) throws  DotDataException, DotSecurityException  
    {

        final InitDataObject initData = this.webResource.init(null, true, req, true, null);
        final User user = initData.getUser();


        Response response = Response.ok(new JsonContentTypeTransformer(APILocator.getContentTypeAPI2().find(id, user)).json()).build();
        return response;
    }
    


}
