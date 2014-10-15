package com.dotcms.rest;

import com.dotcms.cluster.bean.Server;
import com.dotcms.cluster.business.ServerAPIImpl;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.repackage.com.sun.jersey.core.header.FormDataContentDisposition;
import com.dotcms.repackage.com.sun.jersey.multipart.FormDataParam;
import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.json.JSONArray;
import com.dotcms.repackage.org.json.JSONObject;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;


@Path("/license")
public class LicenseResource extends WebResource {
    
    @GET
    @Path("/all/{params:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll(@Context HttpServletRequest request, @PathParam("params") String params) {
        init(params, true, request, true, "9");
        try {
            JSONArray array=new JSONArray();

            for ( Map<String, Object> lic : LicenseUtil.getLicenseRepoList() ) {
                JSONObject obj = new JSONObject();
                for ( Map.Entry<String, Object> entry : lic.entrySet() ) {

                    //Lets exclude some data we don' want/need to expose
                    if ( entry.getKey().equals( "serverid" ) ) {
                        obj.put( entry.getKey(), entry.getValue() != null ? LicenseUtil.getDisplayServerId( (String) lic.get( "serverId" ) ) : "" );
                        obj.put( "fullserverid", entry.getValue() != null ? entry.getValue() : "" );
                    } else if ( entry.getKey().equals( "serverId" ) || entry.getKey().equals( "license" ) ) {
                        //Just ignore these fields
                    } else if ( entry.getKey().equals( "id" ) ) {
                        obj.put( entry.getKey(), entry.getKey() != null ? entry.getValue() : "" );
                        obj.put( "idDisplay", entry.getValue() != null ? LicenseUtil.getDisplaySerial( (String) entry.getValue() ) : "" );
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
        InitDataObject initData = init(params, true, request, true, "9");
        try {
           
            if(inputFile!=null) {
                LicenseUtil.uploadLicenseRepoFile(inputFile);
                
                AdminLogger.log(this.getClass(), "putZipFile", "uploaded zip to license repo", initData.getUser());
                
                return Response.ok().build();
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

    
    
    @DELETE
    @Path("/delete/{params:.*}")
    public Response delete(@Context HttpServletRequest request, @PathParam("params") String params) {
        InitDataObject initData = init(params, true, request, true, "9");
        String id=initData.getParamsMap().get("id");
        try {
            if(UtilMethods.isSet(id)) {
                LicenseUtil.deleteLicense(id);
                
        			//waiting 10seconds just in case the user is only changing the server license
                	// if not the try to remove it
        		//TODO
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
        InitDataObject initData = init(params, true, request, true, "9");
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
        InitDataObject initData = init(params, true, request, true, "9");
        
        String serial = initData.getParamsMap().get("serial");
        String serverId = initData.getParamsMap().get("serverid");
        
        try {
            HibernateUtil.startTransaction();
            
            //If we are removing a remote Server.
            if(UtilMethods.isSet(serial)){
            	LicenseUtil.freeLicenseOnRepo(serial);
            	/*boolean removeServer=true;
    			
            	for(Map<String, Object> lic : LicenseUtil.getLicenseRepoList()){
    				if( serverId.equals((String)lic.get("serverid"))) {
    					removeServer=false;
    					break;
    				}
    			}
    			
            	if(removeServer){
    				APILocator.getServerAPI().removeServerFromCluster(serverId);
    			}*/
            	
            //If the server we are removing license is local.
            }else{
            	LicenseUtil.freeLicenseOnRepo();
            }
            
            HibernateUtil.commitTransaction();
            AdminLogger.log(LicenseResource.class, "freeLicense", "License From Repo Freed", initData.getUser());
            
        } catch(Exception ex) {
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
    
    @POST
    @Path("/requestCode/{params:.*}")
    @Consumes (MediaType.APPLICATION_FORM_URLENCODED)
    public Response requestLicense(@Context HttpServletRequest request, 
    		@FormParam ("licenseLevel") String licenseLevel,
    		@FormParam ("licenseType") String licenseType) {
        InitDataObject initData = init("", true, request, true, "9");
        try {

	        
	        
	        
	        HttpSession session = request.getSession();
            session.setAttribute( "iwantTo", "request_code" );
            session.setAttribute( "license_type", licenseType );
            session.setAttribute( "license_level", licenseLevel );
            if(!"trial".equals(licenseType) && !   
            		"dev".equals(licenseType) && !   
            		"prod".equals(licenseType) 
            		
            		){
            	throw new DotStateException("invalid License Type");
            }

            LicenseUtil.processForm( request );
        	return Response.ok(request.getAttribute("requestCode"), MediaType.APPLICATION_JSON_TYPE).build();
        }
        catch(Exception ex) {
            Logger.error(this, "can't request license ",ex);

            return Response.serverError().build();
        }
        
    }
    
    @POST
    @Path("/applyLicense/{params:.*}")
    @Consumes (MediaType.APPLICATION_FORM_URLENCODED)
    public Response applyLicense(@Context HttpServletRequest request, @PathParam("params") String params,

    		@FormParam ("licenseText") String licenseText) {

        InitDataObject initData = init(params, true, request, true, "9");
        try {
	        HttpSession session = request.getSession();


            session.setAttribute( "applyForm", Boolean.TRUE );
            session.setAttribute( "iwantTo", "paste_license" );
            session.setAttribute( "paste_license", "paste_license" );
            session.setAttribute( "license_text", licenseText );

            String error = LicenseUtil.processForm( request );
            User u = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
            if(error !=null){
            	return Response.ok( LanguageUtil.get(u, "license-bad-id-explanation"), MediaType.APPLICATION_JSON_TYPE).build();
            }
        	return Response.ok( error, MediaType.APPLICATION_JSON_TYPE).build();
        }
        catch(Exception ex) {
            Logger.error(this, "can't request license ",ex);

            return Response.serverError().build();
        }
        
    }
    
    @POST
    @Path("/resetLicense/{params:.*}")
    @Consumes (MediaType.APPLICATION_FORM_URLENCODED)
    public Response resetLicense(@Context HttpServletRequest request, @PathParam("params") String params) {

        InitDataObject initData = init(params, true, request, true, "9");
        try {
        	freeLicense(request, params);
        	
        	
        	
	        HttpSession session = request.getSession();

            session.setAttribute( "applyForm", Boolean.TRUE );
            session.setAttribute( "iwantTo", "paste_license" );
            session.setAttribute( "paste_license", "paste_license" );
            session.setAttribute( "license_text", "blah" );


            String error = LicenseUtil.processForm( request );
        	return Response.ok("", MediaType.APPLICATION_JSON_TYPE).build();
        }
        catch(Exception ex) {
            Logger.error(this, "can't request license ",ex);

            return Response.serverError().build();
        }
        
    }
    
    
    
    
    
    
}
