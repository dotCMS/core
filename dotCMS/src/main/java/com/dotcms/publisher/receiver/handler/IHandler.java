package com.dotcms.publisher.receiver.handler;

import java.io.File;
import java.util.Collections;
import java.util.List;


public interface IHandler {
	
	public void handle(File bundleFolder) throws Exception;
	
	public String getName();

	default List<String> getWarnings(){
        return Collections.emptyList();
    }
}