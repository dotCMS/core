package com.dotmarketing.common.reindex;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.felix.framework.OSGIUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.business.ContentTypeAPIImpl;
import com.dotcms.contenttype.business.ContentTypeFactory;
import com.dotcms.contenttype.business.ContentTypeFactoryImpl;
import com.dotcms.contenttype.business.FieldAPIImpl;
import com.dotcms.contenttype.business.FieldFactoryImpl;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.ThreadUtils;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;

/**
 * Created by Jonathan Gamba. Date: 3/20/12 Time: 12:12 PM
 */
public class ReindexThreadTest {

    private static boolean respectFrontendRoles = false;
    protected static User user;

    protected static Host defaultHost;
    protected static Language lang;
    protected static Folder folder;
    protected static ContentletAPI contentletAPI;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        OSGIUtil.getInstance().initializeFramework(Config.CONTEXT);
        contentletAPI = APILocator.getContentletAPI();
        user = APILocator.systemUser();

        defaultHost = APILocator.getHostAPI().findDefaultHost(user, respectFrontendRoles);
        folder = APILocator.getFolderAPI().findSystemFolder();
        lang = APILocator.getLanguageAPI().getDefaultLanguage();
        HttpServletRequest pageRequest =
                new MockSessionRequest(new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request()).request();
        HttpServletRequestThreadLocal.INSTANCE.setRequest(pageRequest);
    }

    /**
     * https://github.com/dotCMS/core/issues/11716
     * 
     * @throws DotDataException
     * @throws DotSecurityException
     */

    @Test
    public void test_content_that_is_rolled_back_does_not_get_in_the_index() throws DotDataException, DotSecurityException {
        // respect CMS Anonymous permissions

        // stop the reindex thread
        ReindexThread.getInstance().pause();

        int num = 2;
        List<Contentlet> origCons = new ArrayList<>();

        ContentType type = new ContentTypeDataGen()
                .fields(ImmutableList
                        .of(ImmutableTextField.builder().name("Title").variable("title").searchable(true).listed(true).build()))
                .nextPersisted();

        for (int i = 0; i < num; i++) {
            Contentlet content = new ContentletDataGen(type.id()).setProperty("title", "contentTest " + System.currentTimeMillis()).next();

            content.setStringProperty("title", i + "indexFailTestTitle : ");

            content.setIndexPolicy(IndexPolicy.FORCE);

            // check in the content
            content = contentletAPI.checkin(content, user, respectFrontendRoles);

            assertTrue(content.getIdentifier() != null);
            assertTrue(content.isWorking());
            assertFalse(content.isLive());
            // publish the content
            content.setIndexPolicy(IndexPolicy.FORCE);
            contentletAPI.publish(content, user, respectFrontendRoles);
            assertTrue(content.isLive());
            origCons.add(content);
        }

        // commit it index
        HibernateUtil.closeSession();

        for (Contentlet c : origCons) {

            // are we good in the index?
            assertTrue(contentletAPI.indexCount("+live:true +identifier:" + c.getIdentifier() + " +inode:" + c.getInode(), user,
                    respectFrontendRoles) > 0);
        }

        HibernateUtil.startTransaction();
        try {
            List<Contentlet> checkedOut = contentletAPI.checkout(origCons, user, respectFrontendRoles);
            for (Contentlet c : checkedOut) {
                c.setStringProperty("title", c.getStringProperty("title") + " new");
                c.setIndexPolicy(IndexPolicy.FORCE);
                c = contentletAPI.checkin(c, user, respectFrontendRoles);
                c.setIndexPolicy(IndexPolicy.FORCE);
                contentletAPI.publish(c, user, respectFrontendRoles);
                assertTrue(c.isLive());
            }
            throw new DotDataException("uh oh, what happened?");
        } catch (DotDataException e) {
            HibernateUtil.rollbackTransaction();

        } finally {
            HibernateUtil.closeSession();
        }

        ReindexThread.getInstance().unpause();

        // let any expected reindex finish
        DateUtil.sleep(10000);

        // make sure that the index is in the same state as before the failed transaction

        for (Contentlet c : origCons) {
            assertTrue(contentletAPI.indexCount("+live:true +identifier:" + c.getIdentifier() + " +inode:" + c.getInode(), user,
                    respectFrontendRoles) > 0);

        }

    }

    /**
     * https://github.com/dotCMS/core/issues/11716
     * 
     * @throws DotDataException
     * @throws DotSecurityException
     */

    @Test
    public void test_reindex_queue_puts_to_the_index() throws DotDataException, DotSecurityException {

        new DotConnect().setSQL("delete from dist_reindex_journal").loadResult();
        ReindexThread.startThread();

        long startCount = ReindexThread.getInstance().totalESPuts();
        ContentType type = new ContentTypeDataGen()
                .fields(ImmutableList
                        .of(ImmutableTextField.builder().name("Title").variable("title").searchable(true).listed(true).build()))
                .nextPersisted();

        String title = "contentTest " + System.currentTimeMillis();
        Contentlet content = new ContentletDataGen(type.id()).setProperty("title", title).nextPersisted();

        ThreadUtils.sleep(4000);
        contentletAPI.publish(content, user, respectFrontendRoles);

        ThreadUtils.sleep(4000);
        long latestCount = ReindexThread.getInstance().totalESPuts() - startCount;
        // 1 for check in (only working index) 2 more for publish (live & working indexes)
        assert (latestCount == 3);

        contentletAPI.unpublish(content, user, respectFrontendRoles);
        ThreadUtils.sleep(4000);

        // 1 more reindex working (publish was deleted)
        latestCount = ReindexThread.getInstance().totalESPuts() - startCount;
        assert (latestCount == 4);
    }

    /**
     * https://github.com/dotCMS/core/issues/11716
     * 
     * @throws DotDataException
     * @throws DotSecurityException
     */

    @Test
    public void test_reindex_thread_removing_from_index() throws DotDataException, DotSecurityException {
        // respect CMS Anonymous permissions

        // stop the reindex thread
        ReindexThread.startThread();

        ContentType type = new ContentTypeDataGen()
                .fields(ImmutableList
                        .of(ImmutableTextField.builder().name("Title").variable("title").searchable(true).listed(true).build()))
                .nextPersisted();
        final String conTitle = "contentTest " + System.currentTimeMillis();

        Contentlet content = new ContentletDataGen(type.id()).setProperty("title", conTitle).next();

        content.setStringProperty("title", conTitle);

        content.setIndexPolicy(IndexPolicy.FORCE);

        // check in the content
        content = contentletAPI.checkin(content, user, respectFrontendRoles);

        assertTrue(content.getIdentifier() != null);
        assertTrue(content.isWorking());
        assertFalse(content.isLive());
        assertTrue(contentletAPI.indexCount("+live:false +identifier:" + content.getIdentifier() + " +inode:" + content.getInode() + " +languageId:"+ content.getLanguageId(), user,
                respectFrontendRoles) > 0);
        assertTrue(contentletAPI.indexCount("+live:true +identifier:" + content.getIdentifier() + " +inode:" + content.getInode() + " +languageId:"+ content.getLanguageId(), user,
                respectFrontendRoles) == 0);

        content.setIndexPolicy(IndexPolicy.FORCE);
        contentletAPI.publish(content, user, respectFrontendRoles);
        assertTrue(content.isWorking());
        assertTrue(content.isLive());
        assertTrue(contentletAPI.indexCount("+live:true +identifier:" + content.getIdentifier() + " +inode:" + content.getInode() + " +languageId:"+ content.getLanguageId(), user,
                respectFrontendRoles) > 0);
        
        // create a spanish content
        content.setLanguageId(2);
        content.setInode(null);
        content = contentletAPI.checkin(content, user, respectFrontendRoles);
        
        assertTrue(contentletAPI.indexCount("+live:false +identifier:" + content.getIdentifier() + " +inode:" + content.getInode() + " +languageId:"+ content.getLanguageId(), user,
                respectFrontendRoles) > 0);
        assertTrue(contentletAPI.indexCount("+live:true +identifier:" + content.getIdentifier() + " +inode:" + content.getInode() + " +languageId:"+ content.getLanguageId(), user,
                respectFrontendRoles) == 0);
        
        content.setIndexPolicy(IndexPolicy.FORCE);
        contentletAPI.publish(content, user, respectFrontendRoles);
        assertTrue(content.isWorking());
        assertTrue(content.isLive());
        assertTrue(contentletAPI.indexCount("+live:true +identifier:" + content.getIdentifier() + " +inode:" + content.getInode() + " +languageId:"+ content.getLanguageId(), user,
                respectFrontendRoles) > 0);
        
        ReindexThread.startThread();
        ThreadUtils.sleep(4000);
        APILocator.getReindexQueueAPI().addIdentifierDelete(content.getIdentifier());
        ThreadUtils.sleep(4000);
        assertTrue(contentletAPI.indexCount("+live:false +identifier:" + content.getIdentifier() , user,
                respectFrontendRoles) == 0);
        assertTrue(contentletAPI.indexCount("+live:true +identifier:" + content.getIdentifier() , user,
                respectFrontendRoles) == 0);
        
        
        
    }

}
