package com.dotcms.rest;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.cluster.action.ResetLicenseServerAction;
import com.dotcms.enterprise.cluster.action.ServerAction;
import com.dotcms.enterprise.cluster.action.model.ServerActionBean;
import com.dotcms.enterprise.license.DotLicenseRepoEntry;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;


@Path("/license")
@Tag(name = "License")
public class LicenseResource {

    private final WebResource webResource = new WebResource();
    private static final String SERVER_ID = "serverid";

    @NoCache
    @GET
    @Path("/all/{params:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll(@Context HttpServletRequest request,
            @Context final HttpServletResponse response, @PathParam("params") String params) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .params(params)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requiredPortlet(PortletID.CONFIGURATION.toString())
                .init();

        try {
            JSONArray array = new JSONArray();

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.YEAR, -1);

            for (DotLicenseRepoEntry lic : LicenseUtil.getLicenseRepoList()) {
                JSONObject obj = new JSONObject();
                obj.put("serverId", lic.serverId);
                obj.put("perpetual", lic.dotLicense.perpetual);
                obj.put("validUntil", lic.dotLicense.validUntil);
                obj.put("expired", lic.dotLicense.expired);

                obj.put("serial", lic.dotLicense.serial);
                obj.put("level", LicenseManager.getInstance().getLevelName(lic.dotLicense.level));
                obj.put("id", lic.dotLicense.serial);
                obj.put("licenseType", lic.dotLicense.licenseType);
                obj.put("startupTime", (lic.startupTime == 0) ? "n/a" : new Date(lic.startupTime));
                if (lic.lastPing.after(cal.getTime())) {
                    obj.put("lastPingStr", DateUtil.prettyDateSince(lic.lastPing));
                } else {
                    obj.put("lastPingStr", "");
                }

                Calendar outThere = Calendar.getInstance();
                outThere.add(Calendar.YEAR, 25);
                if (lic.dotLicense.validUntil.after(outThere.getTime())) {
                    obj.put("validUntilStr", "-");
                    obj.put("perpetual", true);
                } else {
                    obj.put("validUntilStr",
                            new SimpleDateFormat("MMMM d, yyyy").format(lic.dotLicense.validUntil));
                    obj.put("perpetual", lic.dotLicense.perpetual);

                }

                array.put(obj);
            }

