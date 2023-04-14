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

package com.dotcms.enterprise.publishing.remote.bundler;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.UserWrapper;
import com.dotcms.publishing.*;
import com.dotcms.publishing.output.BundleOutput;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.UserProxyAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PushPublishLogger;
import com.liferay.portal.model.User;

import java.io.File;
import java.io.FileFilter;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.List;

/**
 * @author Jonathan Gamba
 *         Date: 5/29/13
 */
public class UserBundler implements IBundler {

    private PushPublisherConfig config;
    private UserAPI userAPI;
    private UserProxyAPI userProxyAPI;

    public final static String USER_PREFIX = "user_";
    public final static String FOLDER_USERS = "users";
    public final static String WRAPPER_DESCRIPTOR_EXTENSION = ".user.xml";

    @Override
    public String getName () {
        return "User Bundler";
    }

    @Override
    public void setConfig ( PublisherConfig pc ) {
        config = (PushPublisherConfig) pc;
        userAPI = APILocator.getUserAPI();
        userProxyAPI = APILocator.getUserProxyAPI();
    }

    @Override
    public void setPublisher(IPublisher publisher) {
    }

    /**
     * Prepares all the bundler structure in order to push a user
     *
     *
     * @param bundleOutput
     * @param status
     * @throws com.dotcms.publishing.DotBundleException
     *
     */
    @Override
    public void generate(final BundleOutput bundleOutput, final BundlerStatus status) throws DotBundleException {

        if ( LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level ) {
            throw new RuntimeException( "need an enterprise pro license to run this bundler" );
        }

        PushPublisherConfig.Operation operation = config.getOperation();

        try {
            List<PublishQueueElement> assets = config.getAssets();
            final PublisherFilter publisherFilter = APILocator.getPublisherAPI().createPublisherFilter(config.getId());
            for ( PublishQueueElement element : assets ) {

                if(!element.getType().equals("user") || publisherFilter.doesExcludeClassesContainsType(element.getType())) {
                    continue;
                }

                //Getting the user id to publish
                String asset = element.getAsset();
                String userId = asset.replace( USER_PREFIX, "" );

                //Load all the related info to this user id
                User user = userAPI.loadUserById( userId );

                Role userRole = APILocator.getRoleAPI().getUserRole(user);

                //Prepare and Bundle the found user
                UserWrapper wrapper = new UserWrapper( user, userRole );
                wrapper.setOperation( operation );

                //Prepare the file where we are going to write all this user information
                String uri = user.getUserId();
                if ( !uri.endsWith( WRAPPER_DESCRIPTOR_EXTENSION ) ) {
                    uri = uri.replace( WRAPPER_DESCRIPTOR_EXTENSION, "" ).trim();
                    uri += WRAPPER_DESCRIPTOR_EXTENSION;
                }
                final String myFileUrl = File.separator + FOLDER_USERS + File.separator + uri;

                try(final OutputStream outputStream = bundleOutput.addFile(myFileUrl)) {

                    BundlerUtil.objectToXML(wrapper, outputStream);
                }

                bundleOutput.setLastModified( myFileUrl, Calendar.getInstance().getTimeInMillis() );

                if ( Config.getBooleanProperty( "PUSH_PUBLISHING_LOG_DEPENDENCIES", false ) ) {
                    PushPublishLogger.log( getClass(), "User bundled for pushing. Operation : " + config.getOperation() + ", User id: " + userId, config.getId() );
                }
            }
        } catch ( Exception e ) {
            status.addFailure();
            throw new DotBundleException( this.getClass().getName() + " : " + "generate()" + e.getMessage() + ": Unable to pull content", e );
        }
    }

    @Override
    public FileFilter getFileFilter () {
        return new UserBundlerFilter();
    }

    public class UserBundlerFilter implements FileFilter {

        @Override
        public boolean accept ( File pathName ) {
            return (pathName.isDirectory() || pathName.getName().endsWith( WRAPPER_DESCRIPTOR_EXTENSION ));
        }

    }

}