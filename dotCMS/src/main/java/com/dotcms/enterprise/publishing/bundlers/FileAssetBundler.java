package com.dotcms.enterprise.publishing.bundlers;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.staticpublishing.StaticDependencyBundler;
import com.dotcms.publisher.util.DependencySet;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.IPublisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

public class FileAssetBundler implements IBundler {

	private PublisherConfig config;
	private IPublisher publisher;
	private LanguageAPI langAPI = null;
	private ContentletAPI conAPI = null;
	private UserAPI uAPI = null;
	private FileAssetAPI fAPI = null;
	private User systemUser = null;
	
	public final static String  FILE_ASSET_EXTENSION = ".fileAsset.xml" ;
	
	
	@Override
	public String getName() {
		return "File Asset Bundler";
	}
	
	@Override
	public void setConfig(PublisherConfig pc) {
		config = pc;
		langAPI = APILocator.getLanguageAPI();
		conAPI = APILocator.getContentletAPI();
		uAPI = APILocator.getUserAPI();
		fAPI = APILocator.getFileAssetAPI();
		try {
			systemUser = uAPI.getSystemUser();
		} catch (DotDataException e) {
			Logger.fatal(FileAssetBundler.class,e.getMessage(),e);
		}
	}

    @Override
    public void setPublisher(IPublisher publisher) {
    	this.publisher = publisher;
    }

	@Override
	public void generate(File bundleRoot,BundlerStatus status) throws DotBundleException {
	    if(LicenseUtil.getLevel()< LicenseLevel.STANDARD.level)
	        throw new RuntimeException("need an enterprise license to run this bundler");
	    
		
		StringBuilder bob = new StringBuilder("+structuretype:" + Structure.STRUCTURE_TYPE_FILEASSET + " ");
		
		if(config.getExcludePatterns() != null && config.getExcludePatterns().size()>0){
			bob.append("-(" );
			for (String p : config.getExcludePatterns()) {
				if(!UtilMethods.isSet(p)){
					continue;
				}
				//p = p.replace(" ", "+");
				bob.append("path:" + p + " ");
			}
			bob.append(")" );
		}else if(config.getIncludePatterns() != null && config.getIncludePatterns().size()>0){
			bob.append("+(" );
			for (String p : config.getIncludePatterns()) {
				if(!UtilMethods.isSet(p)){
					continue;
				}
				//p = p.replace(" ", "+");
				bob.append("path:" + p + " ");
			}
			bob.append(")" );
		} else {
        	// Ignore static-publishing over file-assets that are not part of include patterns (https://github.com/dotCMS/core/issues/10504)
        	if (config.isStatic()) {
        		return;
        	}
		}
		
		if(config.isIncremental()) {
			Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, 1900);
			
			Date start;
		    Date end;
		    
		    if(config.getStartDate() != null){
		        start = config.getStartDate();
		    } else {
                start = cal.getTime();                
	        }
		    
		    if(config.getEndDate() != null){
		    	end = config.getEndDate();
		    } else {
		    	end = cal.getTime();
		    }
            
	        	        
	        bob.append(" +versionTs:[" + ESMappingAPIImpl.datetimeFormat.format(start) 
	                + " TO " + ESMappingAPIImpl.datetimeFormat.format(end) +"] ");
		}
		
		
		if(config.getHosts() != null && config.getHosts().size() > 0){
			bob.append(" +(" );
			for(Host h : config.getHosts()){
				bob.append("conhost:" + h.getIdentifier() + " ");
			}
			bob.append(" ) " );
		}
		

		Logger.info(FileAssetBundler.class,bob.toString());


