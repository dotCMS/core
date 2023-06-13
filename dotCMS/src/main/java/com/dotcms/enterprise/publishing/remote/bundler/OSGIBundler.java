package com.dotcms.enterprise.publishing.remote.bundler;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;

import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.repackage.org.apache.commons.io.FileUtils;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.OSGIWrapper;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.IPublisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.util.Config;
import org.apache.felix.framework.OSGIUtil;
import com.dotmarketing.util.PushPublishLogger;

/**
 * @author Jonathan Gamba
 *         Date: 5/14/13
 */
public class OSGIBundler implements IBundler {

    private PushPublisherConfig config;

    public final static String BUNDLES_EXTENSION = ".jar";
    public final static String WRAPPER_DESCRIPTOR_EXTENSION = ".osgi.xml";
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
     * @param bundleRoot
     * @param status
     * @throws DotBundleException
     */
    @Override
    public void generate ( File bundleRoot, BundlerStatus status ) throws DotBundleException {

        if ( LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level ) {
            throw new RuntimeException( "need an enterprise pro license to run this bundler" );
        }

        List<PublishQueueElement> assets = config.getAssets();
        try {

            for ( PublishQueueElement element : assets ) {

                if(!element.getType().equals("osgi"))
                    continue;
                
                //Getting the osgi bundle jar name
                String bundleJarName = element.getAsset();

                //This given jar under the felix load folder
                String felixLoadFolder = OSGIUtil.getInstance().getFelixDeployPath();

                String felixJarFilePath = felixLoadFolder + File.separator + bundleJarName;
                File originalBundleJar = new File(felixJarFilePath);

                String bundlerOSGIFolderPath = bundleRoot.getPath() + File.separator + FOLDER_BUNDLES;

                //Verify if the file already exist inside the bundler
                File destinationBundlerJar = new File( bundlerOSGIFolderPath + File.separator + bundleJarName );
                if ( destinationBundlerJar.exists() ) {
                    destinationBundlerJar.delete();
                }

                //Verify if the bundler folder structure was created
                File bundlerOSGIFolder = new File( bundlerOSGIFolderPath );
                if ( !bundlerOSGIFolder.exists() ) {
                    bundlerOSGIFolder.mkdirs();
                }

                //Verify what kind of operation we are doing, we don't need to send the jar for an UNPUBLISH
                PushPublisherConfig.Operation operation = config.getOperation();
                if ( operation.equals( PushPublisherConfig.Operation.PUBLISH ) ) {
                    //Copy the osgi jar bundle to the bundler folder keeping original folders and files structure
                    FileUtils.copyFileToDirectory( originalBundleJar, bundlerOSGIFolder, false );
                }

                //Create our wrapper, needed in order to send the type of operation to the end point
                OSGIWrapper osgiWrapper = new OSGIWrapper( bundleJarName, operation );
                File wrapperDescriptor = new File( bundlerOSGIFolderPath + File.separator + bundleJarName + ".descriptor" + WRAPPER_DESCRIPTOR_EXTENSION );
                BundlerUtil.objectToXML( osgiWrapper, wrapperDescriptor, false );

                if ( Config.getBooleanProperty( "PUSH_PUBLISHING_LOG_DEPENDENCIES", false ) ) {
                    PushPublishLogger.log( getClass(), "OSGIPlugin bundled for pushing. Operation : " + config.getOperation() + ", Name: " + bundleJarName, config.getId() );
                }
            }
        } catch ( IOException e ) {
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