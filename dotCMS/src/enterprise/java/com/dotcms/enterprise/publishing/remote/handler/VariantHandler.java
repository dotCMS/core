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
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.VariantBundler;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.publisher.pusher.wrapper.ExperimentWrapper;
import com.dotcms.publisher.pusher.wrapper.VariantWrapper;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
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
					workingOn, variant.name(),
							variant.name(), e.getMessage());
			Logger.error(this.getClass(), errorMsg, e);
			throw new DotPublishingException(errorMsg, e);
    	}
    }

}
