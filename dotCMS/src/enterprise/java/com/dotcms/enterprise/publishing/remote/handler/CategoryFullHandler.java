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

package com.dotcms.enterprise.publishing.remote.handler;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.publishing.remote.bundler.CategoryFullBundler;
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
    private PublisherConfig config;
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

    		pushCategoryUtil = new PushCategoryUtil(categories,
	                CategoryFullBundler.CATEGORY_FULL_EXTENSION);
	        if (pushCategoryUtil.getCategoryXMLCount() > 0) {

                deleteAllCategories();
                CacheLocator.getCategoryCache().clearCache();
                handleCategories(categories);
                removeInvalidTreeRelationships();
                CacheLocator.getCategoryCache().clearCache();
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
                if (null != topLevel.getChildren() && topLevel.getChildren().size() > 0) {

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
                    "Inode '%s': %s", childCategoryInode, topLevelCategoryInode, e.getMessage());
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
        if (null != wrapper.getChildren() && wrapper.getChildren().size() > 0) {

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
