package com.dotcms;

import com.dotcms.keyvalue.business.KeyValueAPITest;
import com.dotcms.repackage.net.sf.hibernate.HibernateException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.BaseMessageResources;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.io.*;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;

/**
 * Created by Jonathan Gamba.
 * Date: 3/6/12
 * Time: 4:36 PM
 * <p/>
 * Annotations that can be use: {@link org.junit.BeforeClass @BeforeClass}, {@link org.junit.Before @Before},
 * {@link org.junit.Test @Test}, {@link org.junit.AfterClass @AfterClass},
 * {@link org.junit.After @After}, {@link org.junit.Ignore @Ignore}
 * <br>For managing the assertions use the static class {@link org.junit.Assert Assert}
 */
public abstract class IntegrationTestBase extends BaseMessageResources {

    private static Boolean debugMode = Boolean.FALSE;
    private final static PrintStream stdout = System.out;
    private final static ByteArrayOutputStream output = new ByteArrayOutputStream();

    protected static void setDebugMode (final boolean mode) throws UnsupportedEncodingException {

        debugMode = mode;
        if (debugMode) {

            System.setOut(new PrintStream(output, true, "UTF-8"));
        }
    }

    protected static void cleanupDebug (Class clazz) {

        if (debugMode) {
            try {
                final String fileName = clazz.getName() + System.currentTimeMillis() + ".out";
                FileUtils.writeByteArrayToFile(new File(fileName), output.toByteArray());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                System.setOut(stdout);
            }
        }

    }



    @After
    public void after () throws SQLException, DotHibernateException, HibernateException {

        //Closing the session
        HibernateUtil.getSession().connection().close();
        HibernateUtil.getSession().close();
    }

    /**
     * Util method to delete all the roles.
     */
    public void cleanRoles(final List<Role> rolesToDelete) {
        RoleAPI roleAPI = APILocator.getRoleAPI();

        for (Role role : rolesToDelete) {
            try {
                if (role != null && UtilMethods.isSet(role.getId())) {
                    roleAPI.delete(role);
                }
            } catch (DotDataException e) {
                Assert.fail("Can't delete role: " + role.getName() + ", with id:" + role.getId()
                        + ", Exception: " + e.getMessage());
            }
        }
    }

    /**
     * Util method to delete all the folders.
     */
    public void cleanFolders(final List<Folder> foldersToDelete) {
        final FolderAPI folderAPI = APILocator.getFolderAPI();
        final User systemUser = APILocator.systemUser();

        for (Folder folder : foldersToDelete) {
            try {
                if (folder != null && UtilMethods.isSet(folder.getIdentifier())) {
                    folderAPI.delete(folder, systemUser, false);
                }
            } catch (DotSecurityException | DotDataException e) {
                Assert.fail("Can't delete Folder: " + folder.getName() + ", with id:" + folder
                        .getIdentifier()
                        + ", Exception: " + e.getMessage());
            }
        }
    }

    /**
     * Util method to delete all the users.
     */
    public void cleanUsers(final List<User> usersToDelete) {
        UserAPI userAPI = APILocator.getUserAPI();

        for (final User user : usersToDelete) {
            try {
                if (user != null && UtilMethods.isSet(user.getUserId())) {
                    userAPI.delete(user, userAPI.getSystemUser(), false);
                }
            } catch (DotSecurityException | DotDataException e) {
                Assert.fail("Can't delete Folder: " + user.getFullName() + ", with id:" + user
                        .getUserId()
                        + ", Exception: " + e.getMessage());
            }
        }
    }

    /**
     * Util method to delete all the hosts.
     */
    public void cleanHosts(List<Host> hostsToDelete) {
        HostAPI hostAPI = APILocator.getHostAPI();
        User systemUser = APILocator.systemUser();

        for (Host host : hostsToDelete) {
            if (host != null && UtilMethods.isSet(host.getIdentifier())) {
                try {
                    hostAPI.archive(host, systemUser, false);
                    hostAPI.delete(host, systemUser, false);
                } catch (DotDataException | DotSecurityException e) {
                    Assert.fail("Can't delete Folder: " + host.getName() + ", with id:" + host
                            .getIdentifier()
                            + ", Exception: " + e.getMessage());
                }
            }
        }
    }

}