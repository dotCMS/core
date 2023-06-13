package com.dotcms.enterprise.publishing.remote.handler;

import java.io.File;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.PublisherConfig;


public class HostHandler implements IHandler {
	private ContentHandler contentHandler;


	public HostHandler(PublisherConfig config) {
		contentHandler = new ContentHandler(config);
	}

	@Override
	public String getName() {
		return this.getClass().getName();
	}

	@Override
	public void handle(File bundleFolder) throws Exception {
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level)
	        throw new RuntimeException("need an enterprise pro license to run this");
		contentHandler.handle(bundleFolder, true);
	}


}
