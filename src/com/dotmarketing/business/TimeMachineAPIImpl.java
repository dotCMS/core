package com.dotmarketing.business;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublishStatus;
import com.dotcms.publishing.timemachine.TimeMachineConfig;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;

public class TimeMachineAPIImpl implements TimeMachineAPI {

	@Override
	public PublishStatus startTimeMachineForHostAndDate(Host host, Date date) throws DotDataException {
				
		TimeMachineConfig tmconfig = new TimeMachineConfig();

		tmconfig.setUser(APILocator.getUserAPI().getSystemUser());
		tmconfig.setHosts(Arrays.asList(host));

		// tmconfig.setSourceBundle()
		tmconfig.setId("timeMachineBundle");
		tmconfig.setDestinationBundle("tm_" + date.getTime());
		// tmconfig.setExcludePatterns(Arrays.asList("*.dot"));
		// tmconfig.setLiveOnly(false);
		//Optional: time machine publisher will make it true
		tmconfig.setIncremental(true);

		try {
			return APILocator.getPublisherAPI().publish(tmconfig);
		} catch (DotPublishingException e) {
			throw new DotDataException("Error in publishing time machine bundle", e);
		}		
	}
	

	@Override
	public List<Date> getAvailableTimeMachineForSite(Host host) throws DotDataException {
		List<Date> list = new ArrayList<Date>();
		String bundlePath = ConfigUtils.getBundlePath();
		for ( File file : new File(bundlePath).listFiles()) {
			if ( file.isDirectory() && file.getName().startsWith("tm_")) {
				try {
					list.add(new Date(Long.parseLong(file.getName().substring(3))));
				}
				catch (Throwable t) {
					Logger.error(this, "bundle seems a time machine bundle but it is not! " + file.getName());
				}
			}
		}
		return list;
	}

}
