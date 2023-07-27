package com.dotmarketing.portlets.cmsmaintenance.util;

import com.dotmarketing.util.*;
import com.google.common.hash.BloomFilter;
import com.liferay.util.FileUtil;
import io.vavr.Lazy;

import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This {@link FileFilter} implementation provides the actual asset files in the current dotCMS repository. By default,
 * all files and folders that are unnecessary, temporary or application-specific will be excluded.
 *
 * @author Will Ezell
 * @since Dec 4th, 2012
 */
public class AssetFileNameFilter implements FileFilter {

	public AssetFileNameFilter(BloomFilter<String> inodeFilter){
		this.bloomFilter=inodeFilter;
	}
	public AssetFileNameFilter(){
		this(null);
	}

	private final BloomFilter<String> bloomFilter;


    private static final String[] EXCLUDE_FOLDERS_LIST = { "license", "bundles", "tmp_upload",
                    "timemachine", "integrity", "server", "dotGenerated", "monitor"};

	private static final String[] EXCLUDE_FILE_LIST = {"license.zip", ".DS_Store", "license_pack.zip"};


	private Lazy<String[]> excludeFolders = Lazy.of(()->{
		return Config.getStringArrayProperty("ASSET_DOWNLOAD_EXCLUDE_FOLDERS", EXCLUDE_FOLDERS_LIST);
	});

	private Lazy<String[]> excludeFiles = Lazy.of(()->{
		return Config.getStringArrayProperty("ASSET_DOWNLOAD_EXCLUDE_FILES", EXCLUDE_FILE_LIST);
	});


	private Set<String> excludedFolders;


	private final String root = ConfigUtils.getAbsoluteAssetsRootPath().endsWith("/") ? ConfigUtils.getAbsoluteAssetsRootPath().substring(0,ConfigUtils.getAbsoluteAssetsRootPath().lastIndexOf("/")) :ConfigUtils.getAbsoluteAssetsRootPath();


	@Override
	public boolean accept(final File dir) {
		if(dir ==null){
			return false;
		}

		String pathName = dir.getAbsolutePath().replace(root, "");


        if(dir.isDirectory() && Set.of(this.excludeFolders.get()).stream ().anyMatch(f->pathName.contains(f))){
			return false;
		}
		if(dir.isFile() && Set.of(this.excludeFiles.get()).stream().anyMatch(f->pathName.contains(f  ))){
			return false;
		}


		// if no bloomFilter, everything else is a go
		if(bloomFilter==null){
			return true;
		}

		//Always allow messages (language property files)
		if(pathName.startsWith("/messages")){
			return true;
		}



		// if bloomFilter, make sure the inode is in the path
		List<RegExMatch> matches = RegEX.find(pathName, "[\\w]{8}(-[\\w]{4}){3}-[\\w]{12}");
		if(matches.isEmpty()){
			return true;
		}
		return (bloomFilter.mightContain(matches.get(0).getMatch()));


	}

}
