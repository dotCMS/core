package com.dotcms.publisher.receiver.handler;

import java.io.File;


public interface IHandler {
	
	public void handle(File bundleFolder) throws Exception;
	
	public String getName();
}
