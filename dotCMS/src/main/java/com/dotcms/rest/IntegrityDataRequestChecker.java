package com.dotcms.rest;

import com.dotcms.integritycheckers.IntegrityUtil;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

import javax.servlet.http.HttpSession;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Thread to constantly check the integrity check status by hitting on receiver host endpoint.
 * Anything but {@link com.dotcms.rest.IntegrityResource.ProcessStatus}.PROCESSING is considered that the integrity data
 * generation process is done.
 */
public class IntegrityDataRequestChecker implements Runnable {

    private final String authToken;
    private final PublishingEndPoint endpoint;
    private final String integrityDataRequestId;
    private final HttpSession session;
    private final InitDataObject initData;

    public IntegrityDataRequestChecker(String authToken,
                                       PublishingEndPoint endpoint,
                                       String integrityDataRequestId,
                                       HttpSession session,
                                       InitDataObject initData) {
        this.authToken = authToken;
        this.endpoint = endpoint;
        this.integrityDataRequestId = integrityDataRequestId;
        this.session = session;
        this.initData = initData;
    }

    @Override
    public void run() {
        final Map<String, String> paramsMap = initData.getParamsMap();
        final String endpointId = paramsMap.get("endpoint");
        final User loggedUser = initData.getUser();
        final FormDataMultiPart form = new FormDataMultiPart();
        form.field("AUTH_TOKEN",authToken);
        form.field("REQUEST_ID", integrityDataRequestId);
        final String url = endpoint.toURL() + "/api/integrity/getintegritydata/";

        boolean processing = true;
        while(processing) {
            final Response response = IntegrityResource.postWithEndpointState(
                    endpoint.getId(),
                    url,
                    new MediaType("application", "zip"),
                    Entity.entity(form, form.getMediaType()));

            if (response.getStatus() == HttpStatus.SC_OK) {
                processing = false;

                final String outputDir = IntegrityUtil.getIntegrityDataPath(endpoint.getId());
                final InputStream zipFile = response.readEntity(InputStream.class);

                try {
                    IntegrityUtil.unzipFile(zipFile, outputDir);
                } catch(Exception e) {
                    //Special handling if the thread was interrupted
                    if (e instanceof InterruptedException) {
                        //Setting the process status
                        IntegrityResource.setStatus(session, endpointId, IntegrityResource.ProcessStatus.CANCELLED, null);
                        final String message = "Requested interruption of the integrity checking process [unzipping Integrity Data] by the user.";
                        Logger.debug(IntegrityResource.class, message, e);
                        throw new RuntimeException(message, e);
                    }

                    //Setting the process status
                    IntegrityResource.setStatus(session, endpointId, IntegrityResource.ProcessStatus.ERROR, null);
                    final String message = "Error while unzipping Integrity Data";
                    Logger.error(IntegrityResource.class, message, e);
                    throw new RuntimeException(message, e);
                } finally {
                    if (zipFile != null) {
                        try {
                            zipFile.close();
                        } catch ( IOException e ) {
                            Logger.warn(IntegrityResource.class, "Error closing zip file stream", e);
                        }
                    }
                }

                // set session variable
                // call IntegrityChecker
                boolean conflictPresent ;
                try {
                    HibernateUtil.startTransaction();
                    IntegrityUtil.completeDiscardConflicts(endpointId);
                    HibernateUtil.commitTransaction();

                    HibernateUtil.startTransaction();
                    conflictPresent = IntegrityUtil.completeCheckIntegrity(endpointId);
                    HibernateUtil.commitTransaction();
                } catch(Exception e) {
                    try {
                        HibernateUtil.rollbackTransaction();
                    } catch (DotHibernateException e1) {
                        Logger.error(
                                IntegrityResource.class,
                                "Error while rolling back transaction",
                                e);
                    }

                    //Special handling if the thread was interrupted
                    if (e instanceof InterruptedException) {
                        //Setting the process status
                        IntegrityResource.setStatus(
                                session,
                                endpointId,
                                IntegrityResource.ProcessStatus.CANCELLED,
                                null);
                        final String message =
                                "Requested interruption of the integrity checking process by the user.";
                        Logger.debug(IntegrityResource.class, message, e);
                        throw new RuntimeException(message, e);
                    }

                    Logger.error(IntegrityResource.class, "Error checking integrity", e);

                    //Setting the process status
                    IntegrityResource.setStatus(session, endpointId, IntegrityResource.ProcessStatus.ERROR, null);
                    throw new RuntimeException("Error checking integrity", e);
                } finally {
                    try {
                        IntegrityUtil.dropTempTables(endpointId);
                        HibernateUtil.closeSession();
                    } catch (DotHibernateException e) {
                        Logger.warn(this, e.getMessage(), e);
                    } catch (DotDataException e) {
                        Logger.error(IntegrityResource.class, "Error while deleting temp tables", e);
                    }
                }

                if (conflictPresent) {
                    //Setting the process status
                    IntegrityResource.setStatus(session, endpointId, IntegrityResource.ProcessStatus.FINISHED, null);
                } else {
                    String noConflictMessage;
                    try {
                        noConflictMessage = LanguageUtil.get(
                                loggedUser.getLocale(),
                                "push_publish_integrity_conflicts_not_found");
                    } catch ( LanguageException e ) {
                        noConflictMessage = "No Integrity Conflicts found";
                    }
                    //Setting the process status
                    IntegrityResource.setStatus(
                            session,
                            endpointId,
                            IntegrityResource.ProcessStatus.NO_CONFLICTS,
                            noConflictMessage);
                }
            } else if ( response.getStatus() == HttpStatus.SC_PROCESSING ) {
                // do nothing
            } else if (response.getStatus() == HttpStatus.SC_RESET_CONTENT) {
                processing = false;
                //Setting the process status
                IntegrityResource.setStatus(session, endpointId, IntegrityResource.ProcessStatus.CANCELLED, null);
            } else {
                IntegrityResource.setStatus(session, endpointId, IntegrityResource.ProcessStatus.ERROR, null);
                Logger.error(
                        this.getClass(),
                        "Response indicating a " + response.getStatusInfo().getReasonPhrase()
                                + " (" + response.getStatus()
                                + ") Error trying to retrieve the Integrity data from the Endpoint ["
                                + endpointId + "]." );
                processing = false;
            }
        }
    }

}
