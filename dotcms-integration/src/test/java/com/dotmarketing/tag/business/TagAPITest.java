package com.dotmarketing.tag.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.TagDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.portlets.structure.business.StructureAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test the functionality of TagAPI class
 * @author Oswaldo Gallango
 * @since 2/1/2016
 * @version 1.0
 */
public class TagAPITest extends IntegrationTestBase {

	private static HostAPI hostAPI;
	private static UserAPI userAPI;
	private static TagAPI tagAPI;
	private static ContentletAPI conAPI;
	private static LanguageAPI langAPI;
	private static StructureAPI structureAPI;

	//need to add cache validation
	private TagCache tagCache = CacheLocator.getTagCache();

	private static User systemUser;
	private static User testUser;
	private static Host defaultHost;
	private static String defaultHostId;

	private static String WIKI_STRUCTURE_VARNAME="Wiki";
	private static String WIKI_SYSPUBLISHDATE_VARNAME="sysPublishDate";
	private static String WIKI_TITLE_VARNAME="title";
	private static String WIKI_URL_VARNAME="urlTitle";
	private static String WIKI_BYLINEL_VARNAME="byline";
	private static String WIKI_STORY_VARNAME="story";
	private static String  WIKI_TAG_VARNAME="tag";

	@BeforeClass
	public static void prepare () throws Exception {
		//Setting web app environment
        IntegrationTestInitService.getInstance().init();
		
        hostAPI = APILocator.getHostAPI();
        userAPI = APILocator.getUserAPI();
        tagAPI = APILocator.getTagAPI();
        conAPI =APILocator.getContentletAPI();
        langAPI =APILocator.getLanguageAPI();
        structureAPI =APILocator.getStructureAPI();
        
		//Setting the test user
		systemUser = userAPI.getSystemUser();
		testUser = userAPI.loadByUserByEmail("admin@dotcms.com", systemUser, false);
		defaultHost = hostAPI.findDefaultHost( systemUser, false );
		defaultHostId=defaultHost.getIdentifier();
	}

	/**
	 * Test the getAllTags method from the tagAPI
	 * @throws Exception
	 */
	@Test
	public void getAllTags () throws Exception {

		// let's create some tags
		for(int i=0; i<5; i++) {
			saveTag("tagByName" + i + System.currentTimeMillis());
		}

		List<Tag> tags = tagAPI.getAllTags();
		assertNotNull( tags );
		assertFalse( tags.isEmpty() );
	}

	/**
	 * Test the getTagByName method from the tagAPI
	 * @throws Exception
	 */
	@Test
	public void getTagByName() throws Exception {

		String tagName="tagByName" + System.currentTimeMillis();
		createAndGetTagByName(tagName);

		tagName = "tagByName" + System.currentTimeMillis() + "-test1";
		createAndGetTagByName(tagName);

		tagName = "tagByName" + System.currentTimeMillis() + "'test2";
		createAndGetTagByName(tagName);

		tagName = "tagByName" + System.currentTimeMillis() + ".test3";
		createAndGetTagByName(tagName);

		tagName = "tagByName" + System.currentTimeMillis() + ".test4.";
		createAndGetTagByName(tagName);

		tagName = "tagByName" + System.currentTimeMillis() + "'test5'";
		createAndGetTagByName(tagName);

		tagName = "tagByName" + System.currentTimeMillis() + "\\test6";
		createAndGetTagByName(tagName);
	}

	/**
	 * Test the getTagByUser method from the tagAPI
	 * @throws Exception
	 */
	@Test
	public void getTagsForUserByUserId() throws Exception{

		List<Tag> tags = tagAPI.getTagsForUserByUserId(testUser.getUserId());
		assertNotNull(tags);
        assertFalse(tags.isEmpty());

		String tagName = "testapi"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss");
		Tag createdTag = tagAPI.saveTag(tagName, testUser.getUserId(), defaultHostId, false);
		tagAPI.addUserTagInode(createdTag, testUser.getUserId());

		tags = tagAPI.getTagsForUserByUserId(testUser.getUserId());
		assertNotNull(tags);
        assertFalse(tags.isEmpty());
		assertTrue(tags.contains(createdTag));
	}

	/**
	 * Test the getTagsForUserByUserInode method from the tagAPI
	 * @throws Exception
	 */
	@Test
	public void getTagsForUserByUserInode() throws Exception{

		String tagName = "testapi2" + UtilMethods.dateToHTMLDate(new Date(), "MMddyyyyHHmmss");
		tagAPI.addUserTag(tagName, testUser.getUserId(), testUser.getUserId());

		List<Tag> tags =  tagAPI.getTagsForUserByUserInode(testUser.getUserId());    
		assertTrue(tags.size() > 1);
		boolean hasUserInodeTags=false;
		for(Tag tag : tags){
			if(tag.getTagName().equals(tagName)){
				hasUserInodeTags=true;
				break;
			}			                                                        
		}
		assertTrue(hasUserInodeTags);
	}

	/**
	 * Test the getFilteredTags method from the tagAPI
	 * @throws Exception
	 */
	@Test
	public void getFilteredTags() throws Exception{
		String tagName ="test";
		List<Tag> tags = tagAPI.getFilteredTags (tagName, defaultHost.getIdentifier(), true, "tagname", 0, 10 );
		assertTrue(tags.size() > 1);
		for(Tag tag : tags){
			assertTrue(tag.getTagName().contains(tagName));
		}
	}

	/**
	 * Test the getTagAndCreate method from the tagAPI
	 * @throws Exception
	 */
	@Test
	public void getTagAndCreate() throws Exception{
		String tagName ="testapi4"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		Tag newTag = tagAPI.getTagAndCreate ( tagName, testUser.getUserId(), defaultHostId );
		Tag tag = tagAPI.getTagByNameAndHost(tagName, defaultHostId);
		assertNotNull(tag);
        assertEquals(tag, newTag);
	}

	/**
	 * Test the getTagByTagId method from the tagAPI
	 * @throws Exception
	 */
	@Test
	public void getTagByTagId() throws Exception{
		List<Tag> tags = tagAPI.getAllTags();
		assertNotNull(tags);

		String tagId = tags.get(0).getTagId();
		Tag tag = tagAPI.getTagByTagId(tagId); 

		assertNotNull(tag);
        assertEquals(tag.getTagId(), tagId);
	}

	/**
	 * Test the getTagByNameAndHost method from the tagAPI
	 * @throws Exception
	 */
	@Test
	public void getTagByNameAndHost() throws Exception{

		final String tagName = ("testTag" + System.currentTimeMillis()).toLowerCase();

		Tag tag = tagAPI.saveTag(tagName, testUser.getUserId(), defaultHostId, false);
		assertNotNull(tag);
		assertEquals(tagName, tag.getTagName());

		tag = tagAPI.getTagByNameAndHost(tagName, defaultHostId);
		assertNotNull(tag);
		assertEquals(tagName, tag.getTagName());
	}

	/**
	 * Test the two saveTag methods from the tagAPI
	 * @throws Exception
	 */
	@Test
	public void saveTag() throws Exception {
		//save tag first implementation  saveTag ( String tagName, String userId, String hostId )
		String tagName ="testapi4"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss");
		saveTag(tagName);

		//save tag second implementation saveTag ( String tagName, String userId, String hostId, boolean persona )
		String tagName2 ="testapi5"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss");
		saveTag(tagName2, false);
	}

