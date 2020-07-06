package com.dotmarketing.portlets.contentlet.business;

import static com.dotmarketing.portlets.templates.model.Template.ANONYMOUS_PREFIX;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.datagen.*;
import com.dotcms.enterprise.HostAssetsJobProxy;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.init.DotInitScheduler;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.quartz.job.HostCopyOptions;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

/**
 * This class will test operations related with interacting with hosts: Deleting
 * a host, marking a host as default, etc.
 *
 * @author Jorge Urdaneta
 * @since Sep 5, 2013
 *
 */
public class HostAPITest extends IntegrationTestBase  {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        LicenseTestUtil.getLicense();
        DotInitScheduler.start();
    }

    /**
     * This test validates the Content Type under the deleted host is also deleted with the host
     */
    @Test
    public void delete_host_with_content_type() throws Exception {
        deleteHostWithContentType(false, false);
    }

    /**
     * This test validates the Content Type under the deleted host is NOT deleted as it is a default
     * Content Type but the host is changed to SYSTEM_HOST
     */
    @Test
    public void delete_host_with_default_type_content_type() throws Exception {
        deleteHostWithContentType(true, false);
    }

    /**
     * This test validates the Content Type under the deleted host is NOT deleted as it is a system
     * Content Type but the host is changed to SYSTEM_HOST
     */
    @Test
    public void delete_host_with_system_content_type() throws Exception {
        deleteHostWithContentType(false, true);
    }

    @Test
    public void testDeleteHostCleanUpTemplates() throws Exception {



        final User user = APILocator.getUserAPI().getSystemUser();
        final Host host = new SiteDataGen().nextPersisted();

        final DotConnect dc = new DotConnect();
        final String query = "select inode.inode from inode left outer join template on template.inode = inode.inode where template.inode is null and inode.type='template'";

        //Verifies that the environment is clean
        dc.setSQL(query);
        assertTrue(dc.loadObjectResults().isEmpty());

        final String body = "<html><body> I'm mostly empty </body></html>";
        final String title = ANONYMOUS_PREFIX + UUIDGenerator.generateUuid();

        Template template = new Template();
        template.setTitle(title);
        template.setBody(body);
        template = APILocator.getTemplateAPI().saveTemplate(template, host, user, false);

        assertNotNull(template);
        assertNotNull(template.getInode());

        assertFalse(template.isShowOnMenu());

        archiveHost(host, user);
        deleteHost(host, user);

        dc.setSQL(query);

        assertTrue(dc.loadObjectResults().isEmpty());

    }

    @Test
    public void testDeleteHost() throws Exception {

        User user = APILocator.getUserAPI().getSystemUser();

        Host source = new SiteDataGen().nextPersisted();
        final ContentType blogContentType = TestDataUtils
                .getBlogLikeContentType("Blog" + System.currentTimeMillis(), source);
        final ContentType employeeContentType = TestDataUtils
                .getEmployeeLikeContentType("Employee" + System.currentTimeMillis(), source);
        final ContentType newsContentType = TestDataUtils
                .getNewsLikeContentType("News" + System.currentTimeMillis(), source);

        TestDataUtils.getBlogContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                blogContentType.id(), source);
        TestDataUtils.getBlogContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                blogContentType.id(), source);
        TestDataUtils
                .getEmployeeContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        employeeContentType.id(), source);
        TestDataUtils
                .getEmployeeContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        employeeContentType.id(), source);
        TestDataUtils.getNewsContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                newsContentType.id(), source);
        TestDataUtils.getNewsContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                newsContentType.id(), source);

        TestDataUtils.getGenericContentContent(true,
                APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                source);
        TestDataUtils.getGenericContentContent(true,
                APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                source);
        TestDataUtils.getGenericContentContent(true,
                APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                source);

        //Create a new test host
        Host host = createHost("copy" + System.currentTimeMillis() + ".demo.dotcms.com", user);

        Thread.sleep(5000);
        String newHostIdentifier = host.getIdentifier();
        String newHostName = host.getHostname();

        HostCopyOptions options = new HostCopyOptions(true);

        // mocking JobExecutionContext to execute HostAssetsJobProxy
        final JobExecutionContext jobExecutionContext = mock(JobExecutionContext.class);
        final JobDataMap jobDataMap = mock(JobDataMap.class);
        final JobDetail jobDetail = mock(JobDetail.class);

        when(jobExecutionContext.getJobDetail()).thenReturn(jobDetail);
        when(jobExecutionContext.getJobDetail().getName())
                .thenReturn("setup-host-" + host.getIdentifier());
        when(jobExecutionContext.getJobDetail().getGroup()).thenReturn("setup-host-group");
        when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
        when(jobDataMap.getString("sourceHostId")).thenReturn(source.getIdentifier());
        when(jobDataMap.getString("destinationHostId")).thenReturn(host.getIdentifier());
        when((HostCopyOptions) jobDataMap.get("copyOptions")).thenReturn(options);

        HostAssetsJobProxy hostAssetsJobProxy = new HostAssetsJobProxy();
        hostAssetsJobProxy.execute(jobExecutionContext);

        Thread.sleep(600); // wait a bit for the index

        //Archive the just created host in order to be able to delete it
        archiveHost(host, user);

        //Delete the just created host
        deleteHost(host, user);

        //Make sure the host was deleted properly
        hostDoesNotExistCheck(newHostIdentifier, newHostName, user);
    }

    @Test
    public void makeDefault() throws Exception {

        User user = APILocator.getUserAPI().getSystemUser();

        //Getting the default host
        Host defaultHost = APILocator.getHostAPI().findDefaultHost(user, false);
        defaultHost.setIndexPolicy(IndexPolicy.WAIT_FOR);
        if (!defaultHost.isLive()) {
            APILocator.getHostAPI().publish(defaultHost, user, false);
        }

        //Create a new test host
        final String newHostName = "test" + System.currentTimeMillis() + ".dotcms.com";
        Host host = createHost(newHostName, user);
        String newHostIdentifier = host.getIdentifier();

        //Publish the host
        host.setIndexPolicy(IndexPolicy.WAIT_FOR);
        APILocator.getHostAPI().publish(host, user, false);
        //And make it default
        host.setIndexPolicy(IndexPolicy.WAIT_FOR);
        APILocator.getHostAPI().makeDefault(host, user, false);

        host = APILocator.getHostAPI().find(host.getIdentifier(), user, false);
        defaultHost = APILocator.getHostAPI().find(defaultHost.getIdentifier(), user, false);
        assertNotNull(host);
        assertNotNull(defaultHost);

        /*
         * Validate if the previous default host. Is live and not default
         */
        assertTrue(defaultHost.isLive());
        assertFalse(defaultHost.isDefault());

        /*
         * get Back to default the previous host
         */
        APILocator.getHostAPI().makeDefault(defaultHost, user, false);

        host = APILocator.getHostAPI().find(host.getIdentifier(), user, false);
        defaultHost = APILocator.getHostAPI().find(defaultHost.getIdentifier(), user, false);
        assertNotNull(host);
        assertNotNull(defaultHost);

        /*
         * Validate if the new host is not default anymore and if its live
         */
        assertTrue(host.isLive());
        assertFalse(host.isDefault());

        assertTrue(defaultHost.isLive());
        assertTrue(defaultHost.isDefault());

        //Unpublish, archive and delete the host
        unpublishHost(host, user);
        archiveHost(host, user);
        deleteHost(host, user);

        //Make sure the host was deleted properly
        hostDoesNotExistCheck(newHostIdentifier, newHostName, user);

        /*
         * Validate if the current Original default host is the current default one
         */
        host = APILocator.getHostAPI().findDefaultHost(user, false);
        Assert.assertEquals(defaultHost.getIdentifier(), host.getIdentifier());
    }

    @Test
    public void givenSearch_whenNewHost_thenFindsNewHost() throws Exception {

        User user = APILocator.getUserAPI().getSystemUser();

        new SiteDataGen().name("demo.test2" + System.currentTimeMillis() + ".dotcms.com")
                .nextPersisted();

        final String newHostName = "demo.test" + System.currentTimeMillis() + ".dotcms.com";
        //Create a new test host
        Host host = createHost(newHostName, user);
        final String newHostIdentifier = host.getIdentifier();

        //Publish the host
        host.setIndexPolicy(IndexPolicy.FORCE);
        APILocator.getHostAPI().publish(host, user, false);

        PaginatedArrayList<Host> hosts = APILocator.getHostAPI()
                .search("demo", Boolean.FALSE, Boolean.FALSE, 0, 0, user, Boolean.TRUE);
        //Validate if the search is bringing the right amount of results
        assertTrue(hosts.size() >= 2 && hosts.getTotalResults() >= 2);
        assertTrue(hosts.contains(host));

        //Do a more specific search
        hosts = APILocator.getHostAPI()
                .search(newHostName, Boolean.FALSE, Boolean.FALSE, 0, 0, user, Boolean.TRUE);
        //Validate if the search is bringing the right amount of results
        assertTrue(hosts.size() == 1 && hosts.getTotalResults() == 1);
        Assert.assertEquals(hosts.get(0).getHostname(), newHostName);

        //Unpublish, archive and delete the host
        unpublishHost(host, user);
        archiveHost(host, user);
        deleteHost(host, user);

        //Make sure the host was deleted properly
        hostDoesNotExistCheck(newHostIdentifier, newHostName, user);

        hosts = APILocator.getHostAPI()
                .search("nothing", Boolean.FALSE, Boolean.FALSE, 0, 0, user, Boolean.TRUE);
        //Validate if the search doesn't bring results
        assertTrue(hosts.size() == 0 && hosts.getTotalResults() == 0);
    }

    /**
     * Utility method to verify if Content Types under a just deleted host are also deleted or no,
     * the idea is to validate that Content Types under the deleted host are deleted also EXCEPT for
     * Default or System Content Types. If the deleted host have System or Default content types we
     * need to make sure the host for those Content Types is changed to SYSTEM_HOST.
     */
    private void deleteHostWithContentType(boolean defaultType, boolean system)
            throws DotDataException, DotSecurityException, ExecutionException, InterruptedException {

        User user = APILocator.getUserAPI().getSystemUser();

        //Get the current default content type
        ContentType existingDefaultContentType = null;
        if (defaultType) {
            existingDefaultContentType = APILocator.getContentTypeAPI(user).findDefault();
        }

        final String newHostName = "test" + System.currentTimeMillis() + ".dotcms.com";

        //Create a new test host
        Host host = createHost(newHostName, user);
        String newHostIdentifier = host.getIdentifier();

        //Archive the just created host in order to be able to delete it
        archiveHost(host, user);

        //Create a test content type
        final ContentType testContentType = createContentType(host, defaultType, system, user);

        //Delete the just created host
        deleteHost(host, user);

        //Make sure the host was deleted properly
        hostDoesNotExistCheck(newHostIdentifier, newHostName, user);

        if (defaultType || system) {

            //Make sure the content type was NOT deleted
            try {
                final ContentType foundContentType = APILocator.getContentTypeAPI(user)
                        .find(testContentType.variable());
                assertNotNull(
                        foundContentType);
                Assert.assertEquals(system, foundContentType.system());
                Assert.assertEquals(defaultType, foundContentType.defaultType());
                Assert.assertEquals(testContentType.system(), foundContentType.system());
                Assert.assertEquals(testContentType.defaultType(),
                        foundContentType.defaultType());
                Assert.assertEquals(testContentType.id(), foundContentType.id());
                Assert.assertEquals(testContentType.variable(),
                        foundContentType.variable());

                //Make sure the host was changed to SYSTEM_HOST
                Assert.assertEquals(APILocator.getHostAPI().findSystemHost().getIdentifier(),
                        foundContentType.host());
            } catch (Exception e) {
                Assert.fail(String.format("Unable to create delete test content type [%s] [%s]",
                        testContentType.id(),
                        e.getMessage()));
            } finally {

                //Cleaning up the test data
                try {
                    if (defaultType && null != existingDefaultContentType) {
                        APILocator.getContentTypeAPI(user).setAsDefault(existingDefaultContentType);
                    }
                } catch (Exception e) {
                    //Do nothing...
                }

                try {
                    ContentType clonedContentType = ContentTypeBuilder.builder(testContentType)
                            .system(false).defaultType(false).build();
                    APILocator.getContentTypeAPI(user).delete(clonedContentType);
                } catch (Exception e) {
                    //Do nothing...
                }
            }
        } else {

            //Make sure the content type was deleted also
            try {
                final ContentType foundContentType = APILocator.getContentTypeAPI(user)
                        .find(testContentType.variable());
                Assert.assertNull(
                        foundContentType);//The find should throw NotFoundInDbException but just in case
            } catch (NotFoundInDbException e) {
                //Expected, the content type should be deleted
            } catch (Exception e) {
                Assert.fail(String.format("Unable to create delete test content type [%s] [%s]",
                        testContentType.id(),
                        e.getMessage()));
            }
        }
    }

    /**
     * Creates a test content type for a given host
     */
    private ContentType createContentType(final Host host, final boolean defaultType,
            final boolean system,
            final User user)
            throws DotDataException, DotSecurityException {

        Structure structure = new StructureDataGen()
                .structureType(BaseContentType.CONTENT)
                .system(system)
                .host(host)
                .nextPersisted();

        ContentType contentType = new StructureTransformer(structure).from();
        if (defaultType) {
            contentType = APILocator.getContentTypeAPI(user).setAsDefault(contentType);
        }

        final String structureId = structure.id();
        final String structureVarName = structure.getVelocityVarName();
        assertNotNull(structureId);
        assertNotNull(structureVarName);

        //Make sure was created properly
        ContentType foundContentType = APILocator.getContentTypeAPI(user).find(structureVarName);
        assertNotNull(foundContentType);
        Assert.assertEquals(structureId, foundContentType.id());
        Assert.assertEquals(defaultType, foundContentType.defaultType());
        Assert.assertEquals(system, foundContentType.system());
        Assert.assertEquals(structureVarName, foundContentType.variable());
        Assert.assertEquals(host.getIdentifier(), foundContentType.host());

        return contentType;
    }

    /**
     * Creates a test host with a given host name
     */
    private Host createHost(final String hostName, final User user) throws DotHibernateException {

        Host host = new Host();
        host.setHostname(hostName);
        host.setDefault(false);
        try {
            HibernateUtil.startTransaction();
            host.setIndexPolicy(IndexPolicy.FORCE);
            host = APILocator.getHostAPI().save(host, user, false);
            HibernateUtil.closeAndCommitTransaction();
        } catch (Exception e) {
            HibernateUtil.rollbackTransaction();
            Logger.error(HostAPITest.class, e.getMessage(), e);
            Assert.fail(String.format("Unable to create test host [%s] [%s]", hostName,
                    e.getMessage()));
        }

        return host;
    }

    /**
     * Unpublish a given host
     */
    private void unpublishHost(final Host host, final User user) throws DotHibernateException {

        try {
            HibernateUtil.startTransaction();
            host.setIndexPolicy(IndexPolicy.FORCE);
            APILocator.getHostAPI().unpublish(host, user, false);
            HibernateUtil.closeAndCommitTransaction();
        } catch (Exception e) {
            HibernateUtil.rollbackTransaction();
            Logger.error(HostAPITest.class, e.getMessage(), e);
            Assert.fail(String.format("Unable to unpublish test host [%s] [%s]", host.getHostname(),
                    e.getMessage()));
        }
    }

    /**
     * Archives a given host
     */
    private void archiveHost(final Host host, final User user) throws DotHibernateException {

        try {
            HibernateUtil.startTransaction();
            host.setIndexPolicy(IndexPolicy.FORCE);
            APILocator.getHostAPI().archive(host, user, false);
            HibernateUtil.closeAndCommitTransaction();
        } catch (Exception e) {
            HibernateUtil.rollbackTransaction();
            Logger.error(HostAPITest.class, e.getMessage(), e);
            Assert.fail(String.format("Unable to archive test host [%s] [%s]", host.getHostname(),
                    e.getMessage()));
        }
    }

    /**
     * Deletes a given host
     */
    private void deleteHost(final Host host, final User user)
            throws DotHibernateException, InterruptedException, ExecutionException {

        Optional<Future<Boolean>> hostDeleteResult = Optional.empty();
        try {
            HibernateUtil.startTransaction();
            host.setIndexPolicy(IndexPolicy.FORCE);
            hostDeleteResult = APILocator.getHostAPI().delete(host, user, false, true);
            HibernateUtil.closeAndCommitTransaction();
        } catch (Exception e) {
            HibernateUtil.rollbackTransaction();
            Logger.error(HostAPITest.class, e.getMessage());
            Assert.fail(String.format("Unable to delete test host [%s] [%s]", host.getHostname(),
                    e.getMessage()));
        }

        if (!hostDeleteResult.isPresent()) {
            Thread.sleep(6000); // wait a bit for the index
        } else {
            hostDeleteResult.get().get();
        }
    }

    /**
     * Verifies if a just deleted host was properly deleted
     */
    private void hostDoesNotExistCheck(final String identifier, final String name, final User user)
            throws DotSecurityException, DotDataException {

        //Verify the Host does not exist any more
        Host host = APILocator.getHostAPI().find(identifier, user, false);
        Assert.assertNull(host);

        host = APILocator.getHostAPI().findByName(name, user, false);
        Assert.assertNull(host);
    }

    /**
     * Method to test: {@link HostAPI#resolveHostNameWithoutDefault(String, User, boolean)}
     * When a host exist and the user have permission
     * Should return it
     */
    @Test
    public void shouldReturnExistingHost() throws DotSecurityException, DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();

        this.addPermission(role, host);

        final Host hostReturned = APILocator.getHostAPI().resolveHostNameWithoutDefault(host.getHostname(), user, false).get();
        assertEquals(host, hostReturned);
    }

    /**
     * Method to test: {@link HostAPI#resolveHostNameWithoutDefault(String, User, boolean)}
     * When a host exist but the user does not have permission
     * Should throw a {@link DotSecurityException}
     */
    @Test(expected = DotSecurityException.class)
    public void shouldThrowDotSecurityExceptionWhenUserNotHavePermission() throws DotSecurityException, DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();

        APILocator.getHostAPI().resolveHostNameWithoutDefault(host.getHostname(), user, false);
    }

    /**
     * Method to test: {@link HostAPI#resolveHostNameWithoutDefault(String, User, boolean)}
     * When the host does not exist
     * Should return null
     */
    @Test
    public void shouldReturnNull() throws DotSecurityException, DotDataException {

        final Optional<Host> optional = APILocator.getHostAPI().resolveHostNameWithoutDefault(
                "not_exists_host", APILocator.systemUser(), false);
        assertFalse(optional.isPresent());
    }

    /**
     * Method to test: {@link HostAPI#resolveHostName(String, User, boolean)}
     * When the host does not exist
     * Should return the default host and store it into host cache
     */
    @Test
    public void shouldStoreDefaultHostIntoCache() throws DotSecurityException, DotDataException {
        final String hostName = "not_exists_host";
        final Host notExistsHost = APILocator.getHostAPI().resolveHostName(hostName
                , APILocator.systemUser(), false);
        final Host defaultHost = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), true);
        assertEquals(notExistsHost.getIdentifier(), defaultHost.getIdentifier());

        final HostCache hostCache = CacheLocator.getHostCache();
        final Host hostByAlias = hostCache.getHostByAlias(hostName);

        assertEquals(hostByAlias.getIdentifier(), defaultHost.getIdentifier());
    }

    /**
     * Method to test: {@link HostAPI#resolveHostNameWithoutDefault(String, User, boolean)}
     * When a host exist but the user does not have permission and respectFrontendRoles is true
     * Should return the host
     */
    @Test()
    public void shouldReturnHostWhenRespectFrontendRolesIsTrue() throws DotSecurityException, DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();

        final Host hostReturned = APILocator.getHostAPI().resolveHostNameWithoutDefault(host.getHostname(), user, true).get();
        assertEquals(host, hostReturned);
    }

    private void addPermission(final Role role, final Host host)
            throws DotDataException, DotSecurityException {

        final User systemUser = APILocator.systemUser();

        final Permission permission = new Permission();
        permission.setInode(host.getPermissionId());
        permission.setRoleId(role.getId());
        permission.setPermission(PermissionAPI.PERMISSION_READ);

        APILocator.getPermissionAPI().save(CollectionsUtils.list(permission), host, systemUser, false);
    }

    /**
     * Method to test: {@link HostAPI#findByAlias(String, User, boolean)}
     * When create two host: first one with alias equals to demo.dotcms.com and second  one with alias equals to not-demo.dotcms.com
     *      and find by  demo.dotcms.com
     * Should return the first one
     */
    @Test
    public void shouldReturnHostByAlias() throws DotSecurityException, DotDataException {
        final Host host = new SiteDataGen().aliases("demo.dotcms.com").nextPersisted();
        final Host host_2 = new SiteDataGen().aliases("not-demo.dotcms.com").setDefault(true).nextPersisted();

        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();

        this.addPermission(role, host);

        final Host hostReturned = APILocator.getHostAPI().findByAlias("demo.dotcms.com", user, false);
        assertEquals(host, hostReturned);
        assertNotEquals(host_2, hostReturned);
    }

    /**
     * Method to test: {@link HostAPI#findByAlias(String, User, boolean)}
     * When create one host with multiple alias
     * Should return thehost by alias
     */
    @Test
    public void whenHostHasMultipleAliasshouldReturnHostByAlias() throws DotSecurityException, DotDataException {
        final Host host = new SiteDataGen().aliases("demo.dotcms.com\r\ntest.dotcms.com").nextPersisted();

        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();

        this.addPermission(role, host);

        final Host hostReturned = APILocator.getHostAPI().findByAlias("test.dotcms.com", user, false);
        assertEquals(host, hostReturned);
    }

    /**
     * Method to test: {@link HostAPI#findByAlias(String, User, boolean)}
     * When create two host with alias that both start by prod-
     * Should return the right host by alias
     */
    @Test
    public void whenBothAliasStartByProd() throws DotSecurityException, DotDataException {
        final Host host = new SiteDataGen().aliases("prod-client.dotcms.com").nextPersisted();
        final Host host_2 = new SiteDataGen().aliases("prod-anotherclient.dotcms.com").setDefault(true).nextPersisted();

        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();

        this.addPermission(role, host);

        final Host hostReturned = APILocator.getHostAPI().findByAlias("prod-client.dotcms.com", user, false);
        assertEquals(host, hostReturned);
        assertNotEquals(host_2, hostReturned);

        final Host hostReturned2 = APILocator.getHostAPI().findByAlias("prod-anotherclient.dotcms.com", user, false);
        assertNotEquals(host, hostReturned2);
        assertEquals(host_2, hostReturned2);
    }
}