package com.dotcms.publisher.pusher.bundler;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Set;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.PushPublisherConfig.Operation;
import com.dotcms.publisher.pusher.wrapper.StructureWrapper;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

public class StructureBundler implements IBundler {
	private PushPublisherConfig config;
	private User systemUser;
	ContentletAPI conAPI = null;
	UserAPI uAPI = null;
	PublisherAPI pubAPI = null;
	PublishAuditAPI pubAuditAPI = PublishAuditAPI.getInstance();
	FolderAPI fAPI = APILocator.getFolderAPI();
	
	public final static String STRUCTURE_EXTENSION = ".structure.xml" ;

	@Override
	public String getName() {
		return "Structure bundler";
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
	public void generate(File bundleRoot, BundlerStatus status) throws DotBundleException {
		if(LicenseUtil.getLevel()<400)
	        throw new RuntimeException("need an enterprise prime license to run this bundler");

		Set<String> structures = config.getStructures();
		
		try {
			for (String str : structures) {
				Structure s = StructureCache.getStructureByInode(str);
				
				writeStructure(bundleRoot, s, config.getOperation());
			}
		} catch (Exception e) {
			status.addFailure();

			throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
			+ e.getMessage() + ": Unable to pull content", e);
		}
		
	}

	
	
	private void writeStructure(File bundleRoot, Structure structure, Operation op)
			throws IOException, DotBundleException, DotDataException,
			DotSecurityException, DotPublisherException
	{
		StructureWrapper wrapper = 
				new StructureWrapper(structure, 
						FieldsCache.getFieldsByStructureInode(structure.getInode()));
		wrapper.setOperation(op);
		wrapper.setWorkflowSchemaId(APILocator.getWorkflowAPI().findSchemeForStruct(structure).getId());
		
		String liveworking = structure.isLive() ? "live" :  "working";

		String uri = structure.getInode();
		if(!uri.endsWith(STRUCTURE_EXTENSION)){
			uri.replace(STRUCTURE_EXTENSION, "");
			uri.trim();
			uri += STRUCTURE_EXTENSION;
		}
		
		Host h = APILocator.getHostAPI().find(structure.getHost(), systemUser, false);
		
		String myFileUrl = bundleRoot.getPath() + File.separator
				+liveworking + File.separator
				+ h.getHostname() +File.separator + uri;

		File strFile = new File(myFileUrl);
		strFile.mkdirs();

		BundlerUtil.objectToXML(wrapper, strFile, true);
		strFile.setLastModified(Calendar.getInstance().getTimeInMillis());
	}

	@Override
	public FileFilter getFileFilter(){
		return new FolderBundlerFilter();
	}
	
	public class FolderBundlerFilter implements FileFilter{

		@Override
		public boolean accept(File pathname) {

			return (pathname.isDirectory() || pathname.getName().endsWith(STRUCTURE_EXTENSION));
		}

	}
}
