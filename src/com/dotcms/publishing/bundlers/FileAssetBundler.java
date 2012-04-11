package com.dotcms.publishing.bundlers;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.Lists;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

public class FileAssetBundler implements IBundler {

	private PublisherConfig config;
	ContentletAPI conAPI = null;
	UserAPI uAPI = null;
	FileAssetAPI fAPI = null;
	User systemUser = null;

	public final static String  FILE_ASSET_EXTENSION = ".fileasset.xml" ;
	
	
	@Override
	public String getName() {
		return "File Asset Bundler";
	}
	
	@Override
	public void setConfig(PublisherConfig pc) {
		config = pc;
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
	public void generate(File bundleRoot,BundlerStatus status) throws DotBundleException {
		List<ContentletSearch> cs = new ArrayList<ContentletSearch>();
		StringBuilder bob = new StringBuilder("+languageid:" + config.getLanguage() + " +structuretype:" + Structure.STRUCTURE_TYPE_FILEASSET + " ");
		
		if(config.getExcludePatterns() != null && config.getExcludePatterns().size()>0){
			for (String p : config.getExcludePatterns()) {
				if(!UtilMethods.isSet(p)){
					continue;
				}
				p = p.replace(" ", "+");
				bob.append("-uri:" + p + " ");
			}
		}else if(config.getIncludePatterns() != null && config.getIncludePatterns().size()>0){
			for (String p : config.getIncludePatterns()) {
				if(!UtilMethods.isSet(p)){
					continue;
				}
				p = p.replace(" ", "+");
				bob.append("+uri:" + p + " ");
			}
		}
		
		
		if(config.getStartDate() != null){
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.YEAR, 2500);
			String start = ESMappingAPIImpl.datetimeFormat.format(config.getStartDate());
			String forever = ESMappingAPIImpl.datetimeFormat.format(cal.getTime());
			bob.append(" +moddate:[" + start + " TO " + forever +"] ");
		}
		
		if(config.getEndDate() != null){
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.YEAR, 1900);
			
			String end = ESMappingAPIImpl.datetimeFormat.format(config.getEndDate());
			String longAgo = ESMappingAPIImpl.datetimeFormat.format(cal.getTime());
			bob.append(" +moddate:[" + longAgo + " TO " + end +"] ");
		}
		
		
		
		if(config.getHosts() != null && config.getHosts().size() > 0){
			
			for(Host h : config.getHosts()){
				bob.append(" +conhost:" + h.getIdentifier() + " ");
			}
		}
		
		
		
		
		
		Logger.info(FileAssetBundler.class,bob.toString());
		
		try {
			cs = conAPI.searchIndex(bob.toString() + "+live:true", 0, 0, "moddate", systemUser, true);
			if(!config.liveOnly()){
				cs.addAll(conAPI.searchIndex(bob.toString() + "+working:true", 0, 0, "moddate", systemUser, true));
			}
		} catch (Exception e) {
			
			Logger.error(FileAssetBundler.class,e.getMessage(),e);
			throw new DotBundleException(this.getClass().getName() + " : " + "generate()" + e.getMessage() + ": Unable to pull content with query " + bob.toString(), e);
		}
		
		List<List<ContentletSearch>> listsOfCS = Lists.partition(cs, 500);
		for (List<ContentletSearch> l : listsOfCS) {
			List<String> inodes = new ArrayList<String>();
			for (ContentletSearch c : l) {
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
			for (FileAsset fileAsset : assets) {
				try {
					writeFileToDisk(bundleRoot, fileAsset);
					status.addCount();
				} catch (Exception e) {
					Logger.error(FileAssetBundler.class,e.getMessage() + " : Unable to write file",e);
					status.addFailuer();
				}
			}
		}
		
	}


	
	
	private void writeFileToDisk(File bundleRoot, FileAsset fileAsset) throws IOException, DotBundleException{
		
		
		Host h = null;
		try{
			h = APILocator.getHostAPI().find(fileAsset.getHost(), APILocator.getUserAPI().getSystemUser(), true);

			
			FileAssetWrapper wrap = new FileAssetWrapper();
			wrap.setAsset(fileAsset);
			wrap.setInfo(APILocator.getVersionableAPI().getContentletVersionInfo(fileAsset.getIdentifier(), fileAsset.getLanguageId()));
			wrap.setId(APILocator.getIdentifierAPI().find(fileAsset.getIdentifier()));
			
			
			
			
			
			String liveworking = (fileAsset.getInode().equals(wrap.getInfo().getLiveInode() )) ? "live" : "working";
			String myFile = bundleRoot.getPath() + File.separator 
					+liveworking + File.separator 
					+ h.getHostname() 
					+ fileAsset.getURI().replace("/", File.separator) + FILE_ASSET_EXTENSION;

			
			// Should we write or is the file already there:
			Calendar cal = Calendar.getInstance();
			cal.setTime(fileAsset.getModDate());
			cal.set(Calendar.MILLISECOND, 0);
			
			//only write if changed
			File f = new File(myFile);
			if(!f.exists() || f.lastModified() != cal.getTimeInMillis()){
				String dir = myFile.substring(0, myFile.lastIndexOf(File.separator));
				new File(dir).mkdirs();
				String x  = (String) fileAsset.get("metaData");
				fileAsset.setMetaData(x);
				BundlerUtil.objectToXML(wrap, f);
				f.setLastModified(cal.getTimeInMillis());
			}
			
			//only write if changed
			f = new File(myFile.replaceAll(FILE_ASSET_EXTENSION,""));
			if(!f.exists() || f.lastModified() != cal.getTimeInMillis()){
				File oldAsset = new File(APILocator.getFileAssetAPI().getRealAssetPath(fileAsset.getInode(), fileAsset.getFileName()));
				FileUtil.copyFile(oldAsset, f);
				f.setLastModified(cal.getTimeInMillis());
			}
			// set the time of the file
			
		}
		catch(Exception e){
			throw new DotBundleException("cant get host for " + fileAsset + " reason " + e.getMessage());
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
