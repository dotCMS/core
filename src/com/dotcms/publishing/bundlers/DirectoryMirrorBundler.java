package com.dotcms.publishing.bundlers;

import java.io.File;
import java.io.FileFilter;

import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.PublisherConfig;
import com.liferay.util.FileUtil;

/**
 * This bundle will make a copy of a directory using hard links when
 * configuration is "incremental"
 * 
 * @author Michele Mastrogiovanni
 */
public class DirectoryMirrorBundler implements IBundler {

	private PublisherConfig config;
	
	@Override
	public String getName() {
		return "Directory Mirror Bundler";
	}

	@Override
	public void setConfig(PublisherConfig pc) {
		this.config = pc;
	}

	@Override
	public void generate(File bundleRoot, BundlerStatus status) throws DotBundleException {
		String destination = config.getDestinationBundle();
		File destinationBundle = BundlerUtil.getBundleRoot(destination);
		FileUtil.copyDirectory(bundleRoot, destinationBundle, this.config.isIncremental());
	}

	@Override
	public FileFilter getFileFilter() {
		return new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return true;
			}
		};
	}
	
}
