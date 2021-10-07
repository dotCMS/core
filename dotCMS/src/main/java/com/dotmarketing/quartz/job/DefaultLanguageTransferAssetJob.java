package com.dotmarketing.quartz.job;

import com.dotcms.api.system.event.Visibility;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.content.elasticsearch.business.DotIndexException;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.quartz.DotStatefulJob;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.io.Serializable;
import java.util.Map;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;

/**
 * This serves as a task wrapping the execution of the file asset language transfer logic
 */
public class DefaultLanguageTransferAssetJob extends DotStatefulJob {

    /**
     * Run job method
     * @param jobExecutionContext
     * @throws JobExecutionException
     */
    @Override
    public void run(final JobExecutionContext jobExecutionContext) throws JobExecutionException {
        final Trigger trigger = jobExecutionContext.getTrigger();
        final Map<String, Serializable> map = getExecutionData(trigger, DefaultLanguageTransferAssetJob.class);
        final Long oldDefaultLang = (Long)map.get("oldDefaultLanguage");
        final Long newDefaultLanguage = (Long)map.get("newDefaultLanguage");
        Logger.info(DefaultLanguageTransferAssetJob.class, String.format(" Executing default language transfer job from lang `%s` to `%s`.", oldDefaultLang, newDefaultLanguage));
        try {
            APILocator.getLanguageAPI().transferAssets(oldDefaultLang, newDefaultLanguage, APILocator.systemUser());
            jobCompletionSystemNotify();
        } catch (DotDataException | DotIndexException | DotSecurityException e) {
            throw new JobExecutionException(
                    String.format("Error Transfering assets from  the old default language %s to the new default language %s .",oldDefaultLang, newDefaultLanguage), e);
        }
    }

    /**
     * Notify the job conclusion
     */
    protected void jobCompletionSystemNotify()
            throws DotDataException {

        final Role cmsAdminRole = APILocator.getRoleAPI().loadCMSAdminRole();
        final User systemUser = APILocator.systemUser();

        final NotificationAPI notificationAPI = APILocator.getNotificationAPI();

        notificationAPI.generateNotification(new I18NMessage("default-lang-switch-prompt"),
                new I18NMessage("default-lang-switch-transfer-assets-done",
                        "Default Language assets transfer job has completed.", (Object[]) null),
                null,
                NotificationLevel.INFO, NotificationType.GENERIC, Visibility.ROLE,
                cmsAdminRole.getId(), systemUser.getUserId(),
                systemUser.getLocale());
    }

    /**
     * Trigger the job ASAP
     * @param oldDefaultLanguage
     * @param newDefaultLanguage
     */
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