	/**
	 * Test the addTag method from the tagAPI
	 * @throws Exception
	 */
	@Test
	public void addTag() throws Exception {
		
		String tagName = "testapi6" + UtilMethods.dateToHTMLDate(new Date(), "MMddyyyyHHmmss");
		tagAPI.addUserTag(tagName, testUser.getUserId(), testUser.getUserId());

		Tag tag = tagAPI.getTagByNameAndHost(tagName, hostAPI.findSystemHost().getIdentifier());
		assertNotNull(tag);
        assertEquals(tag.getTagName(), tagName);
	}

	/**
	 * Test the two updateTag methods from the tagAPI
	 * @throws Exception
	 */
	@Test
	public void updateTag () throws Exception{

		String tagName ="testapi7"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		Tag newTag = tagAPI.saveTag ( tagName, testUser.getUserId(), defaultHostId, false );

		//testing update tag first implementation public void updateTag ( String tagId, String tagName )
		String tagName2 ="testapi8"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		tagAPI.updateTag(newTag.getTagId(), tagName2 );

		Tag tag = tagAPI.getTagByTagId(newTag.getTagId());
        assertEquals(tag.getTagName(), tagName2);

		//testing update tag second implementation ( String tagId, String tagName, boolean updateTagReference, String hostId )
		String tagName3 ="testapi9"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		Host host = hostAPI.findSystemHost();
		tagAPI.updateTag(newTag.getTagId(), tagName3, true, host.getIdentifier() );

		tag = tagAPI.getTagByTagId(newTag.getTagId());
        assertEquals(tag.getTagName(), tagName3);
        assertEquals(tag.getHostId(), host.getIdentifier());
	}

	/**
	 * Test the two deleteTag methods from the tagAPI
	 * @throws Exception
	 */
	@Test
	public void deleteTag() throws Exception{
		String tagName ="testapi10"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		Tag tag = tagAPI.saveTag ( tagName, testUser.getUserId(), defaultHostId, false );
		String tagId = tag.getTagId();
		//testing first implementation of public void deleteTag ( Tag tag )
		tagAPI.deleteTag(tag);
		tag = tagAPI.getTagByTagId(tagId);
		assertNull(tag);

		tag = tagAPI.saveTag ( tagName, testUser.getUserId(), defaultHostId, false );
		tagId = tag.getTagId();
		//testing first implementation of public void deleteTag ( String tagId )
		tagAPI.deleteTag (tag.getTagId());
		tag = tagAPI.getTagByTagId(tagId);
		assertNull(tag);
	}

	/**
	 * Test the deleteTags (bulk delete) method from the tagAPI
	 * @throws Exception
	 */
	@Test
	public void deleteTags() throws Exception {
		String timestamp = UtilMethods.dateToHTMLDate(new Date(), "MMddyyyyHHmmss");
		
		// Create multiple tags for bulk delete testing
		String tagName1 = "testbulk1_" + timestamp;
		String tagName2 = "testbulk2_" + timestamp;
		String tagName3 = "testbulk3_" + timestamp;
		
		Tag tag1 = tagAPI.saveTag(tagName1, testUser.getUserId(), defaultHostId, false);
		Tag tag2 = tagAPI.saveTag(tagName2, testUser.getUserId(), defaultHostId, false);
		Tag tag3 = tagAPI.saveTag(tagName3, testUser.getUserId(), defaultHostId, false);
		
		// Verify tags exist before deletion
		assertNotNull(tagAPI.getTagByTagId(tag1.getTagId()));
		assertNotNull(tagAPI.getTagByTagId(tag2.getTagId()));
		assertNotNull(tagAPI.getTagByTagId(tag3.getTagId()));
		
		// Test bulk delete with multiple tag IDs
		tagAPI.deleteTags(tag1.getTagId(), tag2.getTagId(), tag3.getTagId());
		
		// Verify all tags are deleted
		assertNull(tagAPI.getTagByTagId(tag1.getTagId()));
		assertNull(tagAPI.getTagByTagId(tag2.getTagId()));
		assertNull(tagAPI.getTagByTagId(tag3.getTagId()));
		
		// Test edge cases - should not throw exceptions
		tagAPI.deleteTags(); // Empty array
		tagAPI.deleteTags((String[]) null); // Null array
		tagAPI.deleteTags("non-existent-tag-id"); // Non-existent tag ID
	}

	/**
	 * Test the editTag method of the tagAPI
	 * @throws Exception
	 */
	@Test
	public void editTag() throws Exception {
		String oldTagName ="testapi11"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		Tag tag = tagAPI.saveTag ( oldTagName, testUser.getUserId(), defaultHostId, false );

		String tagName ="testapi12"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		tagAPI.editTag ( tagName, oldTagName, testUser.getUserId());

		tag = tagAPI.getTagByTagId(tag.getTagId());
		assertNotNull(tag);
		assertNotNull(tag.getTagId());
		assertTrue(tag.getTagName().equals(tagName));
	}

	/**
	 * Test the two addTagInode methods of the tagAPI
	 * @throws Exception
	 */
	@Test
	public void addTagInode() throws Exception {

		Contentlet contentAsset=new Contentlet();
		ContentType contentType = TestDataUtils.getWikiLikeContentType();
		contentAsset.setContentTypeId(contentType.id());
		contentAsset.setHost(defaultHostId);
		contentAsset.setProperty(WIKI_SYSPUBLISHDATE_VARNAME, new Date());
		String name="testtagapi"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss");
		contentAsset.setProperty(WIKI_TITLE_VARNAME, name);
		contentAsset.setProperty(WIKI_URL_VARNAME, name);
		contentAsset.setProperty(WIKI_BYLINEL_VARNAME, "test");
		contentAsset.setProperty(WIKI_STORY_VARNAME, "test");
		contentAsset.setLanguageId(langAPI.getDefaultLanguage().getId());
		contentAsset=conAPI.checkin(contentAsset, testUser, false);
		APILocator.getContentletAPI().publish(contentAsset, testUser, false);

		String tagName ="testapi13"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		//testing first implementation of public TagInode addTagInode ( String tagName, String inode, String hostId )
		Tag tag = tagAPI.getTagAndCreate(tagName, testUser.getUserId(), defaultHostId);
		TagInode tagInode = tagAPI.addContentletTagInode(tagName, contentAsset.getInode(), defaultHostId, WIKI_TAG_VARNAME);

		TagInode tInode = tagAPI.getTagInode(tag.getTagId(), tagInode.getInode(),WIKI_TAG_VARNAME);
		assertTrue(UtilMethods.isSet(tInode.getInode()) && tInode.getInode().equals(tagInode.getInode()) && tInode.getTagId().equals(tag.getTagId()));

		//testing second implementation of public TagInode addTagInode ( Tag tag, String inode )
		String tagName2 ="testapi14"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		Tag tag2 = tagAPI.getTagAndCreate(tagName2, testUser.getUserId(), defaultHostId);
		TagInode tagInode2 = tagAPI.addContentletTagInode(tag2, contentAsset.getInode(), WIKI_TAG_VARNAME);

		TagInode tInode2 = tagAPI.getTagInode(tag2.getTagId(), tagInode2.getInode(),WIKI_TAG_VARNAME);
		assertTrue(UtilMethods.isSet(tInode2.getInode()) && tInode2.getInode().equals(tagInode2.getInode()) && tInode2.getTagId().equals(tag2.getTagId()));

		conAPI.destroy(contentAsset, testUser, false);
	}

