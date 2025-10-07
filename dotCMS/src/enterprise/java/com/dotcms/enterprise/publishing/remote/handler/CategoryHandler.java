/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included 
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.remote.handler;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.CategoryBundler;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.publisher.pusher.wrapper.CategoryWrapper;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publisher.util.PushCategoryUtil;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.PushPublishLogger.PushPublishAction;
import com.dotmarketing.util.PushPublishLogger.PushPublishHandler;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.Lists;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class will be a part of the handle process for Categories, before this improvement the
 * categories were pushed as a whole: it would bundle every category and send it and this class
 * would delete all categories and insert the ones on the bundle. Now with this new logic we only
 * push categories related to the content we are sending, so we need to handle just those
 * cases(insert/update). One important note is that this handler is added always to the {@link
 * com.dotcms.publisher.receiver.BundlePublisher} but will only run if the bundle has files with
 * .category.xml.
 *
 * https://github.com/dotCMS/core/issues/12125
 *
 * @author Oscar Arrieta on 7/17/17.
 */
public class CategoryHandler implements IHandler {

    private final CategoryAPI     categoryAPI;
    private final User            systemUser;
    private final PublisherConfig config;

    private PushCategoryUtil pushCategoryUtil;


    public CategoryHandler(final PublisherConfig config) {
        this.config = config;
        systemUser  = APILocator.systemUser();
        categoryAPI = APILocator.getCategoryAPI();
    }

    @Override
    public void handle(final File bundleFolder) throws Exception {

        if (LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
            throw new RuntimeException("need an enterprise pro license to run this");
        }

        final Collection<File> categories = FileUtil.listFilesRecursively(
                bundleFolder,
                new CategoryBundler().getFileFilter());

        pushCategoryUtil = new PushCategoryUtil(categories, CategoryBundler.CATEGORY_EXTENSION);

        if (pushCategoryUtil.getCategoryXMLCount() > 0) {
            handleCategories(pushCategoryUtil.findTopLevelWrappers(), null);
        }
    }

