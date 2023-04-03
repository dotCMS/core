package com.dotmarketing.quartz.job;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.quartz.DotStatefulJob;
import com.dotmarketing.util.Logger;
import java.io.Serializable;
import java.util.Map;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;
/**
 * This job is responsible for removing all the contentlets associated to a content type
 * @author fabrizzio
 *
 */

public class ContentTypeDeleteJob extends DotStatefulJob {
    @Override
    public void run(JobExecutionContext jobContext) throws JobExecutionException {

        final Trigger trigger = jobContext.getTrigger();
        final Map<String, Serializable> map = getExecutionData(trigger, ContentTypeDeleteJob.class);
        final String inode = (String)map.get("inode");
        final String varName = (String)map.get("varName");

        try {
            final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser()).find(inode);
            //Kick-off deletion
            APILocator.getContentTypeDestroyAPI().destroy(contentType, APILocator.systemUser());
        } catch (DotDataException | DotSecurityException e) {
            throw new JobExecutionException(
                    String.format("Error removing contentlets from CT with inode [%s] and [%s] .",inode, varName), e);
        }

    }

    /**
     * This method is responsible for scheduling the job that will remove all the contentlets associated to a content type
     * @param type
     */
    public static void triggerContentTypeDeletion(final ContentType type) {
        final Map<String, Serializable> nextExecutionData = Map.of("inode", type.inode(), "varName", type.variable());
        try {
            DotStatefulJob.enqueueTrigger(nextExecutionData, ContentTypeDeleteJob.class);
        } catch (Exception e) {
            Logger.error(ContentTypeDeleteJob.class, String.format("Error scheduling an instance of ContentTypeDeleteJob inode[%s], varName[%s] ",type.inode(), type.variable()), e);
            throw new DotRuntimeException("Error scheduling ContentTypeDeleteJob ", e);
        }
    }
}
