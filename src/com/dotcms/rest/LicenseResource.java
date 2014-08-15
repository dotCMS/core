package com.dotcms.rest;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.repackage.com.sun.jersey.core.header.FormDataContentDisposition;
import com.dotcms.repackage.com.sun.jersey.multipart.FormDataParam;
import com.dotcms.repackage.javax.ws.rs.Consumes;
import com.dotcms.repackage.javax.ws.rs.DELETE;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.POST;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.json.JSONArray;
import com.dotcms.repackage.org.json.JSONObject;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;


@Path("/license")
public class LicenseResource extends WebResource {
    
    @GET
    @Path("/all/{params:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll(@Context HttpServletRequest request, @PathParam("params") String params) {
        init(params, true, request, true, "EXT_LICENSE_MANAGER");
        try {
            JSONArray array=new JSONArray();
            
            for(Map<String,Object> lic : LicenseUtil.getLicenseRepoList()) {
                array.put(new JSONObject()
                          .put("id", lic.get("id"))
                          .put("serverid", lic.get("serverid")!=null ? lic.get("serverid") : "")
                          .put("lastping", lic.get("lastping")!=null ? lic.get("lastping") : "")
                          .put("license", lic.get("license")));
            }
            
            return Response.ok(array.toString(), MediaType.APPLICATION_JSON_TYPE).build();
        }
        catch(Exception ex) {
            Logger.error(this, "can't get all license on repo", ex);
            return Response.serverError().build();
        }
        
    }
    
    @POST
    @Path("/upload/{params:.*}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response putZipFile(@Context HttpServletRequest request, @PathParam("params") String params,
            @FormDataParam("file") InputStream inputFile, @FormDataParam("file") FormDataContentDisposition inputFileDetail, 
            @FormDataParam("return") String ret) {
        InitDataObject initData = init(params, true, request, true, "EXT_LICENSE_MANAGER");
        try {
           
            if(inputFile!=null) {
                LicenseUtil.uploadLicenseRepoFile(inputFile);
                
                AdminLogger.log(this.getClass(), "putZipFile", "uploaded zip to license repo", initData.getUser());
                
                return buildReturn(request, ret);
            }
            
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("where is the zip file?")
                           .type(MediaType.TEXT_PLAIN).build();
        }
        catch(Exception ex) {
            Logger.error(this, "can't upload license to repo", ex);
            return Response.serverError().build();
        }
        
    }
    
    protected Response buildReturn(HttpServletRequest request, String ret) throws URISyntaxException {
         
        if(!UtilMethods.isSet(ret)) {
            return Response.ok().build();
        }
        
        return Response.status(302).header("Location", ret).build();
        
    }
    
    @DELETE
    @Path("/delete/{params:.*}")
    public Response delete(@Context HttpServletRequest request, @PathParam("params") String params) {
        InitDataObject initData = init(params, true, request, true, "EXT_LICENSE_MANAGER");
        String id=initData.getParamsMap().get("id");
        try {
            if(UtilMethods.isSet(id)) {
                LicenseUtil.deleteLicense(id);
            }
            else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("no id provided")
                        .type(MediaType.TEXT_PLAIN).build();
            }
            
            AdminLogger.log(this.getClass(), "delete", "Deleted license from repo with id "+id, initData.getUser());
            
            return Response.ok().build();
        }
        catch(Exception ex) {
            Logger.error(this, "can't delete license "+id, ex);
            return Response.serverError().build();
        }
    }
}
