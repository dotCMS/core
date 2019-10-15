package com.dotmarketing.db;

import com.dotcms.datagen.SiteDataGen;
import com.dotcms.repackage.net.sf.hibernate.HibernateException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import junit.framework.Assert;
import com.dotcms.repackage.net.sf.hibernate.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(DataProviderRunner.class)
public class HibernateUtilTest {
    
    static User user;
    static Host host;
    
    @BeforeClass
    public static void init() throws Exception {
    	 //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        user = APILocator.getUserAPI().getSystemUser();
        host = APILocator.getHostAPI().findDefaultHost(user, false);
    }
    
    @After
    @Before
    public void prep() throws Exception {
    	Session session = HibernateUtil.getSession();
    	if (session != null) {
    		session.clear();
    	}
        HibernateUtil.closeSession();
    }
    
    /**
     * This test detects if hibernate is saving objects that have change 
     * in the hibernate session on session close/flush. Its commented as
     * we haven't found a way to disable that behavior for hibernate2.
     * @throws Exception
     */
    @Test
    public void updateOnDirtyObjectWhenFlush() throws Exception {
        HibernateUtil.startTransaction();
        
        Container container = new Container();
        String title = "Test container #"+UUIDGenerator.generateUuid();
        container.setTitle(title);
        container.setPreLoop("pre"); container.setPostLoop("post");
        
        List<ContainerStructure> containerStructureList = new ArrayList<ContainerStructure>();
        ContainerStructure cs=new ContainerStructure();
        cs.setStructureId(CacheLocator.getContentTypeCache().getStructureByVelocityVarName("webPageContent").getInode());
        cs.setCode("$body");
        containerStructureList.add(cs);
        container = APILocator.getContainerAPI().save(container, containerStructureList, host, user, false);
        String cInode=container.getInode();
        
        // clear cache lo ensure we load from hibernate
        CacheLocator.getCacheAdministrator().flushAll();
        
        container = (Container)HibernateUtil.load(Container.class, cInode);
        
        container.setTitle("my dirty title");
        
        HibernateUtil.closeSession();
        
        container = (Container)HibernateUtil.load(Container.class, cInode);
        
        Assert.assertEquals(title,container.getTitle());
    }
    
    /**
     * Test the problem with hibernate updating objects that have changed in the 
     * session. ON A READ! at hibernateUtil we set in the session to never autoflush
     * unless we close the session.
     * @throws Exception
     */
    @Test
    public void updateOnDirtyObjectWhenQuery() throws Exception {
        // now test it can happen at load of another object
        final Host newHost = new SiteDataGen().nextPersisted();

        // 1st
        Container container = new Container();
        String title = "Test container #"+UUIDGenerator.generateUuid();
        container.setTitle(title);
        container.setPreLoop("pre"); container.setPostLoop("post");
        
        List<ContainerStructure> containerStructureList = new ArrayList<ContainerStructure>();
        ContainerStructure cs=new ContainerStructure();
        cs.setStructureId(CacheLocator.getContentTypeCache().getStructureByVelocityVarName("webPageContent").getInode());
        cs.setCode("$body");
        containerStructureList.add(cs);
        container = APILocator.getContainerAPI().save(container, containerStructureList, newHost, user, false);
        String cInode=container.getInode();
        
        
        // 2nd
        container = new Container();
        String title2 = "Test 2nd container #"+UUIDGenerator.generateUuid();
        container.setTitle(title2);
        container.setPreLoop("pre"); container.setPostLoop("post");
        containerStructureList = new ArrayList<ContainerStructure>();
        cs=new ContainerStructure();
        cs.setStructureId(CacheLocator.getContentTypeCache().getStructureByVelocityVarName("webPageContent").getInode());
        cs.setCode("$body"); containerStructureList.add(cs);
        container = APILocator.getContainerAPI().save(container, containerStructureList, newHost, user, false);
        String cInode2=container.getInode();
        container = null;
        
        HibernateUtil.closeSession();
        
        // load the first and make it dirty
        Container fst = (Container)HibernateUtil.load(Container.class, cInode);
        fst.setTitle("my dirty title 2");
        
        // load the second. it might trigger an update on the first one
        HibernateUtil hh = new HibernateUtil(Container.class);
        hh.setQuery("from "+Container.class.getName()+" c where c.inode=?");
        hh.setParam(cInode2);
        Container cc = (Container)hh.load();
        Assert.assertEquals(title2, cc.getTitle());
        
        // lets see if in the db it remains the same
        DotConnect dc = new DotConnect();
        dc.setSQL("select title from " + Inode.Type.CONTAINERS.getTableName() + " where inode=?");
        dc.addParam(cInode);
        Assert.assertEquals(title, dc.loadObjectResults().get(0).get("title"));
    }

