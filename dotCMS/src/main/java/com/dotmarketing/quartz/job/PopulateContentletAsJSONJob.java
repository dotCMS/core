package com.dotmarketing.quartz.job;

import com.dotcms.enterprise.HostAssetsJobProxy;
import com.dotcms.util.content.json.PopulateContentletAsJSONUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.quartz.DotStatefulJob;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Map;

/**
 * Job created to populate in the Contentlet table missing contentlet_as_json columns.
 */
public class PopulateContentletAsJSONJob extends DotStatefulJob {

    public static final String EXCLUDING_ASSET_SUB_TYPE = "excludingAssetSubType";

    @Override
    public void run(JobExecutionContext jobContext) throws JobExecutionException {

        final Trigger trigger = jobContext.getTrigger();
        final Map<String, Serializable> executionData = getExecutionData(trigger, PopulateContentletAsJSONJob.class);

        final String excludingAssetSubType;
        if (executionData.containsKey(EXCLUDING_ASSET_SUB_TYPE)) {
            excludingAssetSubType = (String) executionData.get(EXCLUDING_ASSET_SUB_TYPE);
        } else {
            excludingAssetSubType = null;
        }

        try {
            new PopulateContentletAsJSONUtil().populateExcludingAssetSubType(excludingAssetSubType);
        } catch (SQLException | DotDataException | IOException e) {
            Logger.error(this, "Error executing Contentlet as JSON population job", e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Fires the job to populate the missing contentlet_as_json columns.
     */
    public static void fireJob(final String excludingAssetSubType) {

        final ImmutableMap<String, Serializable> nextExecutionData = ImmutableMap
                .of(EXCLUDING_ASSET_SUB_TYPE, excludingAssetSubType);
        try {
            DotStatefulJob.enqueueTrigger(nextExecutionData, PopulateContentletAsJSONJob.class);
        } catch (Exception e) {
            Logger.error(HostAssetsJobProxy.class, "Error scheduling populate content as JSON job", e);
            throw new DotRuntimeException("Error scheduling populate content as JSON job", e);
        }
    }

}
