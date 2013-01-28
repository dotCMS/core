package com.dotcms.publisher.pusher.bundler;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.PushContentWrapper;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

public class ContentBundler implements IBundler {
	private PushPublisherConfig config;
	private User systemUser;
	ContentletAPI conAPI = null;
	UserAPI uAPI = null;
	PublisherAPI pubAPI = null;
	PublishAuditAPI pubAuditAPI = PublishAuditAPI.getInstance();
	FolderAPI fAPI = APILocator.getFolderAPI();
	
	public final static String CONTENT_EXTENSION = ".content.xml" ;

	@Override
	public String getName() {
		return "Content bundler";
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
			Logger.fatal(ContentBundler.class,e.getMessage(),e);
		}
	}

	@Override
	public void generate(File bundleRoot, BundlerStatus status)
			throws DotBundleException {
		if(LicenseUtil.getLevel()<400)
	        throw new RuntimeException("need an enterprise prime license to run this bundler");

		PublishAuditHistory currentStatusHistory = null;
		try {
			//Updating audit table
			currentStatusHistory = pubAuditAPI.getPublishAuditStatus(config.getId()).getStatusPojo();
			
			currentStatusHistory.setBundleStart(new Date());
			pubAuditAPI.updatePublishAuditStatus(config.getId(), PublishAuditStatus.Status.BUNDLING, currentStatusHistory);
			
			Set<String> contentsIds = config.getContentlets();
			
			if(UtilMethods.isSet(contentsIds) && !contentsIds.isEmpty()) { // this content set is a dependency of other assets, like htmlpages
				Set<Contentlet> contents = new HashSet<Contentlet>();
				for (String contentIdentifier : contentsIds) {
					contents.addAll(conAPI.search("+identifier:"+contentIdentifier+" +live:true +deleted:false", 0, -1, null, systemUser, false));
					contents.addAll(conAPI.search("+identifier:"+contentIdentifier+" +working:true +deleted:false", 0, -1, null, systemUser, false));
				}
				
				//Delete duplicate
				Set<ContentletUniqueWrapper> contentsToProcessFinal = new HashSet<ContentletUniqueWrapper>();
				for(Contentlet con: contents) {
					contentsToProcessFinal.add(new ContentletUniqueWrapper(con));
				}
				
				Iterator<ContentletUniqueWrapper> it = contentsToProcessFinal.iterator();
				for (int ii = 0; it.hasNext(); ii++) {
					Contentlet con = it.next().getContentlet();
					writeFileToDisk(bundleRoot, con, ii);
					status.addCount();
				}
			} 

			//Updating audit table
			currentStatusHistory = pubAuditAPI.getPublishAuditStatus(config.getId()).getStatusPojo();
			
			currentStatusHistory.setBundleEnd(new Date());
			pubAuditAPI.updatePublishAuditStatus(config.getId(), PublishAuditStatus.Status.BUNDLING, currentStatusHistory);
			

		} catch (Exception e) {
			try {
				pubAuditAPI.updatePublishAuditStatus(config.getId(), PublishAuditStatus.Status.FAILED_TO_BUNDLE, currentStatusHistory);
			} catch (DotPublisherException e1) { }
			status.addFailure();

			throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
			+ e.getMessage() + ": Unable to pull content", e);
		}
	}


	private void writeFileToDisk(File bundleRoot, Contentlet con, int countOrder)
			throws IOException, DotBundleException, DotDataException,
				DotSecurityException, DotPublisherException
	{
		
		Calendar cal = Calendar.getInstance();
		File pushContentFile = null;
		Host h = null;

		//Populate wrapper
		ContentletVersionInfo info = APILocator.getVersionableAPI().getContentletVersionInfo(con.getIdentifier(), con.getLanguageId());
		h = APILocator.getHostAPI().find(con.getHost(), APILocator.getUserAPI().getSystemUser(), true);

		PushContentWrapper wrapper=new PushContentWrapper();
	    wrapper.setContent(con);
		wrapper.setInfo(info);
		wrapper.setId(APILocator.getIdentifierAPI().find(con.getIdentifier()));
		wrapper.setTags(APILocator.getTagAPI().getTagsByInode(con.getInode()));
		wrapper.setOperation(config.getOperation());

		//Find MultiTree
		wrapper.setMultiTree(pubAPI.getContentMultiTreeMatrix(con.getIdentifier()));

        //Find Tree
        List<Map<String, Object>> contentTreeMatrix = pubAPI.getContentTreeMatrix( con.getIdentifier() );
        //Now add the categories, we will find categories by inode NOT by identifier
        contentTreeMatrix.addAll( pubAPI.getContentTreeMatrix( con.getInode() ) );
        wrapper.setTree( contentTreeMatrix );

		//Copy asset files to bundle folder keeping original folders structure
		List<Field> fields=FieldsCache.getFieldsByStructureInode(con.getStructureInode());
		File assetFolder = new File(bundleRoot.getPath()+File.separator+"assets");
		String inode=con.getInode();
		for(Field ff : fields) {
			if(ff.getFieldType().toString().equals(Field.FieldType.BINARY.toString())) {
				File sourceFile = con.getBinary( ff.getVelocityVarName());

				if(sourceFile != null && sourceFile.exists()) {
					if(!assetFolder.exists())
						assetFolder.mkdir();

					String folderTree = inode.charAt(0)+File.separator+inode.charAt(1)+File.separator+
					        inode+File.separator+ff.getVelocityVarName()+File.separator+sourceFile.getName();

					File destFile = new File(assetFolder, folderTree);
		            destFile.getParentFile().mkdirs();
		            FileUtil.copyFile(sourceFile, destFile);
				}
		    } 

		}

		String liveworking = con.isLive() ? "live" :  "working";

		String uri = APILocator.getIdentifierAPI().find(con).getURI().replace("/", File.separator);
		if(!uri.endsWith(CONTENT_EXTENSION)){
			uri.replace(CONTENT_EXTENSION, "");
			uri.trim();
			uri += CONTENT_EXTENSION;
		}
		uri = uri.replace(uri.substring(uri.lastIndexOf(File.separator)+1, uri.length()), countOrder +"-"+ uri.substring(uri.lastIndexOf(File.separator)+1, uri.length()));
		
		String assetName = APILocator.getFileAssetAPI().isFileAsset(con)?(File.separator + countOrder +"-" +con.getInode() + CONTENT_EXTENSION):uri;

		String myFileUrl = bundleRoot.getPath() + File.separator
				+liveworking + File.separator
				+ h.getHostname() + File.separator + 
				+ con.getLanguageId() + assetName;

		pushContentFile = new File(myFileUrl);
		pushContentFile.mkdirs();

		BundlerUtil.objectToXML(wrapper, pushContentFile, true);
		pushContentFile.setLastModified(cal.getTimeInMillis());
		
	}

	@Override
	public FileFilter getFileFilter(){
		return new ContentBundlerFilter();
	}
	
	public class ContentBundlerFilter implements FileFilter{

		@Override
		public boolean accept(File pathname) {

			return (pathname.isDirectory() || pathname.getName().endsWith(CONTENT_EXTENSION));
		}

	}
	
}

class ContentletUniqueWrapper {
	private Contentlet contentlet;
	
	public ContentletUniqueWrapper (Contentlet contentlet) {
		this.contentlet = contentlet;
	}
	
	public Contentlet getContentlet() {
		return contentlet;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((contentlet == null) ? 0 : contentlet.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ContentletUniqueWrapper other = (ContentletUniqueWrapper) obj;
		if (contentlet == null) {
			if (other.contentlet != null)
				return false;
		} else if (!contentlet.getInode().equals(other.contentlet.getInode()))
			return false;
		return true;
	}
	
}