            return Response.ok(array.toString(), MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception ex) {
            Logger.error(this, "can't get all license on repo", ex);
            return Response.serverError().build();
        }

    }

    @NoCache
    @POST
    @Path("/upload/{params:.*}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response putZipFile(@Context HttpServletRequest request,
            @Context final HttpServletResponse response, @PathParam("params") String params,
            @FormDataParam("file") InputStream inputFile,
            @FormDataParam("file") FormDataContentDisposition inputFileDetail,
            @FormDataParam("return") String ret) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .params(params)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requiredPortlet(PortletID.CONFIGURATION.toString())
                .init();

        try {

            if (inputFile != null) {
                LicenseUtil.uploadLicenseRepoFile(inputFile);

                AdminLogger.log(this.getClass(), "putZipFile", "uploaded zip to license repo",
                        initData.getUser());

                return Response.ok().build();
            }

            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("where is the zip file?")
                    .type(MediaType.TEXT_PLAIN).build();
        } catch (Exception ex) {
            Logger.error(this, "can't upload license to repo", ex);
            return Response.serverError().build();
        }

    }


    @NoCache
    @DELETE
    @Path("/delete/{params:.*}")
    public Response delete(@Context HttpServletRequest request,
            @Context final HttpServletResponse response, @PathParam("params") String params) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .params(params)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requiredPortlet(PortletID.CONFIGURATION.toString())
                .init();

        String id = initData.getParamsMap().get("id");
        try {
            if (UtilMethods.isSet(id)) {
                LicenseUtil.deleteLicense(id);

                //waiting 10seconds just in case the user is only changing the server license
                // if not the try to remove it
                //TODO
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("no id provided")
                        .type(MediaType.TEXT_PLAIN).build();
            }

            AdminLogger.log(this.getClass(), "delete", "Deleted license from repo with id " + id,
                    initData.getUser());

            return Response.ok().build();
        } catch (Exception ex) {
            Logger.error(this, "can't delete license " + id, ex);
            return Response.serverError().build();
        }
    }

    @NoCache
    @POST
    @Path("/pick/{params:.*}")
    public Response pickLicense(@Context HttpServletRequest request,
            @Context final HttpServletResponse response, @PathParam("params") String params) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .params(params)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requiredPortlet(PortletID.CONFIGURATION.toString())
                .init();

        String serial = initData.getParamsMap().get("serial");

        final long currentLevel = LicenseUtil.getLevel();
        final String currentSerial =
                currentLevel > LicenseLevel.COMMUNITY.level ? LicenseUtil.getSerial() : "";

        if (currentLevel < LicenseLevel.STANDARD.level || !currentSerial.equals(serial)) {

            try {
                HibernateUtil.startTransaction();

                LicenseUtil.pickLicense(serial);

                HibernateUtil.closeAndCommitTransaction();

                AdminLogger.log(LicenseResource.class, "pickLicense",
                        "Picked license from repo. Serial: " + serial, initData.getUser());
            } catch (Exception ex) {
                Logger.error(this, "can't pick license " + serial, ex);
                try {
                    HibernateUtil.rollbackTransaction();
                } catch (DotHibernateException e) {
                    Logger.warn(this, "can't rollback", e);
                }
                return Response.serverError().build();
            } finally {
                HibernateUtil.closeSessionSilently();
            }
        }

        if (currentLevel == LicenseLevel.COMMUNITY.level || currentSerial.equals(
                LicenseUtil.getSerial())) {
            return Response.notModified().build();
        } else {
            return Response.ok().build();
        }
    }

    @NoCache
    @POST
    @Path("/free/{params:.*}")
    public Response freeLicense(@Context HttpServletRequest request,
            @Context final HttpServletResponse response, @PathParam("params") String params) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .params(params)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requiredPortlet(PortletID.CONFIGURATION.toString())
                .init();

        String localServerId = APILocator.getServerAPI().readServerId();
        String remoteServerId = initData.getParamsMap().get(SERVER_ID);
        String serial = initData.getParamsMap().get("serial");

        try {
            //If we are removing a remote Server we need to create a ServerAction.
            if (UtilMethods.isSet(remoteServerId) && !remoteServerId.equals("undefined")) {
                ResetLicenseServerAction resetLicenseServerAction = new ResetLicenseServerAction();
                Long timeoutSeconds = Long.valueOf(1);

                ServerActionBean resetLicenseServerActionBean =
                        resetLicenseServerAction.getNewServerAction(localServerId, remoteServerId,
                                timeoutSeconds);

                resetLicenseServerActionBean = APILocator.getServerActionAPI()
                        .saveServerActionBean(resetLicenseServerActionBean);

                //Waits for 3 seconds in order all the servers respond.
                int maxWaitTime =
                        timeoutSeconds.intValue() * 1000 + Config.getIntProperty(
                                "CLUSTER_SERVER_THREAD_SLEEP", 2000);
                int passedWaitTime = 0;

                //Trying to NOT wait whole 3 secons for returning the info.
                while (passedWaitTime <= maxWaitTime) {
                    try {
                        Thread.sleep(10);
                        passedWaitTime += 10;

                        resetLicenseServerActionBean =
                                APILocator.getServerActionAPI()
                                        .findServerActionBean(resetLicenseServerActionBean.getId());

                        //No need to wait if we have all Action results.
                        if (resetLicenseServerActionBean != null
                                && resetLicenseServerActionBean.isCompleted()) {
                            passedWaitTime = maxWaitTime + 1;
                        }

                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        passedWaitTime = maxWaitTime + 1;
                    }
                }

                //If we reach the timeout and the server didn't respond.
                //We assume the server is down and remove the license from the table.
                if (!resetLicenseServerActionBean.isCompleted()) {

                    resetLicenseServerActionBean.setCompleted(true);
                    resetLicenseServerActionBean.setFailed(true);
                    resetLicenseServerActionBean
                            .setResponse(new com.dotmarketing.util.json.JSONObject()
                                    .put(ServerAction.ERROR_STATE,
                                            "Server did NOT respond on time"));
                    APILocator.getServerActionAPI()
                            .saveServerActionBean(resetLicenseServerActionBean);
                    LicenseUtil.freeLicenseOnRepo(serial, remoteServerId);

                    //If it was completed but we got some error, we need to alert it.
                } else if (resetLicenseServerActionBean.isCompleted()
                        && resetLicenseServerActionBean.isFailed()) {

                    throw new Exception(resetLicenseServerActionBean.getResponse()
                            .getString(ServerAction.ERROR_STATE));
                }

                //If the server we are removing license is local.
            } else {
                HibernateUtil.startTransaction();
                LicenseUtil.freeLicenseOnRepo();
                HibernateUtil.closeAndCommitTransaction();
            }

            AdminLogger.log(LicenseResource.class, "freeLicense", "License From Repo Freed",
                    initData.getUser());

        } catch (Exception exception) {
            Logger.error(this, "can't free license ", exception);

            HibernateUtil.getSessionIfOpened().ifPresent(session -> {
                try {
                    HibernateUtil.rollbackTransaction();
                } catch (DotHibernateException dotHibernateException) {
                    Logger.warn(this, "can't rollback", dotHibernateException);
                }
            });

            return Response.serverError().build();
        }

        return Response.ok().build();
    }

    @POST
    @Path("/requestCode/{params:.*}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response requestLicense(
            @Context HttpServletRequest request, @Context final HttpServletResponse response,
            @PathParam("params") String params
    ) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .params(params)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requiredPortlet(PortletID.CONFIGURATION.toString())
                .init();

        Map<String, String> paramsMap = initData.getParamsMap();

        //Validate the parameters
        String licenseType = paramsMap.get("licensetype");
        String licenseLevel = paramsMap.get("licenselevel");

        StringBuilder responseMessage = new StringBuilder();

        if (!UtilMethods.isSet(licenseType)) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(
                    responseMessage.append("Error: ").append("'licenseType'")
                            .append(" is a required param.")
            ).build();
        }

        if (!UtilMethods.isSet(licenseLevel)) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(
                    responseMessage.append("Error: ").append("'licenseLevel'")
                            .append(" is a required param.")
            ).build();
        }

        try {
            HttpSession session = request.getSession();
            session.setAttribute("iwantTo", "request_code");
            session.setAttribute("license_type", licenseType);
            session.setAttribute("license_level", licenseLevel);
            if (!"trial".equals(licenseType) && !
                    "dev".equals(licenseType) && !
                    "prod".equals(licenseType)

            ) {
                throw new DotStateException("invalid License Type");
            }

            LicenseUtil.processForm(request);

            JSONObject jsonResponse = new JSONObject();
            if (UtilMethods.isSet(request.getAttribute("requestCode"))) {
                jsonResponse.put("success", true);
                jsonResponse.put("requestCode", request.getAttribute("requestCode"));
            } else {
                jsonResponse.put("success", false);
            }
            return Response.ok(jsonResponse.toString(), MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception ex) {
            Logger.error(this, "can't request license ", ex);

            return Response.serverError().build();
        }

    }

    @NoCache
    @POST
    @Path("/applyLicense")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response applyLicense(
            @Context HttpServletRequest request, @Context final HttpServletResponse response,
            @PathParam("params") String params,
            @QueryParam("licenseText") String licenseText
    ) {
        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .params(params)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requiredPortlet(PortletID.CONFIGURATION.toString())
                .init();

        //Validate the parameters
        StringBuilder responseMessage = new StringBuilder();

        if (!UtilMethods.isSet(licenseText)) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(
                    responseMessage.append("Error: ").append("'licenseText'")
                            .append(" is a required param.")
            ).build();
        }

        try {
            HttpSession session = request.getSession();

            session.setAttribute("applyForm", Boolean.TRUE);
            session.setAttribute("iwantTo", "paste_license");
            session.setAttribute("paste_license", "paste_license");
            session.setAttribute("license_text", licenseText);

            String error = LicenseUtil.processForm(request);
            User u = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
            if (error != null) {
                return Response.ok(LanguageUtil.get(u, "license-bad-id-explanation"),
                        MediaType.APPLICATION_JSON_TYPE).build();
            }
            return Response.ok(error, MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception ex) {
            Logger.error(this, "can't request license ", ex);

            return Response.serverError().build();
        }

    }

    @NoCache
    @POST
    @Path("/resetLicense/{params:.*}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response resetLicense(@Context HttpServletRequest request,
            @Context final HttpServletResponse response, @PathParam("params") String params) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .params(params)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requiredPortlet(PortletID.CONFIGURATION.toString())
                .init();

        try {
            freeLicense(request, response, params);

            HttpSession session = request.getSession();

            session.setAttribute("applyForm", Boolean.TRUE);
            session.setAttribute("iwantTo", "paste_license");
            session.setAttribute("paste_license", "paste_license");
            session.setAttribute("license_text", "blah");

            String error = LicenseUtil.processForm(request);
            return Response.ok("", MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception ex) {
            Logger.error(this, "can't request license ", ex);

            return Response.serverError().build();
        }

    }


}
