package com.dotmarketing.portlets.contentlet.business.web;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
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
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;

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
        final String oldTemplatePath = "//" + oldHostName + Constants.TEMPLATE_FOLDER_PATH;
        final String newTemplatePath = "//" + newHostName + Constants.TEMPLATE_FOLDER_PATH;

        try {
            final List<Map<String, Object>> fields = new DotConnect()
                    .setSQL(GET_FIELDS_TO_UPDATE)
                    .loadObjectResults();

            for (final Map<String, Object> field : fields) {
                final String field_contentlet = String.class.cast(field.get("field_contentlet"));
                //Get Contentlets to update, this because we need to remove from cache
                List<Map<String, Object>> contentlets_list = new DotConnect()
                        .setSQL(String.format(GET_CONTENTLETS_TO_UPDATE,field_contentlet))
                        .addParam(oldTemplatePath + "%")
                        .loadObjectResults();


                //UPDATE Fields
                final DotConnect dc = new DotConnect().setSQL(String.format(UPDATE_QUERY,field_contentlet,field_contentlet,field_contentlet));
                dc.addParam(oldTemplatePath);
                dc.addParam(newTemplatePath);
                dc.addParam(oldTemplatePath + "%");
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
                    final Set<Tuple3<String, String, String>> found = findPageContentletToUpdateForJson(oldTemplatePath, dc);

                    final Set<String> templates = found.stream().map(Tuple3::_3)
                            .collect(Collectors.toSet());
                    //This loop is expected to iterate just once
                    for (final String existingTemplate : templates) {
                        try {
                            updateTemplateForJson(
                                    existingTemplate.replace(oldTemplatePath, newTemplatePath), existingTemplate, dc);
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
                        jobContext.getJobDetail().getName(),
                        jobContext.getTrigger().getName()
                );
            } catch (SchedulerException e) {
                Logger.error(UpdatePageTemplatePathJob.class, e.getMessage());
            }
        }
    }

    /**
     * Finds the pages stored as JSON whose file-based template path starts with the given path. The
     * path is bound as a parameter, so it is matched as data whatever characters the site name holds.
     *
     * @param templatePath the start of the template path, such as {@code //old-site/application/templates}
     * @param dotConnect   the connection to run the query on
     * @return the matching pages as (inode, identifier, template path)
     * @throws DotDataException if the query fails
     */
    private Set<Tuple3<String, String, String>> findPageContentletToUpdateForJson(
            final String templatePath, final DotConnect dotConnect)
            throws DotDataException {

        dotConnect.setSQL("SELECT identifier, inode, contentlet_as_json->'fields'->'template'->>'value' as template "
                + "FROM contentlet WHERE contentlet_as_json @> '{\"fields\":{\"template\":{\"type\":\"Custom\"}}}' "
                + "and contentlet_as_json->'fields'->'template'->>'value' LIKE ?");
        dotConnect.addParam(templatePath + "%");
        return dotConnect.loadObjectResults().stream().map(map ->
                Tuple.of(map.get("inode").toString(),
                        map.get("identifier").toString(),
                        map.get("template").toString()
                )
        ).collect(Collectors.toSet());
    }

    /**
     * Replaces a file-based template path with a new one in the pages stored as JSON. Both paths are
     * bound as parameters.
     *
     * @param newTemplateName the new template path
     * @param oldTemplateName the template path to replace
     * @param dotConnect      the connection to run the update on
     * @throws DotDataException if the update fails
     */
    private void updateTemplateForJson(
            final String newTemplateName, final String oldTemplateName, final DotConnect dotConnect)
            throws DotDataException {

        dotConnect.setSQL("UPDATE contentlet SET contentlet_as_json = jsonb_set(contentlet_as_json, '{fields,template}', "
                + "jsonb_build_object('type', 'Custom', 'value', ?::text)) "
                + "WHERE contentlet_as_json @> '{\"fields\":{\"template\":{\"type\":\"Custom\"}}}' "
                + "and contentlet_as_json->'fields'->'template'->>'value' = ?");
        dotConnect.addParam(newTemplateName);
        dotConnect.addParam(oldTemplateName);
        dotConnect.loadResult();
    }

    public static void triggerUpdatePageTemplatePathJob(final String oldHostName, final String newHostName) {
        DotPreconditions.checkNotNull(oldHostName);
        DotPreconditions.checkNotNull(newHostName);

        final JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("oldHostName", oldHostName);
        jobDataMap.put("newHostName", newHostName);

        final String randomID = UUID.randomUUID().toString();

        final JobDetail jobDetail = new JobDetail(
                "updatePageTemplatePathJob-" + randomID,
                "update_page_template_path_job",
                UpdatePageTemplatePathJob.class
        );

        jobDetail.setJobDataMap(jobDataMap);
        jobDetail.setDurability(false);
        jobDetail.setVolatility(false);
        jobDetail.setRequestsRecovery(true);

        long startTime = System.currentTimeMillis();
        final SimpleTrigger trigger = new SimpleTrigger(
                "updatePageTemplatePathTrigger-" + randomID,
                "update_page_template_path_job_triggers",
                new Date(startTime)
        );

        try {
            Scheduler scheduler = QuartzUtils.getScheduler();
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            throw new DotRuntimeException(e);
        }


    }
}