    @DataProvider
    public static Object[] testCases() {

        final Runnable reindexRunnable=new ReindexRunnable(Collections.emptyList(), ReindexRunnable.Action.ADDING) {};

        final Runnable flushCacheRunnable = new FlushCacheRunnable() {
            public void run() {}
        };

        return new AddCommitListenerTestCase[] {
                new AddCommitListenerTestCase.Builder()
                        .asyncReindexCommitListeners(true)
                        .dotRunnable(reindexRunnable)
                        .expectedAsyncListeners(1)
                        .expectedSyncListeners(0)
                        .createReindexCommitListenerTestCase(),

                new AddCommitListenerTestCase.Builder()
                        .asyncReindexCommitListeners(false)
                        .dotRunnable(reindexRunnable)
                        .expectedAsyncListeners(0)
                        .expectedSyncListeners(1)
                        .createReindexCommitListenerTestCase(),

                new AddCommitListenerTestCase.Builder()
                        .asyncReindexCommitListeners(false)
                        .asyncCommitListeners(true)
                        .dotRunnable(reindexRunnable)
                        .expectedAsyncListeners(0)
                        .expectedSyncListeners(1)
                        .createReindexCommitListenerTestCase(),

                new AddCommitListenerTestCase.Builder()
                        .asyncReindexCommitListeners(true)
                        .asyncCommitListeners(false)
                        .dotRunnable(flushCacheRunnable)
                        .expectedAsyncListeners(0)
                        .expectedSyncListeners(1)
                        .createReindexCommitListenerTestCase(),

                new AddCommitListenerTestCase.Builder()
                        .asyncReindexCommitListeners(false)
                        .asyncCommitListeners(true)
                        .dotRunnable(flushCacheRunnable)
                        .expectedAsyncListeners(1)
                        .expectedSyncListeners(0)
                        .createReindexCommitListenerTestCase(),
        };
    }

    @Test
    @UseDataProvider("testCases")
    public void testAddCommitListener(final AddCommitListenerTestCase testCase) throws DotHibernateException,
            HibernateException, SQLException {
        HibernateUtil.getSession().connection().setAutoCommit(false);

        final boolean originalAsyncReindexCommitListenersValue = Config.getBooleanProperty("ASYNC_REINDEX_COMMIT_LISTENERS", true);
        final boolean originalAsyncCommitListenersValue = Config.getBooleanProperty("ASYNC_COMMIT_LISTENERS", true);

        Config.setProperty("ASYNC_REINDEX_COMMIT_LISTENERS", testCase.isAsyncReindexCommitListeners());
        Config.setProperty("ASYNC_COMMIT_LISTENERS", testCase.isAsyncCommitListeners());

        try {

            HibernateUtil.addCommitListener(testCase.getDotRunnable());

            final Map<String, Runnable> asyncCommitListeners = HibernateUtil.asyncCommitListeners.get();
            final Map<String, Runnable> syncCommitListeners = HibernateUtil.syncCommitListeners.get();
            assertEquals(testCase.getExpectedAsyncListeners(), asyncCommitListeners.size());
            assertEquals(testCase.getExpectedSyncListeners(), syncCommitListeners.size());
            HibernateUtil.getSession().connection().setAutoCommit(true);

        } finally {
            Config.setProperty("ASYNC_REINDEX_COMMIT_LISTENERS", originalAsyncReindexCommitListenersValue);
            Config.setProperty("ASYNC_COMMIT_LISTENERS", originalAsyncCommitListenersValue);
        }
    }


}