		for(Long languageId : getSortedConfigLanguages(this.config, langAPI.getDefaultLanguage().getId())){

			processFileAssets(bundleRoot, bob.toString() + "+languageid:" + languageId + " ", status);
		}
	}

	protected void processFileAssets(File bundleRoot, String luceneQuery, BundlerStatus status) throws DotBundleException {
		int limit = 200;
		int page = 0;

		status.setTotal(0);
		
		while(true){
			List<ContentletSearch> cs = new ArrayList<ContentletSearch>();
		
			try {
				cs.addAll(conAPI.searchIndex(luceneQuery + " +live:true", limit, page * limit, "inode", systemUser, true));
			}
			catch(Exception e){
				Logger.debug(FileAssetBundler.class,e.getMessage(),e);
			}
			try{
			    if(!config.liveOnly() || config.isIncremental())
			        cs.addAll(conAPI.searchIndex(luceneQuery + " +working:true", limit, page * limit, "inode", systemUser, true));
			} catch (Exception e) {
				Logger.debug(FileAssetBundler.class,e.getMessage(),e);
			}

			page++;
			
			// no more content
			if(cs.size() ==0 || page > 100000000){
				break;
			}
			
			
			status.setTotal(status.getTotal() + cs.size());
			List<String> inodes = new ArrayList<String>();
			for (ContentletSearch c : cs) {
				inodes.add(c.getInode());
			}
			List<FileAsset> assets = new ArrayList<FileAsset>();
			try {
				List<Contentlet> cons = conAPI.findContentlets(inodes);
				assets = fAPI.fromContentlets(cons);
			} catch (Exception e) {
				Logger.error(FileAssetBundler.class,e.getMessage(),e);
				throw new DotBundleException(this.getClass().getName() + " : " + "generate()" + e.getMessage() + ": Unable to retrieve content", e);
			}

			Optional<DependencySet> contents = new StaticDependencyBundler().getDependencySet(config, "content");
			
			for (FileAsset fileAsset : assets) {
				try {
					//We will check is already pushed only for static.
					if (contents.isPresent()){
						//If publisher reports that a file should be pushed (issue https://github.com/dotCMS/core/issues/10560)
						//For instance, in case bucket does not exist in a static push scenario
						if (publisher != null && publisher.shouldForcePush(fileAsset.getHost(), fileAsset.getLanguageId())) {
							writeFileAsset(bundleRoot, fileAsset);
						}
						//If we are able to add to DependencySet means that page will be pushed and we need to write file.
						//Send fileAsset.getIdentifier() because Push Publish standard.
						else if ( contents.get().addOrClean(fileAsset.getIdentifier(), fileAsset.getModDate()) ) {
							writeFileAsset(bundleRoot, fileAsset);
						} else {
							//If we are not able to add to DependencySet means that the file will not be generated and
							//the status total will be decreased.
							status.setTotal(status.getTotal() - 1);
						}
					} else {
						writeFileAsset(bundleRoot, fileAsset);
						status.addCount();
					}
				} catch (Exception e) {
					Logger.error(FileAssetBundler.class,e.getMessage() + " : Unable to write file",e);
					status.addFailure();
				}
			}
		}
	}

	private void writeFileAsset(File bundleRoot, FileAsset fileAsset) throws IOException, DotBundleException{
		try{
			Host host = APILocator.getHostAPI().find(fileAsset.getHost(), APILocator.getUserAPI().getSystemUser(), true);

			FileAssetWrapper wrap = new FileAssetWrapper();
			wrap.setAsset(fileAsset);
			wrap.setInfo(APILocator.getVersionableAPI().getContentletVersionInfo(fileAsset.getIdentifier(), fileAsset.getLanguageId()));
			wrap.setId(APILocator.getIdentifierAPI().find(fileAsset.getIdentifier()));

			// Replicate file-assets from default language across all languages (https://github.com/dotCMS/core/issues/10573)
			if (com.dotmarketing.util.Config.getBooleanProperty("DEFAULT_FILE_TO_DEFAULT_LANGUAGE", true) &&
				fileAsset.getLanguageId() == langAPI.getDefaultLanguage().getId() &&
				!this.config.getLanguages().isEmpty()
			) {

				for(String languageId : this.config.getLanguages()) {
					writeFileToDisk(host, languageId, bundleRoot, wrap);
				}

			} else {

				writeFileToDisk(host, String.valueOf(fileAsset.getLanguageId()), bundleRoot, wrap);
			}
		}
		catch(Exception e){
			throw new DotBundleException("cant get host for " + fileAsset + " reason " + e.getMessage());
		}
	}
	
	private void writeFileToDisk(Host host, String languageId, File bundleRoot, FileAssetWrapper fileAssetWrapper) throws IOException, DotDataException, DotSecurityException {		

		String liveworking = (fileAssetWrapper.getAsset().getInode().equals(fileAssetWrapper.getInfo().getLiveInode() )) ? "live" : "working";
		String myFile = bundleRoot.getPath() + File.separator 
				+liveworking + File.separator 
				+ host.getHostname() + File.separator + languageId
				+ fileAssetWrapper.getAsset().getURI().replace("/", File.separator) + FILE_ASSET_EXTENSION;

		
		// Should we write or is the file already there:
		Calendar cal = Calendar.getInstance();
		cal.setTime(fileAssetWrapper.getInfo().getVersionTs());
		cal.set(Calendar.MILLISECOND, 0);
		
		String dir = myFile.substring(0, myFile.lastIndexOf(File.separator));
		new File(dir).mkdirs();
		
		
		//only write if changed
		File f = new File(myFile);

    	/*
    	 * Inhibited xml-file generation for static-publishing scenario
    	 * Performance enhancement due to https://github.com/dotCMS/core/issues/12291
    	 */
		if( !config.isStatic() && (!f.exists() || f.lastModified() != cal.getTimeInMillis()) ){
		    if(f.exists()) f.delete(); // unlink possible existing hard link
			String x  = (String) fileAssetWrapper.getAsset().get("metaData");
			fileAssetWrapper.getAsset().setMetaData(x);
			BundlerUtil.objectToXML(fileAssetWrapper, f, true);
			f.setLastModified(cal.getTimeInMillis());
		}
		
		boolean deleteFile=config.liveOnly() && config.isIncremental() && !fileAssetWrapper.getAsset().isLive();
		
		f = new File(myFile.replaceAll(FILE_ASSET_EXTENSION,""));
		if(deleteFile) {
		    if(f.isFile()) {
		        f.delete();
		    }
		    f = new File(bundleRoot.getPath() + File.separator 
                    +"live" + File.separator 
                    + host.getHostname() + File.separator + languageId
                    + fileAssetWrapper.getAsset().getURI().replace("/", File.separator));
		    if(f.isFile()) {
                f.delete();
            }
		}
		else {
		    //only write if changed
			if(!f.exists() || f.lastModified() != cal.getTimeInMillis()){
				File oldAsset = new File(APILocator.getFileAssetAPI().getRealAssetPath(fileAssetWrapper.getAsset().getInode(), fileAssetWrapper.getAsset().getFileName()));
				if(f.exists()) f.delete();
				FileUtil.copyFile(oldAsset, f, true);
				f.setLastModified(cal.getTimeInMillis());
			}
		}		
	}
	
	@Override
	public FileFilter getFileFilter(){
		return new FileObjectBundlerFilter();
		
	}
	
	
	
	
	
	public class FileObjectBundlerFilter implements FileFilter{

		@Override
		public boolean accept(File pathname) {

			return (pathname.isDirectory() || pathname.getName().endsWith(FILE_ASSET_EXTENSION));
		}

	}
	
}
