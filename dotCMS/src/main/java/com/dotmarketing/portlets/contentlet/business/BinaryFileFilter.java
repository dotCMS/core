package com.dotmarketing.portlets.contentlet.business;

import java.io.FileFilter;

import com.dotmarketing.util.Config;

public class BinaryFileFilter implements FileFilter {


	public boolean accept(java.io.File pathname) {
		if(pathname.getName().contains(Config.GENERATED_FILE)){
			return false;
		}
		if(pathname.getName().startsWith(".")){
			return false;
		}
		else{
			return true;
		}
	}


}
