package com.dotmarketing.tag.business;

import static com.dotcms.repackage.org.junit.Assert.assertTrue;

import java.util.List;

import com.dotcms.TestBase;
import com.dotcms.repackage.org.junit.BeforeClass;
import com.dotcms.repackage.org.junit.Test;
import com.dotmarketing.beans.Host;
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

	private static User user;
    private static Host defaultHost;

    @BeforeClass
    public static void prepare () throws DotSecurityException, DotDataException {
        //Setting the test user
        user = userAPI.getSystemUser();
        defaultHost = hostAPI.findDefaultHost( user, false );
    }
    
	
	@Test
	public void getAllTags () throws Exception {		
		List<Tag> tags = tagAPI.getAllTags();
		assertTrue( tags.size() == 14 );
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
		List<Tag> tags = tagAPI.getTagByUser(user.getUserId());
		assertTrue(tags.size() == 14);
		for(Tag tag : tags){
			assertTrue(tag.getUserId().equals(user.getUserId()));
		}
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
		String tagName="North America";
		Tag tag = tagAPI.getTagByNameAndHost( tagName, defaultHost.getIdentifier());
		
		assertTrue(UtilMethods.isSet(tag));
		assertTrue(tag.getTagName().equals(tagName));
	}


	

}
