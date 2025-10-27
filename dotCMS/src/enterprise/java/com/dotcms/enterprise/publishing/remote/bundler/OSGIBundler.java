/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.remote.bundler;

import com.dotcms.publishing.*;
import com.dotcms.publishing.output.BundleOutput;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import com.dotcms.enterprise.license.LicenseLevel;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.OSGIWrapper;
import com.dotmarketing.util.Config;
import org.apache.felix.framework.OSGIUtil;
import com.dotmarketing.util.PushPublishLogger;

/**
 * @author Jonathan Gamba
 *         Date: 5/14/13
 */
public class OSGIBundler implements IBundler {

    private PushPublisherConfig config;

    public final static String WRAPPER_DESCRIPTOR_EXTENSION = ".osgi.json";
    public final static String FOLDER_BUNDLES = "osgiBundles";

    @Override
    public String getName () {
        return "OSGI Bundler";
    }

    @Override
    public void setConfig ( PublisherConfig pc ) {
        config = (PushPublisherConfig) pc;
    }

    @Override
    public void setPublisher(IPublisher publisher) {
    }

    /**
     * Prepares all the bundler structure in order to push osgi jar bundles
     *
     *
     * @param output
     * @param status
     * @throws DotBundleException
     */
    @Override
    public void generate(final BundleOutput output, final BundlerStatus status) throws DotBundleException {

        if ( LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level ) {
            throw new RuntimeException( "need an enterprise pro license to run this bundler" );
        }

        List<PublishQueueElement> assets = config.getAssets();
        try {
            final PublisherFilter publisherFilter = APILocator.getPublisherAPI().createPublisherFilter(config.getId());
            for ( PublishQueueElement element : assets ) {

                if(!element.getType().equals("osgi") || publisherFilter.doesExcludeClassesContainsType(element.getType())) {
                    continue;
                }
                
                //Getting the osgi bundle jar name
                final String bundleJarName = element.getAsset();
                //This given jar under the felix load folder
                final String felixLoadFolder = OSGIUtil.getInstance().getFelixDeployPath();
                final String felixJarFilePath = felixLoadFolder + File.separator + bundleJarName;
                final File originalBundleJar = new File(felixJarFilePath);

                //Verify if the file already exist inside the bundler
                final String bundlerOSGIFolderPath = File.separator + FOLDER_BUNDLES;
                if (output.exists(bundlerOSGIFolderPath)) {
                    output.delete(bundlerOSGIFolderPath);
                }

                //Verify if the bundler folder structure was created
                final String bundlerOSGIFolder = bundlerOSGIFolderPath + File.separator + bundleJarName;
                if (!output.exists(bundlerOSGIFolder)) {
                    output.mkdirs(bundlerOSGIFolder);
                }

                //Verify what kind of operation we are doing, we don't need to send the jar for an UNPUBLISH
                final PushPublisherConfig.Operation operation = config.getOperation();
                if (operation.equals( PushPublisherConfig.Operation.PUBLISH)) {
                    //Copy the osgi jar bundle to the bundler folder keeping original folders and files structure
                    output.copyFile(originalBundleJar, bundlerOSGIFolder);
                }

                //Create our wrapper, needed in order to send the type of operation to the end point
                final OSGIWrapper osgiWrapper = new OSGIWrapper(bundleJarName, operation);
                final String wrapperDescriptorPath =
                        bundlerOSGIFolder
                        + ".descriptor"
                        + WRAPPER_DESCRIPTOR_EXTENSION;

                try(final OutputStream outputStream = output.addFile(wrapperDescriptorPath)) {
                    BundlerUtil.writeObject(osgiWrapper, outputStream, wrapperDescriptorPath);
                }

                if ( Config.getBooleanProperty( "PUSH_PUBLISHING_LOG_DEPENDENCIES", false ) ) {
                    PushPublishLogger.log( getClass(), "OSGIPlugin bundled for pushing. Operation : " + config.getOperation() + ", Name: " + bundleJarName, config.getId() );
                }
            }
        } catch (IOException | DotDataException | DotSecurityException e ) {
            status.addFailure();
            throw new DotBundleException( this.getClass().getName() + " : " + "generate()" + e.getMessage() + ": Unable to pull content", e );
        }
    }

    @Override
    public FileFilter getFileFilter () {
        return new OSGIBundlerFilter();
    }

    public class OSGIBundlerFilter implements FileFilter {

        @Override
        public boolean accept ( File pathName ) {
            return (pathName.isDirectory() || pathName.getName().endsWith( WRAPPER_DESCRIPTOR_EXTENSION ));
        }

    }

}