	/**
	 * Test the getTagInodesByInode method of the tagAPI
	 * @throws Exception
	 */
	@Test
	public void getTagInodesByInode() throws Exception{

		Contentlet contentAsset=new Contentlet();
		ContentType contentType = TestDataUtils.getWikiLikeContentType();
		contentAsset.setContentTypeId(contentType.id());
		contentAsset.setHost(defaultHostId);
		contentAsset.setProperty(WIKI_SYSPUBLISHDATE_VARNAME, new Date());
		String name="testtagapi15"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss");
		contentAsset.setProperty(WIKI_TITLE_VARNAME, name);
		contentAsset.setProperty(WIKI_URL_VARNAME, name);
		contentAsset.setProperty(WIKI_BYLINEL_VARNAME, "test");
		contentAsset.setProperty(WIKI_STORY_VARNAME, "test");
		contentAsset.setLanguageId(langAPI.getDefaultLanguage().getId());
		contentAsset=conAPI.checkin(contentAsset, testUser, false);
		APILocator.getContentletAPI().publish(contentAsset, testUser, false);

		String tagName ="testapi15"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		tagAPI.getTagAndCreate(tagName, testUser.getUserId(), defaultHostId);
		TagInode tagInode = tagAPI.addContentletTagInode(tagName, contentAsset.getInode(), defaultHostId, WIKI_TAG_VARNAME);

		List<TagInode> tagInodes = tagAPI.getTagInodesByInode( contentAsset.getInode());
		assertTrue(tagInodes.size() >=1);
		boolean existTagInode = false;
		for(TagInode tgI : tagInodes){
			if(tgI.getInode().equals(tagInode.getInode()) && tgI.getTagId().equals(tagInode.getTagId())){
				existTagInode=true;
				break;
			}

		}
		assertTrue(existTagInode);

		conAPI.destroy(contentAsset, testUser, false);
	}

	/**
	 * Test the getTagInodesByTagId method of the tagAPI
	 * @throws Exception
	 */
	@Test
	public void getTagInodesByTagId() throws Exception{
		Contentlet contentAsset=new Contentlet();
		ContentType contentType = TestDataUtils.getWikiLikeContentType();
		contentAsset.setContentTypeId(contentType.id());
		contentAsset.setHost(defaultHostId);
		contentAsset.setProperty(WIKI_SYSPUBLISHDATE_VARNAME, new Date());
		String name="testtagapi16"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss");
		contentAsset.setProperty(WIKI_TITLE_VARNAME, name);
		contentAsset.setProperty(WIKI_URL_VARNAME, name);
		contentAsset.setProperty(WIKI_BYLINEL_VARNAME, "test");
		contentAsset.setProperty(WIKI_STORY_VARNAME, "test");
		contentAsset.setLanguageId(langAPI.getDefaultLanguage().getId());
		contentAsset=conAPI.checkin(contentAsset, testUser, false);
		APILocator.getContentletAPI().publish(contentAsset, testUser, false);

		String tagName ="testapi16"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		Tag tag = tagAPI.getTagAndCreate(tagName, testUser.getUserId(), defaultHostId);
		TagInode tagInode = tagAPI.addContentletTagInode(tagName, contentAsset.getInode(), defaultHostId, WIKI_TAG_VARNAME);

		List<TagInode> tagInodes = tagAPI.getTagInodesByTagId(tag.getTagId());
		assertTrue(tagInodes.size() >=1);
		boolean existTagInode = false;
		for(TagInode tgI : tagInodes){
			if(tgI.getInode().equals(tagInode.getInode()) && tgI.getTagId().equals(tagInode.getTagId())){
				existTagInode=true;
				break;
			}

		}
		assertTrue(existTagInode);

		conAPI.destroy(contentAsset, testUser, false);
	}

	/**
	 * Test the getTagInode method of the tagAPI
	 * @throws Exception
	 */
	@Test
	public void getTagInode() throws Exception {

		Contentlet contentAsset=new Contentlet();
		ContentType contentType = TestDataUtils.getWikiLikeContentType();
		contentAsset.setContentTypeId(contentType.id());
		contentAsset.setHost(defaultHostId);
		contentAsset.setProperty(WIKI_SYSPUBLISHDATE_VARNAME, new Date());
		String name="testtagapi17"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss");
		contentAsset.setProperty(WIKI_TITLE_VARNAME, name);
		contentAsset.setProperty(WIKI_URL_VARNAME, name);
		contentAsset.setProperty(WIKI_BYLINEL_VARNAME, "test");
		contentAsset.setProperty(WIKI_STORY_VARNAME, "test");
		contentAsset.setLanguageId(langAPI.getDefaultLanguage().getId());
		contentAsset=conAPI.checkin(contentAsset, testUser, false);
		APILocator.getContentletAPI().publish(contentAsset, testUser, false);

		String tagName ="testapi17"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		Tag tag = tagAPI.getTagAndCreate(tagName, testUser.getUserId(), defaultHostId);
		TagInode tagInode = tagAPI.addContentletTagInode(tagName, contentAsset.getInode(), defaultHostId, WIKI_TAG_VARNAME);

		TagInode tagInode2 = tagAPI.getTagInode ( tag.getTagId(), contentAsset.getInode(),WIKI_TAG_VARNAME);

		assertNotNull(tagInode);
		assertNotNull(tagInode2);
		assertTrue(UtilMethods.isSet(tagInode.getTagId()));
		assertTrue(UtilMethods.isSet(tagInode2.getTagId()));
        assertEquals(tagInode.getTagId(), tagInode2.getTagId());
        assertEquals(tagInode.getInode(), tagInode2.getInode());

		conAPI.destroy(contentAsset, testUser, false);
	}

	/**
	 * Test the three deleteTagInode methods of the tagAPI
	 * @throws Exception
	 */
	@Test
	public void deleteTagInode() throws Exception {
		Contentlet contentAsset=new Contentlet();
		ContentType contentType = TestDataUtils.getWikiLikeContentType();
		contentAsset.setContentTypeId(contentType.id());
		contentAsset.setHost(defaultHostId);
		contentAsset.setProperty(WIKI_SYSPUBLISHDATE_VARNAME, new Date());
		String name="testtagapi18"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss");
		contentAsset.setProperty(WIKI_TITLE_VARNAME, name);
		contentAsset.setProperty(WIKI_URL_VARNAME, name);
		contentAsset.setProperty(WIKI_BYLINEL_VARNAME, "test");
		contentAsset.setProperty(WIKI_STORY_VARNAME, "test");
		contentAsset.setLanguageId(langAPI.getDefaultLanguage().getId());
		contentAsset=conAPI.checkin(contentAsset, testUser, false);
		APILocator.getContentletAPI().publish(contentAsset, testUser, false);

		String tagName ="testapi18"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		Tag tag = tagAPI.getTagAndCreate(tagName, testUser.getUserId(), defaultHostId);
		TagInode tagInode = tagAPI.addContentletTagInode(tagName, contentAsset.getInode(), defaultHostId, WIKI_TAG_VARNAME);
		//testing second implementation of public TagInode addContentletTagInode ( TagInode tagInode )
		tagAPI.deleteTagInode( tagInode );

		tagInode = tagAPI.getTagInode ( tag.getTagId(), contentAsset.getInode(),WIKI_TAG_VARNAME);
		assertNull(tagInode);

		String tagName2 ="testapi19"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		Tag tag2 = tagAPI.getTagAndCreate(tagName, testUser.getUserId(), defaultHostId);
		TagInode tagInode2 = tagAPI.addContentletTagInode(tagName, contentAsset.getInode(), defaultHostId, WIKI_TAG_VARNAME);
		//testing second implementation of public TagInode addContentletTagInode ( Tag tag, String inode )
		tagAPI.deleteTagInode( tag2, contentAsset.getInode(),WIKI_TAG_VARNAME);

		tagInode2 = tagAPI.getTagInode(tag2.getTagId(), contentAsset.getInode(),WIKI_TAG_VARNAME);
		assertNull(tagInode2);

		tag2 = tagAPI.getTagByTagId(tag2.getTagId());
		assertNotNull(tag2);

		//testing third implementation of public TagInode addContentletTagInode ( String tagName, String inode )
		String tagName3 ="testapi20"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		Tag tag3 = tagAPI.getTagAndCreate(tagName3, testUser.getUserId(), defaultHostId);
		TagInode tagInode3 = tagAPI.addContentletTagInode(tagName3, contentAsset.getInode(), defaultHostId, WIKI_TAG_VARNAME);
		tagAPI.deleteTagInode( tag3.getTagName(), contentAsset.getInode(),WIKI_TAG_VARNAME);

		tagInode3 = tagAPI.getTagInode(tag3.getTagId(), contentAsset.getInode(),WIKI_TAG_VARNAME);
		assertNull(tagInode3);

		tag3 = tagAPI.getTagByTagId(tag3.getTagId());
		assertNotNull(tag3);

		conAPI.destroy(contentAsset, testUser, false);
	}

