package com.dotmarketing.tag.business;

import static com.dotcms.repackage.org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import com.dotcms.TestBase;
import com.dotcms.repackage.org.junit.BeforeClass;
import com.dotcms.repackage.org.junit.Test;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * Test the functionality of TagAPI class
 * @author Oswaldo Gallango
 * @since 2/1/2016
 * @version 1.0
 */
public class TagAPITest extends TestBase {

	private static HostAPI hostAPI = APILocator.getHostAPI();
	private static UserAPI userAPI = APILocator.getUserAPI();
	private static TagAPI tagAPI = APILocator.getTagAPI();
	
	//need to add cache validation
	private TagCache tagCache = CacheLocator.getTagCache();

	private static User systemUser;
	private static User testUser;
    private static Host defaultHost;
    private static String defaulHostId;

    @BeforeClass
    public static void prepare () throws DotSecurityException, DotDataException {
        //Setting the test user
        systemUser = userAPI.getSystemUser();
        testUser = userAPI.loadByUserByEmail("admin@dotcms.com", systemUser, false);
        defaultHost = hostAPI.findDefaultHost( systemUser, false );
        defaulHostId=defaultHost.getIdentifier();
    }
    
	
	@Test
	public void getAllTags () throws Exception {		
		List<Tag> tags = tagAPI.getAllTags();
		assertTrue( tags.size() >= 125 );
	}

	@Test
	public void getTagByName() throws Exception {
        String tagName="china";
		List<Tag> tags = tagAPI.getTagByName(tagName);
		assertTrue( tags.size() == 1 );
		for(Tag tag : tags){
			assertTrue(tag.getTagName().equals(tagName));
		}
	}
	
	@Test
	public void getTagByUser() throws Exception{
		List<Tag> tags = tagAPI.getTagByUser(systemUser.getUserId());
		assertTrue(tags.size() == 0);
		
		String tagName = "testapi"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss");
		Tag tg = tagAPI.saveTag(tagName, testUser.getUserId(), defaulHostId, false);
		
		tags = tagAPI.getTagByUser(testUser.getUserId());
		assertTrue(tags.size() >= 1);
		assertTrue(tags.contains(tg));		
	}
		
	@Test
	public void getFilteredTags() throws Exception{
		String tagName ="test";
		List<Tag> tags = tagAPI.getFilteredTags (tagName, defaultHost.getIdentifier(), true, "tagname", 0, 10 );
		assertTrue(tags.size() > 1);
		for(Tag tag : tags){
			assertTrue(tag.getTagName().indexOf(tagName) != -1);
		}
    }

	@Test
	public void getTagAndCreate() throws Exception{
		String tagName ="testapi2"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		Tag newTag = tagAPI.getTagAndCreate ( tagName, testUser.getUserId(), defaulHostId );
		Tag tag = tagAPI.getTagByNameAndHost(tagName, defaulHostId);
		assertTrue(UtilMethods.isSet(tag) && tag.equals(newTag));
	}
	
	@Test
	public void getTagByTagId() throws Exception{
		String tagId = tagAPI.getAllTags().get(0).getTagId();
		Tag tag = tagAPI.getTagByTagId(tagId); 

		assertTrue(UtilMethods.isSet(tag));
		assertTrue(tag.getTagId().equals(tagId));
	}
	
	@Test
	public void getTagByNameAndHost() throws Exception{
		String tagName="inflation";
		Tag tag = tagAPI.getTagByNameAndHost( tagName, defaulHostId);
		
		assertTrue(UtilMethods.isSet(tag));
		assertTrue(tag.getTagName().equals(tagName));
	}

	@Test
	public void saveTag() throws Exception {
		//save tag first implementation  saveTag ( String tagName, String userId, String hostId )
		String tagName ="testapi3"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		Tag newTag = tagAPI.saveTag ( tagName, testUser.getUserId(), defaulHostId );
		Tag tag = tagAPI.getTagByNameAndHost(tagName, defaulHostId);
		assertTrue(UtilMethods.isSet(tag) && tag.getTagName().equals(tagName));
		
		//save tag second implementation saveTag ( String tagName, String userId, String hostId, boolean persona )
		String tagName2 ="testapi4"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		Tag newTag2 = tagAPI.saveTag ( tagName2, testUser.getUserId(), defaulHostId, false );
		Tag tag2 = tagAPI.getTagByNameAndHost(tagName2, defaulHostId);
		assertTrue(UtilMethods.isSet(tag2) && tag2.getTagName().equals(tagName2) && tag2.isPersona()==false);
	}
	
	@Test
	public void addTag() throws Exception {
		UserProxy userProxy = APILocator.getUserProxyAPI().getUserProxy(testUser.getUserId(),systemUser, false);
		String tagName ="testapi5"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		tagAPI.addTag ( tagName, testUser.getUserId(), userProxy.getInode() );
		
		Tag tag = tagAPI.getTagByNameAndHost(tagName, hostAPI.findSystemHost().getIdentifier());
		assertTrue(UtilMethods.isSet(tag));
		assertTrue(tag.getTagName().equals(tagName));
	}
	
	@Test
	public void updateTag () throws Exception{
		
		String tagName ="testapi6"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		Tag newTag = tagAPI.saveTag ( tagName, testUser.getUserId(), defaulHostId, false );
		
		//testing update tag first implementation public void updateTag ( String tagId, String tagName )
		String tagName2 ="testapi7"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		tagAPI.updateTag(newTag.getTagId(), tagName2 );
		
		Tag tag = tagAPI.getTagByTagId(newTag.getTagId());
		assertTrue(tag.getTagName().equals(tagName2));
		
		//testing update tag second implementation ( String tagId, String tagName, boolean updateTagReference, String hostId )
		String tagName3 ="testapi8"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		Host host = hostAPI.findSystemHost();
		tagAPI.updateTag(newTag.getTagId(), tagName3, true, host.getIdentifier() );
		
		tag = tagAPI.getTagByTagId(newTag.getTagId());
		assertTrue(tag.getTagName().equals(tagName3));
		assertTrue(tag.getHostId().equals(host.getIdentifier()));
	}
	
	@Test
	public void deleteHost() throws Exception{
		String tagName ="testapi9"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		Tag tag = tagAPI.saveTag ( tagName, testUser.getUserId(), defaulHostId, false );
		String tagId = tag.getTagId();
		//testing first implementation of public void deleteTag ( Tag tag )
		tagAPI.deleteTag(tag);
		tag = tagAPI.getTagByTagId(tagId);
		assertTrue(!UtilMethods.isSet(tag.getTagId()));
		
		tag = tagAPI.saveTag ( tagName, testUser.getUserId(), defaulHostId, false );
		tagId = tag.getTagId();
		//testing first implementation of public void deleteTag ( Tag tag )
		tagAPI.deleteTag (tag.getTagId());
		tag = tagAPI.getTagByTagId(tagId);
		assertTrue(!UtilMethods.isSet(tag.getTagId()));
	}

}
