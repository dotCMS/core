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

package com.dotcms.enterprise.publishing.remote.bundler;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.publishing.remote.bundler.util.CategoryBundlerUtil;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.CategoryWrapper;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.*;
import com.dotcms.publishing.output.BundleOutput;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.UtilMethods;
import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Category Bundler: in this case we don't need any kind of categories Set because we push every
 * time all the system categories.
 *
 * Note: this class used to be {@link CategoryBundler} but was renamed to only be used from the
 * Category portlet when the user wants to PP/sync the whole Category tree.
 *
 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
 *
 * Mar 6, 2013 - 9:34:34 AM
 */
public class CategoryFullBundler implements IBundler {

    private PushPublisherConfig config;
    private CategoryAPI catAPI = null;
    private UserAPI userAPI = null;
    public final static String CATEGORY_FULL_EXTENSION = ".category.xml";

    @Override
    public String getName() {
        return "Category Full Bundler";
    }

    @Override
    public void setConfig(PublisherConfig pc) {
        if (LicenseUtil.getLevel() < 300) {
            throw new RuntimeException("need an enterprise pro license to run this");
        }
        config = (PushPublisherConfig) pc;
        catAPI = APILocator.getCategoryAPI();
        userAPI = APILocator.getUserAPI();
    }

    @Override
    public void setPublisher(IPublisher publisher) {
    }

    @Override
    public void generate(final BundleOutput output, final BundlerStatus status) throws DotBundleException {
        if (LicenseUtil.getLevel() < 300) {
            throw new RuntimeException("need an enterprise pro license to run this bundler");
        }
        try {
            final PublisherFilter publisherFilter = APILocator.getPublisherAPI().createPublisherFilter(config.getId());
            if(!publisherFilter.doesExcludeClassesContainsType(PusheableAsset.CATEGORY.getType())) {
                // retrieve all top level categories
                List<Category> topLevelCategories = catAPI
                        .findTopLevelCategories(userAPI.getSystemUser(), true);
                for (Category topLevel : topLevelCategories) {
                    CategoryWrapper wrapper = new CategoryWrapper();
                    wrapper.setTopLevel(true);
                    wrapper.setCategory(topLevel);
                    wrapper.setOperation(config.getOperation());
                    // get all children
                    Set<String> childrenInodes = getChildrenInodes(
                            catAPI.findChildren(userAPI.getSystemUser(), topLevel.getInode(), true,
                                    null));
                    wrapper.setChildren(childrenInodes);
                    CategoryBundlerUtil.writeCategory(output, CATEGORY_FULL_EXTENSION, wrapper);
                    writeChildren(output, childrenInodes);
                }

                if (Config.getBooleanProperty("PUSH_PUBLISHING_LOG_DEPENDENCIES", false)) {
                    PushPublishLogger
                            .log(getClass(), "Categories bundled for pushing", config.getId());
                }
            }

        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
        }

    }

    @Override
    public FileFilter getFileFilter() {
        return new CategoryBundlerFilter();
    }

    private Set<String> getChildrenInodes(List<Category> children) {
        Set<String> inodes = new HashSet<>();
        for (Category child : children) {
            inodes.add(child.getInode());
        }
        return inodes;
    }

    /**
     * For each top level category creates the children's xml recursively.
     *
     * Mar 6, 2013 - 9:33:00 AM
     */
    private void writeChildren(final BundleOutput bundleOutput, final Set<String> inodes)
            throws DotDataException, DotSecurityException {

        for (String inode : inodes) {
            Category cat = catAPI.find(inode, userAPI.getSystemUser(), true);
            if (null != cat && UtilMethods.isSet(cat.getInode())) {
                List<Category> children = catAPI
                        .findChildren(userAPI.getSystemUser(), cat.getInode(), true, null);
                if (children.size() > 0) {
                    CategoryWrapper wrapper = new CategoryWrapper();
                    wrapper.setTopLevel(false);
                    wrapper.setCategory(cat);
                    wrapper.setOperation(config.getOperation());
                    wrapper.setChildren(getChildrenInodes(children));
                    CategoryBundlerUtil.writeCategory(bundleOutput, CATEGORY_FULL_EXTENSION, wrapper);
                    Set<String> childrenInodes = getChildrenInodes(children);
                    writeChildren(bundleOutput, childrenInodes);
                } else { // write the category
                    CategoryWrapper wrapper = new CategoryWrapper();
                    wrapper.setTopLevel(false);
                    wrapper.setCategory(cat);
                    wrapper.setOperation(config.getOperation());
                    wrapper.setChildren(null);
                    CategoryBundlerUtil.writeCategory(bundleOutput, CATEGORY_FULL_EXTENSION, wrapper);
                }
            }
        }

    }

    public class CategoryBundlerFilter implements FileFilter {

        @Override
        public boolean accept(File pathname) {
            return (pathname.isDirectory() || pathname.getName().endsWith(CATEGORY_FULL_EXTENSION));
        }

    }
}
