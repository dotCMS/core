package com.dotmarketing.portlets.contentlet.business;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.elasticsearch.client.Client;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotcms.contenttype.business.ContentTypeAPIImpl;
import com.dotcms.contenttype.business.ContentTypeFactory;
import com.dotcms.contenttype.business.ContentTypeFactoryImpl;
import com.dotcms.contenttype.business.FieldAPIImpl;
import com.dotcms.contenttype.business.FieldFactoryImpl;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.ContentletBaseTest;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.portal.model.User;

/**
 * Created by Jonathan Gamba.
 * Date: 3/20/12
 * Time: 12:12 PM
 */
public class ReindexIndexAPITest extends ContentletBaseTest {

    private static boolean respectFrontendRoles = false;
    protected static User user;
    protected static ContentTypeFactory contentTypeFactory;
    protected static ContentTypeAPIImpl contentTypeApi;
    protected static FieldFactoryImpl fieldFactory;
    protected static FieldAPIImpl fieldApi;
    protected static Host defaultHost;
    protected static Language lang;
    protected static Folder folder ;


    @BeforeClass
    public static void prepare () throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        contentletAPI = APILocator.getContentletAPI();
        user = APILocator.systemUser();
        contentTypeApi = (ContentTypeAPIImpl) APILocator.getContentTypeAPI(user);
        contentTypeFactory = new ContentTypeFactoryImpl();
        fieldFactory = new FieldFactoryImpl();
        fieldApi = new FieldAPIImpl();
        defaultHost = APILocator.getHostAPI().findDefaultHost(user, respectFrontendRoles);
        folder = APILocator.getFolderAPI().findSystemFolder();
        lang = APILocator.getLanguageAPI().getDefaultLanguage();
        HttpServletRequest pageRequest = new MockSessionRequest(
            new MockAttributeRequest(
                new MockHttpRequest("localhost", "/").request()
            ).request())
            .request();
        HttpServletRequestThreadLocal.INSTANCE.setRequest(pageRequest);
        Client client = new ESClient().getClient();
    }

    /**
     * https://github.com/dotCMS/core/issues/11716
     * @throws DotDataException
     * @throws DotSecurityException
     */

    @Test
    public void addRemoveContentFromIndex () throws DotDataException, DotSecurityException {
        // respect CMS Anonymous permissions

        Host host = APILocator.getHostAPI().findDefaultHost(user, respectFrontendRoles);
        Folder folder = APILocator.getFolderAPI().findSystemFolder();

        int num=2;
        ContentType type = APILocator.getContentTypeAPI(user).find("webPageContent");
        List<Contentlet> origCons = new ArrayList<>();

        Map map = new HashMap<>();
        map.put("stInode", type.id());
        map.put("host", host.getIdentifier());
        map.put("folder", folder.getInode());
        map.put("languageId", lang.getId());
        map.put("sortOrder", new Long(0));
        map.put("body", "body");


        //add 5 contentlets
        for(int i = 0;i<num;i++){
            map.put("title", i+ "indexFailTestTitle : ");

            // create a new piece of content backed by the map created above
            Contentlet content = new Contentlet(map);

            // check in the content
            content= contentletAPI.checkin(content,user, respectFrontendRoles);

            assertTrue( content.getIdentifier()!=null );
            assertTrue( content.isWorking());
            assertFalse( content.isLive());
            // publish the content
            contentletAPI.publish(content, user, respectFrontendRoles);
            assertTrue( content.isLive());
            origCons.add(content);
        }


        //commit it index
        HibernateUtil.closeSession();


        // let any expected reindex finish
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        for(Contentlet c : origCons){

            // are we good in the index?
            assertTrue(contentletAPI.indexCount("+live:true +identifier:" +c.getIdentifier() + " +inode:" + c.getInode() , user, respectFrontendRoles)>0);
        }


        HibernateUtil.startTransaction();
        try{
            List<Contentlet> checkedOut=contentletAPI.checkout(origCons, user, respectFrontendRoles);
            for(Contentlet c : checkedOut){
                c.setStringProperty("title", c.getStringProperty("title") + " new");
                c = contentletAPI.checkin(c,user, respectFrontendRoles);
                contentletAPI.publish(c, user, respectFrontendRoles);
                assertTrue( c.isLive());
            }
            throw new DotDataException("uh oh, what happened?");
        }
        catch(DotDataException e){
            HibernateUtil.rollbackTransaction();

        }
        finally{
            HibernateUtil.closeSession();
        }


        // let any expected reindex finish
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        // make sure that the index is in the same state as before the failed transaction
        for(Contentlet c : origCons){
            assertTrue(contentletAPI.indexCount("+live:true +identifier:" +c.getIdentifier() + " +inode:" + c.getInode() , user, respectFrontendRoles)>0);
        }

    }



}