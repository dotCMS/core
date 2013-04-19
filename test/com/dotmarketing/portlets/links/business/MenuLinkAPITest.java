package com.dotmarketing.portlets.links.business;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.TestBase;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import static org.junit.Assert.*;

public class MenuLinkAPITest extends TestBase {
    static PermissionAPI pAPI = APILocator.getPermissionAPI();
    static FolderAPI fAPI = APILocator.getFolderAPI();
    static MenuLinkAPI mAPI = APILocator.getMenuLinkAPI();
    static ContentletAPI cAPI = APILocator.getContentletAPI();
    static HostAPI hAPI = APILocator.getHostAPI();
    static UserAPI uAPI = APILocator.getUserAPI();
    static Host host=null;
    static User user=null;
    
    @BeforeClass
    public static void prepare() throws Exception {
        user = uAPI.getSystemUser();
        host = new Host();
        host.setHostname("MenuLinkTest"+UUIDGenerator.generateUuid());
        host = hAPI.save(host, user, false);
        hAPI.publish(host, user, false);
        cAPI.isInodeIndexed(host.getInode(),true);
        pAPI.permissionIndividually(hAPI.findSystemHost(),host, user, false);
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        hAPI.unpublish(host, user, false);
        hAPI.archive(host, user, false);
    }
    
    @Test
    public void move() throws Exception {
        
        /*
         * Make sure chaning from a folder to other respect target folder permissions and inheritance
         */
        Folder parent1 = fAPI.createFolders("/parent1/sub", host, user, false);
        Folder parent2 = fAPI.createFolders("/parent2/sub", host, user, false);
        pAPI.permissionIndividually(host, parent2, user, false);
        
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
        assertEquals(existingIdent,link.getIdentifier());
        assertEquals(existingInode,link.getInode());
    }
}
