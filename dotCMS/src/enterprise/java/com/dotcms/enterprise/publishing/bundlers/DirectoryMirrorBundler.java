/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.bundlers;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publishing.*;
import com.dotcms.publishing.output.BundleOutput;
import com.dotmarketing.util.Logger;
import com.liferay.util.FileUtil;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

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
    public void setPublisher(IPublisher publisher) {
    }

	
	public File getDestinationFile(String destination){
		
		return BundlerUtil.getBundleRoot(destination);
	}


    @Override
    public void generate(BundleOutput output, BundlerStatus status) throws DotBundleException {
        if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
            throw new RuntimeException("need an enterprise license to run this");
        }

        String destination = config.getDestinationBundle();
        File destinationBundle = getDestinationFile(destination);

        final List<FileFilter> bundlerFilters = new ArrayList<>();
        try {
            for (Class clazz : config.getPublishers()) {
                for (Class bundler : ((IPublisher) clazz.getDeclaredConstructor().newInstance()).getBundlers()) {
                    if (!bundler.equals(DirectoryMirrorBundler.class)) {
                        bundlerFilters.add(((IBundler) bundler.getDeclaredConstructor().newInstance()).getFileFilter());
                    }
                }
            }

            FileUtil.copyDirectory(output.getFile(), destinationBundle, this.config.isIncremental(), new FileFilter() {
                @Override
                public boolean accept(File file) {
                    try {
                        if (!file.isDirectory()) {
                            if (file.length()==0L){
                                return false;
                            }

                            for (FileFilter filter : bundlerFilters) {
                                if (filter.accept(file)) {
                                    return false;
                                }
                            }
                        }
                    } catch (Exception ex) {
                        Logger.warn(this, ex.getMessage(), ex);
                    }
                    return true;
                }
            });
        } catch (Exception ex) {
            throw new DotBundleException(ex.getMessage(), ex);
        }
    }

	@Override
	public FileFilter getFileFilter() {
		return new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return false;
			}
		};
	}
	
}
