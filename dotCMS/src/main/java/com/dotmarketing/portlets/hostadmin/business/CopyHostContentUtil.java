package com.dotmarketing.portlets.hostadmin.business;

import com.dotcms.enterprise.HostAssetsJobProxy;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.quartz.job.HostCopyOptions;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.liferay.portal.model.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CopyHostContentUtil{

	public CopyHostContentUtil(){
		
	}

	/**
	 * Reads the values of all the copy options that the User selected when copying a Site, and
	 * schedules that Job that actually takes care of the data copy process.
	 *
	 * @param newSite     The {@link Contentlet} that represents the new Site.
	 * @param user        The {@link User} that is performing the copy operation.
	 * @param copyOptions The String that contains all the copy options selected by the User.
	 */
	public void checkHostCopy(final Contentlet newSite, final User user, final String copyOptions) {

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
				final boolean copyContentTypes = copyParams.get("copy_content_types").equals("on");
				final Host sourceHost = hostAPI.find(copyFromHostId, user, false);
				final HostCopyOptions hostCopyOptions = copyAll?
					new HostCopyOptions(copyAll):
					new HostCopyOptions(copyTemplatesContainers, copyFolders, copyLinks, copyContentOnPages, copyContentOnHost,copyHostVariables,copyContentTypes);

			    HostAssetsJobProxy.fireJob(newSite.getIdentifier(), sourceHost.getIdentifier(), hostCopyOptions, user.getUserId());
		} catch (Throwable e) {

			Logger.error(this, "Site: " + newSite.getIdentifier() + ", msg: " + e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
	}
}