	/**
	 * Test the removeTagRelationAndTagWhenPossible method of the tagAPI
	 * @throws Exception
	 */
	@Test
	public void removeTagRelationAndTagWhenPossible() throws Exception {

		Contentlet contentAsset=new Contentlet();
		ContentType contentType = TestDataUtils.getWikiLikeContentType();
		contentAsset.setContentTypeId(contentType.id());
		contentAsset.setHost(defaultHostId);
		contentAsset.setProperty(WIKI_SYSPUBLISHDATE_VARNAME, new Date());
		String name="testtagapi21"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss");
		contentAsset.setProperty(WIKI_TITLE_VARNAME, name);
		contentAsset.setProperty(WIKI_URL_VARNAME, name);
		contentAsset.setProperty(WIKI_BYLINEL_VARNAME, "test");
		contentAsset.setProperty(WIKI_STORY_VARNAME, "test");
		contentAsset.setLanguageId(langAPI.getDefaultLanguage().getId());
		contentAsset=conAPI.checkin(contentAsset, testUser, false);
		APILocator.getContentletAPI().publish(contentAsset, testUser, false);

		String tagName ="testapi21"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		Tag tag = tagAPI.getTagAndCreate(tagName, testUser.getUserId(), defaultHostId);
		tagAPI.addContentletTagInode(tagName, contentAsset.getInode(), defaultHostId, WIKI_TAG_VARNAME);

		TagInode tagInode = tagAPI.getTagInode ( tag.getTagId(), contentAsset.getInode(),WIKI_TAG_VARNAME);
		assertNotNull(tagInode);		
		tagAPI.removeTagRelationAndTagWhenPossible(tag.getTagId(), contentAsset.getInode(),WIKI_TAG_VARNAME);

		tagInode = tagAPI.getTagInode ( tag.getTagId(), contentAsset.getInode(),WIKI_TAG_VARNAME);
		assertNull(tagInode);	

		tag = tagAPI.getTagByTagId(tag.getTagId());
		assertNull(tag);


		conAPI.destroy(contentAsset, testUser, false);
	}

	/**
	 * Test the getSuggestedTag method of the tagAPI
	 * @throws Exception
	 */
	@Test
	public void getSuggestedTag() throws Exception{
		String tagName="test";
		List<Tag> tags = tagAPI.getSuggestedTag (tagName, defaultHostId);
		int tagSize = tags.size();
		assertTrue(tagSize > 1);
		for(Tag tag : tags){
			assertTrue(tag.getTagName().contains(tagName));
		}

		tagName="testing";
		tags = tagAPI.getSuggestedTag (tagName, defaultHostId);
        assertFalse(tags.isEmpty());
		for(Tag tag : tags){
			assertTrue(tag.getTagName().contains(tagName));
		}

		assertTrue(tags.size() < tagSize);
	}

	/**
	 * Test the updateTagReferences method of the tagAPI
	 * @throws Exception
	 */
	@Test
	public void updateTagReferences() throws Exception{

		Host newHost = null;
		try {
			//Creates a new Host
			Contentlet host = new Contentlet();
			Structure st = structureAPI.findByVarName("Host", systemUser);
			host.setContentTypeId(st.getInode());
			String hostName = "testtagapiHost" + System.currentTimeMillis() + ".com";
			host.setProperty(Host.HOST_NAME_KEY, hostName);
			host.setLanguageId(langAPI.getDefaultLanguage().getId());
			host.setIndexPolicy(IndexPolicy.FORCE);
			host.setProperty(Host.IS_DEFAULT_KEY, false);
			host = conAPI.checkin(host, testUser, false);
			host.setIndexPolicy(IndexPolicy.FORCE);
			conAPI.publish(host, testUser, false);

			//Finds the new Host and set the Tag Storage to the new host id
			newHost = hostAPI.findByName(hostName, systemUser, false);
			host.setProperty(Host.TAG_STORAGE, newHost.getIdentifier());
			host.setInode(null);
			host.setIndexPolicy(IndexPolicy.FORCE);
			host = conAPI.checkin(host, testUser, false);
			host.setIndexPolicy(IndexPolicy.FORCE);
			conAPI.publish(host, testUser, false);

			//Gets the default Host Tags
			List<Tag> tags = tagAPI.getTagsByHostId(defaultHostId);
			assertNotNull(tags);
			int defaultHostInitialNumberOfTags = tags.size();
			assertTrue(defaultHostInitialNumberOfTags > 0);

			//Gets the new Host Tags
			tags = tagAPI.getTagsByHostId(newHost.getIdentifier());
			assertNotNull(tags);
			int newHostinitialNumberOfTags = tags.size();
            assertEquals(0, newHostinitialNumberOfTags);

			//Move the Tags to the new Host
			tagAPI.updateTagReferences(defaultHostId, defaultHostId, newHost.getIdentifier());

			//to refresh cache
			tagCache.clearCache();

			//Checks that the tags have been moved
			List<Tag> newHostTagsAfterUpdate = tagAPI.getTagsByHostId(newHost.getIdentifier());
			assertTrue(newHostTagsAfterUpdate.size() > newHostinitialNumberOfTags);

			List<Tag> defaultHostTagsAfterUpdate = tagAPI.getTagsByHostId(defaultHostId);
			assertTrue(defaultHostTagsAfterUpdate.size() < defaultHostInitialNumberOfTags);

			//return tags to original host
			tagAPI.updateTagReferences(defaultHostId, newHost.getIdentifier(), defaultHostId);
			tagCache.clearCache();

			defaultHostTagsAfterUpdate = tagAPI.getTagsByHostId(defaultHostId);
            assertEquals(defaultHostTagsAfterUpdate.size(), defaultHostInitialNumberOfTags);

			/*here the amount is not 0 because is entering in the condition
			 * if((hostIdentifier.equals(newTagStorageId) && hostTagList.size() == 0) && !newTagStorageId.equals(Host.SYSTEM_HOST)) {
			 * saveTag(tag.getTagName(), "", hostIdentifier);
			 */
			newHostTagsAfterUpdate = tagAPI.getTagsByHostId(newHost.getIdentifier());
            assertEquals(newHostTagsAfterUpdate.size(), defaultHostInitialNumberOfTags);
		} finally {

			if (null != newHost) {
				//delete host
				hostAPI.archive(newHost,systemUser,false);
				hostAPI.delete(newHost,systemUser,false);
                assertEquals(0, tagAPI.getTagsByHostId(newHost.getIdentifier()).size());
			}

		}
	}

