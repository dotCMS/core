/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
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
