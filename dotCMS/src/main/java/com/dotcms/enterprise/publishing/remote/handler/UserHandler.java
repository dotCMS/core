package com.dotcms.enterprise.publishing.remote.handler;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.UserBundler;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.UserWrapper;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.UserProxyAPI;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.PushPublishLogger.PushPublishAction;
import com.dotmarketing.util.PushPublishLogger.PushPublishHandler;
import com.liferay.portal.model.Address;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;

/**
 * @author Jonathan Gamba
 *         Date: 5/29/13
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

        try {

            UserAPI userAPI = APILocator.getUserAPI();
            UserProxyAPI userProxyAPI = APILocator.getUserProxyAPI();

            //First we should get the authentication type for this company
            Company company = PublicCompanyFactory.getDefaultCompany();
            String authType = company.getAuthType();

            //Get the list of wrapper files
            Collection<File> usersData = FileUtil.listFilesRecursively( bundleFolder, new UserBundler().getFileFilter() );
            XStream xstream = new XStream( new DomDriver() );
            for ( File userData : usersData ) {

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
                User user = wrapper.getUser();
                UserProxy userProxy = wrapper.getUserProxy();
                List<Address> addresses = wrapper.getAddresses();
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
                    //Do nothing...., we just didn't found a user with the given user id
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

                    //Verify if a user proxy exist
                    UserProxy foundProxy = userProxyAPI.getUserProxy( user.getUserId(), userAPI.getSystemUser(), false );
                    if ( foundProxy != null ) {
                        userProxy.setInode( foundProxy.getInode() );
                        userProxy.setIdentifier( foundProxy.getIdentifier() );
                    } else {
                        userProxy.setIdentifier( null );
                        userProxy.setInode( null );
                    }
                    //Saving the proxy info
                    userProxyAPI.saveUserProxy( userProxy, userAPI.getSystemUser(), false );

                    //Verify if this user already have some addresses, if it have lets remove them in order to add the new ones
                    List<Address> foundAddresses = userAPI.loadUserAddresses( user, userAPI.getSystemUser(), false );
                    if ( foundAddresses != null ) {
                        for ( Address address : foundAddresses ) {
                            userAPI.deleteAddress( address, userAPI.getSystemUser(), false );
                        }
                    }

                    //Saving the received user address
                    if ( addresses != null ) {
                        for ( Address address : addresses ) {
                            address.setAddressId( null );
                            userAPI.saveAddress( user, address, userAPI.getSystemUser(), false );
                        }
                    }

                    PushPublishLogger.log(getClass(), PushPublishHandler.USER, PushPublishAction.PUBLISH,
                            user.getUserId(), null, user.getFullName(), config.getId());
                }

            }
        } catch ( Exception e ) {
            throw new DotPublishingException( e.getMessage(), e );
        }
    }

}