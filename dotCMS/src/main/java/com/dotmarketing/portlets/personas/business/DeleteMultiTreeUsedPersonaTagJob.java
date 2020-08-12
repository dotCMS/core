package com.dotmarketing.portlets.personas.business;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;

import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Deletes the unused persona tags on the multi tree.
 * @author jsanca
 */
public class DeleteMultiTreeUsedPersonaTagJob implements StatefulJob {

    private final MultiTreeAPI multiTreeAPI;
    private final PersonaAPI   personaAPI;

    public DeleteMultiTreeUsedPersonaTagJob() {

        this.multiTreeAPI = APILocator.getMultiTreeAPI();
        this.personaAPI   = APILocator.getPersonaAPI();
    }

    /**
     * Trigger the Job in order to remove the unused persona tags on the multitree
     * @param user {@link User} user that triggers the job
     * @param respectFrontEndRoles {@link Boolean}
     */
    public static void triggerDeleteMultiTreeUsedPersonaTagJob(final User user,
                                            final boolean respectFrontEndRoles) {

        final String randomID    = UUID.randomUUID().toString();
        final JobDataMap dataMap = new JobDataMap();

        dataMap.put("user", user);
        dataMap.put("respectFrontEndRoles", respectFrontEndRoles);

        final JobDetail jobDetail = new JobDetail("DeleteMultiTreeUsedPersonaTagJob-" + randomID,
                "delete_multitree_unusedpersonatag_jobs", DeleteMultiTreeUsedPersonaTagJob.class);
        jobDetail.setJobDataMap(dataMap);
        jobDetail.setDurability(false);
        jobDetail.setVolatility(false);
        jobDetail.setRequestsRecovery(true);

        final long startTime = System.currentTimeMillis();
        final SimpleTrigger trigger = new SimpleTrigger("deleteMultitreeUnusedPersonasTrigger-" + randomID,
                "delete_multitree_unusedpersonatag_triggers",
                new Date(startTime));

        try {

            final Scheduler scheduler = QuartzUtils.getSequentialScheduler();
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (Exception e) {

            Logger.error(DeleteMultiTreeUsedPersonaTagJob.class, "Error scheduling DeleteMultiTreeUsedPersonaTagJob", e);
            throw new DotRuntimeException("Error scheduling DeleteMultiTreeUsedPersonaTagJob", e);
        }

        AdminLogger.log(DeleteMultiTreeUsedPersonaTagJob.class, "triggerJobImmediately",
                String.format("Deleting UnUsed Persona Tag on MultiTree table"));
    }

    @Override
    public void execute(final JobExecutionContext jobExecutionContext) throws JobExecutionException {

        final JobDataMap map  = jobExecutionContext.getJobDetail().getJobDataMap();
        final User       user = (User) map.get("user");
        final boolean    respectFrontEndRoles = (Boolean)map.get("respectFrontEndRoles");

        this.execute(user, respectFrontEndRoles);
    }

    /**
     * Executes the clean up job personalization
     * @param user {@link User} user that triggers the job
     * @param respectFrontEndRoles {@link Boolean}
     * @throws JobExecutionException
     */
    public void execute(final User user, final boolean respectFrontEndRoles) throws JobExecutionException {

        try {

            final Set<String> removedPersonaTagSet = this.multiTreeAPI.cleanUpUnusedPersonalization(personalization -> // clean up non-existing persona tags.
                    UtilMethods.isSet(personalization) &&
                            personalization.startsWith(Persona.DOT_PERSONA_PREFIX_SCHEME) &&
                            !existsPersonaTag(personalization, user, respectFrontEndRoles));

            Logger.info(this, "Removed the unused persona tags: " + removedPersonaTagSet);
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
        }
    }

    private boolean existsPersonaTag (final String personalization, final User user, final boolean respectFrontEndRoles) {

        final Supplier<Boolean> existsPersonaTag = Sneaky.sneaked(()->
                this.personaAPI.findPersonaByTag(
                        this.unwrapPersonalizationOnPersonaTag(personalization), user, respectFrontEndRoles).isPresent());

        return existsPersonaTag.get();
    }

    private String unwrapPersonalizationOnPersonaTag (final String personalization) {

        return StringUtils.replace(personalization, Persona.DOT_PERSONA_PREFIX_SCHEME + StringPool.COLON, StringPool.BLANK);
    }
}
