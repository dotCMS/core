package com.dotmarketing.portlets.hostadmin.business;

import java.io.File;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;

import com.dotcms.enterprise.HostAssetsJobProxy;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPIPostHook;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.SimpleScheduledTask;
import com.dotmarketing.quartz.job.HostCopyOptions;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class CopyHostContentUtil{

	public CopyHostContentUtil(){
		
	}
	

	public void checkHostCopy(Contentlet contentlet, User user, String copyOptions) {
		try {
				
					HostAPI hostAPI = APILocator.getHostAPI();
					
					Map<String, String> copyParams = new HashMap<String, String>();
					List<RegExMatch> matches = RegEX.find(copyOptions, "(?:(\\w+):([\\w-]+);?)");
					for (RegExMatch match : matches) {
						String varName = match.getGroups().get(0).getMatch();
						String varValue = match.getGroups().get(1).getMatch();
						copyParams.put(varName, varValue);
					}

					String copyFromHostId = copyParams.get("copy_from_host_id");
					boolean copyAll = copyParams.get("copy_all").equals("on");
					boolean copyTemplatesContainers = copyParams.get("copy_templates_containers").equals("on");
					boolean copyContentOnPages = copyParams.get("copy_content_on_pages").equals("on");
					boolean copyFolders = copyParams.get("copy_folders").equals("on");
					boolean copyContentOnHost = copyParams.get("copy_content_on_host").equals("on");
					boolean copyFiles = copyParams.get("copy_files").equals("on");
					boolean copyPages = copyParams.get("copy_pages").equals("on");
					boolean copyVirtualLinks = copyParams.get("copy_virtual_links").equals("on");
					boolean copyHostVariables = copyParams.get("copy_host_variables").equals("on");

					Host source = hostAPI.find(copyFromHostId, user, false);
					HostCopyOptions hostCopyOptions = null;
					if (copyAll)
						hostCopyOptions = new HostCopyOptions(copyAll);
					else
						hostCopyOptions = new HostCopyOptions(copyTemplatesContainers, copyFolders, copyFiles, copyPages, copyContentOnPages, copyContentOnHost,
								copyVirtualLinks, copyHostVariables);

					Map<String, Object> parameters = new HashMap<String, Object>();
					parameters.put("sourceHostId", source.getIdentifier());
					parameters.put("destinationHostId", contentlet.getIdentifier());
					parameters.put("copyOptions", hostCopyOptions);
				

					// We make sure we schedule the copy only once even if the
					// browser for any reason sends the request twice
					if (!QuartzUtils.isJobSequentiallyScheduled("setup-host-" + contentlet.getIdentifier(), "setup-host-group")) {
						Calendar startTime = Calendar.getInstance();
						SimpleScheduledTask task = new SimpleScheduledTask("setup-host-" + contentlet.getIdentifier(), "setup-host-group", "Setups host "
								+ contentlet.getIdentifier() + " from host " + source.getIdentifier(), HostAssetsJobProxy.class.getCanonicalName(), false,
								"setup-host-" + contentlet.getIdentifier() + "-trigger", "setup-host-trigger-group", startTime.getTime(), null,
								SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT, 5, true, parameters, 0, 0);
						QuartzUtils.scheduleTask(task);
					}
				

		} catch (SchedulerException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (ParseException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (ClassNotFoundException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (DotSecurityException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}				
		
	}
	

}
