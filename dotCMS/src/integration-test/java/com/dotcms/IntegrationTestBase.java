package com.dotcms;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.repackage.net.sf.hibernate.HibernateException;
import com.dotcms.util.VoidDelegate;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.BaseMessageResources;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.List;

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

    @BeforeClass
    public static void beforeInit() throws Exception {
        HibernateUtil.setAsyncCommitListenersFinalization(true);
    }

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

    /**
     * Runs a delegate on non-license mode
     * @param delegate
     * @throws Exception
     */
    protected static void runNoLicense(final VoidDelegate delegate) throws Exception {


        final String licenseSerial = LicenseUtil.getSerial();

        try {

            LicenseUtil.freeLicenseOnRepo();
            Assert.assertFalse(LicenseUtil.getLevel() > LicenseLevel.STANDARD.level);

            delegate.execute();
        } finally {
            try {
                LicenseUtil.pickLicense(licenseSerial);
                Assert.assertTrue(LicenseUtil.getLevel() > LicenseLevel.STANDARD.level);
            } catch (Exception e) {
                Assert.fail(e.getMessage());
            }
        }

    } // runNoLicense.

    @Before
    public void beforeBase ()  {

        this.initConnection();
    }

    @After
    public void after ()  {

        //Closing the session
        try {
            HibernateUtil.closeAndCommitTransaction();
            if (null != HibernateUtil.getSession()) {
                HibernateUtil.getSession().connection().close();
                HibernateUtil.getSession().close();
            }
        } catch (Exception e) {} finally {
            DbConnectionFactory.closeSilently();
        }
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
                Assert.fail("Can't delete User: " + user.getFullName() + ", with id:" + user
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
                    Assert.fail("Can't delete Host: " + host.getName() + ", with id:" + host
                            .getIdentifier()
                            + ", Exception: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Util method to delete all the categories.
     */
    public void cleanCategories(List<Category> categoriesToDelete) {
        try {
            final CategoryAPI categoryAPI = APILocator.getCategoryAPI();
            final User systemUser = APILocator.systemUser();

            for (Category category : categoriesToDelete) {
                if (category != null && UtilMethods.isSet(category.getInode())) {
                    try {
                        categoryAPI.delete(category, systemUser, false);
                    } catch (DotDataException | DotSecurityException e) {
                        Assert.fail("Can't delete Category: " + category.getCategoryName() + ", with id:"
                                + category.getInode() + ", Exception: " + e.getMessage());
                    }
                }
            }
        }catch (Exception e) {

        }
    }

    protected void initConnection() {

        if (DbConnectionFactory.connectionExists()) {
            DbConnectionFactory.closeSilently(); // start always we a new one
        }
    }
}