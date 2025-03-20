/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.bundlers;

import java.io.File;
import java.io.FileFilter;

import com.dotmarketing.util.ConfigUtils;


/**
 * This bundle will make a copy of a directory using hard links when
 * configuration is "incremental"
 * 
 * @author Michele Mastrogiovanni
 */
public class TimeMachineBundler extends DirectoryMirrorBundler {

	@Override
	public String getName() {
		return "TimeMachine Bundler";
	}
	
	@Override
	public File getDestinationFile(String destination){
		
		//return super.getDestinationFile( destination);
		
		return new File(ConfigUtils.getTimeMachinePath() + File.separator + destination);

	}
	
	@Override
	public FileFilter getFileFilter() {
		return new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				if(pathname.getAbsolutePath().contains("working")){
					return true;
					
				}
				return false;
			}
		};
	}
}
