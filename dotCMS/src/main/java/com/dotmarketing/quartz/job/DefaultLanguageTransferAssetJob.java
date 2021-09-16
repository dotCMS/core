package com.dotmarketing.quartz.job;

import com.dotcms.content.elasticsearch.business.DotIndexException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.quartz.DotStatefulJob;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableMap;
import java.io.Serializable;
import java.util.Map;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;

public class DefaultLanguageTransferAssetJob extends DotStatefulJob {

    @Override
    public void run(final JobExecutionContext jobExecutionContext) throws JobExecutionException {
        final Trigger trigger = jobExecutionContext.getTrigger();
        final Map<String, Serializable> map = getExecutionData(trigger, DefaultLanguageTransferAssetJob.class);
        final Long oldDefaultLang = (Long)map.get("oldDefaultLanguage");
        final Long newDefaultLanguage = (Long)map.get("newDefaultLanguage");
        Logger.info(DefaultLanguageTransferAssetJob.class, String.format(" Executing default language transfer job from lang `%s` to `%s`.", oldDefaultLang, newDefaultLanguage));
        try {
            APILocator.getLanguageAPI().transferAssets(oldDefaultLang, newDefaultLanguage);
        } catch (DotDataException | DotIndexException e) {
            throw new JobExecutionException(
                    String.format("Error Transfering assets from  the old default language %s to the new default language %s .",oldDefaultLang, newDefaultLanguage), e);
        }
    }

    public static void triggerDefaultLanguageTransferAssetJob(final Long oldDefaultLanguage,
            final Long newDefaultLanguage) {

        final ImmutableMap<String, Serializable> nextExecutionData = ImmutableMap
                .of("oldDefaultLanguage", oldDefaultLanguage, "newDefaultLanguage", newDefaultLanguage);
        try {
            DotStatefulJob.enqueueTrigger(nextExecutionData, DefaultLanguageTransferAssetJob.class);
        } catch (Exception e) {
            Logger.error(DefaultLanguageTransferAssetJob.class,
                    String.format("Error scheduling the DefaultLanguageTransferAssetJob transfering assets from language %s to %s",oldDefaultLanguage, newDefaultLanguage), e);
            throw new DotRuntimeException("Error scheduling DefaultLanguageTransferAssetJob ", e);
        }
    }

}