	/**
	 * Test getTagsByHostId
	 */
	@Test
	public void testGetTagsByHostId_returnAllTagsOfAHost() throws Exception{
		//Gets the default Host Tags
		List<Tag> tags = tagAPI.getTagsByHostId(defaultHostId);
		int defaultHostInitialNumberOfTags = tags.size();
		assertNotNull(tags);
		assertTrue(defaultHostInitialNumberOfTags > 0);
	}

	/**
	 * Test the getTagsByInode method of the tagAPI
	 * @throws Exception
	 */
	@Test
	public void getTagsByInode() throws Exception{
		Contentlet contentAsset=new Contentlet();
		ContentType contentType = TestDataUtils.getWikiLikeContentType();
		contentAsset.setContentTypeId(contentType.id());
		contentAsset.setHost(defaultHostId);
		contentAsset.setProperty(WIKI_SYSPUBLISHDATE_VARNAME, new Date());
		String name="testtagapi22"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss");
		contentAsset.setProperty(WIKI_TITLE_VARNAME, name);
		contentAsset.setProperty(WIKI_URL_VARNAME, name);
		contentAsset.setProperty(WIKI_BYLINEL_VARNAME, "test");
		contentAsset.setProperty(WIKI_STORY_VARNAME, "test");
		contentAsset.setLanguageId(langAPI.getDefaultLanguage().getId());
		contentAsset=conAPI.checkin(contentAsset, testUser, false);
		APILocator.getContentletAPI().publish(contentAsset, testUser, false);

		String tagName ="testapi22"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		Tag tag = tagAPI.getTagAndCreate(tagName, testUser.getUserId(), defaultHostId);
		tagAPI.addContentletTagInode(tagName, contentAsset.getInode(), defaultHostId, WIKI_TAG_VARNAME);

		String tagName2 ="testapi23"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		Tag tag2 = tagAPI.getTagAndCreate(tagName2, testUser.getUserId(), defaultHostId);
		tagAPI.addContentletTagInode(tagName2, contentAsset.getInode(), defaultHostId, WIKI_TAG_VARNAME);

		List<Tag> tags = tagAPI.getTagsByInode( contentAsset.getInode());
		assertNotNull(tags);
		assertTrue(tags.size() == 2);
		assertTrue(tags.contains(tag));
		assertTrue(tags.contains(tag2));

		conAPI.destroy(contentAsset, testUser, false);
	}

	/**
	 * Test the getTagsByInode method of the tagAPI
	 * @throws Exception
	 */
	@Test
	public void getTagsInText() throws Exception{
		String text="test1, test 2, test number three,another\n testing\t to'do\r now"; 
		List<Tag> tags = tagAPI.getTagsInText (text, testUser.getUserId(), defaultHostId);
		assertNotNull(tags);
        assertEquals(7, tags.size());
	}

	/**
	 * Test the Personas tags functionality
	 * @throws Exception
	 */
	@Test
	public void validatePersonaTags() throws Exception {

		Contentlet persona = new Contentlet();
		persona.setContentTypeId(PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_INODE);
		persona.setHost(defaultHostId);
		persona.setLanguageId(langAPI.getDefaultLanguage().getId());
		String name="testtagapipersona1"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss");
		String othertags="testapipersona_1"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss");
		persona.setProperty(PersonaAPI.NAME_FIELD, name);
		persona.setProperty(PersonaAPI.KEY_TAG_FIELD, name);
		persona.setProperty(PersonaAPI.TAGS_FIELD, othertags);
		persona.setProperty(PersonaAPI.DESCRIPTION_FIELD,"test to delete");
		persona.setIndexPolicy(IndexPolicy.FORCE);
		persona=conAPI.checkin(persona, testUser, false);
		persona.setIndexPolicy(IndexPolicy.FORCE);
		conAPI.publish(persona, testUser, false);

		/*if the persona is publish, the tag should be mark as persona
		 * in the tag table 
		 */
		Tag tag = tagAPI.getTagByNameAndHost(name, defaultHostId);
		assertNotNull(tag);
		assertTrue(tag.isPersona());
		assertTrue(tag.getTagName().equals(name));

		tag = tagAPI.getTagByNameAndHost(othertags, defaultHostId);
		assertNotNull(tag);
		assertFalse(tag.isPersona());
		assertTrue(tag.getTagName().equals(othertags));

		/*if the persona is not publish, the tag should not be mark as persona
		 * in the tag table 
		 */
		persona.setIndexPolicy(IndexPolicy.FORCE);
		conAPI.unpublish(persona, systemUser, false);
		tag = tagAPI.getTagByNameAndHost(name, defaultHostId);
		assertNotNull(tag);
		assertFalse(tag.isPersona());
		assertTrue(tag.getTagName().equals(name));

		tag = tagAPI.getTagByNameAndHost(othertags, defaultHostId);
		assertNotNull(tag);
		assertFalse(tag.isPersona());
		assertTrue(tag.getTagName().equals(othertags));		

		//republish persona and check tags
		persona.setIndexPolicy(IndexPolicy.FORCE);
		conAPI.publish(persona, systemUser, false);
		tag = tagAPI.getTagByNameAndHost(name, defaultHostId);
		assertNotNull(tag);
		assertTrue(tag.isPersona());
		assertTrue(tag.getTagName().equals(name));

		tag = tagAPI.getTagByNameAndHost(othertags, defaultHostId);
		assertNotNull(tag);
		assertFalse(tag.isPersona());
		assertTrue(tag.getTagName().equals(othertags));

		conAPI.destroy(persona, testUser, false);
	}

	/**
	 * Test the tags cache functionality
	 * @throws Exception
	 */
	@Test
	public void validateTagCache() throws Exception {
		/*
		 * Test cache by TagId
		 */
		String tagName ="testapi24"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		Tag tag = tagAPI.saveTag(tagName, testUser.getUserId(), defaultHostId);

		//Verify the cache -> THE SAVE SHOULD ADD NOTHING TO CACHE, JUST THE LOAD
		Tag cachedTag = tagCache.get(tag.getTagId());
		assertNull( cachedTag );

		//The find should add the category to the cache
		Tag foundTag = tagAPI.getTagByTagId(tag.getTagId());
		assertNotNull( foundTag );

		cachedTag = tagCache.get(tag.getTagId());
		assertNotNull( cachedTag );
		assertEquals( cachedTag, tag );

		/*
		 * test cache by tagname and hostid
		 */
		tagName ="testapi25"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		tag = tagAPI.saveTag(tagName, testUser.getUserId(), defaultHostId);

		//Verify the cache -> THE SAVE SHOULD ADD NOTHING TO CACHE, JUST THE LOAD
		cachedTag = tagCache.get(tag.getTagName(), defaultHostId);
		assertNull( cachedTag );

		//The find should add the category to the cache
		foundTag = tagAPI.getTagByNameAndHost(tag.getTagName(), defaultHostId);
		assertNotNull( foundTag );

		cachedTag = tagCache.get(tag.getTagName(), defaultHostId);
		assertNotNull( cachedTag );
		assertEquals( cachedTag, tag );

		/*
		 * Test cache by TagName
		 */
		tagName ="testapi26"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		tag = tagAPI.saveTag(tagName, testUser.getUserId(), defaultHostId);

		//Verify the cache -> THE SAVE SHOULD ADD NOTHING TO CACHE, JUST THE LOAD
		List<Tag> cachedTags = tagCache.getByName(tagName);
		assertNull( cachedTags );

		//The find should add the category to the cache
		List<Tag> foundTags = tagAPI.getTagsByName(tag.getTagName());
		assertNotNull( foundTags );

		cachedTags = tagCache.getByName(tagName);
		assertNotNull( cachedTags );
		assertEquals( cachedTags, foundTags );

		/*
		 * Test cache by Inode
		 */
		Contentlet contentAsset=new Contentlet();
		ContentType contentType = TestDataUtils.getWikiLikeContentType();
		contentAsset.setContentTypeId(contentType.id());
		contentAsset.setHost(defaultHostId);
		contentAsset.setProperty(WIKI_SYSPUBLISHDATE_VARNAME, new Date());
		String name="testtagapi27"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss");
		contentAsset.setProperty(WIKI_TITLE_VARNAME, name);
		contentAsset.setProperty(WIKI_URL_VARNAME, name);
		contentAsset.setProperty(WIKI_BYLINEL_VARNAME, "test");
		contentAsset.setProperty(WIKI_STORY_VARNAME, "test");
		contentAsset.setLanguageId(langAPI.getDefaultLanguage().getId());
		contentAsset=conAPI.checkin(contentAsset, testUser, false);
		APILocator.getContentletAPI().publish(contentAsset, testUser, false);

		tagName ="testapi27"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		tagAPI.saveTag(tagName, testUser.getUserId(), defaultHostId);
		tagAPI.addContentletTagInode(tagName, contentAsset.getInode(), defaultHostId, WIKI_TAG_VARNAME);

		//Verify the cache -> THE SAVE SHOULD ADD NOTHING TO CACHE, JUST THE LOAD
		cachedTags = tagCache.getByInode(contentAsset.getInode());
		assertNull( cachedTags );

		//The find should add the category to the cache
		foundTags = tagAPI.getTagsByInode(contentAsset.getInode());
		assertNotNull( foundTags );

		cachedTags = tagCache.getByInode(contentAsset.getInode());
		assertNotNull( cachedTags );
		assertEquals( cachedTags, foundTags );

		/*
		 * Test cache by hostId
		 */
		tagCache.clearCache();

		cachedTags = tagCache.getByHost(defaultHostId);
		assertNull( cachedTags );


		TagFactory tagFactory = FactoryLocator.getTagFactory();
		foundTags = tagFactory.getTagsByHost(defaultHostId);
		assertNotNull( foundTags );

		cachedTags = tagCache.getByHost(defaultHostId);
		assertNotNull( cachedTags );
		assertEquals( cachedTags, foundTags );
   
		conAPI.destroy(contentAsset, testUser, false);
	}

