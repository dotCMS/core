/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included 
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.remote.bundler;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.util.CategoryBundlerUtil;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.CategoryWrapper;
import com.dotcms.publishing.*;
import com.dotcms.publishing.output.BundleOutput;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * This class will be a part of the bundle process for Categories, before this improvement the
 * categories were pushed as a whole: it would bundle every category and send it. Now with this
 * new logic we only push categories related to the content we are sending. One important
 * note is that this bundler is added to the {@link com.dotcms.publisher.pusher.PushPublisher}
 * every time except when the user DO want to push (sync) every category from the Categories Portlet.
 *
 * https://github.com/dotCMS/core/issues/12125
 *
 * @author Oscar Arrieta on 7/17/17.
 */
public class CategoryBundler implements IBundler {

    private PushPublisherConfig config;

    private CategoryAPI categoryAPI;
    private User systemUser;

    public final static String CATEGORY_EXTENSION = ".category.dpc.xml";

    @Override
    public String getName() {
        return "Category Bundler";
    }

    @Override
    public void setConfig(PublisherConfig pc) {
        config = (PushPublisherConfig) pc;

        categoryAPI = APILocator.getCategoryAPI();
        systemUser = APILocator.systemUser();
    }

    @Override
    public void setPublisher(IPublisher publisher) {

    }

    @Override
    public void generate(
            final BundleOutput output, final BundlerStatus status) throws DotBundleException {
        if (LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
            throw new RuntimeException("need an enterprise pro license to run this bundler");
        }

        try {
            final List<CategoryWrapper> categoryWrapperTree = createCategoryWrapperTree
                    (config.getCategories());
            for (CategoryWrapper categoryWrapper : categoryWrapperTree) {
                CategoryBundlerUtil.writeCategory(output, CATEGORY_EXTENSION, categoryWrapper);
                status.addCount();
            }
        } catch (Exception e) {
            status.addFailure();

            throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
                    + e.getMessage() + ": Unable to pull content", e);
        }
    }

    /**
     * Util method to create a List with all the information we need to PP the CategoryWrapper.
     * Basically in order to push a single category we need the whole hierarchy (parents) for that
     * object. For example, if we have:
     *
     * Top Level -> Grandparent Category Second Level -> Parent Category Third Level -> My Category
     * Third Level -> Sibling Category
     *
     * Let's asy we have a content with "My category" as dependency, we don't want to push all
     * categories, just that one but in we will need the complete hierarchy. Third -> Second -> Top
     * so in this case we would need the info for all those objects in order to create proper
     * CategoryWrapper.
     *
     * @param categories List of category inodes as {@link String}
     */
    private List<CategoryWrapper> createCategoryWrapperTree(Set<String> categories)
            throws DotSecurityException, DotDataException {

        ArrayList<CategoryWrapper> categoryWrapperList = Lists.newArrayList();

        for (String categoryId : categories) {
            // For each category id we need to get the Category Object.
            final Category category = categoryAPI.find
                    (categoryId, systemUser, false);
            // Now we need all the parents of that Category.
            final List<Category> categoryTreeUp = categoryAPI.getCategoryTreeUp
                    (category, systemUser, false);
            // We need to remove first element because is a "Fake Cat".
            categoryTreeUp.remove(0);
            // Get the iterator so we can move to <- and ->.
            final ListIterator<Category> categoryListIterator = categoryTreeUp.listIterator();

            boolean topLevel = true;
            while (categoryListIterator.hasNext()) {
                // We gather all the information needed for the CategoryWrapper.
                final Category categoryToWrite = categoryListIterator.next();
                Set<String> children = Sets.newHashSet();
                // If current Category has next element means that it has children.
                if (categoryListIterator.hasNext()) {
                    Category nextCategory = categoryTreeUp.get(categoryListIterator.nextIndex());
                    children.add(nextCategory.getInode());
                }

                // Create the CategoryWrapper.
                CategoryWrapper categoryWrapper = new CategoryWrapper(
                        topLevel,
                        categoryToWrite,
                        children,
                        config.getOperation());

                topLevel = false;

                // If the CategoryWrapper already exists in the list, this means that another
                // evaluated category already processed all the hierarchy and that we only
                // need to add the current inode as children node.
                if (categoryWrapperList.contains(categoryWrapper)){
                    final int i = categoryWrapperList.indexOf(categoryWrapper);
                    final Set<String> categoryWrapperChildren = categoryWrapper.getChildren();

                    if (categoryWrapperChildren != null){
                        categoryWrapperList.get(i).getChildren().addAll(categoryWrapperChildren);
                    }
                } else {
                    categoryWrapperList.add(categoryWrapper);
                }
            }
        }

        return categoryWrapperList;
    }

    @Override
    public FileFilter getFileFilter() {
        return new CategoryBundlerFilter();
    }

    public class CategoryBundlerFilter implements FileFilter {

        @Override
        public boolean accept(File pathname) {
            return (pathname.isDirectory() || pathname.getName().endsWith(CATEGORY_EXTENSION));
        }

    }

}
