package com.dotcms.timemachine.business;

import java.util.Date;
import java.util.List;

import com.dotcms.publishing.PublishStatus;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;

public interface TimeMachineAPI {

	public List<Date> getAvailableTimeMachineForSite(Host host) throws DotDataException;
	
	public PublishStatus startTimeMachineForHostAndDate(Host host, Date date) throws DotDataException;
	
}
