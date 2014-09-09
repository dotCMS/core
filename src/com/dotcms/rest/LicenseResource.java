package com.dotcms.rest;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.repackage.com.sun.jersey.core.header.FormDataContentDisposition;
import com.dotcms.repackage.com.sun.jersey.multipart.FormDataParam;
import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.json.JSONArray;
import com.dotcms.repackage.org.json.JSONObject;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Map;


@Path("/license")
public class LicenseResource extends WebResource {
    
    @GET
    @Path("/all/{params:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll(@Context HttpServletRequest request, @PathParam("params") String params) {
        init(params, true, request, true, "EXT_LICENSE_MANAGER");
        try {
            JSONArray array=new JSONArray();

            for ( Map<String, Object> lic : LicenseUtil.getLicenseRepoList() ) {
                JSONObject obj = new JSONObject();
                for ( Map.Entry<String, Object> entry : lic.entrySet() ) {

                    //Lets exclude some data we don' want/need to expose
                    if ( entry.getKey().equals( "serverid" ) ) {
                        obj.put( entry.getKey(), entry.getValue() != null ? LicenseUtil.getDisplayServerId( (String) lic.get( "serverId" ) ) : "" );
                    } else if ( entry.getKey().equals( "serverId" ) || entry.getKey().equals( "license" ) ) {
                        //Just ignore these fields
                    } else {
                        obj.put( entry.getKey(), entry.getKey() != null ? entry.getValue() : "" );
                    }

                }
                array.put( obj );
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
    
    @POST
    @Path("/pick/{params:.*}")
    public Response pickLicense(@Context HttpServletRequest request, @PathParam("params") String params) {
        InitDataObject initData = init(params, true, request, true, "EXT_LICENSE_MANAGER");
        String serial = initData.getParamsMap().get("serial");
        
        final long currentLevel=LicenseUtil.getLevel();
        final String currentSerial=currentLevel>100 ? LicenseUtil.getSerial() : "";
        
        if(currentLevel<200 || !currentSerial.equals(serial)) {
            
            try {
                HibernateUtil.startTransaction();
                
                LicenseUtil.pickLicense(serial);
                
                LicenseUtil.updateLicenseHeartbeat();
                
                HibernateUtil.commitTransaction();
                
                AdminLogger.log(LicenseResource.class, "pickLicense", "Picked license from repo. Serial: "+serial, initData.getUser());
            }
            catch(Exception ex) {
                Logger.error(this, "can't pick license "+serial,ex);
                try {
                    HibernateUtil.rollbackTransaction();
                } catch (DotHibernateException e) {
                    Logger.warn(this, "can't rollback", e);
                }
                return Response.serverError().build();
            }
        }
        
        if(currentLevel==100 || currentSerial.equals(LicenseUtil.getSerial())) {
            return Response.notModified().build();
        }
        else {
            return Response.ok().build();
        }
    }
    
    @POST
    @Path("/free/{params:.*}")
    public Response freeLicense(@Context HttpServletRequest request, @PathParam("params") String params) {
        InitDataObject initData = init(params, true, request, true, "EXT_LICENSE_MANAGER");
        try {
            HibernateUtil.startTransaction();
            
            LicenseUtil.freeLicenseOnRepo();
            
            HibernateUtil.commitTransaction();
            
            AdminLogger.log(LicenseResource.class, "freeLicense", "freed license from repo", initData.getUser());
        }
        catch(Exception ex) {
            Logger.error(this, "can't free license ",ex);
            try {
                HibernateUtil.rollbackTransaction();
            } catch (DotHibernateException e) {
                Logger.warn(this, "can't rollback", e);
            }
            return Response.serverError().build();
        }
        
        return Response.ok().build();
    }
}
