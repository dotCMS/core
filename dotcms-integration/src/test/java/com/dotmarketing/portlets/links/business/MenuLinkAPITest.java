package com.dotmarketing.portlets.links.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.datagen.LinkDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * A test class for verifying the functionalities and behaviors of the MenuLinkAPI. This class
 * includes various test methods to validate the creation, movement, retrieval, and saving of menu
 * links in the application. Designed as an integration test, it operates in the context of a fully
 * initialized application environment.
 * <p>
 * The `MenuLinkAPITest` class extends `IntegrationTestBase`, ensuring that all tests execute in a
 * consistent integration test setup.
 * <p>
 * Features of `MenuLinkAPITest`:
 * <ul>
 *     <li>Validates permissions management when moving menu links between folders.</li>
 *     <li>Ensures proper saving of menu links and their attributes.</li>
 *     <li>Provides retrieval and verification of links based on their inode and other attributes.
 *     </li>
 *     <li>Tests exception handling when accessing non-existent inodes.</li>
 * </ul>
 *
 * @author Oscar Arrieta
 * @since Sep 26th, 2016
 */
public class MenuLinkAPITest extends IntegrationTestBase {

    static PermissionAPI pAPI;
    static FolderAPI fAPI;
    static MenuLinkAPI mAPI;
    static ContentletAPI cAPI;
    static HostAPI hAPI;
    static UserAPI uAPI;
    static Host site = null;
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
        site = new Host();
        site.setHostname("MenuLinkTest"+UUIDGenerator.generateUuid());
        site.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
        try{
        	HibernateUtil.startTransaction();
        	site = hAPI.save(site, user, false);
        	HibernateUtil.closeAndCommitTransaction();
        }catch(Exception e){
        	HibernateUtil.rollbackTransaction();
        	Logger.error(MenuLinkAPITest.class, e.getMessage());
        }
        
        hAPI.publish(site, user, false);
        cAPI.isInodeIndexed(site.getInode(),true);
        pAPI.permissionIndividually(hAPI.findSystemHost(), site, user);
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        try{
        	HibernateUtil.startTransaction();
        	hAPI.unpublish(site, user, false);
        	hAPI.archive(site, user, false);
        	hAPI.delete(site,user,false);
        	HibernateUtil.closeAndCommitTransaction();
        }catch(Exception e){
        	HibernateUtil.rollbackTransaction();
        	Logger.error(MenuLinkAPITest.class, e.getMessage());
        }

    }
    
    @Test
    public void move() throws Exception {
        final boolean permissionReferencesUpdateAsyncOldValue = Config.getBooleanProperty(
                "PERMISSION_REFERENCES_UPDATE_ASYNC", true);
        Config.setProperty("PERMISSION_REFERENCES_UPDATE_ASYNC", false);

        try {
            /*
             * Make sure chaning from a folder to other respect target folder permissions and inheritance
             */
            Folder parent1 = fAPI.createFolders("/parent1/sub", site, user, false);
            Folder parent2 = fAPI.createFolders("/parent2/sub", site, user, false);
            pAPI.permissionIndividually(site, parent2, user);

            Link link = new Link();
            link.setFriendlyName("test link");
            link.setTitle(link.getFriendlyName());
            link.setHostId(site.getIdentifier());
            link.setLinkType(Link.LinkType.EXTERNAL.toString());
            link.setUrl("google.com");
            link.setProtocal("http://");
            mAPI.save(link, parent1, user, false);

            Logger.info(MenuLinkAPITest.class, "getPermissionId " + link.getPermissionId());
            // must be getting permissions from the host
            assertEquals(site.getPermissionId(),
                    pAPI.findParentPermissionable(link).getPermissionId());
            assertTrue(mAPI.move(link, parent2, user, false));

            // then it should live under parent2
            assertEquals(parent2.getPermissionId(),
                    pAPI.findParentPermissionable(link).getPermissionId());
        } finally {
            Config.setProperty("PERMISSION_REFERENCES_UPDATE_ASYNC", permissionReferencesUpdateAsyncOldValue);
        }
    }
    
    @Test
    public void save() throws Exception {
    	HibernateUtil.startTransaction();
        Folder folder = fAPI.createFolders("/testsave", site, user, false);
        Link link = new Link();
        link.setFriendlyName("test link");
        link.setTitle(link.getFriendlyName());
        link.setHostId(site.getIdentifier());
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
        link.setHostId(site.getIdentifier());
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
        final Link link = new LinkDataGen().nextPersisted();

        final List<Link> result = mAPI
                .findLinks(user, false, null, hAPI.findDefaultHost(user, false).getIdentifier(), link.getInode(), null, null, 0, -1, null);

        assertNotNull(result);
        assertTrue(!result.isEmpty());
        assertEquals(link.getInode(), result.get(0).getInode());
    }

    @Test (expected = NotFoundInDbException.class)
    public void testFind_InodeNotExists_ReturnNotFoundInDBException()
            throws DotSecurityException, DotDataException {

        final Link link = mAPI.find("inodeNotExists",user,false);
    }

    @Test
    public void testFind_returnLink()
            throws DotSecurityException, DotDataException {
        final Link menuLink = new LinkDataGen(fAPI.findSystemFolder()).hostId(site.getIdentifier())
                .showOnMenu(true).nextPersisted();
        APILocator.getVersionableAPI().setLive(menuLink);

        final Link link = mAPI.find(menuLink.getInode(), user, false);

        assertEquals(menuLink.getInode(), link.getInode());
        assertEquals(menuLink.getLinkType(), link.getLinkType());
        assertEquals(menuLink.getUrl(), link.getUrl());
    }

}
