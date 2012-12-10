package com.dotcms.publisher.receiver.handler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.FileUtils;

import com.dotcms.publisher.pusher.bundler.LanguageBundler;
import com.dotcms.publishing.DotPublishingException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.liferay.util.FileUtil;

public class LanguageHandler implements IHandler {
	@Override
	public String getName() {
		return this.getClass().getName();
	}
	
	@Override
	public void handle(File bundleFolder) throws Exception {
		//For each content take the wrapper and save it on DB
        Collection<File> messages = new ArrayList<File>();
        if(new File(bundleFolder + File.separator + "messages").exists()){
        	messages = FileUtil.listFilesRecursively(new File(bundleFolder + File.separator + "messages"), new LanguageBundler().getFileFilter());
        }
        
		handleMessages(messages);
	}
	
	private void handleMessages(Collection<File> messages) throws DotPublishingException, DotDataException, IOException{
		String messagesPath = APILocator.getFileAPI().getRealAssetPath() + File.separator + "messages";
		File messagesDir = new File(messagesPath);
		
		for(File language: messages)
			FileUtils.copyFileToDirectory(language, messagesDir, false);
	}
}
