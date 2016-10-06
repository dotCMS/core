package com.dotmarketing.sitesearch.ajax;

import java.net.URL;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * Test if the edit job in the sitesearch generates duplicate jobs.
 * Reference https://github.com/dotCMS/dotCMS/issues/4926
 * @author Oswaldo Gallango
 * @since 3/11/14
 */

@Ignore("Temporarily ignore this. https://github.com/dotCMS/core/issues/9785")
public class SiteSearchAjaxActionTest {

	protected String baseURL=null;

	@Before
	public void prepare() throws Exception {
		HttpServletRequest req=ServletTestRunner.localRequest.get();
		baseURL = "http://"+req.getServerName()+":"+req.getServerPort()+"/DotAjaxDirector/com.dotmarketing.sitesearch.ajax.SiteSearchAjaxAction/cmd/scheduleJob/u/admin@dotcms.com/p/admin";
	}

	@Test
	public void scheduleJob() throws Exception {

		User user=APILocator.getUserAPI().getSystemUser();
		Host host=APILocator.getHostAPI().findByName("demo.dotcms.com", user, false);
		int initialAmountOfJobs = APILocator.getSiteSearchAPI().getTasks().size();
		String taskName = "SiteSearch Test "+UtilMethods.dateToHTMLDate(new Date(),"MM-dd-yyyy-HHmmss");

		URL scheduleJobUrl = new URL(baseURL+"?indexhost="+UtilMethods.encodeURIComponent(host.getIdentifier())+"&RUN_NOW=false&QUARTZ_JOB_NAME="+UtilMethods.encodeURIComponent(taskName)+"&indexAlias=create-new&langToIndex="+APILocator.getLanguageAPI().getDefaultLanguage().getId()+"&includeExclude=exclude&paths="+UtilMethods.encodeURIComponent("*.vtl,*.css,*.js")+"&CRON_EXPRESSION="+UtilMethods.encodeURIComponent("0 0/60 * * * ?"));
		IOUtils.toString(scheduleJobUrl.openStream(),"UTF-8");
		int currentAmountOfJobs = APILocator.getSiteSearchAPI().getTasks().size();
		/**
		 * Validate if the job was created
		 */
		Assert.assertTrue(currentAmountOfJobs > initialAmountOfJobs);

		/*
		 * Editing exiting sitesearch job	
		 */
		String newCronExpression="0 0/30 * * * ?";
		String newPaths="*.vtl,*.css,*.js,*.vtl";
		scheduleJobUrl = new URL(baseURL+"?indexhost="+UtilMethods.encodeURIComponent(host.getIdentifier())+"&RUN_NOW=false&QUARTZ_JOB_NAME="+UtilMethods.encodeURIComponent(taskName)+"&indexAlias=create-new&langToIndex="+APILocator.getLanguageAPI().getDefaultLanguage().getId()+"&includeExclude=exclude&paths="+UtilMethods.encodeURIComponent(newPaths)+"&CRON_EXPRESSION="+UtilMethods.encodeURIComponent(newCronExpression));
		IOUtils.toString(scheduleJobUrl.openStream(),"UTF-8");

		int newAmountOfJobs = APILocator.getSiteSearchAPI().getTasks().size();

		/*
		 * Validate if the job was edited 
		 */
		Assert.assertTrue(currentAmountOfJobs == newAmountOfJobs);
		Map<String, Object> props = APILocator.getSiteSearchAPI().getTask(taskName).getProperties();
		Assert.assertTrue(newCronExpression.equals(UtilMethods.webifyString((String) props.get("CRON_EXPRESSION"))));

		/*
		 * Removing the sitesearch test job and validate the removal
		 */
		APILocator.getSiteSearchAPI().deleteTask(taskName);
		newAmountOfJobs = APILocator.getSiteSearchAPI().getTasks().size();
		Assert.assertTrue(initialAmountOfJobs == newAmountOfJobs);
	}

}
