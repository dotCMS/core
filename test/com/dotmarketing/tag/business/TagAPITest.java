package com.dotmarketing.tag.business;

import static com.dotcms.repackage.org.junit.Assert.assertNotNull;
import static com.dotcms.repackage.org.junit.Assert.assertNull;
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
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.business.StructureAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;
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
	private static ContentletAPI conAPI =APILocator.getContentletAPI();
	private static LanguageAPI langAPI =APILocator.getLanguageAPI();
	private static StructureAPI structureAPI =APILocator.getStructureAPI();

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

	@BeforeClass
	public static void prepare () throws DotSecurityException, DotDataException {
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
		List<Tag> tags = tagAPI.getAllTags();
		assertTrue( tags.size() >= 125 );
	}

	/**
	 * Test the getTagByName method from the tagAPI
	 * @throws Exception
	 */
	@Test
	public void getTagByName() throws Exception {
		String tagName="china";
		List<Tag> tags = tagAPI.getTagsByName(tagName);
		assertTrue( tags.size() == 1 );
		for(Tag tag : tags){
			assertTrue(tag.getTagName().equals(tagName));
		}
	}

	/**
	 * Test the getTagByUser method from the tagAPI
	 * @throws Exception
	 */
	@Test
	public void getTagsForUserByUserId() throws Exception{

		List<Tag> tags = tagAPI.getTagsForUserByUserId(testUser.getUserId());
		assertNotNull(tags);
		assertTrue(tags.size() > 0);

		String tagName = "testapi"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss");
		Tag createdTag = tagAPI.saveTag(tagName, testUser.getUserId(), defaultHostId, false);
		tagAPI.addTagInode(createdTag,
				APILocator.getUserProxyAPI().getUserProxy(testUser.getUserId(), APILocator.getUserAPI().getSystemUser(), false).getInode());

		tags = tagAPI.getTagsForUserByUserId(testUser.getUserId());
		assertNotNull(tags);
		assertTrue(tags.size() > 0);
		assertTrue(tags.contains(createdTag));
	}

	/**
	 * Test the getTagsForUserByUserInode method from the tagAPI
	 * @throws Exception
	 */
	@Test
	public void getTagsForUserByUserInode() throws Exception{
		UserProxy userProxy = APILocator.getUserProxyAPI().getUserProxy(testUser.getUserId(),systemUser, false);
		String tagName ="testapi2"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		tagAPI.addTag ( tagName, testUser.getUserId(), userProxy.getInode() );

		List<Tag> tags =  tagAPI.getTagsForUserByUserInode(userProxy.getInode());    
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
			assertTrue(tag.getTagName().indexOf(tagName) != -1);
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
		assertTrue(UtilMethods.isSet(tag) && tag.equals(newTag));
	}

	/**
	 * Test the getTagByTagId method from the tagAPI
	 * @throws Exception
	 */
	@Test
	public void getTagByTagId() throws Exception{
		String tagId = tagAPI.getAllTags().get(0).getTagId();
		Tag tag = tagAPI.getTagByTagId(tagId); 

		assertTrue(UtilMethods.isSet(tag));
		assertTrue(tag.getTagId().equals(tagId));
	}

	/**
	 * Test the getTagByNameAndHost method from the tagAPI
	 * @throws Exception
	 */
	@Test
	public void getTagByNameAndHost() throws Exception{
		String tagName="inflation";
		Tag tag = tagAPI.getTagByNameAndHost( tagName, defaultHostId);

		assertTrue(UtilMethods.isSet(tag));
		assertTrue(tag.getTagName().equals(tagName));
	}

	/**
	 * Test the two saveTag methods from the tagAPI
	 * @throws Exception
	 */
	@Test
	public void saveTag() throws Exception {
		//save tag first implementation  saveTag ( String tagName, String userId, String hostId )
		String tagName ="testapi4"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		tagAPI.saveTag ( tagName, testUser.getUserId(), defaultHostId );
		Tag tag = tagAPI.getTagByNameAndHost(tagName, defaultHostId);
		assertTrue(UtilMethods.isSet(tag) && tag.getTagName().equals(tagName));

		//save tag second implementation saveTag ( String tagName, String userId, String hostId, boolean persona )
		String tagName2 ="testapi5"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		tagAPI.saveTag ( tagName2, testUser.getUserId(), defaultHostId, false );
		Tag tag2 = tagAPI.getTagByNameAndHost(tagName2, defaultHostId);
		assertTrue(UtilMethods.isSet(tag2) && tag2.getTagName().equals(tagName2) && tag2.isPersona()==false);
	}

	/**
	 * Test the addTag method from the tagAPI
	 * @throws Exception
	 */
	@Test
	public void addTag() throws Exception {
		UserProxy userProxy = APILocator.getUserProxyAPI().getUserProxy(testUser.getUserId(),systemUser, false);
		String tagName ="testapi6"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		tagAPI.addTag ( tagName, testUser.getUserId(), userProxy.getInode() );

		Tag tag = tagAPI.getTagByNameAndHost(tagName, hostAPI.findSystemHost().getIdentifier());
		assertTrue(UtilMethods.isSet(tag));
		assertTrue(tag.getTagName().equals(tagName));
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
		assertTrue(tag.getTagName().equals(tagName2));

		//testing update tag second implementation ( String tagId, String tagName, boolean updateTagReference, String hostId )
		String tagName3 ="testapi9"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		Host host = hostAPI.findSystemHost();
		tagAPI.updateTag(newTag.getTagId(), tagName3, true, host.getIdentifier() );

		tag = tagAPI.getTagByTagId(newTag.getTagId());
		assertTrue(tag.getTagName().equals(tagName3));
		assertTrue(tag.getHostId().equals(host.getIdentifier()));
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
		Structure st = structureAPI.findByVarName(WIKI_STRUCTURE_VARNAME, systemUser);
		contentAsset.setStructureInode(st.getInode());
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
		TagInode tagInode = tagAPI.addTagInode ( tagName, contentAsset.getInode(), defaultHostId);

		TagInode tInode = tagAPI.getTagInode(tag.getTagId(), tagInode.getInode());
		assertTrue(UtilMethods.isSet(tInode.getInode()) && tInode.getInode().equals(tagInode.getInode()) && tInode.getTagId().equals(tag.getTagId()));

		//testing second implementation of public TagInode addTagInode ( Tag tag, String inode )
		String tagName2 ="testapi14"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		Tag tag2 = tagAPI.getTagAndCreate(tagName2, testUser.getUserId(), defaultHostId);
		TagInode tagInode2 = tagAPI.addTagInode ( tag2, contentAsset.getInode());

		TagInode tInode2 = tagAPI.getTagInode(tag2.getTagId(), tagInode2.getInode());
		assertTrue(UtilMethods.isSet(tInode2.getInode()) && tInode2.getInode().equals(tagInode2.getInode()) && tInode2.getTagId().equals(tag2.getTagId()));
	}

	/**
	 * Test the getTagInodesByInode method of the tagAPI
	 * @throws Exception
	 */
	@Test
	public void getTagInodesByInode() throws Exception{

		Contentlet contentAsset=new Contentlet();
		Structure st = structureAPI.findByVarName(WIKI_STRUCTURE_VARNAME, systemUser);
		contentAsset.setStructureInode(st.getInode());
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
		TagInode tagInode = tagAPI.addTagInode ( tagName, contentAsset.getInode(), defaultHostId);

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
	}

	/**
	 * Test the getTagInodesByTagId method of the tagAPI
	 * @throws Exception
	 */
	@Test
	public void getTagInodesByTagId() throws Exception{
		Contentlet contentAsset=new Contentlet();
		Structure st = structureAPI.findByVarName(WIKI_STRUCTURE_VARNAME, systemUser);
		contentAsset.setStructureInode(st.getInode());
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
		TagInode tagInode = tagAPI.addTagInode ( tagName, contentAsset.getInode(), defaultHostId);

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
	}

	/**
	 * Test the getTagInode method of the tagAPI
	 * @throws Exception
	 */
	@Test
	public void getTagInode() throws Exception {

		Contentlet contentAsset=new Contentlet();
		Structure st = structureAPI.findByVarName(WIKI_STRUCTURE_VARNAME, systemUser);
		contentAsset.setStructureInode(st.getInode());
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
		TagInode tagInode = tagAPI.addTagInode ( tagName, contentAsset.getInode(), defaultHostId);

		TagInode tagInode2 = tagAPI.getTagInode ( tag.getTagId(), contentAsset.getInode() );

		assertNotNull(tagInode);
		assertNotNull(tagInode2);
		assertTrue(UtilMethods.isSet(tagInode.getTagId()));
		assertTrue(UtilMethods.isSet(tagInode2.getTagId()));
		assertTrue(tagInode.getTagId().equals(tagInode2.getTagId()));
		assertTrue(tagInode.getInode().equals(tagInode2.getInode()));
	}

	/**
	 * Test the three deleteTagInode methods of the tagAPI
	 * @throws Exception
	 */
	@Test
	public void deleteTagInode() throws Exception {
		Contentlet contentAsset=new Contentlet();
		Structure st = structureAPI.findByVarName(WIKI_STRUCTURE_VARNAME, systemUser);
		contentAsset.setStructureInode(st.getInode());
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
		TagInode tagInode = tagAPI.addTagInode ( tagName, contentAsset.getInode(), defaultHostId);
		//testing second implementation of public TagInode addTagInode ( TagInode tagInode )
		tagAPI.deleteTagInode( tagInode );

		tagInode = tagAPI.getTagInode ( tag.getTagId(), contentAsset.getInode() );
		assertNull(tagInode);
		
		String tagName2 ="testapi19"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		Tag tag2 = tagAPI.getTagAndCreate(tagName, testUser.getUserId(), defaultHostId);
		TagInode tagInode2 = tagAPI.addTagInode (tagName, contentAsset.getInode(), defaultHostId);
		//testing second implementation of public TagInode addTagInode ( Tag tag, String inode )
		tagAPI.deleteTagInode( tag2, contentAsset.getInode());
		
		tagInode2 = tagAPI.getTagInode(tag2.getTagId(), contentAsset.getInode());
		assertNull(tagInode2);
		
		tag2 = tagAPI.getTagByTagId(tag2.getTagId());
		assertNotNull(tag2);
		
		//testing third implementation of public TagInode addTagInode ( String tagName, String inode )
		String tagName3 ="testapi20"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		Tag tag3 = tagAPI.getTagAndCreate(tagName3, testUser.getUserId(), defaultHostId);
		TagInode tagInode3 = tagAPI.addTagInode (tagName3, contentAsset.getInode(), defaultHostId);
		tagAPI.deleteTagInode( tag3.getTagName(), contentAsset.getInode());
		
		tagInode3 = tagAPI.getTagInode(tag3.getTagId(), contentAsset.getInode());
		assertNull(tagInode3);
		
		tag3 = tagAPI.getTagByTagId(tag3.getTagId());
		assertNotNull(tag3);
	}

	/**
	 * Test the removeTagRelationAndTagWhenPossible method of the tagAPI
	 * @throws Exception
	 */
	@Test
	public void removeTagRelationAndTagWhenPossible() throws Exception {

		Contentlet contentAsset=new Contentlet();
		Structure st = structureAPI.findByVarName(WIKI_STRUCTURE_VARNAME, systemUser);
		contentAsset.setStructureInode(st.getInode());
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
		tagAPI.addTagInode ( tagName, contentAsset.getInode(), defaultHostId);

		TagInode tagInode = tagAPI.getTagInode ( tag.getTagId(), contentAsset.getInode() );
		assertNotNull(tagInode);		
		tagAPI.removeTagRelationAndTagWhenPossible(tag.getTagId(), contentAsset.getInode());

		tagInode = tagAPI.getTagInode ( tag.getTagId(), contentAsset.getInode() );
		assertNull(tagInode);	

		tag = tagAPI.getTagByTagId(tag.getTagId());
		assertNull(tag);	
	}
}