	private Tag saveTag (final String tagName) throws DotDataException {

		tagAPI.saveTag ( tagName, testUser.getUserId(), defaultHostId );
		Tag tag = tagAPI.getTagByNameAndHost(tagName, defaultHostId);
		assertNotNull(tag);
		assertEquals(tagName.toLowerCase(), tag.getTagName());

		return tag;
	}

	private Tag saveTag (final String tagName, boolean isPersona) throws DotDataException {

		tagAPI.saveTag ( tagName, testUser.getUserId(), defaultHostId, isPersona);
		Tag tag = tagAPI.getTagByNameAndHost(tagName, defaultHostId);
		assertNotNull(tag);
		assertEquals(tagName, tag.getTagName());
		assertFalse(tag.isPersona());

		return tag;
	}

	private void createAndGetTagByName (final String tagName) throws DotDataException {

		saveTag(tagName);
		List<Tag> tags = tagAPI.getTagsByName(tagName);
		assertTrue( tags.size() >= 1 );
		for(Tag tag : tags){
			assertEquals(tagName.toLowerCase(), tag.getTagName());
		}
	}

	// ==================== Permission-Checked Delete Tests ====================

	/**
	 * Method to test: {@link TagAPI#canDeleteTag(User, String)}
	 * Given scenario: Tag with no contentlet associations (orphan tag)
	 * Expected result: Returns true (deletion allowed)
	 */
	@Test
	public void canDeleteTag_OrphanTag_ReturnsTrue() throws Exception {
		// Create a tag with no associations
		final String tagName = "candeleteorphan" + System.currentTimeMillis();
		final Tag tag = tagAPI.saveTag(tagName, testUser.getUserId(), defaultHostId, false);
		assertNotNull(tag);

		// Check permission - should return true (allowed)
		final boolean result = tagAPI.canDeleteTag(testUser, tag.getTagId());
		assertTrue("Orphan tag should be deletable", result);

		// Verify tag still exists (canDeleteTag doesn't delete)
		final Tag foundTag = tagAPI.getTagByTagId(tag.getTagId());
		assertNotNull("Tag should still exist after canDeleteTag check", foundTag);

		// Cleanup
		tagAPI.deleteTag(tag.getTagId());
	}

	/**
	 * Method to test: {@link TagAPI#canDeleteTag(User, String)}
	 * Given scenario: Tag associated with contentlet user cannot edit
	 * Expected result: Returns false (deletion denied)
	 */
	@Test
	public void canDeleteTag_WithoutPermission_ReturnsFalse() throws Exception {
		// Create a limited user with no permissions
		final Role limitedRole = new RoleDataGen().nextPersisted();
		final User limitedUser = new UserDataGen().roles(limitedRole).nextPersisted();

		Contentlet contentAsset = null;
		Tag tag = null;

		try {
			// Create a contentlet as admin
			contentAsset = createTestContentlet();

			// Create a tag and associate with the contentlet
			final String tagName = "candeletedenied" + System.currentTimeMillis();
			tag = tagAPI.getTagAndCreate(tagName, testUser.getUserId(), defaultHostId);
			tagAPI.addContentletTagInode(tagName, contentAsset.getInode(), defaultHostId, WIKI_TAG_VARNAME);

			// Check permission - should return false (denied)
			final boolean result = tagAPI.canDeleteTag(limitedUser, tag.getTagId());
			assertFalse("Should return false when permission denied", result);

			// Verify tag still exists (canDeleteTag doesn't delete)
			final Tag foundTag = tagAPI.getTagByTagId(tag.getTagId());
			assertNotNull("Tag should still exist after canDeleteTag check", foundTag);

		} finally {
			// Cleanup
			if (tag != null) {
				try { tagAPI.deleteTag(tag.getTagId()); } catch (Exception e) { /* ignore */ }
			}
			if (contentAsset != null) {
				try { conAPI.destroy(contentAsset, systemUser, false); } catch (Exception e) { /* ignore */ }
			}
			try { UserDataGen.remove(limitedUser); } catch (Exception e) { /* ignore */ }
		}
	}

	/**
	 * Method to test: {@link TagAPI#deleteTag(User, String)}
	 * Given scenario: Tag with no contentlet associations (orphan tag)
	 * Expected result: Tag should be deleted successfully
	 */
	@Test
	public void deleteTag_OrphanTag_ShouldDelete() throws Exception {
		// Create a tag with no associations
		final String tagName = "orphantag" + System.currentTimeMillis();
		final Tag tag = tagAPI.saveTag(tagName, testUser.getUserId(), defaultHostId, false);
		assertNotNull(tag);
		assertNotNull(tag.getTagId());

		// Verify tag exists
		Tag foundTag = tagAPI.getTagByTagId(tag.getTagId());
		assertNotNull(foundTag);

		// Delete using permission-checked method - should succeed since no associations
		tagAPI.deleteTag(testUser, tag.getTagId());

		// Verify tag is deleted
		foundTag = tagAPI.getTagByTagId(tag.getTagId());
		assertNull(foundTag);
	}

