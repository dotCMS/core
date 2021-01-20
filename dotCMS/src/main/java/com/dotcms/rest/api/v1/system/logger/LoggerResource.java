package com.dotcms.rest.api.v1.system.logger;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.apps.ExportSecretForm;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.servlets.taillog.TailLogServlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.model.User;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collectors;

/**
 * Endpoint to interact with the Loggers, can see the level and set it too.
 * @author jsanca
 */
@Path("/v1/logger")
public class LoggerResource {

    /**
     * Get the logger for a specific class, 404 if logger does not exists
     * User must be Admin
     * @param request     {@link HttpServletRequest}
     * @param response    {@link HttpServletResponse}
     * @param loggerName  {@link String}
     * @return Response
     */
    @Path("/{loggerName}")
    @GET
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getLogger(@Context final HttpServletRequest request,
                              @Context final HttpServletResponse response,
                              @PathParam("loggerName") final String loggerName) throws DotSecurityException {

        if (!new WebResource.InitBuilder()
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init().getUser().isAdmin()) {

            throw new DotSecurityException("User is not admin");
        }

        final Object loggerClass = Logger.getLogger(loggerName);
        if (null != loggerClass) {

            return Response.ok(new ResponseEntityView(this.toView(loggerClass))).build();
        }

        throw new DoesNotExistException("Logger: " + loggerName + " does not exists");
    }

    /**
     * Get the loggers
     * User must be Admin
     * @param request     {@link HttpServletRequest}
     * @param response    {@link HttpServletResponse}
     * @return Response
     */
    @GET
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getLoggers(@Context final HttpServletRequest request,
                              @Context final HttpServletResponse response) throws DotSecurityException {

        if (!new WebResource.InitBuilder()
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init().getUser().isAdmin()) {

            throw new DotSecurityException("User is not admin");
        }

        return Response.ok(new ResponseEntityView(Logger.getCurrentLoggers()
                .stream().map(this::toView).collect(Collectors.toList()))).build();

    }

    /**
     * Change the log level, 404 if Logger does not exists, 400 if Level is not valid
     * User must be Admin
     * @param request           {@link HttpServletRequest}
     * @param response          {@link HttpServletResponse}
     * @param changeLoggerForm  {@link ChangeLoggerForm}
     * @return                  Response
     * @throws DotSecurityException
     */
    @PUT
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response changeLoggerLevel(@Context final HttpServletRequest request,
                                @Context final HttpServletResponse response,
                                final ChangeLoggerForm changeLoggerForm) throws DotSecurityException {

        if (!new WebResource.InitBuilder()
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init().getUser().isAdmin()) {

            throw new DotSecurityException("User is not admin");
        }

        final String loggerName = changeLoggerForm.getName();
        final String level      = changeLoggerForm.getLevel();

        if (!Logger.isValidLevel(level)) {

            throw new IllegalArgumentException("Level: " + level + " is not valid");
        }

        final Object logger     = Logger.setLevel(loggerName, level);

        if (null != logger) {

            return Response.ok(new ResponseEntityView(this.toView(logger))).build();
        }

        throw new DoesNotExistException("Logger: " + loggerName + " does not exists");
    }

    private LoggerView toView (final Object loggerObject) {

        if (loggerObject instanceof org.apache.logging.log4j.core.Logger) {

            final org.apache.logging.log4j.core.Logger log4jLogger =
                    (org.apache.logging.log4j.core.Logger)loggerObject;

            return new LoggerView(log4jLogger.getName(), log4jLogger.getLevel().name());
        }

        return new LoggerView("unkown", "unkown");
    }


    /**
     * Downloads a log file
     * @param request
     * @param response
     * @param fileName
     * @return
     */
    @GET
    @Path("/_download/{fileName}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response exportSecrets(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("fileName") final String fileName) throws DotSecurityException, IOException {

        if (!new WebResource.InitBuilder()
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .requiredPortlet("maintenance")
                .rejectWhenNoUser(true).init().getUser().isAdmin()) {

            throw new DotSecurityException("User is not admin");
        }

        String tailLogLofFolder = Config.getStringProperty("TAIL_LOG_LOG_FOLDER", "./dotsecure/logs/");
        if (!tailLogLofFolder.endsWith(java.io.File.separator)) {
            tailLogLofFolder = tailLogLofFolder + java.io.File.separator;
        }

        final File logFolder = new File(com.dotmarketing.util.FileUtil.getAbsolutlePath(tailLogLofFolder));
        final File logFile 	 = new File(com.dotmarketing.util.FileUtil.getAbsolutlePath(tailLogLofFolder + fileName));

        // if the logFile is outside of of the logFolder, die
        if (!logFolder.exists() || !logFile.getCanonicalPath().startsWith(logFolder.getCanonicalPath())) {

            SecurityLogger.logInfo(TailLogServlet.class,  "Invalid File request:" + logFile.getCanonicalPath() + " from:" +request.getRemoteHost() + " " );
            // todo: throw exception for 403
        }

        Logger.info(this.getClass(), "Requested logFile:" + logFile.getCanonicalPath());

        //no need to close i'll get closed upon writing the response
        return Response.ok(new BufferedInputStream(Files.newInputStream(logFile.toPath())), MediaType.APPLICATION_OCTET_STREAM)
                .header("content-disposition", "attachment; filename=" + fileName)
                .build(); // 200
    }
}
