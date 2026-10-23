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
import com.dotcms.enterprise.publishing.remote.bundler.OSGIBundler;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.OSGIWrapper;
import com.dotcms.publisher.receiver.BundlePluginAccess;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.util.PathContainmentUtil;
import com.dotmarketing.exception.DotRuntimeException;
import org.apache.commons.io.FileUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.PushPublishLogger.PushPublishAction;
import com.dotmarketing.util.PushPublishLogger.PushPublishHandler;
import com.liferay.util.FileUtil;
import org.apache.felix.framework.OSGIUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.Collection;

/**
 * @author Jonathan Gamba
 *         Date: 5/16/13
 */
public class OSGIHandler implements IHandler {

	private PublisherConfig config;

	public OSGIHandler(PublisherConfig config) {
		this.config = config;
	}

    @Override
    public String getName () {
        return this.getClass().getName();
    }

    /**
     * Method that will verify if a given bundler contains inside it osgi bundles, if it does this method
     * will copy them inside the felix load folder.
     * <p>Nothing is copied or removed unless the bundle was received in a way that allows plugin
     * deployment (see {@link BundlePluginAccess#mayDeployPlugins(PublisherConfig)}); a bundle whose
     * origin is not recorded is treated as not allowed. The jar name in each descriptor must be a plain file name
     * that resolves to a regular file inside the expected folder, otherwise the whole bundle fails.
     *
     * @param bundleFolder Folder where the bundle was extracted.
     * @throws Exception If a descriptor names a jar that is not a plain file name inside the folder.
     */
    @Override
    public void handle ( File bundleFolder ) throws Exception {

        if ( LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level ) {
            throw new RuntimeException( "need an enterprise pro license to run this" );
        }

        //Get the felix folder on this server
        final String felixLoadFolderPath = OSGIUtil.getInstance().getFelixDeployPath();
        final File felixLoadFolder = new File( felixLoadFolderPath );

        final String felixUploadFolderPath = OSGIUtil.getInstance().getFelixUploadPath();

        final File bundlerOSGIFolder = new File( bundleFolder.getPath() + File.separator + OSGIBundler.FOLDER_BUNDLES );

        //Verify if on this bundler we shipped some bundles
        if ( bundlerOSGIFolder.exists() ) {

            if ( !BundlePluginAccess.mayDeployPlugins( config ) ) {
                Logger.warn( this, "Plugins in the bundle were ignored: this bundle may not deploy plugins" );
                return;
            }

            //Search for the list of bungles inside this bundler
            final Collection<File> bundles = FileUtil.listFilesRecursively(
                    bundlerOSGIFolder,
                    new OSGIBundler().getFileFilter());
            //Copy each of them inside de felix load folder
            for ( final File wrapperDescriptor : bundles ) {

                if (wrapperDescriptor.isDirectory()) {
                    Logger.debug(this, String.format("Skipping directory bundle %s", wrapperDescriptor.getName()));
                    continue;
                }

                //Get our wrapper
                final OSGIWrapper wrapper = BundlerUtil.jsonToObject(wrapperDescriptor, OSGIWrapper.class);
                if ( wrapper == null ) {
                    throw new DotRuntimeException( "The bundle has an invalid plugin descriptor" );
                }
                final PushPublisherConfig.Operation operation = wrapper.getOperation();
                final String bundleJarName = wrapper.getJarName();

                if ( !PathContainmentUtil.isPlainSegment( bundleJarName ) ) {
                    throw new DotRuntimeException( "The bundle has an invalid plugin file name" );
                }

                if ( operation.equals( PushPublisherConfig.Operation.UNPUBLISH ) ) {
                    //On UnPublish lets REMOVE the jar from the felix load folder
                    final File jarFile = new File( felixLoadFolder, bundleJarName );
                    if ( Files.exists( jarFile.toPath(), LinkOption.NOFOLLOW_LINKS ) ) {
                        requireRegularFileInside( felixLoadFolder, jarFile );
                        jarFile.delete();
                        PushPublishLogger.log(getClass(), PushPublishHandler.OSGI, PushPublishAction.UNPUBLISH,
                                felixLoadFolderPath + File.separator + bundleJarName, config.getId());
                    }
                } else {
                    //On Publish lets COPY the jar we received into the felix upload folder
                    final File jarFile = new File( bundlerOSGIFolder, bundleJarName );
                    if ( Files.exists( jarFile.toPath(), LinkOption.NOFOLLOW_LINKS ) ) {
                        requireRegularFileInside( bundlerOSGIFolder, jarFile );
                        FileUtils.copyFileToDirectory( jarFile, new File(felixUploadFolderPath), false );
                        PushPublishLogger.log(getClass(), PushPublishHandler.OSGI, PushPublishAction.PUBLISH,
                                felixLoadFolderPath + File.separator + bundleJarName, config.getId());
                    }
                }
            }
        }
    }

    /**
     * Makes sure a file is a regular file (not a link, not a directory) that resolves to a location
     * inside the given folder.
     *
     * @param folder The folder that must contain the file.
     * @param file   The file to check.
     * @throws DotRuntimeException If the file is not a regular file inside the folder.
     */
    private static void requireRegularFileInside( final File folder, final File file ) {
        if ( !Files.isRegularFile( file.toPath(), LinkOption.NOFOLLOW_LINKS )
                || !PathContainmentUtil.isContainedIn( folder, file ) ) {
            throw new DotRuntimeException( "The bundle has an invalid plugin file name" );
        }
    }

}
