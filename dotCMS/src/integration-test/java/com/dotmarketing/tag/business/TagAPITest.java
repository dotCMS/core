package com.dotmarketing.tag.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import com.dotcms.TestBase;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.portlets.structure.business.StructureAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;
import com.dotmarketing.util.IntegrationTestInitService;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * Test the functionality of TagAPI class
 * @author Oswaldo Gallango
 * @since 2/1/2016
 * @version 1.0
 */
public class TagAPITest extends TestBase {

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
		assertTrue( tags.size() >= 1 );
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
		tagAPI.addUserTagInode(createdTag, APILocator.getUserProxyAPI().getUserProxy(testUser.getUserId(), APILocator.getUserAPI().getSystemUser(), false).getInode());

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
		String tagName = "testapi2" + UtilMethods.dateToHTMLDate(new Date(), "MMddyyyyHHmmss");
		tagAPI.addUserTag(tagName, testUser.getUserId(), userProxy.getInode());

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
		assertNotNull(tag);
		assertTrue(tag.equals(newTag));
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

		assertNotNull(tag);
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
		assertNotNull(tag);
		assertTrue(tag.getTagName().equals(tagName));

		//save tag second implementation saveTag ( String tagName, String userId, String hostId, boolean persona )
		String tagName2 ="testapi5"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		tagAPI.saveTag ( tagName2, testUser.getUserId(), defaultHostId, false );
		Tag tag2 = tagAPI.getTagByNameAndHost(tagName2, defaultHostId);
		assertNotNull(tag2);
		assertTrue(tag2.getTagName().equals(tagName2) && tag2.isPersona()==false);
	}

	/**
	 * Test the addTag method from the tagAPI
	 * @throws Exception
	 */
	@Test
	public void addTag() throws Exception {
		UserProxy userProxy = APILocator.getUserProxyAPI().getUserProxy(testUser.getUserId(),systemUser, false);
		String tagName = "testapi6" + UtilMethods.dateToHTMLDate(new Date(), "MMddyyyyHHmmss");
		tagAPI.addUserTag(tagName, testUser.getUserId(), userProxy.getInode());

		Tag tag = tagAPI.getTagByNameAndHost(tagName, hostAPI.findSystemHost().getIdentifier());
		assertNotNull(tag);
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
		TagInode tagInode = tagAPI.addContentletTagInode(tagName, contentAsset.getInode(), defaultHostId, WIKI_TAG_VARNAME);

		TagInode tInode = tagAPI.getTagInode(tag.getTagId(), tagInode.getInode(),WIKI_TAG_VARNAME);
		assertTrue(UtilMethods.isSet(tInode.getInode()) && tInode.getInode().equals(tagInode.getInode()) && tInode.getTagId().equals(tag.getTagId()));

		//testing second implementation of public TagInode addTagInode ( Tag tag, String inode )
		String tagName2 ="testapi14"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		Tag tag2 = tagAPI.getTagAndCreate(tagName2, testUser.getUserId(), defaultHostId);
		TagInode tagInode2 = tagAPI.addContentletTagInode(tag2, contentAsset.getInode(), WIKI_TAG_VARNAME);

		TagInode tInode2 = tagAPI.getTagInode(tag2.getTagId(), tagInode2.getInode(),WIKI_TAG_VARNAME);
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
		TagInode tagInode = tagAPI.addContentletTagInode(tagName, contentAsset.getInode(), defaultHostId, WIKI_TAG_VARNAME);

		TagInode tagInode2 = tagAPI.getTagInode ( tag.getTagId(), contentAsset.getInode(),WIKI_TAG_VARNAME);

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
		tagAPI.addContentletTagInode(tagName, contentAsset.getInode(), defaultHostId, WIKI_TAG_VARNAME);

		TagInode tagInode = tagAPI.getTagInode ( tag.getTagId(), contentAsset.getInode(),WIKI_TAG_VARNAME);
		assertNotNull(tagInode);		
		tagAPI.removeTagRelationAndTagWhenPossible(tag.getTagId(), contentAsset.getInode(),WIKI_TAG_VARNAME);

		tagInode = tagAPI.getTagInode ( tag.getTagId(), contentAsset.getInode(),WIKI_TAG_VARNAME);
		assertNull(tagInode);	

		tag = tagAPI.getTagByTagId(tag.getTagId());
		assertNull(tag);	
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
			assertTrue(tag.getTagName().indexOf(tagName) != -1);
		}

		tagName="testing";
		tags = tagAPI.getSuggestedTag (tagName, defaultHostId);
		assertTrue(tags.size() >= 1);
		for(Tag tag : tags){
			assertTrue(tag.getTagName().indexOf(tagName) != -1);
		}

		assertTrue(tags.size() < tagSize);
	}

	/**
	 * Test the updateTagReferences method of the tagAPI
	 * @throws Exception
	 */
	@Test
	public void updateTagReferences() throws Exception{

		Contentlet contentAsset=new Contentlet();
		Structure st = structureAPI.findByVarName("Host", systemUser);
		contentAsset.setStructureInode(st.getInode());
		String hostName="testtagapiHost"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss");
		contentAsset.setProperty(Host.HOST_NAME_KEY, hostName);
		contentAsset.setLanguageId(langAPI.getDefaultLanguage().getId());
		contentAsset=conAPI.checkin(contentAsset, testUser, false);
		conAPI.publish(contentAsset, testUser, false);
		assertTrue(conAPI.isInodeIndexed(contentAsset.getInode(), 100));

		Host newHost = hostAPI.findByName(hostName, systemUser, false);
		contentAsset.setProperty(Host.TAG_STORAGE, newHost.getIdentifier());
		contentAsset.setInode(null);
		contentAsset=conAPI.checkin(contentAsset, testUser, false);
		conAPI.publish(contentAsset, testUser, false);

		TagFactory tagFactory = FactoryLocator.getTagFactory();
		List<Tag> tags = tagFactory.getTagsByHost(defaultHostId);
		int initialNumberOfTagsDemo =tags.size();
		assertNotNull(tags);
		assertTrue(initialNumberOfTagsDemo > 0);

		tags = tagFactory.getTagsByHost(newHost.getIdentifier());
		int initialNumberOfTagsNewHost =tags.size();
		assertNotNull(tags);
		assertTrue(initialNumberOfTagsNewHost >= 0);

		//Move tags to other host
		tagAPI.updateTagReferences( defaultHostId, defaultHostId, newHost.getIdentifier() );

		//to refresh cache
		tagCache.clearCache();

		List<Tag> newHostTags = tagFactory.getTagsByHost(newHost.getIdentifier());
		assertTrue(newHostTags.size() > initialNumberOfTagsNewHost);

		List<Tag> tagsAfterUpdate = tagFactory.getTagsByHost(defaultHostId);
		assertTrue(tagsAfterUpdate.size() < initialNumberOfTagsDemo);


		//return tags to original host
		tagAPI.updateTagReferences( defaultHostId, newHost.getIdentifier(), defaultHostId );
		tagCache.clearCache();

		tagsAfterUpdate = tagFactory.getTagsByHost(defaultHostId);
		assertTrue(tagsAfterUpdate.size() == initialNumberOfTagsDemo);
		/*here the amount is not 0 because is entering in the condition 
		 * if((hostIdentifier.equals(newTagStorageId) && hostTagList.size() == 0) && !newTagStorageId.equals(Host.SYSTEM_HOST)) {
		 * saveTag(tag.getTagName(), "", hostIdentifier);
		 */
		newHostTags = tagFactory.getTagsByHost(newHost.getIdentifier());
		assertTrue(newHostTags.size() == initialNumberOfTagsDemo);

		//delete host
		conAPI.unpublish(newHost, systemUser, false);
		conAPI.archive(newHost, systemUser, false);
		conAPI.delete(newHost, systemUser, false);
	}

	/**
	 * Test the getTagsByInode method of the tagAPI
	 * @throws Exception
	 */
	@Test
	public void getTagsByInode() throws Exception{
		Contentlet contentAsset=new Contentlet();
		Structure st = structureAPI.findByVarName(WIKI_STRUCTURE_VARNAME, systemUser);
		contentAsset.setStructureInode(st.getInode());
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
		assertTrue(tags.size()==7);
	}

	/**
	 * Test the Personas tags functionality
	 * @throws Exception
	 */
	@Test
	public void validatePersonaTags() throws Exception {

		Contentlet persona = new Contentlet();
		persona.setStructureInode(PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_INODE);
		persona.setHost(defaultHostId);
		persona.setLanguageId(langAPI.getDefaultLanguage().getId());
		String name="testtagapipersona1"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss");
		String othertags="testapipersona_1"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss");
		persona.setProperty(PersonaAPI.NAME_FIELD, name);
		persona.setProperty(PersonaAPI.KEY_TAG_FIELD, name);
		persona.setProperty(PersonaAPI.TAGS_FIELD, othertags);
		persona.setProperty(PersonaAPI.DESCRIPTION_FIELD,"test to delete");
		persona=conAPI.checkin(persona, testUser, false);
		conAPI.publish(persona, testUser, false);
		assertTrue(conAPI.isInodeIndexed(persona.getInode(), 500));

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
		conAPI.publish(persona, systemUser, false);
		tag = tagAPI.getTagByNameAndHost(name, defaultHostId);
		assertNotNull(tag);
		assertTrue(tag.isPersona());
		assertTrue(tag.getTagName().equals(name));

		tag = tagAPI.getTagByNameAndHost(othertags, defaultHostId);
		assertNotNull(tag);
		assertFalse(tag.isPersona());
		assertTrue(tag.getTagName().equals(othertags));
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
		Structure st = structureAPI.findByVarName(WIKI_STRUCTURE_VARNAME, systemUser);
		contentAsset.setStructureInode(st.getInode());
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
		tag = tagAPI.saveTag(tagName, testUser.getUserId(), defaultHostId);
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
	}
}
