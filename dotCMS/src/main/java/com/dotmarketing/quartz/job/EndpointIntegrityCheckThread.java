package com.dotmarketing.quartz.job;

import com.dotcms.integritycheckers.IntegrityUtil;
import com.dotcms.rest.IntegrityResource;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONException;

import java.util.concurrent.Callable;

public class EndpointIntegrityCheckThread implements Callable<IntegrityUtil.IntegrityDataExecutionMetadata> {

    private final String endpointId;

    public EndpointIntegrityCheckThread(String endpointId) {
        this.endpointId = endpointId;
    }

    @Override
    public IntegrityUtil.IntegrityDataExecutionMetadata call() throws Exception {
        Logger.info(IntegrityCheckJob.class, String.format("Starting IntegrityCheck task for %s", endpointId));
        // After hours of thinking what should I evaluate here in order to evaluate if the task
        // did completed or if it has a failure behavior, the best option is to try to emulate
        // what IntegrityResource.checkIntegrity() method does.
        // Maybe we can refactor it in a way it can be reused in its very core functionality,
        // that is no http request, session or response what's so ever.

        // return if we already have the data
        final IntegrityUtil integrityUtil = new IntegrityUtil();
        try {
            if (integrityUtil.doesIntegrityConflictsDataExist(endpointId)) {
                jsonResponse.put("success", true );
                jsonResponse.put("message", "Integrity Checking Initialized...");

                //Setting the process status
                setStatus(httpServletRequest, endpointId, IntegrityResource.ProcessStatus.FINISHED);

                return response(jsonResponse.toString(), false);
            }
        } catch(JSONException e) {
            Logger.error(
                    IntegrityResource.class,
                    "Error setting return message in JSON response",
                    e);
            return response("Error setting return message in JSON response", true);
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e);
        } catch(Exception e) {
            Logger.error(IntegrityResource.class, "Error checking existence of integrity data", e);
            return response( "Error checking existence of integrity data" , true );
        }

        return null;
    }

}
