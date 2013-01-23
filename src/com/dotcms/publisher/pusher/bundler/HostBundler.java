package com.dotcms.publisher.pusher.bundler;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.HostWrapper;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

public class HostBundler implements IBundler {
	private PushPublisherConfig config;
	private User systemUser;
	ContentletAPI conAPI = null;
	UserAPI uAPI = null;
	PublisherAPI pubAPI = null;
	PublishAuditAPI pubAuditAPI = PublishAuditAPI.getInstance();
	FolderAPI fAPI = APILocator.getFolderAPI();
	
	public final static String HOST_EXTENSION = ".host.xml" ;

	@Override
	public String getName() {
		return "Host bundler";
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
			Logger.fatal(HostBundler.class,e.getMessage(),e);
		}
	}

	@Override
	public void generate(File bundleRoot, BundlerStatus status)
			throws DotBundleException {
		if(LicenseUtil.getLevel()<400)
	        throw new RuntimeException("need an enterprise prime license to run this bundler");

		try {
			//Updating audit table
			
			Set<String> contents = config.getHostSet();
			
			if(UtilMethods.isSet(contents) && !contents.isEmpty()) { // this content set is a dependency of other assets, like htmlpages
				List<Contentlet> contentList = new ArrayList<Contentlet>();
				for (String contentIdentifier : contents) {
					try{
						contentList.add(APILocator.getContentletAPI().findContentletByIdentifier(contentIdentifier, true, APILocator.getLanguageAPI().getDefaultLanguage().getId(), systemUser, false));
					}catch (Exception e) {
						try{
							contentList.add(APILocator.getContentletAPI().findContentletByIdentifier(contentIdentifier, false, APILocator.getLanguageAPI().getDefaultLanguage().getId(), systemUser, false));
						}catch (Exception e1) {	}
					}
				}
				Set<Contentlet> contentsToProcessWithFiles = getRelatedFilesAndContent(contentList);
				
				for (Contentlet con : contentsToProcessWithFiles) {
					writeFileToDisk(bundleRoot, con);
					status.addCount();
				}
			} 


		} catch (Exception e) {
			status.addFailure();

			throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
			+ e.getMessage() + ": Unable to pull content", e);
		}
	}

	private Set<Contentlet> getRelatedFilesAndContent(List<Contentlet> cs) throws DotDataException,
			DotSecurityException {
		
		Set<Contentlet> contentsToProcess = new HashSet<Contentlet>();
		
		//Getting all related content
		for (Contentlet con : cs) {
			Map<Relationship, List<Contentlet>> contentRel =
					conAPI.findContentRelationships(con, systemUser);
			
			for (Relationship rel : contentRel.keySet()) {
				contentsToProcess.addAll(contentRel.get(rel));
			}
			
			contentsToProcess.add(con);
		}
		
		Set<Contentlet> contentsToProcessWithFiles = new HashSet<Contentlet>();
		//Getting all linked files
		for(Contentlet con: contentsToProcess) {
			List<Field> fields=FieldsCache.getFieldsByStructureInode(con.getStructureInode());
			for(Field ff : fields) {
				if(ff.getFieldType().toString().equals(Field.FieldType.FILE.toString())) {
					String identifier = (String) con.get(ff.getVelocityVarName());
					contentsToProcessWithFiles.addAll(conAPI.search("+identifier:"+identifier, 0, -1, null, systemUser, false));
			    }
			}
			contentsToProcessWithFiles.add(con);
		}
		return contentsToProcessWithFiles;
	}

	private void writeFileToDisk(File bundleRoot, Contentlet host)
			throws IOException, DotBundleException, DotDataException,
				DotSecurityException, DotPublisherException
	{
		
		Calendar cal = Calendar.getInstance();
		File pushContentFile = null;
		Host h = null;

		//Populate wrapper
		ContentletVersionInfo info = APILocator.getVersionableAPI().getContentletVersionInfo(host.getIdentifier(), host.getLanguageId());
		h = APILocator.getHostAPI().find(host.getHost(), APILocator.getUserAPI().getSystemUser(), true);

		HostWrapper wrapper=new HostWrapper();
	    wrapper.setContent(host);
		wrapper.setInfo(info);
		wrapper.setId(APILocator.getIdentifierAPI().find(host.getIdentifier()));
		wrapper.setTags(APILocator.getTagAPI().getTagsByInode(host.getInode()));
		wrapper.setOperation(config.getOperation());

		//Find MultiTree
		wrapper.setMultiTree(pubAPI.getContentMultiTreeMatrix(host.getIdentifier()));

        //Find Tree
        List<Map<String, Object>> contentTreeMatrix = pubAPI.getContentTreeMatrix( host.getIdentifier() );
        //Now add the categories, we will find categories by inode NOT by identifier
        contentTreeMatrix.addAll( pubAPI.getContentTreeMatrix( host.getInode() ) );
        wrapper.setTree( contentTreeMatrix );

		//Copy asset files to bundle folder keeping original folders structure
		List<Field> fields=FieldsCache.getFieldsByStructureInode(host.getStructureInode());
		File assetFolder = new File(bundleRoot.getPath()+File.separator+"assets");
		String inode=host.getInode();
		for(Field ff : fields) {
			if(ff.getFieldType().toString().equals(Field.FieldType.BINARY.toString())) {
				File sourceFile = host.getBinary( ff.getVelocityVarName());

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

		String liveworking = host.isLive() ? "live" :  "working";

		String uri = APILocator.getIdentifierAPI().find(host).getURI().replace("/", File.separator);
		if(!uri.endsWith(HOST_EXTENSION)){
			uri.replace(HOST_EXTENSION, "");
			uri.trim();
			uri += HOST_EXTENSION;
		}
		String assetName = APILocator.getFileAssetAPI().isFileAsset(host)?(File.separator + host.getInode() + HOST_EXTENSION):uri;

		String myFileUrl = bundleRoot.getPath() + File.separator
				+liveworking + File.separator
				+ h.getHostname() + File.separator
				+ host.getLanguageId() + assetName;

		pushContentFile = new File(myFileUrl);
		pushContentFile.mkdirs();

		BundlerUtil.objectToXML(wrapper, pushContentFile, true);
		pushContentFile.setLastModified(cal.getTimeInMillis());
		
		Set<String> htmlIds = PublisherUtil.getPropertiesSet(wrapper.getMultiTree(), "parent1");
		Set<String> containerIds = PublisherUtil.getPropertiesSet(wrapper.getMultiTree(), "parent2");
		
		// adding content dependencies only if pushing content explicitly, not when it is a dependency of other asset
		if(!UtilMethods.isSet(config.getContentlets()) || config.getContentlets().isEmpty()) 
			addToConfig(host.getFolder(), htmlIds, containerIds, host.getStructureInode());
		
	}

	@Override
	public FileFilter getFileFilter(){
		return new HostBundlerFilter();
	}
	
	public class HostBundlerFilter implements FileFilter{

		@Override
		public boolean accept(File pathname) {

			return (pathname.isDirectory() || pathname.getName().endsWith(HOST_EXTENSION));
		}

	}
	
	private void addToConfig(String folder, Set<String> htmlPages, Set<String> containers, String structure) 
			throws DotStateException, DotHibernateException, DotDataException, DotSecurityException
	{
		//Get Id from folder
		if(Config.getBooleanProperty("PUSH_PUBLISHING_PUSH_ALL_FOLDER_PAGES")) {
			List<HTMLPage> folderHtmlPages = APILocator.getHTMLPageAPI().findLiveHTMLPages(
					APILocator.getFolderAPI().find(folder, systemUser, false));
			folderHtmlPages.addAll(APILocator.getHTMLPageAPI().findWorkingHTMLPages(
					APILocator.getFolderAPI().find(folder, systemUser, false)));
			for(HTMLPage htmlPage: folderHtmlPages) {
				config.getHTMLPages().add(htmlPage.getIdentifier());
			}
		}
		
		config.getHTMLPages().addAll(htmlPages);

		config.getFolders().add(folder);

		config.getContainers().addAll(containers);
		
		if(Config.getBooleanProperty("PUSH_PUBLISHING_PUSH_STRUCTURES")) {
			config.getStructures().add(structure);
		}
		
	}

}