	/**
	 * Method to test: {@link TagAPI#deleteTag(User, String)}
	 * Given scenario: Tag associated with contentlet that user has EDIT permission on
	 * Expected result: Tag should be deleted successfully
	 */
	@Test
	public void deleteTag_WithPermission_ShouldDelete() throws Exception {
		// Create a contentlet
		final Contentlet contentAsset = createTestContentlet();

		try {
			// Create a tag and associate with the contentlet
			final String tagName = "permittedtag" + System.currentTimeMillis();
			final Tag tag = tagAPI.getTagAndCreate(tagName, testUser.getUserId(), defaultHostId);
			tagAPI.addContentletTagInode(tagName, contentAsset.getInode(), defaultHostId, WIKI_TAG_VARNAME);

			// Verify tag exists and is associated
			Tag foundTag = tagAPI.getTagByTagId(tag.getTagId());
			assertNotNull(foundTag);
			List<TagInode> tagInodes = tagAPI.getTagInodesByTagId(tag.getTagId());
			assertFalse(tagInodes.isEmpty());

			// Delete using admin user who has EDIT permission - should succeed
			tagAPI.deleteTag(testUser, tag.getTagId());

			// Verify tag is deleted
			foundTag = tagAPI.getTagByTagId(tag.getTagId());
			assertNull(foundTag);
		} finally {
			conAPI.destroy(contentAsset, systemUser, false);
		}
	}

	/**
	 * Method to test: {@link TagAPI#deleteTag(User, String)}
	 * Given scenario: Tag associated with contentlet that user does NOT have EDIT permission on
	 * Expected result: Returns false (permission denied, no exception thrown)
	 */
	@Test
	public void deleteTag_WithoutPermission_ShouldReturnFalse() throws Exception {
		// Create a limited user with no permissions
		final Role limitedRole = new RoleDataGen().nextPersisted();
		final User limitedUser = new UserDataGen().roles(limitedRole).nextPersisted();

		Contentlet contentAsset = null;
		Tag tag = null;

		try {
			// Create a contentlet as admin
			contentAsset = createTestContentlet();

			// Create a tag and associate with the contentlet
			final String tagName = "restrictedtag" + System.currentTimeMillis();
			tag = tagAPI.getTagAndCreate(tagName, testUser.getUserId(), defaultHostId);
			tagAPI.addContentletTagInode(tagName, contentAsset.getInode(), defaultHostId, WIKI_TAG_VARNAME);

			// Verify tag exists and is associated
			final Tag foundTag = tagAPI.getTagByTagId(tag.getTagId());
			assertNotNull(foundTag);

			// Verify the limited user does NOT have EDIT permission
			final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
			assertFalse("Limited user should NOT have EDIT permission",
					permissionAPI.doesUserHavePermission(contentAsset, PermissionAPI.PERMISSION_EDIT, limitedUser));

			// Try to delete using limited user - should return false (permission denied)
			final boolean deleted = tagAPI.deleteTag(limitedUser, tag.getTagId());
			assertFalse("Delete should return false when permission denied", deleted);

			// Verify tag still exists (was not deleted)
			final Tag tagAfterAttempt = tagAPI.getTagByTagId(tag.getTagId());
			assertNotNull("Tag should still exist after failed delete attempt", tagAfterAttempt);
		} finally {
			// Cleanup
			if (tag != null) {
				try {
					tagAPI.deleteTag(tag.getTagId());
				} catch (Exception e) {
					// Ignore cleanup errors
				}
			}
			if (contentAsset != null) {
				try {
					conAPI.destroy(contentAsset, systemUser, false);
				} catch (Exception e) {
					// Ignore cleanup errors
				}
			}
			try {
				UserDataGen.remove(limitedUser);
			} catch (Exception e) {
				// Ignore cleanup errors
			}
		}
	}

	/**
	 * Method to test: {@link TagAPI#deleteTag(User, String)}
	 * Given scenario: Tag associated with contentlet that was deleted (orphan association)
	 * Expected result: Tag should be deleted successfully since association points to non-existent content
	 */
	@Test
	public void deleteTag_OrphanAssociation_ShouldDelete() throws Exception {
		// Create a contentlet
		Contentlet contentAsset = createTestContentlet();
		final String contentletInode = contentAsset.getInode();

		// Create a tag and associate with the contentlet
		final String tagName = "orphanassoctag" + System.currentTimeMillis();
		final Tag tag = tagAPI.getTagAndCreate(tagName, testUser.getUserId(), defaultHostId);
		tagAPI.addContentletTagInode(tagName, contentletInode, defaultHostId, WIKI_TAG_VARNAME);

		// Verify tag exists and is associated
		Tag foundTag = tagAPI.getTagByTagId(tag.getTagId());
		assertNotNull(foundTag);
		List<TagInode> tagInodes = tagAPI.getTagInodesByTagId(tag.getTagId());
		assertFalse(tagInodes.isEmpty());

		// Delete the contentlet (creates orphan association)
		conAPI.destroy(contentAsset, systemUser, false);

		// Verify contentlet is deleted
		contentAsset = conAPI.find(contentletInode, systemUser, false);
		assertNull(contentAsset);

		// Verify tag still exists with orphan association
		foundTag = tagAPI.getTagByTagId(tag.getTagId());
		assertNotNull(foundTag);

		// Delete using permission-checked method - should succeed since contentlet doesn't exist
		tagAPI.deleteTag(testUser, tag.getTagId());

		// Verify tag is deleted
		foundTag = tagAPI.getTagByTagId(tag.getTagId());
		assertNull(foundTag);
	}

	/**
	 * Method to test: {@link TagAPI#deleteTag(User, String)}
	 * Given scenario: Tag associated with multiple contentlets - user has permission on all
	 * Expected result: Tag should be deleted successfully
	 */
	@Test
	public void deleteTag_MultipleContentletsWithPermission_ShouldDelete() throws Exception {
		Contentlet contentAsset1 = null;
		Contentlet contentAsset2 = null;

		try {
			// Create two contentlets
			contentAsset1 = createTestContentlet();
			contentAsset2 = createTestContentlet();

			// Create a tag and associate with both contentlets
			final String tagName = "multitag" + System.currentTimeMillis();
			final Tag tag = tagAPI.getTagAndCreate(tagName, testUser.getUserId(), defaultHostId);
			tagAPI.addContentletTagInode(tagName, contentAsset1.getInode(), defaultHostId, WIKI_TAG_VARNAME);
			tagAPI.addContentletTagInode(tagName, contentAsset2.getInode(), defaultHostId, WIKI_TAG_VARNAME);

			// Verify tag has two associations
			List<TagInode> tagInodes = tagAPI.getTagInodesByTagId(tag.getTagId());
			assertEquals(2, tagInodes.size());

			// Delete using admin user who has EDIT permission on both - should succeed
			tagAPI.deleteTag(testUser, tag.getTagId());

			// Verify tag is deleted
			Tag foundTag = tagAPI.getTagByTagId(tag.getTagId());
			assertNull(foundTag);
		} finally {
			if (contentAsset1 != null) {
				conAPI.destroy(contentAsset1, systemUser, false);
			}
			if (contentAsset2 != null) {
				conAPI.destroy(contentAsset2, systemUser, false);
			}
		}
	}

