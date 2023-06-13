package com.dotcms.enterprise.publishing.timemachine;

import java.util.ArrayList;
import java.util.List;

import com.dotcms.publishing.PublisherConfig;

public class TimeMachineConfig extends PublisherConfig {

	public TimeMachineConfig() {
		super();
	}
	
	@Override
	public List<Class> getPublishers() {
		List<Class> clazz = new ArrayList<Class>();
		clazz.add(TimeMachinePublisher.class);
		return clazz;
	}
	

	@Override
	public boolean liveOnly() {
		return true;
	}

}
