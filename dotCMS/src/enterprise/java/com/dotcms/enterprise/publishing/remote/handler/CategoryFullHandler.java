/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.remote.handler;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.publishing.remote.bundler.CategoryFullBundler;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.publisher.pusher.wrapper.CategoryWrapper;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publisher.util.PushCategoryUtil;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.db.HibernateUtil.TransactionListenerStatus;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.PushPublishLogger.PushPublishAction;
import com.dotmarketing.util.PushPublishLogger.PushPublishHandler;
import com.liferay.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * Note: this class used to be {@link CategoryHandler} but was renamed to only be used from the
 * Category portlet when the user wants to PP/sync the whole Category tree. Important to node that
 * this handler is added always to {@link com.dotcms.publisher.receiver.BundlePublisher} but it will
 * run just when if finds files with .category.full.bundler extension. We need to be careful because
 * if the bundle has those extension files it will delete the local categories and insert all the
 * categories coming in the bundle. This handler will run after {@link CategoryHandler}
 *
 * @author Jorge Urdaneta
 */
public class CategoryFullHandler implements IHandler {

    private final CategoryAPI categoryAPI = APILocator.getCategoryAPI();
    private final UserAPI     userAPI     = APILocator.getUserAPI();
    private PushCategoryUtil pushCategoryUtil;
    private final PublisherConfig config;
    private int categoryHandledCounter = 0;

    public CategoryFullHandler(PublisherConfig config) {
        this.config = config;
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void handle(File bundleFolder) throws Exception {
        if (LicenseUtil.getLevel() < 300) {
            throw new RuntimeException("need an enterprise pro license to run this");
        }

        final Collection<File> categories = FileUtil
                .listFilesRecursively(bundleFolder, new CategoryFullBundler().getFileFilter());

        final TransactionListenerStatus listenersStatus = HibernateUtil.getTransactionListenersStatus();
    	try {
    		// https://github.com/dotCMS/core/issues/12225
    		HibernateUtil.setTransactionListenersStatus(TransactionListenerStatus.DISABLED);

            for (String extension : CategoryFullBundler.CATEGORY_FULL_EXTENSIONS) {
                pushCategoryUtil = new PushCategoryUtil(categories, extension);
                if (pushCategoryUtil.getCategoryXMLCount() > 0) {

                    deleteAllCategories();
                    CacheLocator.getCategoryCache().clearCache();
                    handleCategories(categories);
                    removeInvalidTreeRelationships();
                    CacheLocator.getCategoryCache().clearCache();
                }
            }
    	} finally {
    		HibernateUtil.setTransactionListenersStatus(listenersStatus);
    	}
    }

    /**
     * Handle categories: first we need all the top level categories and than for each one
     * recursively publish the children.
     *
     * Mar 6, 2013 - 9:41:42 AM
     */
    private void handleCategories(final Collection<File> categories) throws DotPublishingException {

        if (LicenseUtil.getLevel() < 300) {
            throw new RuntimeException("need an enterprise pro license to run this");
        }
        String topLevelCategoryInode = null;
        String childCategoryInode = null;
        try {
            // get the top level categories
            final List<CategoryWrapper> topLevels = pushCategoryUtil.findTopLevelWrappers();

            for (final CategoryWrapper topLevel : topLevels) {
                topLevelCategoryInode = topLevel.getCategory().getInode();
            	handleCategory(null, topLevel.getCategory());
            	
                // try with children
                if (null != topLevel.getChildren() && !topLevel.getChildren().isEmpty()) {

                    for (final String inode : topLevel.getChildren()) {
                        childCategoryInode = inode;
                        handleChildrenCategories(topLevel,
                                pushCategoryUtil.getCategoryWrapperFromInode(inode));
                    }
                }
            }

            PushPublishLogger.log(getClass(), "Categories published", config.getId());
        } catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when publishing Category Inode '%s', Parent " +
                    "Inode '%s': %s", childCategoryInode, topLevelCategoryInode, ExceptionUtil.getErrorMessage(e));
            Logger.error(this, errorMsg, e);
            throw new DotPublishingException(errorMsg, e);
        }
    }

    private void handleChildrenCategories(final CategoryWrapper parent,
                                          final CategoryWrapper wrapper)
            throws DotDataException, DotSecurityException, IOException {

        if (LicenseUtil.getLevel() < 300) {
            throw new RuntimeException("need an enterprise pro license to run this");
        }

        handleCategory(parent.getCategory(), wrapper.getCategory());

        // the category has children
        if (null != wrapper.getChildren() && !wrapper.getChildren().isEmpty()) {

            for (final String inode : wrapper.getChildren()) {

                handleChildrenCategories(wrapper,
                        pushCategoryUtil.getCategoryWrapperFromInode(inode));
            }
        }
    }


    private void handleCategory(final Category parent,
                                final Category object) throws DotDataException, DotSecurityException {

    	categoryHandledCounter++;

    	categoryAPI.publishRemote(parent, object, userAPI.getSystemUser(), true);

        PushPublishLogger.log(getClass(), PushPublishHandler.CATEGORY, PushPublishAction.PUBLISH,
                                object.getIdentifier(), object.getInode(), object.getCategoryName(), config.getId());
    	Logger.info(this, "* Handled category '"+ object.getInode() +"' ("+ categoryHandledCounter +")");
    }

    @WrapInTransaction
    private void deleteAllCategories() throws DotDataException {
        HibernateUtil
                .delete("from category in class com.dotmarketing.portlets.categories.model.Category");
    }

    @WrapInTransaction
    private void removeInvalidTreeRelationships() throws DotDataException {
        DotConnect db = new DotConnect();
        db.setSQL(
                "delete from tree where parent not in (select inode from category) and relation_type = 'child' ");
        db.loadResult();
    }

}