	/**
	 * Method to test: {@link TagAPI#deleteTag(User, String)}
	 * Given scenario: Tag associated with multiple contentlets - user lacks permission on one
	 * Expected result: Returns false (permission denied, no exception thrown)
	 */
	@Test
	public void deleteTag_MultipleContentletsMixedPermissions_ShouldReturnFalse() throws Exception {
		// Create a limited user with a role
		final Role limitedRole = new RoleDataGen().nextPersisted();
		final User limitedUser = new UserDataGen().roles(limitedRole).nextPersisted();

		Contentlet contentAsset1 = null;
		Contentlet contentAsset2 = null;
		Tag tag = null;

		try {
			// Create two contentlets
			contentAsset1 = createTestContentlet();
			contentAsset2 = createTestContentlet();

			// Give limited user EDIT permission on first contentlet only
			final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
			final Permission permission = new Permission();
			permission.setPermission(PermissionAPI.PERMISSION_EDIT);
			permission.setRoleId(limitedRole.getId());
			permission.setInode(contentAsset1.getPermissionId());
			permissionAPI.save(permission, contentAsset1, systemUser, false);

			// Verify permissions
			assertTrue("Limited user should have EDIT permission on first contentlet",
					permissionAPI.doesUserHavePermission(contentAsset1, PermissionAPI.PERMISSION_EDIT, limitedUser));
			assertFalse("Limited user should NOT have EDIT permission on second contentlet",
					permissionAPI.doesUserHavePermission(contentAsset2, PermissionAPI.PERMISSION_EDIT, limitedUser));

			// Create a tag and associate with both contentlets
			final String tagName = "mixedpermtag" + System.currentTimeMillis();
			tag = tagAPI.getTagAndCreate(tagName, testUser.getUserId(), defaultHostId);
			tagAPI.addContentletTagInode(tagName, contentAsset1.getInode(), defaultHostId, WIKI_TAG_VARNAME);
			tagAPI.addContentletTagInode(tagName, contentAsset2.getInode(), defaultHostId, WIKI_TAG_VARNAME);

			// Verify tag has two associations
			List<TagInode> tagInodes = tagAPI.getTagInodesByTagId(tag.getTagId());
			assertEquals(2, tagInodes.size());

			// Try to delete using limited user - should return false
			// because user lacks permission on second contentlet
			final boolean deleted = tagAPI.deleteTag(limitedUser, tag.getTagId());
			assertFalse("Delete should return false when permission denied on any contentlet", deleted);

			// Verify tag still exists (was not deleted)
			final Tag tagAfterAttempt = tagAPI.getTagByTagId(tag.getTagId());
			assertNotNull("Tag should still exist after failed delete attempt", tagAfterAttempt);
		} finally {
			// Cleanup
			if (tag != null) {
				try {
					tagAPI.deleteTag(tag.getTagId());
				} catch (Exception e) {
					// Ignore cleanup errors
				}
			}
			if (contentAsset1 != null) {
				try {
					conAPI.destroy(contentAsset1, systemUser, false);
				} catch (Exception e) {
					// Ignore cleanup errors
				}
			}
			if (contentAsset2 != null) {
				try {
					conAPI.destroy(contentAsset2, systemUser, false);
				} catch (Exception e) {
					// Ignore cleanup errors
				}
			}
			try {
				UserDataGen.remove(limitedUser);
			} catch (Exception e) {
				// Ignore cleanup errors
			}
		}
	}

	/**
	 * Helper method to create a test contentlet for permission tests
	 */
	private Contentlet createTestContentlet() throws Exception {
		final Contentlet contentAsset = new Contentlet();
		final ContentType contentType = TestDataUtils.getWikiLikeContentType();
		contentAsset.setContentTypeId(contentType.id());
		contentAsset.setHost(defaultHostId);
		contentAsset.setProperty(WIKI_SYSPUBLISHDATE_VARNAME, new Date());
		final String name = "testtagperm" + System.currentTimeMillis();
		contentAsset.setProperty(WIKI_TITLE_VARNAME, name);
		contentAsset.setProperty(WIKI_URL_VARNAME, name);
		contentAsset.setProperty(WIKI_BYLINEL_VARNAME, "test");
		contentAsset.setProperty(WIKI_STORY_VARNAME, "test");
		contentAsset.setLanguageId(langAPI.getDefaultLanguage().getId());
		contentAsset.setIndexPolicy(IndexPolicy.FORCE);
		final Contentlet checkedIn = conAPI.checkin(contentAsset, testUser, false);
		conAPI.publish(checkedIn, testUser, false);
		return checkedIn;
	}

	/**
	 * Method to test: {@link TagAPI#getTagsInText(String, String)}
	 * Given scenario: An existing persona tag and provided that persona tag name with a suffix of ":persona"
	 * Expected result: Should return the existing tag and not create a duplicate with the ":persona" suffix.
	 */
	@Test
	public void getTagsInText_givenTagWithPersonaSuffix_shouldNotCreateDup() throws Exception{
		final Tag personaTag = new TagDataGen().name("personaTagIT").persona(true)
				.nextPersisted();
		final List<Tag> fetchedTags = tagAPI
				.getTagsInText(personaTag.getTagName()+":persona", defaultHostId);

		assertEquals(personaTag.getTagName(), fetchedTags.get(0).getTagName());
		assertEquals(personaTag.getTagId(), fetchedTags.get(0).getTagId());
	}

	/**
	 * Method to test: {@link TagAPI#findTopTags(String)}
	 * Given scenario: Save 200 times a couple tags to highlight on the top ten
	 * Expected result: The top tags should be not null, not empty and contain the tags saved
	 */
	@Test
	public void findTopTags_should_be_not_null_not_empty_and_contains_one_popular_tags() throws Exception{

		final String tagvalue1 = "mytesttag1";

		final Tag tag1 = Try.of(()->APILocator.getTagAPI().saveTag(tagvalue1, testUser.getUserId(), defaultHostId)).getOrNull();
		IntStream.range(0, 100).forEach(r -> {

			try {
				final Contentlet contentAsset = new Contentlet();
				ContentType contentType = TestDataUtils.getWikiLikeContentType();
				contentAsset.setContentTypeId(contentType.id());
				contentAsset.setHost(defaultHostId);
				contentAsset.setProperty(WIKI_SYSPUBLISHDATE_VARNAME, new Date());
				String name = "testtagapi" + UtilMethods.dateToHTMLDate(new Date(), "MMddyyyyHHmmss");
				contentAsset.setProperty(WIKI_TITLE_VARNAME, name);
				contentAsset.setProperty(WIKI_URL_VARNAME, name);
				contentAsset.setProperty(WIKI_BYLINEL_VARNAME, "test");
				contentAsset.setProperty(WIKI_STORY_VARNAME, "test");
				contentAsset.setLanguageId(langAPI.getDefaultLanguage().getId());
				final Contentlet savedContentAsset = conAPI.checkin(contentAsset, testUser, false);
				APILocator.getContentletAPI().publish(savedContentAsset, testUser, false);
				tagAPI.addContentletTagInode(tagvalue1, contentAsset.getInode(), defaultHostId, WIKI_TAG_VARNAME);
			} catch (Exception e) {
				Logger.error(this, e.getMessage(), e);
			}
		});

		final Set<String> topTagsSet = APILocator.getTagAPI().findTopTags(defaultHostId);

		assertNotNull("The top tags should be not null",topTagsSet);
		assertFalse("The top tags should be not empty", topTagsSet.isEmpty());
		assertTrue("The mytesttag1 should be on the top ten", topTagsSet.contains(tagvalue1));
	}

	/**
	 * Method to test: {@link TagAPI#findTopTags(String)}
	 * Given scenario: pass site id as a null
	 * Expected result: Expecting {@link IllegalArgumentException} to be thrown
	 */
	@Test(expected = IllegalArgumentException.class)
	public void findTopTags_on_null_throw_exception() throws Exception{

			APILocator.getTagAPI().findTopTags(null);
			fail("should not reach this section");
	}

	/**
	 * Method to test: {@link TagAPI#findTopTags(String)}
	 * Given scenario: pass site id as an empty string
	 * Expected result: Expecting {@link IllegalArgumentException} to be thrown
	 */
	@Test(expected = IllegalArgumentException.class)
	public void findTopTags_on_empty_throw_exception() throws Exception{

		APILocator.getTagAPI().findTopTags("");
		fail("should not reach this section");
	}

}
