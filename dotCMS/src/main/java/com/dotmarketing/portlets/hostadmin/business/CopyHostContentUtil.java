package com.dotmarketing.portlets.hostadmin.business;

import com.dotcms.enterprise.HostAssetsJobProxy;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.SimpleScheduledTask;
import com.dotmarketing.quartz.job.HostCopyOptions;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.liferay.portal.model.User;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;

import java.text.ParseException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CopyHostContentUtil{

	public CopyHostContentUtil(){
		
	}

	public void checkHostCopy(final Contentlet contentlet, final User user, final String copyOptions) {

		try {
				
				final HostAPI hostAPI = APILocator.getHostAPI();
				final Map<String, String> copyParams = new HashMap<>();
				final List<RegExMatch> matches = RegEX.find(copyOptions, "(?:(\\w+):([\\w-]+);?)");
				for (final RegExMatch match : matches) {

					String varName = match.getGroups().get(0).getMatch();
					String varValue = match.getGroups().get(1).getMatch();
					copyParams.put(varName, varValue);
				}

				final String copyFromHostId = copyParams.get("copy_from_host_id");
				final boolean copyAll = copyParams.get("copy_all").equals("on");
				final boolean copyTemplatesContainers = copyParams.get("copy_templates_containers").equals("on");
				final boolean copyContentOnPages = copyParams.get("copy_content_on_pages").equals("on");
				final boolean copyFolders = copyParams.get("copy_folders").equals("on");
				final boolean copyContentOnHost = copyParams.get("copy_content_on_host").equals("on");
				final boolean copyLinks = copyParams.get("copy_links").equals("on");
				final boolean copyHostVariables = copyParams.get("copy_host_variables").equals("on");

				final Host source = hostAPI.find(copyFromHostId, user, false);
				final HostCopyOptions hostCopyOptions = copyAll?
					new HostCopyOptions(copyAll):
					new HostCopyOptions(copyTemplatesContainers, copyFolders, copyLinks, copyContentOnPages, copyContentOnHost,copyHostVariables);

				final Map<String, Object> parameters = new HashMap<>();
				parameters.put("sourceHostId", source.getIdentifier());
				parameters.put("destinationHostId", contentlet.getIdentifier());
				parameters.put("copyOptions", hostCopyOptions);


				// We make sure we schedule the copy only once even if the
				// browser for any reason sends the request twice

				if (!QuartzUtils.isJobScheduled("setup-host-" + contentlet.getIdentifier(), "setup-host-group")) {

					final Calendar startTime = Calendar.getInstance();
					final SimpleScheduledTask task = new SimpleScheduledTask(
							"setup-host-" + contentlet.getIdentifier(),
							"setup-host-group",
							"Setups host " + contentlet.getIdentifier() + " from host " + source.getIdentifier(),
							HostAssetsJobProxy.class.getCanonicalName(),
							false,
							"setup-host-" + contentlet.getIdentifier() + "-trigger",
							"setup-host-trigger-group",
							startTime.getTime(),
							null,
							SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT,
							5,
							true,
							parameters,
							0,
							0);
					QuartzUtils.scheduleTask(task);
				}
		} catch (SchedulerException | ParseException |
				ClassNotFoundException | DotDataException | DotSecurityException e) {

			Logger.error(this, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
	}
}
