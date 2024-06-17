package com.dotmarketing.portlets.contentlet.business.web;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.quartz.DotStatefulJob;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

/**
 * Should be run after a change in a host's name, it update all the file asset templates of pages
 * to use the new host's name.
 */
public class UpdatePageTemplatePathJob extends DotStatefulJob {

    final String GET_FIELDS_TO_UPDATE = "select DISTINCT f.field_contentlet from field f inner join structure s on s.inode = f.structure_inode where s.structuretype = '5' and f.velocity_var_name = 'template'";
    final String GET_CONTENTLETS_TO_UPDATE = "select identifier,inode from contentlet where %s like ?";
    final String UPDATE_QUERY = "update contentlet set %s = REPLACE(%s,?,?) where %s like ?";

    @WrapInTransaction
    @Override
    public void run(final JobExecutionContext jobContext) throws JobExecutionException {
        final JobDataMap jobDataMap = jobContext.getJobDetail().getJobDataMap();
        final String oldHostName = jobDataMap.get("oldHostName").toString();
        final String newHostName = jobDataMap.get("newHostName").toString();

        try {
            final List<Map<String, Object>> fields = new DotConnect()
                    .setSQL(GET_FIELDS_TO_UPDATE)
                    .loadObjectResults();

            for (final Map<String, Object> field : fields) {
                final String field_contentlet = String.class.cast(field.get("field_contentlet"));
                //Get Contentlets to update, this because we need to remove from cache
                List<Map<String, Object>> contentlets_list = new DotConnect()
                        .setSQL(String.format(GET_CONTENTLETS_TO_UPDATE,field_contentlet))
                        .addParam("//"+oldHostName+ Constants.TEMPLATE_FOLDER_PATH+"%")
                        .loadObjectResults();


                //UPDATE Fields
                final DotConnect dc = new DotConnect().setSQL(String.format(UPDATE_QUERY,field_contentlet,field_contentlet,field_contentlet));
                dc.addParam("//"+oldHostName+ Constants.TEMPLATE_FOLDER_PATH);
                dc.addParam("//"+newHostName+ Constants.TEMPLATE_FOLDER_PATH);
                dc.addParam("//"+oldHostName+ Constants.TEMPLATE_FOLDER_PATH+"%");
                dc.loadResult();

                //Flush Cache
                for(final Map<String, Object> inode : contentlets_list) {
                    final String contentletInode = String.class.cast(inode.get("inode"));
                    final String contentletId = String.class.cast(inode.get("identifier"));
                    CacheLocator.getContentletCache().remove(contentletInode);
                    CacheLocator.getHTMLPageCache().remove(contentletId);
                }
               //The following blocks takes care of the json fields
                if(APILocator.getContentletJsonAPI().isPersistContentAsJson()) {
                    final Set<Tuple3<String, String, String>> found = findPageContentletToUpdateForJson("//" + oldHostName + Constants.TEMPLATE_FOLDER_PATH, dc);

                    final Set<String> templates = found.stream().map(Tuple3::_3)
                            .collect(Collectors.toSet());
                    //This loop is expected to iterate just once
                    for (final String existingTemplate : templates) {
                        try {
                            updateTemplateForJson(
                                    existingTemplate.replaceAll(oldHostName,newHostName) ,existingTemplate, dc);
                        } catch (Exception e) {
                            Logger.error(UpdatePageTemplatePathJob.class, "", e);
                        }
                    }
                    found.forEach(tuple -> {
                        CacheLocator.getContentletCache().remove(tuple._1);
                        CacheLocator.getHTMLPageCache().remove(tuple._2);
                    });
                }
            }
            CacheLocator.getTemplateCache().clearCache();
            HibernateUtil.flush();
        } catch (DotDataException e) {
            throw new JobExecutionException(e);
        } finally {
            try {
                QuartzUtils.getScheduler().unscheduleJob(
                        TriggerKey.triggerKey(
                                jobContext.getTrigger().getKey().getName(),
                                jobContext.getTrigger().getKey().getGroup()
                        )
                );
            } catch (SchedulerException e) {
                Logger.error(UpdatePageTemplatePathJob.class, e.getMessage());
            }
        }
    }

