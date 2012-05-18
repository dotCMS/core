package com.dotcms.publishing.timemachine;

import java.util.ArrayList;
import java.util.List;

import com.dotcms.publishing.PublisherConfig;

public class TimeMachineConfig extends PublisherConfig {

	public TimeMachineConfig() {
		super();
		setIncremental(true);
	}
	
	@Override
	public List<Class> getPublishers() {
		List<Class> clazz = new ArrayList<Class>();
		clazz.add(TimeMachinePublisher.class);
		return clazz;
	}

}
