package com.dotmarketing.portlets.contentlet.business.web;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.Params;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.containers.model.Container;

import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.quartz.DotStatefulJob;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.quartz.*;

import java.util.*;


/**
 * Should be run after a change in a host's name, it update all the path into the {@link TemplateLayout}
 * to use the new host's name.
 */
public class UpdateContainersPathsJob extends DotStatefulJob  {

    final String GET_TEMPLATES_QUERY = "SELECT DISTINCT template.inode, template.drawed_body, template.body, identifier.host_inode " +
            "FROM ((identifier " +
            "INNER JOIN template ON identifier.id = template.identifier) " +
            "INNER JOIN template_version_info ON identifier.id = template_version_info.identifier) " +
            "where template.drawed_body is not null and template.drawed_body LIKE ? " +
            "and (template.inode = template_version_info.working_inode or template.inode = template_version_info.live_inode)";

    @WrapInTransaction
    @Override
    public void run(final JobExecutionContext jobContext) throws JobExecutionException {
        final JobDataMap jobDataMap = jobContext.getJobDetail().getJobDataMap();
        final String oldHostName = jobDataMap.get("oldHostName").toString();
        final String newHostName = jobDataMap.get("newHostName").toString();

        try {
            final List<Map<String, Object>> templates = getAllTemplatesByPath(oldHostName);

            if (null != templates && !templates.isEmpty()) {
                final Host host = APILocator.getHostAPI().find(templates.get(0).get("host_inode").toString(),
                        APILocator.systemUser(), false);

                final List<Params> params = getParameters(templates, oldHostName, newHostName);

                if (!params.isEmpty()) {
                    final DotConnect dotConnect = new DotConnect();
                    dotConnect.executeBatch("update template set drawed_body = ?, body = ? where inode =?", params);
                    cleanCache(templates, host);
                }
            }
        } catch (DotDataException | DotSecurityException e) {
            throw new JobExecutionException(e);
        } finally {
            try {
                QuartzUtils.getSequentialScheduler().unscheduleJob(
                        jobContext.getJobDetail().getName(),
                        jobContext.getTrigger().getName()
                );
            } catch (SchedulerException e) {
               Logger.error(UpdateContainersPathsJob.class, e.getMessage());
            }
        }
    }

    private void cleanCache(final List<Map<String, Object>> templates, final Host host) throws DotDataException {
        APILocator.getHostAPI().updateCache(host);

        for (final Map<String, Object> template : templates) {
            try {
                final String templateInode = template.get("inode").toString();
                DotTemplateTool.removeFromLayoutCache(templateInode);
                CacheLocator.getTemplateCache().remove(templateInode);

                final String drawedBody = (String) template.get("drawed_body");
                final TemplateLayout templateLayout = DotTemplateTool.getTemplateLayout(drawedBody);

                for (final String containerIdOrPath : templateLayout.getContainersIdentifierOrPath()) {

                    if (FileAssetContainerUtil.getInstance().isFolderAssetContainerId(containerIdOrPath)) {
                        final String relativePath = FileAssetContainerUtil.getInstance().getRelativePath(containerIdOrPath);
                        final Container container = APILocator.getContainerAPI().getWorkingContainerByFolderPath(relativePath, host,
                                APILocator.systemUser(), false);

                        CacheLocator.getContainerCache().remove(container);
                    }
                }
            } catch(DotSecurityException e) {
                Logger.warn(this.getClass(), e.getMessage());
                continue;
            }
        }
    }

    public static void triggerUpdateContainersPathsJob(final String oldHostName, final String newHostName) {
        DotPreconditions.checkNotNull(oldHostName);
        DotPreconditions.checkNotNull(newHostName);

        final JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("oldHostName", oldHostName);
        jobDataMap.put("newHostName", newHostName);

        final String randomID = UUID.randomUUID().toString();

        final JobDetail jd = new JobDetail(
                "updateContainersPathsJob-" + randomID,
                "update_containers_paths_job",
                UpdateContainersPathsJob.class
        );

        jd.setJobDataMap(jobDataMap);
        jd.setDurability(false);
        jd.setVolatility(false);
        jd.setRequestsRecovery(true);

        long startTime = System.currentTimeMillis();
        final SimpleTrigger trigger = new SimpleTrigger(
                "updateContainersPathsTrigger",
                "update_containers_paths_job_triggers",
                new Date(startTime)
        );

        try {
            Scheduler scheduler = QuartzUtils.getSequentialScheduler();
            scheduler.scheduleJob(jd, trigger);
        } catch (SchedulerException e) {
            throw new DotRuntimeException(e);
        }


    }

    @NotNull
    private List<Params> getParameters(
            final List<Map<String, Object>> templates,
            final String oldHostName,
            final String newHostName) {

        final List<Params> params = new ArrayList<>();

        for (final Map<String, Object> template : templates) {
            final String drawedBody = (String) template.get("drawed_body");
            final String body = (String) template.get("body");

            final String newDrawBody = drawedBody.replaceAll(oldHostName, newHostName);
            final String newBody = body != null ? body.replaceAll(oldHostName, newHostName) : null;

            Params templateParams = new Params.Builder()
                    .add(newDrawBody, newBody, template.get("inode"))
                    .build();

            params.add(templateParams);
        }

        return params;
    }

    private List<Map<String, Object>> getAllTemplatesByPath(final String hostName) throws DotDataException {
        return new DotConnect()
                .setSQL(GET_TEMPLATES_QUERY)
                .addParam(String.format("%%//%s%%", hostName))
                .loadObjectResults();
    }
}
