package com.dotcms.content.elasticsearch.business.listeners;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import com.dotcms.content.elasticsearch.business.ESMappingMemory;

public class ESMappingMemoryListener implements HttpSessionListener {

	@Override
	public void sessionCreated(HttpSessionEvent event) {}

	@Override
	public void sessionDestroyed(HttpSessionEvent envent) {
		ESMappingMemory.INSTANCE.clean();
	}

}
