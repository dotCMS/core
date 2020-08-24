package com.dotmarketing.quartz.job;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.quartz.CronScheduledTask;
import com.dotmarketing.quartz.DotStatefulJob;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.ScheduledTask;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import io.vavr.control.Try;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

/**
 * Stateful job used to remove content type field references before its deletion
 * @author nollymar
 */
public class CleanUpFieldReferencesJob extends DotStatefulJob {

    public static final String CLEAN_UP_FIELD_REFERENCES_JOB_NAME = "CleanUpFieldReferencesJob3";
    public static final String CLEAN_UP_FIELD_REFERENCES_JOB_DESC = "CleanUpFieldReferencesJob";
    public static final String CLEAN_UP_FIELD_REFERENCES_JOB_GROUP = "CleanUpFieldReferencesJobGroup3";
    public static final String CLEAN_UP_FIELD_REFERENCES_TRIGGER_NAME = "CleanUpFieldReferencesTrigger-%s";
    public static final String CLEAN_UP_FIELD_REFERENCES_TRIGGER_GROUP = "CleanUpFieldReferencesTriggerGroup3";
    public static final String TRIGGER_JOB_DETAIL = "trigger_job_detail";

    @Override
    @WrapInTransaction
    public void run(final JobExecutionContext jobContext) throws JobExecutionException {
        final Trigger trigger = jobContext.getTrigger();
        final DotCacheAdministrator cacheAdministrator = CacheLocator.getCacheAdministrator();
        @SuppressWarnings("unchecked")
        final Map<String, Serializable> executionData = Try.of(() ->
            (Map<String, Serializable>) cacheAdministrator
                    .get(trigger.getName(), CLEAN_UP_FIELD_REFERENCES_JOB_NAME)
        ).getOrNull();

        if(null == executionData){
            throw new IllegalArgumentException(String.format("Unable to get execution data from cache %s ",trigger.getName()));
        }

        final Optional<Map<String, Object>> jobDetailOptional = getTriggerJobDetail();
        if(jobDetailOptional.isPresent()){
            final Map<String, Object> jobDetail = jobDetailOptional.get();
            final Map<String, Serializable> o = (Map<String, Serializable>) jobDetail.get(trigger.getName());
            if(null == o) {
                throw new IllegalArgumentException(
                        String.format("Unable to get trigger execution data %s ", trigger.getName()));
            }
            System.out.println(":::Params from detail::Thread::" + o.get("field"));
        } else {
            throw new IllegalArgumentException(
                    String.format("Unable to get job detail data %s ", trigger.getName()));
        }

        final User user = (User)executionData.get("user");
        final Field field = (Field) executionData.get("field");
        final Date deletionDate = (Date) executionData.get("deletionDate");
        System.out.println(":::Job-Started::Thread:" + Thread.currentThread().getName() +"::"+field.name()+":: "+this);
/*
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final UserAPI userAPI = APILocator.getUserAPI();

        try {
            final ContentType type = contentTypeAPI.find(field.contentTypeId());

            final Structure structure = new StructureTransformer(type).asStructure();

            com.dotmarketing.portlets.structure.model.Field legacyField = new LegacyFieldTransformer(field).asOldField();

            if (!(field instanceof CategoryField) &&
                    !(field instanceof ConstantField) &&
                    !(field instanceof HiddenField) &&
                    !(field instanceof LineDividerField) &&
                    !(field instanceof TabDividerField) &&
                    !(field instanceof RelationshipsTabField) &&
                    !(field instanceof RelationshipField) &&
                    !(field instanceof PermissionTabField) &&
                    !(field instanceof HostFolderField) &&
                    structure != null
            ) {

                contentletAPI.cleanField(structure, deletionDate, legacyField, userAPI.getSystemUser(), false);

            }

            //Refreshing permissions
            if (field instanceof HostFolderField) {
                try {
                    contentletAPI.cleanHostField(structure, userAPI.getSystemUser(), false);
                } catch(DotMappingException e) {}

                permissionAPI.resetChildrenPermissionReferences(structure);
            }

            // remove the file from the cache
            new ContentletLoader().invalidate(structure);
        } catch (DotSecurityException | DotDataException e) {
            Logger.error(CleanUpFieldReferencesJob.class,
                    "Error cleaning up field references. Field velocity var: " + field.variable(), e);
        }
        */
        try {
            Thread.sleep(25000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("::: done!");

    }


  private static String nextTriggerName(){
      final String randomID = UUID.randomUUID().toString();
      return String.format(CLEAN_UP_FIELD_REFERENCES_TRIGGER_NAME,randomID);
  }

  private static Optional<JobExecutionContext> getJobExecutionContext(){
      try {
          final Scheduler sequentialScheduler = QuartzUtils.getSequentialScheduler();
          @SuppressWarnings("unchecked")
          final List<JobExecutionContext> executingJobs = sequentialScheduler.getCurrentlyExecutingJobs();
          return executingJobs.stream().filter(jobExecutionContext -> {
              final JobDetail jobDetail = jobExecutionContext.getJobDetail();
              return jobDetail != null && CLEAN_UP_FIELD_REFERENCES_JOB_NAME.equals(jobDetail.getName());
          }).findFirst();
      } catch (Exception e) {
          Logger.error(CleanUpFieldReferencesJob.class, "Error retrieving execution context. " , e);
      }
      return Optional.empty();
  }

    private static Optional<Map <String,Object>> getTriggerJobDetail(){
        final JobDetail jobDetail = Try.of(()-> QuartzUtils.getSequentialScheduler().getJobDetail(CLEAN_UP_FIELD_REFERENCES_JOB_NAME, CLEAN_UP_FIELD_REFERENCES_JOB_GROUP)).getOrNull();
        if(null == jobDetail){
           return Optional.empty();
        }
        @SuppressWarnings("unchecked")
        final Map<String, Object> dataMap = (Map<String, Object>) jobDetail.getJobDataMap()
                .get(TRIGGER_JOB_DETAIL);
        if(null == dataMap){
            return Optional.empty();
        }
        return Optional.of(dataMap);
    }

    public synchronized static void triggerCleanUpJob(final Field field, final User user) {

        final String nextTriggerName = nextTriggerName();
        final Map <String,Object> triggersData = new HashMap<>();

        final Map<String, Serializable> nextExecutionData = ImmutableMap
                .of("field", field,
                    "deletionDate", Calendar.getInstance().getTime(),
                    "user", user);

        final DotCacheAdministrator cacheAdministrator = CacheLocator.getCacheAdministrator();
        cacheAdministrator.put(nextTriggerName, nextExecutionData, CLEAN_UP_FIELD_REFERENCES_JOB_NAME);

        final Map <String,Object> jobProperties = new HashMap<>();
        final Optional<Map<String, Object>> jobDetailOption = getTriggerJobDetail();
        jobDetailOption.ifPresent(triggersData::putAll);
        triggersData.put(nextTriggerName, nextExecutionData);
        jobProperties.put(TRIGGER_JOB_DETAIL, triggersData);

        final Calendar cal = Calendar.getInstance();
        // cal.add(Calendar.SECOND, 30);

        final String cronString = new SimpleDateFormat("ss mm H d M ? yyyy").format(cal.getTime());
        final ScheduledTask task = new CronScheduledTask(CLEAN_UP_FIELD_REFERENCES_JOB_NAME, CLEAN_UP_FIELD_REFERENCES_JOB_GROUP, CLEAN_UP_FIELD_REFERENCES_JOB_DESC, CleanUpFieldReferencesJob.class.getCanonicalName(),false,
                nextTriggerName, CLEAN_UP_FIELD_REFERENCES_TRIGGER_GROUP,null,null, SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW,10,true, jobProperties, cronString);
        task.setSequentialScheduled(true);
        task.setDurability(true);

        HibernateUtil.addCommitListenerNoThrow(Sneaky.sneaked(() -> {
                    QuartzUtils.scheduleTask(task);
                    System.out.println(":::scheduled.");
                }
        ));
    }
}
