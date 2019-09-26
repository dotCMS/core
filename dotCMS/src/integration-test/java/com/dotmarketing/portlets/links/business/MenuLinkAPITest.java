package com.dotmarketing.portlets.links.business;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.datagen.LinkDataGen;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;

import static org.junit.Assert.*;

public class MenuLinkAPITest extends IntegrationTestBase {
    static PermissionAPI pAPI;
    static FolderAPI fAPI;
    static MenuLinkAPI mAPI;
    static ContentletAPI cAPI;
    static HostAPI hAPI;
    static UserAPI uAPI;
    static Host host=null;
    static User user=null;
    
    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        
        pAPI = APILocator.getPermissionAPI();
        fAPI = APILocator.getFolderAPI();
        mAPI = APILocator.getMenuLinkAPI();
        cAPI = APILocator.getContentletAPI();
        hAPI = APILocator.getHostAPI();
        uAPI = APILocator.getUserAPI();
        
        user = uAPI.getSystemUser();
        host = new Host();
        host.setHostname("MenuLinkTest"+UUIDGenerator.generateUuid());
        try{
        	HibernateUtil.startTransaction();
        	host = hAPI.save(host, user, false);
        	HibernateUtil.closeAndCommitTransaction();
        }catch(Exception e){
        	HibernateUtil.rollbackTransaction();
        	Logger.error(MenuLinkAPITest.class, e.getMessage());
        }
        
        hAPI.publish(host, user, false);
        cAPI.isInodeIndexed(host.getInode(),true);
        pAPI.permissionIndividually(hAPI.findSystemHost(),host, user);
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        try{
        	HibernateUtil.startTransaction();
        	hAPI.unpublish(host, user, false);
        	hAPI.archive(host, user, false);
        	hAPI.delete(host,user,false);
        	HibernateUtil.closeAndCommitTransaction();
        }catch(Exception e){
        	HibernateUtil.rollbackTransaction();
        	Logger.error(MenuLinkAPITest.class, e.getMessage());
        }

    }
    
    @Test
    public void move() throws Exception {
        
        /*
         * Make sure chaning from a folder to other respect target folder permissions and inheritance
         */
        Folder parent1 = fAPI.createFolders("/parent1/sub", host, user, false);
        Folder parent2 = fAPI.createFolders("/parent2/sub", host, user, false);
        pAPI.permissionIndividually(host, parent2, user);
        
        Link link = new Link();
        link.setFriendlyName("test link");
        link.setTitle(link.getFriendlyName());
        link.setHostId(host.getIdentifier());
        link.setLinkType(Link.LinkType.EXTERNAL.toString());
        link.setUrl("google.com");
        link.setProtocal("http://");
        mAPI.save(link, parent1, user, false);
        
        // must be getting permissions from the host
        assertEquals(host.getPermissionId(), pAPI.findParentPermissionable(link).getPermissionId());
        assertTrue(mAPI.move(link, parent2, user, false));
        // then it should live under parent2
        assertEquals(parent2.getPermissionId(), pAPI.findParentPermissionable(link).getPermissionId());
        
    }
    
    @Test
    public void save() throws Exception {
    	HibernateUtil.startTransaction();
        Folder folder = fAPI.createFolders("/testsave", host, user, false);
        Link link = new Link();
        link.setFriendlyName("test link");
        link.setTitle(link.getFriendlyName());
        link.setHostId(host.getIdentifier());
        link.setLinkType(Link.LinkType.EXTERNAL.toString());
        link.setUrl("google.com");
        link.setProtocal("http://");
        mAPI.save(link, folder, user, false);
        assertTrue(InodeUtils.isSet(link.getInode()));
        assertTrue(InodeUtils.isSet(link.getIdentifier()));
        
        link = new Link();
        String existingInode = UUIDGenerator.generateUuid();
        String existingIdent = UUIDGenerator.generateUuid();
        link.setInode(existingInode);
        link.setIdentifier(existingIdent);
        link.setFriendlyName("test link");
        link.setTitle(link.getFriendlyName());
        link.setHostId(host.getIdentifier());
        link.setLinkType(Link.LinkType.EXTERNAL.toString());
        link.setUrl("google.com");
        link.setProtocal("http://");
        mAPI.save(link, folder, user, false);
        HibernateUtil.closeAndCommitTransaction();
        assertEquals(existingIdent,link.getIdentifier());
        assertEquals(existingInode,link.getInode());
    }

    @Test
    public void testFindLinks() throws DotDataException, DotSecurityException {
        final List<Link> result = mAPI
                .findLinks(user, false, null, hAPI.findDefaultHost(user, false).getIdentifier(), null, null, null, 0, -1, null);

        assertNotNull(result);
        assertTrue(!result.isEmpty());
    }

    @Test (expected = NotFoundInDbException.class)
    public void testFind_InodeNotExists_ReturnNotFoundInDBException()
            throws DotSecurityException, DotDataException {

        final Link link = mAPI.find("inodeNotExists",user,false);
    }

    @Test
    public void testFind_returnLink()
            throws DotSecurityException, DotDataException {
        final Link menuLink = new LinkDataGen(fAPI.findSystemFolder()).hostId(host.getIdentifier())
                .showOnMenu(true).nextPersisted();
        APILocator.getVersionableAPI().setLive(menuLink);

        final Link link = mAPI.find(menuLink.getInode(), user, false);

        assertEquals(menuLink.getInode(), link.getInode());
        assertEquals(menuLink.getLinkType(), link.getLinkType());
        assertEquals(menuLink.getUrl(), link.getUrl());
    }

}