    /**
     * Given a template name we retrieve page info containing (inode, identifier, template)
     * @param templateName
     * @param dotConnect
     * @return
     * @throws DotDataException
     */
    private Set<Tuple3<String, String, String>> findPageContentletToUpdateForJson(
            final String templateName, final DotConnect dotConnect)
            throws DotDataException {

        String select = null;
        if (DbConnectionFactory.isPostgres()) {
            select = String
                    .format("SELECT identifier, inode, contentlet_as_json->'fields'->'template'->>'value' as template FROM contentlet WHERE contentlet_as_json @> '{\"fields\":{\"template\":{\"type\":\"Custom\"}}}' and contentlet_as_json-> 'fields' ->'template'->>'value' LIKE '%s' ",
                            templateName + "%");
        }
        if (DbConnectionFactory.isMsSql()) {
            select = String
                    .format("SELECT identifier, inode, JSON_VALUE(contentlet_as_json,'$.fields.template.value') as template FROM contentlet WHERE JSON_VALUE(contentlet_as_json,'$.fields.template.type') = 'Custom' AND JSON_VALUE(contentlet_as_json,'$.fields.template.value') LIKE '%s' ",
                            templateName + "%");
        }

        if (null == select) {
            throw new IllegalStateException(
                    "Unable to determine what database with json support I am on.");
        }

        dotConnect.setSQL(select);
        return dotConnect.loadObjectResults().stream().map(map ->
                Tuple.of(map.get("inode").toString(),
                        map.get("identifier").toString(),
                        map.get("template").toString()
                )
        ).collect(Collectors.toSet());
    }

    /**
     * update the template name for a given old template path
     * @param newTemplateName
     * @param oldTemplateName
     * @param dotConnect
     * @throws DotDataException
     */
    private void updateTemplateForJson(
            final String newTemplateName, final String oldTemplateName, final DotConnect dotConnect)
            throws DotDataException {
        String update = null;
        if(DbConnectionFactory.isPostgres()) {
            update = String
                    .format("UPDATE contentlet SET contentlet_as_json = jsonb_set(contentlet_as_json,'{fields,template}', jsonb '{\"type\":\"Custom\", \"value\":\"%s\" }') WHERE contentlet_as_json @> '{\"fields\":{\"template\":{\"type\":\"Custom\"}}}' and contentlet_as_json-> 'fields' ->'template'->>'value' = '%s' ",
                            newTemplateName, oldTemplateName);
        }
        if(DbConnectionFactory.isMsSql()) {
            update = String
                    .format("UPDATE contentlet SET contentlet_as_json = JSON_MODIFY(contentlet_as_json,'$.fields.template.value','%s') WHERE JSON_VALUE(contentlet_as_json,'$.fields.template.type') = 'Custom' AND JSON_VALUE(contentlet_as_json,'$.fields.template.value') = '%s'  ",
                            newTemplateName, oldTemplateName);
        }

        if(null == update){
           throw new IllegalStateException("Unable to determine what database with json support I am on.");
        }

        Logger.debug(UpdatePageTemplatePathJob.class, update);

        dotConnect.setSQL(update);
        dotConnect.loadResult();
    }

    public static void triggerUpdatePageTemplatePathJob(final String oldHostName, final String newHostName) {
        DotPreconditions.checkNotNull(oldHostName);
        DotPreconditions.checkNotNull(newHostName);

        final JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("oldHostName", oldHostName);
        jobDataMap.put("newHostName", newHostName);

        final String randomID = UUID.randomUUID().toString();

        final JobDetail jobDetail = JobBuilder.newJob(UpdatePageTemplatePathJob.class)
                .withIdentity("updatePageTemplatePathJob-" + randomID, "update_page_template_path_job")
                .setJobData(jobDataMap)
                .storeDurably(false)
                .requestRecovery(true)
                .build();

        long startTime = System.currentTimeMillis();
        final Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("updatePageTemplatePathTrigger-" + randomID, "update_page_template_path_job_triggers")
                .startAt(new Date(startTime))
                .forJob(jobDetail)
                .build();

        try {
            Scheduler scheduler = QuartzUtils.getScheduler();
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            throw new DotRuntimeException(e);
        }


    }
}
