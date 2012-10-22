package com.dotcms.publisher.myTest;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublisherAPI;
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
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

public class PushPublisherBundler implements IBundler {
	private PublisherConfig config;
	private User systemUser;
	ContentletAPI conAPI = null;
	UserAPI uAPI = null;
	PublisherAPI pubAPI = null;
	PublishAuditAPI pubAuditAPI = PublishAuditAPI.getInstance();
	
	@Override
	public String getName() {
		return "Push publisher bundler";
	}

	@Override
	public void setConfig(PublisherConfig pc) {
		config = pc;
		conAPI = APILocator.getContentletAPI();
		uAPI = APILocator.getUserAPI();
		pubAPI = PublisherAPI.getInstance();  
		
		try {
			systemUser = uAPI.getSystemUser();
		} catch (DotDataException e) {
			Logger.fatal(PushPublisherBundler.class,e.getMessage(),e);
		}
	}

	@Override
	public void generate(File bundleRoot, BundlerStatus status)
			throws DotBundleException {
		if(LicenseUtil.getLevel()<200)
	        throw new RuntimeException("need an enterprise license to run this bundler");
	    
		
		
		List<Contentlet> cs = new ArrayList<Contentlet>();
		
		Logger.info(PushPublisherBundler.class, config.getLuceneQuery());
		
		try {
			pubAuditAPI.updatePublishAuditStatus(config.getId(), PublishAuditStatus.Status.BUNDLING);
			cs = conAPI.search(config.getLuceneQuery(), 0, 0, "moddate", systemUser, false);
			
			status.setTotal(cs.size());
			
			for (Contentlet con : cs) {
				writeFileToDisk(bundleRoot, con);
				status.addCount();
			}
		} catch (Exception e) {
			try {
				pubAuditAPI.updatePublishAuditStatus(config.getId(), PublishAuditStatus.Status.FAILED_TO_BUNDLE);
			} catch (DotPublisherException e1) { }
			status.addFailure();
			
			throw new DotBundleException(this.getClass().getName() + " : " + "generate()" 
			+ e.getMessage() + ": Unable to pull content with query " + config.getLuceneQuery(), e);
		}
	}
	
	private void writeFileToDisk(File bundleRoot, Contentlet con) 
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
		
		//Find MultiTree
		wrapper.setMultiTree(pubAPI.getContentMultiTreeMatrix(con.getIdentifier()));
		
		//Find Tree
		wrapper.setTree(pubAPI.getContentTreeMatrix(con.getIdentifier()));
		
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
		
		String myFileUrl = bundleRoot.getPath() + File.separator 
				+liveworking + File.separator 
				+ h.getHostname() + File.separator + config.getLanguage()
				+ APILocator.getIdentifierAPI().find(con).getURI().replace("/", File.separator);
		
		pushContentFile = new File(myFileUrl);
		pushContentFile.mkdirs();
				
		BundlerUtil.objectToXML(wrapper, pushContentFile, true);
		pushContentFile.setLastModified(cal.getTimeInMillis());
	}
	
	@Override
	public FileFilter getFileFilter() {
		// TODO Auto-generated method stub
		return null;
	}

}
