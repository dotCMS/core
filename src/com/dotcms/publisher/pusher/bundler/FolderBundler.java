package com.dotcms.publisher.pusher.bundler;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.FolderWrapper;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
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
	public void generate(File bundleRoot, BundlerStatus status)
			throws DotBundleException {
		if(LicenseUtil.getLevel()<400)
	        throw new RuntimeException("need an enterprise prime license to run this bundler");

		Set<String> folders = config.getFolders();
		
		try {
			for (String folder : folders) {
				writeFolderTree(bundleRoot, folder);
			}
		} catch (Exception e) {
			status.addFailure();

			throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
			+ e.getMessage() + ": Unable to pull content", e);
		}
		
	}

	
	
	private void writeFolderTree(File bundleRoot, String idFolder)
			throws IOException, DotBundleException, DotDataException,
			DotSecurityException, DotPublisherException
	{
		//Get Folder tree
		Folder folder = fAPI.find(idFolder, systemUser, false);
		List<String> path = new ArrayList<String>();
		List<FolderWrapper> folderWrappers = new ArrayList<FolderWrapper>();
		while(folder != null && !folder.getName().equals(FolderAPI.SYSTEM_FOLDER)) {
			path.add(folder.getName());
			
			Host host =  APILocator.getHostAPI().find(folder.getHostId(), systemUser, false);
			folderWrappers.add(
					new FolderWrapper(folder, 
							APILocator.getIdentifierAPI().find(folder.getIdentifier()),
							host,
							APILocator.getIdentifierAPI().find(host.getIdentifier())));
			
			folder = fAPI.findParentFolder(folder, systemUser, false);
		}
		
		if(path.size() > 0) {
			Collections.reverse(path);
			Collections.reverse(folderWrappers);
			StringBuilder b = new StringBuilder(File.separator);
			for (String f : path) {
				b.append(f);
				b.append(File.separator);
				
				String myFolderUrl = bundleRoot.getPath() + 
						File.separator + "ROOT" +
						b.toString();
				
				File fsFolder = new File(myFolderUrl);
				
				if(!fsFolder.exists())
					fsFolder.mkdirs();
				
				FolderWrapper wrapper = folderWrappers.remove(0);
				String myFileUrl = fsFolder.getParent()+ File.separator +
						wrapper.getFolder().getIdentifier()+FOLDER_EXTENSION;
				
				File fileWrapper = new File(myFileUrl);
				
				BundlerUtil.objectToXML(wrapper, fileWrapper, true);
			}
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
