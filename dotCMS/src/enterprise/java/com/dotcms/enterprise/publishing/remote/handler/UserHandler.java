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
import com.dotcms.enterprise.publishing.remote.bundler.UserBundler;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.UserWrapper;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.util.xstream.XStreamHandler;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.UserProxyAPI;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.PushPublishLogger.PushPublishAction;
import com.dotmarketing.util.PushPublishLogger.PushPublishHandler;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collection;

/**
 * This handler class is part of the Push Publishing mechanism that deals with User-related information inside a
 * bundle and saves it in the receiving instance. This class will read and process only the {@link User} data files.
 * <p>
 * There are two types of user logins in dotCMS: front-end and back-end. Front end user accounts give access to
 * restricted content and/or restricted sections of the front-end (public facing portion) of your site - very much like
 * a traditional account login system used at many other sites. Back-end users are users who are able to login into the
 * dotCMS back-end console. Back-end users are extended Permissions which give them access to tools used to add and
 * modify content or otherwise change your site (as their permissions allow).
 *
 * @author Jonathan Gamba
 * @since 5/29/13
 */
public class UserHandler implements IHandler {

    private PublisherConfig config;

    public UserHandler ( PublisherConfig config ) {
        this.config = config;
    }

    @Override
    public String getName () {
        return this.getClass().getName();
    }

    /**
     * Method that will handle pushed users operations
     *
     * @param bundleFolder
     * @throws Exception
     */
    @Override
    public void handle ( File bundleFolder ) throws Exception {

        if ( LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level ) {
            throw new RuntimeException( "need an enterprise pro license to run this" );
        }
        File workingOn = null;
        User user = null;
        try {

            UserAPI userAPI = APILocator.getUserAPI();
            UserProxyAPI userProxyAPI = APILocator.getUserProxyAPI();

            //First we should get the authentication type for this company
            Company company = PublicCompanyFactory.getDefaultCompany();
            String authType = company.getAuthType();

            //Get the list of wrapper files
            Collection<File> usersData = FileUtil.listFilesRecursively( bundleFolder, new UserBundler().getFileFilter() );
            XStream xstream = XStreamHandler.newXStreamInstance();
            for ( File userData : usersData ) {
                workingOn = userData;
                if ( userData.isDirectory() ) {
                    continue;
                }

                //Reconstruct the wrapper object with all the user information
                UserWrapper wrapper;
                try(final InputStream input = Files.newInputStream(userData.toPath())){
                    wrapper = (UserWrapper) xstream.fromXML(input);
                }
                PushPublisherConfig.Operation operation = wrapper.getOperation();
                //Retrieve all the user information saved on the wrapper
                user = wrapper.getUser();
                Role userRole = wrapper.getUserRole();

                //Verify if the user already exist
                User foundUser = null;
                try {
                    if ( authType.equals( Company.AUTH_TYPE_ID ) ) {
                        foundUser = userAPI.loadUserById( user.getUserId(), userAPI.getSystemUser(), false );
                    } else {
                        try {
                            foundUser = userAPI.loadByUserByEmail( user.getEmailAddress(), userAPI.getSystemUser(), false );
                        } catch (NoSuchUserException nsuex){
                            //Fallback to user id.
                            foundUser = userAPI.loadUserById( user.getUserId(), userAPI.getSystemUser(), false );
                        }
                    }
                } catch ( NoSuchUserException e ) {
                    //Do nothing...., we just didn't find a user with the given user id
                }

                if ( operation.equals( PushPublisherConfig.Operation.UNPUBLISH ) ) {
                    //Delete the user, this will delete also all its related data
                    if ( foundUser != null ) {
                        userAPI.delete( user, userAPI.getSystemUser(), false );

                        PushPublishLogger.log(getClass(), PushPublishHandler.USER, PushPublishAction.UNPUBLISH,
                                user.getUserId(), null, user.getFullName(), config.getId());
                    }
                } else {

                    //If the user doesn't exist we need to create it
                    if ( foundUser == null ) {
                        User savedUser = userAPI.createUser( user.getUserId(), user.getEmailAddress() );
                        user.setCompanyId( savedUser.getCompanyId() );
                        APILocator.getRoleAPI().save(userRole, userRole.getId());
                    }
                    //Saving the user
                    userAPI.save( user, userAPI.getSystemUser(), false );



                    PushPublishLogger.log(getClass(), PushPublishHandler.USER, PushPublishAction.PUBLISH,
                            user.getUserId(), null, user.getFullName(), config.getId());
                }

            }
        } catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when processing User in '%s' with ID '%s': %s",
                    workingOn, (null == user ? "(empty)" : user.getEmailAddress()), (null == user ? "(empty)" : user
                            .getUserId()), e.getMessage());
            Logger.error(this.getClass(), errorMsg, e);
            throw new DotPublishingException(errorMsg, e);
        }
    }

}