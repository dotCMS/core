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

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.OSGIBundler;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.OSGIWrapper;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.XMLSerializerUtil;
import org.apache.commons.io.FileUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.PushPublishLogger.PushPublishAction;
import com.dotmarketing.util.PushPublishLogger.PushPublishHandler;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;
import org.apache.felix.framework.OSGIUtil;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
     *
     * @param bundleFolder
     * @throws Exception
     */
    @Override
    public void handle ( File bundleFolder ) throws Exception {

        if ( LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level ) {
            throw new RuntimeException( "need an enterprise pro license to run this" );
        }

        //Get the felix folder on this server
        final String felixLoadFolderPath = OSGIUtil.getInstance().getFelixDeployPath();
        final File felixLoadFileFolder = new File( felixLoadFolderPath );

        final String felixUploadFolderPath = OSGIUtil.getInstance().getFelixUploadPath();


        final String bundlerOSGIFolderPath = bundleFolder.getPath() + File.separator + OSGIBundler.FOLDER_BUNDLES;

        //Verify if on this bundler we shipped some bundles
        final File bundlesFileFolder = new File( bundlerOSGIFolderPath );
        if ( bundlesFileFolder.exists() ) {

            final XStream xstream = XMLSerializerUtil.getInstance().getXmlSerializer();

            //Search for the list of bungles inside this bundler
            final Collection<File> bundles = FileUtil.listFilesRecursively(
                    bundlesFileFolder,
                    new OSGIBundler().getFileFilter());
            //Copy each of them inside de felix load folder
            for ( final File wrapperDescriptor : bundles ) {

                if (wrapperDescriptor.isDirectory()) {
                    Logger.debug(this, String.format("Skipping directory bundle %s", wrapperDescriptor.getName()));
                    continue;
                }

                //Get our wrapper
                final Path path = wrapperDescriptor.toPath();
                    final OSGIWrapper wrapper = BundlerUtil.jsonToObject(path.toFile(), OSGIWrapper.class);
                final PushPublisherConfig.Operation operation = wrapper.getOperation();
                final String bundleJarName = wrapper.getJarName();

                if ( operation.equals( PushPublisherConfig.Operation.UNPUBLISH ) ) {
                    //On UnPublish lets REMOVE the jar from the felix load folder
                    final File jarFile = new File( felixLoadFolderPath + File.separator + bundleJarName );
                    if ( jarFile.exists() ) {
                        jarFile.delete();
                        PushPublishLogger.log(getClass(), PushPublishHandler.OSGI, PushPublishAction.UNPUBLISH,
                                felixLoadFolderPath + File.separator + bundleJarName, config.getId());
                    }
                } else {
                    //On Publish lets COPY the jar we received into the felix upload folder
                    final File jarFile = new File( bundlerOSGIFolderPath + File.separator + bundleJarName );
                    if ( jarFile.exists() ) {
                        FileUtils.copyFileToDirectory( jarFile, new File(felixUploadFolderPath), false );
                        PushPublishLogger.log(getClass(), PushPublishHandler.OSGI, PushPublishAction.PUBLISH,
                                felixLoadFolderPath + File.separator + bundleJarName, config.getId());
                    }
                }
            }
        }
    }

}