    /**
     * This method will read all the Top Level Categories from the bundle and it will insert/update
     * them locally. After that it will call the recursive method handleChildrenCategories to handle
     * all the children.
     *
     * This is the flow that we are going to handle:
     *
     * 1. If we have same key.
     *   1.1 If different inode.
     *     1.1.1 Copy remote properties except inode.
     *   1.2 Update.
     *
     * 2. Else-If we have same inode.
     *   2.1 If key is blank.
     *     2.1.1 Save Remote.
     *   2.2 ELse: key is NOT blank.
     *     2.2.1 Update.
     *
     * 3. Else: different key and different inode.
     *   3.1 Publish Remote.
     *
     * 4. Repeat with each children.
     */
    private void handleCategories(List<CategoryWrapper> categoryWrappers, Category parentCategory)
            throws DotPublishingException {

        if (LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
            throw new RuntimeException("need an enterprise pro license to run this");
        }
        String categoryInode = null;
        Set<String> categoryChildren = new HashSet<>();
        try {
            for (final CategoryWrapper wrapper : categoryWrappers) {

                final Category categoryBundle = wrapper.getCategory();
                categoryInode = categoryBundle.getInode();
                final Category categoryFound  =
                        UtilMethods.isSet(categoryBundle.getKey())
                                ? findByKey(categoryBundle.getKey()) : null;

                // 1. If we have same key.
                if (categoryFound != null){
                    // 1.1 If different inode.
                    if (!categoryFound.getInode().equals(categoryBundle.getInode())) {
                        // 1.1.1 Copy remote properties except inode.
                        final String format = String.format("Category found in Server with same "
                                        + "key: '%s' but different inode; bundle: '%s', local: '%s'. "
                                        + "Updating local Category to match the one from bundle.",
                                categoryFound.getKey(),
                                categoryBundle.getInode(),
                                categoryFound.getInode());
                        Logger.warn(this, format);
                        categoryBundle.setInode(categoryFound.getInode());
                    }
                    // 1.2 Update.
                    updateCategory(categoryBundle, parentCategory);
                // 2. Else-If we have same inode.
                } else if (findByInode(categoryBundle.getInode()) != null) {
                    // 2.1 If key is blank.
                    if (!UtilMethods.isSet(categoryBundle.getKey())){
                        // 2.1.1 Save Remote.
                        final String format = String.format("Category found in Server with same "
                                        + "inode: '%s' but the Category Key from bundle is blank. "
                                        + "Updating local category without key.",
                                categoryBundle.getInode());
                        Logger.warn(this, format);
                        updateCategoryWithoutKey(categoryBundle, parentCategory);
                    } else { // 2.2 ELse: key is NOT blank.
                        // 2.2.1 Update.
                        updateCategory(categoryBundle, parentCategory);
                    }
                } else { // 3. Else: different key and different inode.
                    // 3.1 Publish Remote.
                    final String format = String.format("Category: '%s' not found in Server, "
                                    + "creating a new one.", categoryBundle.getCategoryName());
                    Logger.info(this, format);
                    createCategory(categoryBundle, parentCategory);
                }

                // 4. Repeat with each children.
                if (null != wrapper.getChildren() && !wrapper.getChildren().isEmpty()) {
                    categoryChildren = wrapper.getChildren();
                    final List<CategoryWrapper> childrenCategoryWrappers =
                            getChildrenCategoryWrappers(wrapper.getChildren());

                    handleCategories(childrenCategoryWrappers, categoryBundle);
                }
            }
        } catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when publishing Category Inode '%s', children [ " +
                    "%s ]: %s", categoryInode, categoryChildren, ExceptionUtil.getErrorMessage(e));
            Logger.error(this, errorMsg, e);
            throw new DotPublishingException(errorMsg, e);
        }
    }

    /**
     * Read evey children inode of the wrapper and search each one in the bundle files.
     */
    private List<CategoryWrapper> getChildrenCategoryWrappers(final Set<String> childrenInodes) {

        final List<CategoryWrapper> categoryWrappers = Lists.newArrayList();

        for (final String inode : childrenInodes) {
            try {
                final CategoryWrapper categoryWrapperFromInode =
                        pushCategoryUtil.getCategoryWrapperFromInode(inode);

                categoryWrappers.add(categoryWrapperFromInode);
            } catch (IOException e) {
                Logger.error(this, "Error reading File for inode: " + inode, e);
            }
        }

        return categoryWrappers;
    }

    /**
     * Wrapper method because we always use same user and false as respectFrontEndRoles.
     */
    private Category findByKey(String key)
            throws DotSecurityException, DotDataException {
        return categoryAPI.findByKey(key, systemUser, false);
    }

    /**
     * Wrapper method because we always use same user and false as respectFrontEndRoles.
     */
    private Category findByInode(String inode)
            throws DotSecurityException, DotDataException {
        return categoryAPI.find(inode, systemUser, false);
    }

    /**
     * Wrapper method because we always use same user and false as respectFrontEndRoles.
     */
    private void updateCategory(Category category, Category parent)
            throws DotSecurityException, DotDataException {
        categoryAPI.save(parent, category, systemUser, false);

        PushPublishLogger.log(getClass(), PushPublishHandler.CATEGORY, PushPublishAction.PUBLISH_UPDATE,
                category.getIdentifier(), category.getInode(), category.getCategoryName(), config.getId());
    }

    /**
     * Wrapper method because we always use same user and false as respectFrontEndRoles.
     */
    private void updateCategoryWithoutKey(Category category, Category parent)
            throws DotSecurityException, DotDataException {
        categoryAPI.saveRemote(parent, category, systemUser, false);

        PushPublishLogger.log(getClass(), PushPublishHandler.CATEGORY, PushPublishAction.PUBLISH_UPDATE,
                category.getIdentifier(), category.getInode(), category.getCategoryName(), config.getId());
    }

    /**
     * Wrapper method because we always use same user and false as respectFrontEndRoles.
     */
    private void createCategory(Category category, Category parent)
            throws DotSecurityException, DotDataException {
        categoryAPI.publishRemote(parent, category, systemUser, false);

        PushPublishLogger.log(getClass(), PushPublishHandler.CATEGORY, PushPublishAction.PUBLISH_CREATE,
                category.getIdentifier(), category.getInode(), category.getCategoryName(), config.getId());
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

}
