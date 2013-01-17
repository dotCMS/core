package com.dotcms.publisher.receiver.handler;

import java.io.File;


public class HostHandler implements IHandler {
	private ContentHandler contentHandler;
	
	public HostHandler() {
		contentHandler = new ContentHandler();
	}
	
	@Override
	public String getName() {
		return this.getClass().getName();
	}
	
	@Override
	public void handle(File bundleFolder) throws Exception {
		contentHandler.handle(bundleFolder, true);
	}
	

}
