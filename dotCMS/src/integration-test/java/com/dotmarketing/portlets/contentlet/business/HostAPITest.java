package com.dotmarketing.portlets.contentlet.business;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import com.dotcms.LicenseTestUtil;
import com.dotcms.enterprise.HostAssetsJobProxy;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.init.DotInitScheduler;
import com.dotmarketing.quartz.job.HostCopyOptions;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;

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

    @Test
    public void testDeleteHost() throws Exception {
        User user=APILocator.getUserAPI().getSystemUser();
        
        Host source=APILocator.getHostAPI().findByName("demo.dotcms.com", user, false);
        
        Host host=new Host();
        host.setHostname("copy"+System.currentTimeMillis()+".demo.dotcms.com");
        host.setDefault(false);
        try{
        	HibernateUtil.startTransaction();
        	host=APILocator.getHostAPI().save(host, user, false);
        	HibernateUtil.commitTransaction();
        }catch(Exception e){
        	HibernateUtil.rollbackTransaction();
        	Logger.error(HostAPITest.class, e.getMessage());
        }
        APILocator.getContentletAPI().isInodeIndexed(host.getInode());
        Thread.sleep(5000);
        String hostIdent=host.getIdentifier();
        String hostName=host.getHostname();
        
        HostCopyOptions options=new HostCopyOptions(true);
        
        // mocking JobExecutionContext to execute HostAssetsJobProxy
        final JobExecutionContext jobExecutionContext = mock(JobExecutionContext.class);
        final JobDataMap jobDataMap = mock(JobDataMap.class);
        final JobDetail jobDetail = mock(JobDetail.class);
        		
        when(jobExecutionContext.getJobDetail()).thenReturn(jobDetail);
        when(jobExecutionContext.getJobDetail().getName()).thenReturn("setup-host-" + host.getIdentifier());
        when(jobExecutionContext.getJobDetail().getGroup()).thenReturn("setup-host-group");
        when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
        when(jobDataMap.getString("sourceHostId")).thenReturn(source.getIdentifier());
        when(jobDataMap.getString("destinationHostId")).thenReturn(host.getIdentifier());
        when((HostCopyOptions) jobDataMap.get("copyOptions")).thenReturn(options);
		
        HostAssetsJobProxy hostAssetsJobProxy = new HostAssetsJobProxy();
        hostAssetsJobProxy.execute(jobExecutionContext);
        
        Thread.sleep(600); // wait a bit for the index
        
        try{
        	HibernateUtil.startTransaction();
        	APILocator.getHostAPI().archive(host, user, false);
        	APILocator.getHostAPI().delete(host, user, false);
        	HibernateUtil.commitTransaction();
        }catch(Exception e){
        	HibernateUtil.rollbackTransaction();
        	Logger.error(HostAPITest.class, e.getMessage());
        }

        Thread.sleep(600); // wait a bit for the index
        
        host = APILocator.getHostAPI().find(hostIdent, user, false);
        
        if(host!=null){
        	APILocator.getHostAPI().delete(host, user, false);
        	Thread.sleep(10000);
        }
        
        host = APILocator.getHostAPI().find(hostIdent, user, false);
        
        Assert.assertNull(host);
        
        host = APILocator.getHostAPI().findByName(hostName, user, false);
        
        Assert.assertNull(host);
    }
    
    @Test
    public void makeDefault() throws Exception {
    	try {
            HibernateUtil.setAsyncCommitListenersFinalization(false);

            User user = APILocator.getUserAPI().getSystemUser();
    	/*
    	 * Get the current Default host
    	 */
            Host hdef = APILocator.getHostAPI().findDefaultHost(user, false);
    	
    	/*
    	 * Create a new Host and make it default
    	 */
            Host host = new Host();
            host.setHostname("test" + System.currentTimeMillis() + ".demo.dotcms.com");
            host.setDefault(false);
            try {
                HibernateUtil.startTransaction();
                host = APILocator.getHostAPI().save(host, user, false);
                HibernateUtil.commitTransaction();
            } catch (Exception e) {
                HibernateUtil.rollbackTransaction();
                Logger.error(HostAPITest.class, e.getMessage());
            }

            APILocator.getHostAPI().publish(host, user, false);
            APILocator.getHostAPI().makeDefault(host, user, false);

            host = APILocator.getHostAPI().find(host.getIdentifier(), user, false);
            APILocator.getContentletAPI().isInodeIndexed(host.getInode());
            APILocator.getContentletAPI().isInodeIndexed(host.getInode(), true);
            hdef = APILocator.getHostAPI().find(hdef.getIdentifier(), user, false);
            APILocator.getContentletAPI().isInodeIndexed(hdef.getInode());
            APILocator.getContentletAPI().isInodeIndexed(hdef.getInode(), true);
        
        /*
         * Validate if the previous default host. Is live and not default
         */
            Assert.assertTrue(hdef.isLive());
            Assert.assertFalse(hdef.isDefault());
        
        /*
         * get Back to default the previous host
         */
            APILocator.getHostAPI().makeDefault(hdef, user, false);

            host = APILocator.getHostAPI().find(host.getIdentifier(), user, false);
            APILocator.getContentletAPI().isInodeIndexed(host.getInode());
            APILocator.getContentletAPI().isInodeIndexed(host.getInode(), true);
            hdef = APILocator.getHostAPI().find(hdef.getIdentifier(), user, false);
            APILocator.getContentletAPI().isInodeIndexed(hdef.getInode());
            APILocator.getContentletAPI().isInodeIndexed(hdef.getInode(), true);
        
        /*
         * Validate if the new host is not default anymore and if its live
         */
            Assert.assertFalse(host.isDefault());
            Assert.assertTrue(host.isLive());

            Assert.assertTrue(hdef.isLive());
            Assert.assertTrue(hdef.isDefault());
        
        /*
         * Delete the new test host
         */
            Thread.sleep(600); // wait a bit for the index
            try {
                HibernateUtil.startTransaction();
                APILocator.getHostAPI().archive(host, user, false);
                APILocator.getHostAPI().delete(host, user, false);
                HibernateUtil.commitTransaction();
            } catch (Exception e) {
                HibernateUtil.rollbackTransaction();
                Logger.error(HostAPITest.class, e.getMessage());
            }
            Thread.sleep(600); // wait a bit for the index
        /*
         * Validate if the current Original default host is the current default one
         */
            host = APILocator.getHostAPI().findDefaultHost(user, false);
            Assert.assertEquals(hdef.getIdentifier(), host.getIdentifier());
        } finally {
            HibernateUtil.setAsyncCommitListenersFinalization(true);
        }
    }
    
    @Test
    public void search() throws Exception {
    	User user=APILocator.getUserAPI().getSystemUser();
    	/*
    	 * Get the current Default host
    	 */
    	Host hdef = APILocator.getHostAPI().findDefaultHost(user, false);
    	
    	/*
    	 * Create a new Host 
    	 */
    	String hostname ="demo.test"+System.currentTimeMillis()+".dotcms.com";
    	Host host=new Host();
        host.setHostname(hostname);
        host.setDefault(false);
        try{
        	HibernateUtil.startTransaction();
        	host=APILocator.getHostAPI().save(host, user, false);
        	HibernateUtil.commitTransaction();
        }catch(Exception e){
        	HibernateUtil.rollbackTransaction();
        	Logger.error(HostAPITest.class, e.getMessage());
        }
        APILocator.getHostAPI().publish(host, user, false);
        APILocator.getContentletAPI().isInodeIndexed(host.getInode());
        PaginatedArrayList<Host> hosts = APILocator.getHostAPI().search("demo", Boolean.FALSE, Boolean.FALSE, 0, 0, user, Boolean.TRUE);
                
        /*
         * Validate if the search is bringing the rigth amount of results
         */
        Assert.assertTrue(hosts.size() >= 2 && hosts.getTotalResults() >= 2);
        Assert.assertTrue(hosts.contains(host));
        
        hosts = APILocator.getHostAPI().search(hostname, Boolean.FALSE, Boolean.FALSE, 0, 0, user, Boolean.TRUE);
        
        Assert.assertTrue(hosts.size() == 1 && hosts.getTotalResults() == 1);
        Assert.assertTrue(hosts.get(0).getHostname().equals(hostname));
        
        /*
         * Delete the new test host
         */
        Thread.sleep(600); // wait a bit for the index
        try{
        	HibernateUtil.startTransaction();
        	APILocator.getHostAPI().unpublish(host, user, false);
        	APILocator.getHostAPI().archive(host, user, false);
        	APILocator.getHostAPI().delete(host, user, false);
        	HibernateUtil.commitTransaction();
        }catch(Exception e){
        	HibernateUtil.rollbackTransaction();
        	Logger.error(HostAPITest.class, e.getMessage());
        }
        Thread.sleep(600); // wait a bit for the index
        
        hosts = APILocator.getHostAPI().search("nothing", Boolean.FALSE, Boolean.FALSE, 0, 0, user, Boolean.TRUE);
        
        /*
         * Validate if the search doesn't bring results
         */
        Assert.assertTrue(hosts.size() == 0 && hosts.getTotalResults() == 0);

    }
    
    
}
