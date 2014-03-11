package com.dotmarketing.sitesearch.ajax;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotcms.enterprise.publishing.sitesearch.SiteSearchConfig;
import com.dotcms.repackage.junit_4_8_1.org.junit.Assert;
import com.dotcms.repackage.junit_4_8_1.org.junit.Test;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * Test if the edit job in the sitesearch duplicate jobs
 * @author Oswaldo Gallango
 * @since 3/11/14
 */
public class SiteSearchAjaxActionTest {

	@Test
	public void scheduleJob() throws Exception {

		int initialAmountOfJobs = APILocator.getSiteSearchAPI().getTasks().size();
		User user=APILocator.getUserAPI().getSystemUser();

		Host host=APILocator.getHostAPI().findByName("demo.dotcms.com", user, false);
		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);
		List<String> excludePatterns = new ArrayList<String>();
		excludePatterns.add("*.vtl");
		excludePatterns.add("*.css");

		String taskName = "SiteSearch Test";
		Map<String,Object> map= new HashMap<String,Object>();
		map.put("indexhost",host.getIdentifier());
		map.put("RUN_NOW",false);
		map.put("QUARTZ_JOB_NAME", taskName);
		map.put("indexAlias", "create-new");
		map.put("langToIndex", APILocator.getLanguageAPI().getDefaultLanguage().getId());
		map.put("includeExclude", "exclude");
		map.put("paths", "*.vtl, *.css,*.js");
		map.put("CRON_EXPRESSION", "0 0/60 * * * ?");
				
		SiteSearchConfig config = new SiteSearchConfig();
		for(String key : map.keySet()){
			if(((String[]) map.get(key)).length ==1 && !key.equals("langToIndex")){
				config.put(key,((String[]) map.get(key))[0]);
			}
			else{
				config.put(key,map.get(key));
			}
		}

		APILocator.getSiteSearchAPI().scheduleTask(config);
		int currentAmountOfJobs = APILocator.getSiteSearchAPI().getTasks().size();
		Assert.assertTrue(currentAmountOfJobs > initialAmountOfJobs);

		Map<String, Object> props = APILocator.getSiteSearchAPI().getTask(taskName).getProperties();
		config = new SiteSearchConfig();
		for(String key : props.keySet()){
			if(((String[]) props.get(key)).length ==1 && !key.equals("langToIndex")){
				config.put(key,((String[]) props.get(key))[0]);
			}
			else{
				config.put(key,props.get(key));
			}
		}

		String newCronExpression = "0 0/30 * * * ?";
		config.setCronExpression(newCronExpression);
		APILocator.getSiteSearchAPI().scheduleTask(config);

		int newAmountOfJobs = APILocator.getSiteSearchAPI().getTasks().size();
		props = APILocator.getSiteSearchAPI().getTask(taskName).getProperties();

		Assert.assertTrue(currentAmountOfJobs == newAmountOfJobs);
		Assert.assertTrue(newCronExpression.equals(UtilMethods.webifyString((String) props.get("CRON_EXPRESSION"))));
		APILocator.getSiteSearchAPI().deleteTask(taskName);
		Assert.assertTrue(currentAmountOfJobs > newAmountOfJobs);
	}

}
