package com.dotmarketing.portlets.contentlet.business.web;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Constants;
import java.util.Date;
import java.util.UUID;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleTrigger;

/**
 * Integration tests for {@link UpdatePageTemplatePathJob}, which moves pages that use a file-based
 * template from the old site name to the new one after a site is renamed.
 */
public class UpdatePageTemplatePathJobTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link UpdatePageTemplatePathJob#run(JobExecutionContext)}
     * Given: a page whose template path starts with an old site name that contains a single quote
     * Expected: the site names are treated as data, so the job finishes and the page's template path
     * now starts with the new site name
     */
    @Test
    public void test_run_oldHostNameWithQuote_shouldMoveTheTemplatePath() throws Exception {
        final String suffix = UUID.randomUUID().toString();
        final String oldHostName = "old'site-" + suffix;
        final String newHostName = "new-site-" + suffix;
        final Template template = new TemplateDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(APILocator.getHostAPI()
                .findDefaultHost(APILocator.systemUser(), false), template).nextPersisted();

        try {
            setTemplatePath(page.getInode(), "//" + oldHostName + Constants.TEMPLATE_FOLDER_PATH + "/main.vtl");

            new UpdatePageTemplatePathJob().run(newContext(oldHostName, newHostName));

            assertEquals("//" + newHostName + Constants.TEMPLATE_FOLDER_PATH + "/main.vtl",
                    templatePath(page.getInode()));
        } finally {
            ContentletDataGen.destroy(page);
            TemplateDataGen.remove(template);
        }
    }

    private static void setTemplatePath(final String inode, final String path) throws DotDataException {
        new DotConnect()
                .setSQL("UPDATE contentlet SET contentlet_as_json = jsonb_set(contentlet_as_json, "
                        + "'{fields,template}', jsonb_build_object('type', 'Custom', 'value', ?::text)) WHERE inode = ?")
                .addParam(path)
                .addParam(inode)
                .loadResult();
    }

    private static String templatePath(final String inode) {
        return new DotConnect()
                .setSQL("SELECT contentlet_as_json->'fields'->'template'->>'value' AS template "
                        + "FROM contentlet WHERE inode = ?")
                .addParam(inode)
                .getString("template");
    }

    private static JobExecutionContext newContext(final String oldHostName, final String newHostName) {
        final String name = "updatePageTemplatePathJobTest-" + UUID.randomUUID();
        final JobDataMap dataMap = new JobDataMap();
        dataMap.put("oldHostName", oldHostName);
        dataMap.put("newHostName", newHostName);
        final JobDetail jobDetail = new JobDetail(name, "update_page_template_path_job",
                UpdatePageTemplatePathJob.class);
        jobDetail.setJobDataMap(dataMap);

        final JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getJobDetail()).thenReturn(jobDetail);
        when(context.getTrigger()).thenReturn(new SimpleTrigger(name, "update_page_template_path_job_triggers", new Date()));
        return context;
    }
}
