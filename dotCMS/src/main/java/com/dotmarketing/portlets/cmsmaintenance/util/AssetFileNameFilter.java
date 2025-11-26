package com.dotmarketing.portlets.cmsmaintenance.util;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.google.common.hash.BloomFilter;
import com.liferay.util.StringPool;
import io.vavr.Lazy;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

/**
 * This {@link FileFilter} implementation provides the actual asset files in the current dotCMS
 * repository. By default, all files and folders that are temporary, symlinks, or
 * application-specific, will be excluded from the filtered list. For more details, please see
 * {@link #EXCLUDE_FOLDERS_LIST} and {@link #EXCLUDE_FILE_LIST}.
 * <p>If additional files or folders need to be excluded in the future, the following configuration
 * properties can be set, which take a comma-separated list of values:</p>
 * <ul>
 *     <li>{@code ASSET_DOWNLOAD_EXCLUDE_FOLDERS}</li>
 *     <li>{@code ASSET_DOWNLOAD_EXCLUDE_FILES}</li>
 * </ul>
 *
 * @author Will Ezell
 * @since Dec 4th, 2012
 */
public class AssetFileNameFilter implements FileFilter {

	public AssetFileNameFilter(final BloomFilter<String> inodeFilter) {
		this(inodeFilter, -1);
	}
	public AssetFileNameFilter(final BloomFilter<String> inodeFilter,  long maxFileSize) {
		this.bloomFilter = inodeFilter;
		this.maxFileSize = maxFileSize;
	}

	public final long maxFileSize;

	public AssetFileNameFilter(long maxFileSize){
		this(null,maxFileSize);
	}

	private final BloomFilter<String> bloomFilter;

    private static final String[] EXCLUDE_FOLDERS_LIST = { "license", "bundles", "tmp_upload",
                    "timemachine", "integrity", "server", "dotGenerated", "monitor" };

	private static final String[] EXCLUDE_FILE_LIST = {"license.zip", ".DS_Store", "license_pack.zip"};

	private final Lazy<String[]> excludeFolders = Lazy.of(()-> Config.getStringArrayProperty("ASSET_DOWNLOAD_EXCLUDE_FOLDERS", EXCLUDE_FOLDERS_LIST));

	private final Lazy<String[]> excludeFiles = Lazy.of(()-> Config.getStringArrayProperty("ASSET_DOWNLOAD_EXCLUDE_FILES", EXCLUDE_FILE_LIST));

	private final String root = ConfigUtils.getAssetPath().endsWith(StringPool.FORWARD_SLASH) ?
			ConfigUtils.getAssetPath().substring(0,
					ConfigUtils.getAssetPath().lastIndexOf(StringPool.FORWARD_SLASH)) :
			ConfigUtils.getAssetPath();

	@Override
	public boolean accept(final File dir) {
		if (dir == null || Files.isSymbolicLink(dir.toPath())) {
			return false;
		}
		final String pathName = dir.getAbsolutePath().replace(root, StringPool.BLANK);
		if (dir.isDirectory() && Set.of(this.excludeFolders.get()).stream().anyMatch(pathName::contains)) {
			return false;
		}
		if (dir.isFile() && Set.of(this.excludeFiles.get()).stream().anyMatch(pathName::contains)) {
			return false;
		}
		if (maxFileSize > 0 && dir.length() > maxFileSize) {
			Logger.warn(AssetFileNameFilter.class,"Skipping  " + dir.getAbsolutePath() + ", size: " + dir.length() + ",  Max allowed:" + maxFileSize);
			return false;
		}
		// if no bloomFilter, everything else is a go
		if (bloomFilter == null) {
			return true;
		}
		//Always allow messages (language property files)
		if (pathName.startsWith("/messages")) {
			return true;
		}
		// if bloomFilter, make sure the inode is in the path
		final List<RegExMatch> matches = RegEX.find(pathName, "[\\w]{8}(-[\\w]{4}){3}-[\\w]{12}");
		if (matches.isEmpty()) {
			return true;
		}
		return (bloomFilter.mightContain(matches.get(0).getMatch()));
	}

}
