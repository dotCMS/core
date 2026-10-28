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
import com.dotcms.enterprise.PasswordFactoryProxy;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.UserBundler;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.UserWrapper;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.util.xstream.XStreamHandler;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.PushPublishLogger.PushPublishAction;
import com.dotmarketing.util.PushPublishLogger.PushPublishHandler;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Base64;
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

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PublisherConfig config;

    public UserHandler ( PublisherConfig config ) {
        this.config = config;
    }

    @Override
    public String getName () {
        return this.getClass().getName();
    }

    /**
     * Reads the users carried by the bundle and creates the ones that do not exist on this
     * instance yet.
     * <p>
     * A user that already exists here, matched either by user ID or by email address, is kept
     * exactly as it is: the bundle can neither change nor remove it. A new user is created with
     * the names and details from the bundle, but with a newly generated password instead of the
     * one in the bundle, so the person signs in through SSO or resets the password. Its own role
     * keeps the ID it has on the sending instance, so other pushed assets that refer to it still
     * match, while every other attribute of that role is set the way dotCMS sets it for any user.
     * A new user whose ID is the key of an existing role is not created, because a user's own
     * role is found by the user ID. Neither is a new user whose ID is not trimmed and in lower
     * case, the form dotCMS stores user IDs in, since that check compares keys exactly.
     *
     * @param bundleFolder the folder where the bundle was extracted
     * @throws Exception if a user file cannot be read or a new user cannot be saved
     */
    @Override
    public void handle ( File bundleFolder ) throws Exception {

        if ( LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level ) {
            throw new RuntimeException( "need an enterprise pro license to run this" );
        }
        File workingOn = null;
        User user = null;
        try {

            final UserAPI userAPI = APILocator.getUserAPI();

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

                if ( this.existsLocally( user ) ) {
                    Logger.info( this, String.format( "User '%s' already exists on this instance, it is kept as it is",
                            forLog( user.getUserId() ) ) );
                    continue;
                }

                if ( operation.equals( PushPublisherConfig.Operation.UNPUBLISH ) ) {
                    //Nothing to remove, the user does not exist here
                    continue;
                }

                //dotCMS stores user IDs trimmed and in lower case, and the checks below need the stored form
                if ( UtilMethods.isSet( user.getUserId() ) && !isStoredForm( user.getUserId() ) ) {
                    Logger.warn( this, String.format( "User '%s' was not created: its ID is not trimmed and in lower case",
                            forLog( user.getUserId() ) ) );
                    continue;
                }

                //A user's own role is found by the user ID, so the ID cannot be the key of another role
                if ( UtilMethods.isSet( user.getUserId() )
                        && null != APILocator.getRoleAPI().loadRoleByKey( user.getUserId() ) ) {
                    Logger.warn( this, String.format( "User '%s' was not created: its ID is the key of an existing role",
                            forLog( user.getUserId() ) ) );
                    continue;
                }

                final User savedUser = userAPI.createUser( user.getUserId(), user.getEmailAddress() );
                user.setCompanyId( savedUser.getCompanyId() );
                user.setPassword( PasswordFactoryProxy.generateHash( newRandomPassword() ) );
                user.setPasswordEncrypted( true );
                this.savePersonalRole( user, wrapper.getUserRole() );
                //Saving the user
                userAPI.save( user, userAPI.getSystemUser(), false );

                PushPublishLogger.log(getClass(), PushPublishHandler.USER, PushPublishAction.PUBLISH,
                        user.getUserId(), null, user.getFullName(), config.getId());
            }
        } catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when processing User in '%s' with email '%s' [%s]: %s",
                    workingOn, (null == user ? "(empty)" : forLog( user.getEmailAddress() )),
                    (null == user ? "(empty)" : forLog( user.getUserId() )), ExceptionUtil.getErrorMessage(e));
            Logger.error(this.getClass(), errorMsg, e);
            throw new DotPublishingException(errorMsg, e);
        }
    }

    /**
     * Tells whether the given user already exists on this instance, looking it up both by user ID
     * and by email address, whatever login type the company uses.
     *
     * @param user the {@link User} read from the bundle
     * @return {@code true} if a user with the same ID or the same email address exists here
     * @throws DotDataException if the users cannot be looked up
     * @throws DotSecurityException if the system user is not allowed to look the users up
     */
    private boolean existsLocally ( final User user ) throws DotDataException, DotSecurityException {
        final UserAPI userAPI = APILocator.getUserAPI();
        final User systemUser = userAPI.getSystemUser();
        if ( UtilMethods.isSet( user.getUserId() ) ) {
            try {
                if ( null != userAPI.loadUserById( user.getUserId(), systemUser, false ) ) {
                    return true;
                }
            } catch ( final NoSuchUserException e ) {
                //No user with this ID, keep looking by email address
            }
        }
        if ( UtilMethods.isSet( user.getEmailAddress() ) ) {
            try {
                return null != userAPI.loadByUserByEmail( user.getEmailAddress(), systemUser, false );
            } catch ( final NoSuchUserException e ) {
                //No user with this email address either
            }
        }
        return false;
    }

    /**
     * Creates the own role of a new user with the ID that role has on the sending instance, so
     * workflows and other pushed assets that refer to it by ID still match. Only the ID is taken
     * from the bundle; the name, key, parent and flags are the ones dotCMS gives any user's own
     * role. When the bundle has no usable ID, or a role with that ID already exists, nothing is
     * created here and dotCMS sets up the user's own role, with a new ID, when the user is saved.
     *
     * @param user the new {@link User}, whose ID is not the key of any existing role
     * @param roleFromBundle the user's own {@link Role} as it came in the bundle, may be {@code null}
     * @throws DotDataException if the role cannot be saved
     */
    private void savePersonalRole ( final User user, final Role roleFromBundle ) throws DotDataException {
        if ( null == roleFromBundle || !UtilMethods.isSet( roleFromBundle.getId() ) ) {
            return;
        }
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final String roleId = roleFromBundle.getId();
        if ( null != roleAPI.loadRoleById( roleId ) ) {
            Logger.info( this, String.format( "Role ID '%s' of user '%s' is already used on this instance, "
                    + "the user gets a new one", forLog( roleId ), forLog( user.getUserId() ) ) );
            return;
        }
        final Role usersRoot = roleAPI.loadRoleByKey( RoleAPI.USERS_ROOT_ROLE_KEY );
        final Role personalRole = new Role();
        personalRole.setId( roleId );
        personalRole.setName( user.getFullName() );
        personalRole.setDescription( "" );
        personalRole.setRoleKey( user.getUserId() );
        personalRole.setParent( usersRoot.getId() );
        personalRole.setDBFQN( usersRoot.getId() + " --> " + roleId );
        personalRole.setEditPermissions( true );
        personalRole.setEditUsers( false );
        personalRole.setEditLayouts( true );
        personalRole.setLocked( false );
        personalRole.setSystem( true );
        roleAPI.save( personalRole, roleId );
    }

    /**
     * Tells whether the user ID is in the form dotCMS stores user IDs in: trimmed and in lower
     * case. dotCMS always creates IDs in that form, so a bundle from another instance carries them
     * that way.
     *
     * @param userId the user ID read from the bundle, not {@code null}
     * @return {@code true} if the ID is trimmed and in lower case
     */
    private static boolean isStoredForm ( final String userId ) {
        return userId.equals( userId.trim().toLowerCase() );
    }

    /**
     * Generates a long random password that nobody knows, for users created from a bundle.
     *
     * @return the generated password, in plain text, to be hashed before it is stored
     */
    private static String newRandomPassword () {
        final byte[] bytes = new byte[32];
        RANDOM.nextBytes( bytes );
        return Base64.getEncoder().encodeToString( bytes );
    }

    /**
     * Turns a value read from the bundle into a single line of limited length, so it can be
     * written to the log as one entry.
     *
     * @param value the value to log, may be {@code null}
     * @return the value without line breaks, cut at 128 characters
     */
    private static String forLog ( final String value ) {
        if ( null == value ) {
            return "null";
        }
        final String oneLine = value.replaceAll( "[\\r\\n]", "_" );
        return oneLine.length() > 128 ? oneLine.substring( 0, 128 ) + "..." : oneLine;
    }

}
