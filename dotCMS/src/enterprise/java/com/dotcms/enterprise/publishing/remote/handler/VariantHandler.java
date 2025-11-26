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
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.VariantBundler;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.publisher.pusher.wrapper.VariantWrapper;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.liferay.util.FileUtil;

import java.io.File;
import java.util.Collection;
import java.util.Optional;

/**
 * This handler class is part of the Push Publishing mechanism that deals with Experiments-related information inside a
 * bundle and saves it in the receiving instance. This class will read and process only the {@link com.dotcms.variant.model.Variant} data
 * files.
 *
 */
public class VariantHandler implements IHandler {

	private final PublisherConfig config;
	private final VariantAPI variantAPI;
	public VariantHandler(final PublisherConfig config) {

		this.config = config;
		this.variantAPI = APILocator.getVariantAPI();
	}

	@Override
	public String getName() {
		return this.getClass().getName();
	}

	@WrapInTransaction
	@Override
	public void handle(final File bundleFolder) throws Exception {

	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
	        throw new RuntimeException("need an enterprise pro license to run this");
        }

		final Collection<File> variants = FileUtil.listFilesRecursively
				(bundleFolder, new VariantBundler().getFileFilter());

        handleVariants(variants);
	}

	private void handleVariants(final Collection<File> variants) throws DotPublishingException {

	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {

	        throw new RuntimeException("need an enterprise pro license to run this");
        }
		File workingOn = null;
        Variant variant = null;
		try {
	        //Handle folders
	        for(final File variantFile: variants) {
				workingOn = variantFile;
	        	if(variantFile.isDirectory()) {

	        		continue;
				}

	        	final VariantWrapper variantWrapper = BundlerUtil
						.jsonToObject(variantFile, VariantWrapper.class);

				variant = variantWrapper.getVariant();

	        	Optional<Variant> localVariant = variantAPI.get(variant.name());

	        	if(variantWrapper.getOperation().equals(Operation.UNPUBLISH)) {
	        		// delete operation
	        	    if(localVariant.isPresent()) {
						variantAPI.delete(localVariant.orElseThrow().name());
					}
	        	} else {
	        		// save or update Experiment
					if(localVariant.isPresent()) {
						variantAPI.update(variant);
					} else {
						variantAPI.save(variant);
					}
				}
			}
    	} catch (final Exception e) {
			final String errorMsg = String.format("An error occurred when processing Variant in '%s' with name '%s': %s",
					workingOn, null != variant ? variant.name() : "- null -", ExceptionUtil.getErrorMessage(e));
			Logger.error(this.getClass(), errorMsg, e);
			throw new DotPublishingException(errorMsg, e);
    	}
    }

}
