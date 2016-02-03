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
		List<Tag> tags = tagAPI.getTagByName(tagName);
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
		List<Tag> tags = tagAPI.getTagsForUserByUserId(systemUser.getUserId());
		assertTrue(tags.size() == 0);

		String tagName = "testapi"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss");
		Tag tg = tagAPI.saveTag(tagName, testUser.getUserId(), defaultHostId, false);

		tags = tagAPI.getTagsForUserByUserId(testUser.getUserId());
		assertTrue(tags.size() >= 1);
		assertTrue(tags.contains(tg));		
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
		String tagName ="testapi2"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
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
		String tagName ="testapi3"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		tagAPI.saveTag ( tagName, testUser.getUserId(), defaultHostId );
		Tag tag = tagAPI.getTagByNameAndHost(tagName, defaultHostId);
		assertTrue(UtilMethods.isSet(tag) && tag.getTagName().equals(tagName));

		//save tag second implementation saveTag ( String tagName, String userId, String hostId, boolean persona )
		String tagName2 ="testapi4"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
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
		String tagName ="testapi5"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
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

		String tagName ="testapi6"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		Tag newTag = tagAPI.saveTag ( tagName, testUser.getUserId(), defaultHostId, false );

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

	/**
	 * Test the two deleteTag methods from the tagAPI
	 * @throws Exception
	 */
	@Test
	public void deleteTag() throws Exception{
		String tagName ="testapi9"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		Tag tag = tagAPI.saveTag ( tagName, testUser.getUserId(), defaultHostId, false );
		String tagId = tag.getTagId();
		//testing first implementation of public void deleteTag ( Tag tag )
		tagAPI.deleteTag(tag);
		tag = tagAPI.getTagByTagId(tagId);
		assertTrue(!UtilMethods.isSet(tag.getTagId()));

		tag = tagAPI.saveTag ( tagName, testUser.getUserId(), defaultHostId, false );
		tagId = tag.getTagId();
		//testing first implementation of public void deleteTag ( String tagId )
		tagAPI.deleteTag (tag.getTagId());
		tag = tagAPI.getTagByTagId(tagId);
		assertTrue(!UtilMethods.isSet(tag.getTagId()));
	}

	/**
	 * Test the editTag method of the tagAPI
	 * @throws Exception
	 */
	@Test
	public void editTag() throws Exception {
		String oldTagName ="testapi10"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		Tag tag = tagAPI.saveTag ( oldTagName, testUser.getUserId(), defaultHostId, false );

		String tagName ="testapi11"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		tagAPI.editTag ( tagName, oldTagName, testUser.getUserId());

		tag = tagAPI.getTagByTagId(tag.getTagId());
		assertTrue(!UtilMethods.isSet(tag.getTagId()));
		assertTrue(tag.getTagName().equals(tagName));
	}

	/**
	 * Test the getAllTag method of the tagAPI
	 * @throws Exception
	 */
	@Test
	public void getAllTag() throws Exception{
		String tagName ="testapi12"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		tagAPI.saveTag ( tagName, testUser.getUserId(), defaultHostId, false );
		List<Tag> tags = tagAPI.getAllTag(testUser.getUserId());
		assertTrue(tags.size() >= 1);
		for(Tag t : tags){
			assertTrue(t.getUserId().equals(testUser.getUserId()));
		}
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

		TagInode tInode = tagAPI.getTagInode(tagName, tagInode.getInode());
		assertTrue(UtilMethods.isSet(tInode.getInode()) && tInode.getInode().equals(tagInode.getInode()) && tInode.getTagId().equals(tag.getTagId()));

		//testing second implementation of public TagInode addTagInode ( Tag tag, String inode )
		String tagName2 ="testapi14"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss"); 
		Tag tag2 = tagAPI.getTagAndCreate(tagName2, testUser.getUserId(), defaultHostId);
		TagInode tagInode2 = tagAPI.addTagInode ( tag2, contentAsset.getInode());

		TagInode tInode2 = tagAPI.getTagInode(tagName, tagInode.getInode());
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
		String name="testtagapi3"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss");
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
		String name="testtagapi4"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss");
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
		String name="testtagapi5"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss");
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

		assertTrue(UtilMethods.isSet(tagInode2.getTagId()));
		assertTrue(tagInode2.equals(tagInode));
	}

	/**
	 * Test the getTagInode method of the tagAPI
	 * @throws Exception
	 */
	@Test
	public void deleteTagInode() throws Exception {
		Contentlet contentAsset=new Contentlet();
		Structure st = structureAPI.findByVarName(WIKI_STRUCTURE_VARNAME, systemUser);
		contentAsset.setStructureInode(st.getInode());
		contentAsset.setHost(defaultHostId);
		contentAsset.setProperty(WIKI_SYSPUBLISHDATE_VARNAME, new Date());
		String name="testtagapi6"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss");
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

		tagAPI.deleteTagInode( tagInode );

		TagInode tagInode2 = tagAPI.getTagInode ( tag.getTagId(), contentAsset.getInode() );

		assertTrue(!UtilMethods.isSet(tagInode2.getTagId()));
	}
}
