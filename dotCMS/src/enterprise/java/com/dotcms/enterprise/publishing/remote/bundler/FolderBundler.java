/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

package com.dotcms.enterprise.publishing.remote.bundler;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.FolderWrapper;
import com.dotcms.publishing.*;
import com.dotcms.publishing.output.BundleOutput;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.liferay.portal.model.User;

public class FolderBundler implements IBundler {
	private PushPublisherConfig config;
	private User systemUser;
	ContentletAPI conAPI = null;
	UserAPI uAPI = null;
	PublisherAPI pubAPI = null;
	PublishAuditAPI pubAuditAPI = PublishAuditAPI.getInstance();
	FolderAPI fAPI = APILocator.getFolderAPI();

	public final static String FOLDER_EXTENSION = ".folder.xml" ;

	@Override
	public String getName() {
		return "Folder bundler";
	}

	@Override
	public void setConfig(PublisherConfig pc) {
		config = (PushPublisherConfig) pc;
		conAPI = APILocator.getContentletAPI();
		uAPI = APILocator.getUserAPI();
		pubAPI = PublisherAPI.getInstance();

		try {
			systemUser = uAPI.getSystemUser();
		} catch (DotDataException e) {
			Logger.fatal(FolderBundler.class,e.getMessage(),e);
		}
	}

    @Override
    public void setPublisher(IPublisher publisher) {
    }

	@Override
	public void generate(final BundleOutput output, final BundlerStatus status)
			throws DotBundleException {
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level)
	        throw new RuntimeException("need an enterprise pro license to run this bundler");

		Set<String> folders = config.getFolders();

		try {
			for (String folder : folders) {
				writeFolderTree(output, folder);
			}
		} catch (Exception e) {
			status.addFailure();

			throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
			+ e.getMessage() + ": Unable to pull content", e);
		}

	}



	private void writeFolderTree(BundleOutput bundleOutput, String idFolder)
			throws IOException, DotBundleException, DotDataException,
			DotSecurityException, DotPublisherException
	{
		//Get Folder tree
		Folder folder = fAPI.find(idFolder, systemUser, false);
		String folderName = folder.getName();
		List<String> path = new ArrayList<>();
		List<FolderWrapper> folderWrappers = new ArrayList<>();
		while(folder != null && !folder.isSystemFolder()) {
			path.add(folder.getName());

			// include parent folders only when publishing
			if(config.getOperation().equals(PushPublisherConfig.Operation.PUBLISH) || folder.getName().equals(folderName)) {
				
				Host host =  APILocator.getHostAPI().find(folder.getHostId(), systemUser, false);
				folderWrappers.add(
						new FolderWrapper(folder,
								APILocator.getIdentifierAPI().find(folder.getIdentifier()),
								host,
								APILocator.getIdentifierAPI().find(host.getIdentifier()),config.getOperation()));
			}

			folder = fAPI.findParentFolder(folder, systemUser, false);
		}

		if(path.size() > 0) {
			Collections.reverse(path);
			Collections.reverse(folderWrappers);
			StringBuilder b = new StringBuilder(File.separator);
			for (String f : path) {
				b.append(f);
				b.append(File.separator);
				
				// exclude other folders but the one being pushed, when unpublishing
				if(config.getOperation().equals(PushPublisherConfig.Operation.UNPUBLISH) && !f.equals(folderName)) {
					continue;
				}

				String myFolderUrl = File.separator + "ROOT" +
						b.toString();

				File fsFolder = new File(myFolderUrl);
				bundleOutput.mkdirs(myFolderUrl);

				FolderWrapper wrapper = folderWrappers.remove(0);
				String myFileUrl = fsFolder.getParent()+ File.separator +
						wrapper.getFolder().getIdentifier()+FOLDER_EXTENSION;

				try (final OutputStream outputStream = bundleOutput.addFile(myFileUrl)) {
					BundlerUtil.objectToXML(wrapper, outputStream);
				}
			}
		}

		if(Config.getBooleanProperty("PUSH_PUBLISHING_LOG_DEPENDENCIES", false)) {
			PushPublishLogger.log(getClass(), "Folder bundled for pushing. Operation: "+config.getOperation()+", Id: "+ idFolder, config.getId());
		}
	}

	@Override
	public FileFilter getFileFilter(){
		return new FolderBundlerFilter();
	}

	public class FolderBundlerFilter implements FileFilter{

		@Override
		public boolean accept(File pathname) {

			return (pathname.isDirectory() || pathname.getName().endsWith(FOLDER_EXTENSION));
		}

	}

}
