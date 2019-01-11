package com.dotmarketing.portlets.contentlet.business;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.LicenseTestUtil;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.StructureDataGen;
import com.dotcms.enterprise.HostAssetsJobProxy;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.init.DotInitScheduler;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.quartz.job.HostCopyOptions;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
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
public class HostAPITest {

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
    public void testDeleteHost() throws Exception {

        User user = APILocator.getUserAPI().getSystemUser();

        Host source = APILocator.getHostAPI().findByName("demo.dotcms.com", user, false);

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
        defaultHost.setIndexPolicy(IndexPolicy.FORCE);

        //Create a new test host
        final String newHostName = "test" + System.currentTimeMillis() + ".dotcms.com";
        Host host = createHost(newHostName, user);
        String newHostIdentifier = host.getIdentifier();

        //Publish the host
        host.setIndexPolicy(IndexPolicy.FORCE);
        APILocator.getHostAPI().publish(host, user, false);
        //And make it default
        host.setIndexPolicy(IndexPolicy.FORCE);
        APILocator.getHostAPI().makeDefault(host, user, false);

        host = APILocator.getHostAPI().find(host.getIdentifier(), user, false);
        defaultHost = APILocator.getHostAPI().find(defaultHost.getIdentifier(), user, false);
        Assert.assertNotNull(host);
        Assert.assertNotNull(defaultHost);

        /*
         * Validate if the previous default host. Is live and not default
         */
        Assert.assertTrue(defaultHost.isLive());
        Assert.assertFalse(defaultHost.isDefault());

        /*
         * get Back to default the previous host
         */
        APILocator.getHostAPI().makeDefault(defaultHost, user, false);

        host = APILocator.getHostAPI().find(host.getIdentifier(), user, false);
        defaultHost = APILocator.getHostAPI().find(defaultHost.getIdentifier(), user, false);
        Assert.assertNotNull(host);
        Assert.assertNotNull(defaultHost);

        /*
         * Validate if the new host is not default anymore and if its live
         */
        Assert.assertTrue(host.isLive());
        Assert.assertFalse(host.isDefault());

        Assert.assertTrue(defaultHost.isLive());
        Assert.assertTrue(defaultHost.isDefault());

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
        Assert.assertTrue(hosts.size() >= 2 && hosts.getTotalResults() >= 2);
        Assert.assertTrue(hosts.contains(host));

        //Do a more specific search
        hosts = APILocator.getHostAPI()
                .search(newHostName, Boolean.FALSE, Boolean.FALSE, 0, 0, user, Boolean.TRUE);
        //Validate if the search is bringing the right amount of results
        Assert.assertTrue(hosts.size() == 1 && hosts.getTotalResults() == 1);
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
        Assert.assertTrue(hosts.size() == 0 && hosts.getTotalResults() == 0);
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

        final String newHostName = "test" + System.currentTimeMillis() + ".dotcms.com";

        //Create a new test host
        Host host = createHost(newHostName, user);
        String newHostIdentifier = host.getIdentifier();

        //Archive the just created host in order to be able to delete it
        archiveHost(host, user);

        //Create a test content type
        Structure testStructure = new StructureDataGen()
                .structureType(BaseContentType.CONTENT)
                .defaultType(defaultType)
                .system(system)
                .host(host)
                .nextPersisted();
        createStructure(testStructure, newHostIdentifier, user);

        //Delete the just created host
        deleteHost(host, user);

        //Make sure the host was deleted properly
        hostDoesNotExistCheck(newHostIdentifier, newHostName, user);

        if (defaultType || system) {

            //Make sure the content type was NOT deleted
            try {
                final ContentType foundContentType = APILocator.getContentTypeAPI(user)
                        .find(testStructure.getVelocityVarName());
                Assert.assertNotNull(
                        foundContentType);
                Assert.assertEquals(foundContentType.id(), testStructure.id());
                Assert.assertEquals(foundContentType.variable(),
                        testStructure.getVelocityVarName());

                //Make sure the host was changed to SYSTEM_HOST
                Assert.assertEquals(foundContentType.host(),
                        APILocator.getHostAPI().findSystemHost().getIdentifier());
            } catch (Exception e) {
                Assert.fail(String.format("Unable to create delete test content type [%s] [%s]",
                        testStructure.id(),
                        e.getMessage()));
            }
        } else {

            //Make sure the content type was deleted also
            try {
                final ContentType foundContentType = APILocator.getContentTypeAPI(user)
                        .find(testStructure.getVelocityVarName());
                Assert.assertNull(
                        foundContentType);//The find should throw NotFoundInDbException but just in case
            } catch (NotFoundInDbException e) {
                //Expected, the content type should be deleted
            } catch (Exception e) {
                Assert.fail(String.format("Unable to create delete test content type [%s] [%s]",
                        testStructure.id(),
                        e.getMessage()));
            }
        }
    }

    /**
     * Creates a test structure for a given host id
     */
    private void createStructure(final Structure structure, final String hostId, final User user)
            throws DotDataException, DotSecurityException {

        final String structureId = structure.id();
        final String structureVarName = structure.getVelocityVarName();
        Assert.assertNotNull(structureId);
        Assert.assertNotNull(structureVarName);

        //Make sure was created properly
        ContentType foundContentType = APILocator.getContentTypeAPI(user).find(structureVarName);
        Assert.assertNotNull(foundContentType);
        Assert.assertEquals(foundContentType.id(), structureId);
        Assert.assertEquals(foundContentType.variable(), structureVarName);
        Assert.assertEquals(foundContentType.host(), hostId);
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

}