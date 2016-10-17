package com.dotmarketing.portlets.folder.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPIImpl;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.IntegrationTestInitService;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class FolderAPITest {

	private final String LOGO_GIF_1 = "logo.gif";
	private final String LOGO_GIF_2 = "logo2.gif";
	
    @BeforeClass
    public static void prepare () throws Exception {
    	
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();

	@Test
	public void renameFolder() throws Exception {
		User user = APILocator.getUserAPI().getSystemUser();
		Host demo = APILocator.getHostAPI().findByName("demo.dotcms.com", user, false);
		Folder ftest = APILocator.getFolderAPI().createFolders("/folderTest"+System.currentTimeMillis(), demo, user, false);
		Folder ftest1 = APILocator.getFolderAPI().createFolders(ftest.getPath()+"/ff1", demo, user, false);
		Folder ftest2 = APILocator.getFolderAPI().createFolders(ftest.getPath()+"/ff1/ff2", demo, user, false);
		Folder ftest3 = APILocator.getFolderAPI().createFolders(ftest.getPath()+"/ff1/ff2/ff3", demo, user, false);

		// get identifiers to cache
		APILocator.getIdentifierAPI().find(ftest);
		APILocator.getIdentifierAPI().find(ftest1);
		APILocator.getIdentifierAPI().find(ftest2);
		APILocator.getIdentifierAPI().find(ftest3);

		Assert.assertTrue(APILocator.getFolderAPI().renameFolder(ftest, "folderTestXX"+System.currentTimeMillis(), user, false));

		// those should be cleared from cache
		Assert.assertNull(APILocator.getIdentifierAPI().loadFromCache(ftest1.getIdentifier()));
		Assert.assertNull(APILocator.getIdentifierAPI().loadFromCache(ftest2.getIdentifier()));
		Assert.assertNull(APILocator.getIdentifierAPI().loadFromCache(ftest3.getIdentifier()));

		// make sure the rename is properly propagated on children (that's done in a db trigger)
		Identifier ident=APILocator.getIdentifierAPI().find(ftest),ident1=APILocator.getIdentifierAPI().find(ftest1),
				ident2=APILocator.getIdentifierAPI().find(ftest2),ident3=APILocator.getIdentifierAPI().find(ftest3);
		Assert.assertTrue(ident.getAssetName().startsWith("folderTestXX"));
		Assert.assertEquals(ident.getPath(),ident1.getParentPath());
		Assert.assertEquals(ident1.getPath(),ident2.getParentPath());
		Assert.assertEquals(ident2.getPath(),ident3.getParentPath());

	}

	/**
	 * Test move folders with subfolders 
	 * @throws Exception
	 */
	@Test
	public void move() throws Exception {
		User user = APILocator.getUserAPI().getSystemUser();
		Host demo = APILocator.getHostAPI().findByName("demo.dotcms.com", user, false);
		long langId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
		
		//create folders and assets
		Folder ftest = APILocator.getFolderAPI().createFolders("/folderMoveSourceTest"+System.currentTimeMillis(), demo, user, false);
		Folder ftest1 = APILocator.getFolderAPI().createFolders(ftest.getPath()+"/ff1", demo, user, false);

		//adding page
		String page0Str ="page0";
		List<Template> templates = APILocator.getTemplateAPI().findTemplatesAssignedTo(demo);
		Template template =null;
		for(Template temp: templates){
			if(temp.getTitle().equals("Quest - 1 Column")){
				template=temp;
				break;
			}
		}
		Contentlet contentAsset=new Contentlet();
		contentAsset.setStructureInode(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
		contentAsset.setHost(demo.getIdentifier());
		contentAsset.setProperty(HTMLPageAssetAPIImpl.FRIENDLY_NAME_FIELD, page0Str);
		contentAsset.setProperty(HTMLPageAssetAPIImpl.URL_FIELD, page0Str);
		contentAsset.setProperty(HTMLPageAssetAPIImpl.TITLE_FIELD, page0Str);
		contentAsset.setProperty(HTMLPageAssetAPIImpl.CACHE_TTL_FIELD, "0");
		contentAsset.setProperty(HTMLPageAssetAPIImpl.TEMPLATE_FIELD, template.getIdentifier());
		contentAsset.setLanguageId(langId);
		contentAsset.setFolder(ftest1.getInode());
		contentAsset=APILocator.getContentletAPI().checkin(contentAsset, user, false);
		APILocator.getContentletAPI().publish(contentAsset, user, false);
		
		/*adding menu link*/
		String linkStr="link";
  		Link link = new Link();
		link.setTitle(linkStr);
		link.setFriendlyName(linkStr);
		link.setParent(ftest1.getInode());
		link.setTarget("_blank");
		link.setModUser(user.getUserId());
		IHTMLPage page = APILocator.getHTMLPageAssetAPI().getPageByPath("/about-us/locations/index", demo, langId, true);

  		Identifier internalLinkIdentifier = APILocator.getIdentifierAPI().findFromInode(page.getIdentifier());
		link.setLinkType(Link.LinkType.INTERNAL.toString());
		link.setInternalLinkIdentifier(internalLinkIdentifier.getId());
		link.setProtocal("http://");

		StringBuffer myURL = new StringBuffer();
		if(InodeUtils.isSet(internalLinkIdentifier.getHostId())) {
			myURL.append(demo.getHostname());
		}
		myURL.append(internalLinkIdentifier.getURI());
		link.setUrl(myURL.toString());
		WebAssetFactory.createAsset(link, user.getUserId(), ftest1);
		APILocator.getVersionableAPI().setLive(link);

		Folder ftest2 = APILocator.getFolderAPI().createFolders(ftest.getPath()+"/ff1/ff2", demo, user, false);

		//adding page
		String page1Str="page1";
		contentAsset=new Contentlet();
		contentAsset.setStructureInode(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
		contentAsset.setHost(demo.getIdentifier());
		contentAsset.setProperty(HTMLPageAssetAPIImpl.FRIENDLY_NAME_FIELD, page1Str);
		contentAsset.setProperty(HTMLPageAssetAPIImpl.URL_FIELD, page1Str);
		contentAsset.setProperty(HTMLPageAssetAPIImpl.TITLE_FIELD, page1Str);
		contentAsset.setProperty(HTMLPageAssetAPIImpl.CACHE_TTL_FIELD, "0");
		contentAsset.setProperty(HTMLPageAssetAPIImpl.TEMPLATE_FIELD, template.getIdentifier());
		contentAsset.setLanguageId(langId);
		contentAsset.setFolder(ftest2.getInode());
		contentAsset=APILocator.getContentletAPI().checkin(contentAsset, user, false);
		APILocator.getContentletAPI().publish(contentAsset, user, false);

		/*Adding menu link*/
		String linkStr2="link2";
  		Link link2 = new Link();
		link2.setTitle(linkStr2);
		link2.setFriendlyName(linkStr2);
		link2.setParent(ftest2.getInode());
		link2.setTarget("_blank");
		link2.setModUser(user.getUserId());
		page = APILocator.getHTMLPageAssetAPI().getPageByPath("/about-us/locations/index", demo, langId, true);

  		internalLinkIdentifier = APILocator.getIdentifierAPI().findFromInode(page.getIdentifier());
		link2.setLinkType(Link.LinkType.INTERNAL.toString());
		link2.setInternalLinkIdentifier(internalLinkIdentifier.getId());
		link2.setProtocal("http://");

		myURL = new StringBuffer();
		if(InodeUtils.isSet(internalLinkIdentifier.getHostId())) {
			myURL.append(demo.getHostname());
		}
		myURL.append(internalLinkIdentifier.getURI());
		link2.setUrl(myURL.toString());
		WebAssetFactory.createAsset(link2, user.getUserId(), ftest2);
		APILocator.getVersionableAPI().setLive(link2);
		
		/*Adding file asset to folder */
        String fileTitle = "testMove.txt";
        File destFile = testFolder.newFile(fileTitle);
		FileUtil.write(destFile, "helloworld");

		Contentlet contentAsset3=new Contentlet();
		Structure st = StructureFactory.getStructureByVelocityVarName("FileAsset");
		contentAsset3.setStructureInode(st.getInode());
		contentAsset3.setHost(demo.getIdentifier());
		contentAsset3.setProperty(FileAssetAPI.FILE_NAME_FIELD, fileTitle);
		contentAsset3.setProperty(FileAssetAPI.BINARY_FIELD, destFile);
		contentAsset3.setLanguageId(langId);
		contentAsset3.setProperty(FileAssetAPI.TITLE_FIELD, fileTitle);
		contentAsset3.setFolder(ftest2.getInode());
		contentAsset3=APILocator.getContentletAPI().checkin(contentAsset3, user, false);
		APILocator.getContentletAPI().publish(contentAsset3, user, false);
				
		//adding folder
		Folder ftest3 = APILocator.getFolderAPI().createFolders(ftest.getPath()+"/ff1/ff3", demo, user, false);

		//adding page
		String page2Str ="page2";
		contentAsset=new Contentlet();
		contentAsset.setStructureInode(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
		contentAsset.setHost(demo.getIdentifier());
		contentAsset.setProperty(HTMLPageAssetAPIImpl.FRIENDLY_NAME_FIELD, page2Str);
		contentAsset.setProperty(HTMLPageAssetAPIImpl.URL_FIELD, page2Str);
		contentAsset.setProperty(HTMLPageAssetAPIImpl.TITLE_FIELD, page2Str);
		contentAsset.setProperty(HTMLPageAssetAPIImpl.CACHE_TTL_FIELD, "0");
		contentAsset.setProperty(HTMLPageAssetAPIImpl.TEMPLATE_FIELD, template.getIdentifier());
		contentAsset.setLanguageId(langId);
		contentAsset.setFolder(ftest3.getInode());
		contentAsset=APILocator.getContentletAPI().checkin(contentAsset, user, false);
		APILocator.getContentletAPI().publish(contentAsset, user, false);
		
		/*adding page content*/
		String title="movetest2";
		Contentlet contentAsset2=new Contentlet();
		st = StructureFactory.getStructureByVelocityVarName("webPageContent");
		contentAsset2.setStructureInode(st.getInode());
		contentAsset2.setHost(demo.getIdentifier());
		contentAsset2.setProperty("title", title);
		contentAsset2.setLanguageId(langId);
		contentAsset2.setProperty("body", title);
		contentAsset2.setFolder(ftest1.getInode());
		contentAsset2=APILocator.getContentletAPI().checkin(contentAsset2, user, false);
		APILocator.getContentletAPI().publish(contentAsset2, user, false);
		Container container =null;
		List<Container> containers = APILocator.getContainerAPI().findContainersForStructure(st.getInode());
		for(Container c : containers){
			if(c.getTitle().equals("Large Column (lg-1)")){
				container=c;
				break;
			}
		}
		/*Relate content to page*/
		MultiTree m = new MultiTree(contentAsset.getIdentifier(), container.getIdentifier(), contentAsset2.getIdentifier());
		MultiTreeFactory.saveMultiTree(m);
		

		Folder destinationftest = APILocator.getFolderAPI().createFolders("/folderMoveDestinationTest"+System.currentTimeMillis(), demo, user, false);

		//moving folder and assets
		Thread.sleep(2000);
		APILocator.getFolderAPI().move(ftest1, destinationftest, user, false);
				
		//validate that the folder and assets were moved
		Folder  newftest1 = APILocator.getFolderAPI().findFolderByPath(destinationftest.getPath()+ftest1.getName(), demo, user, false);
		Assert.assertTrue("Folder ("+ftest1.getName()+") wasn't moved", newftest1 != null);

		List<Link> links = APILocator.getMenuLinkAPI().findFolderMenuLinks(newftest1);
		Assert.assertTrue(links.size()==1 && links.get(0).getTitle().equals(linkStr));
		
		List<IHTMLPage> pages = APILocator.getHTMLPageAssetAPI().getLiveHTMLPages(newftest1,user, false);
		Assert.assertTrue(pages.size() == 1 && pages.get(0).getTitle().equals(page0Str));
		
		Folder  newftest2 = APILocator.getFolderAPI().findFolderByPath(newftest1.getPath()+ftest2.getName(), demo, user, false);
		Assert.assertNotNull(newftest2);
		
		pages = APILocator.getHTMLPageAssetAPI().getLiveHTMLPages(newftest2, user, false);
		Assert.assertTrue(pages.size() == 1 && pages.get(0).getTitle().equals(page1Str));
		
		links = APILocator.getMenuLinkAPI().findFolderMenuLinks(newftest2);
		Assert.assertTrue(links.size()==1 && links.get(0).getTitle().equals(linkStr2));

		List<FileAsset> files = APILocator.getFileAssetAPI().findFileAssetsByFolder(newftest2, user, false);
		Assert.assertTrue(files.size()==1 && files.get(0).getTitle().equals(fileTitle));
				
		Folder  newftest3 = APILocator.getFolderAPI().findFolderByPath(newftest1.getPath()+ftest3.getName(), demo, user, false);
		Assert.assertNotNull(newftest3);
		
		pages = APILocator.getHTMLPageAssetAPI().getLiveHTMLPages(newftest3,user, false);
		Assert.assertTrue(pages.size() == 1 && pages.get(0).getTitle().equals(page2Str));		
		
		List<MultiTree> mt= MultiTreeFactory.getMultiTree(pages.get(0).getIdentifier());
		Assert.assertTrue(mt.size() ==1 && mt.get(0).getParent2().equals(container.getIdentifier()) && mt.get(0).getChild().equals(contentAsset2.getIdentifier()) );
		
	}

	/**
	 * Test move folders with subfolders 
	 * @throws Exception
	 */
	@SuppressWarnings("resource")
	@Test
	public void copy() throws Exception {
		User user = APILocator.getUserAPI().getSystemUser();
		Host demo = APILocator.getHostAPI().findByName("demo.dotcms.com", user, false);
		long langId = APILocator.getLanguageAPI().getDefaultLanguage().getId();

		/*
		 * Test 1:
		 * copy a page in the same folder
		 */
		Folder ftest = APILocator.getFolderAPI().createFolders("/folderCopySourceTest"+System.currentTimeMillis(), demo, user, false);
		//adding page
		String pageStr ="mypage";
		List<Template> templates = APILocator.getTemplateAPI().findTemplatesAssignedTo(demo);
		Template template =null;
		for(Template temp: templates){
			if(temp.getTitle().equals("Quest - 1 Column")){
				template=temp;
				break;
			}
		}
		Contentlet contentAsset=new Contentlet();
		contentAsset.setStructureInode(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
		contentAsset.setHost(demo.getIdentifier());
		contentAsset.setProperty(HTMLPageAssetAPIImpl.FRIENDLY_NAME_FIELD, pageStr);
		contentAsset.setProperty(HTMLPageAssetAPIImpl.URL_FIELD, pageStr);
		contentAsset.setProperty(HTMLPageAssetAPIImpl.TITLE_FIELD, pageStr);
		contentAsset.setProperty(HTMLPageAssetAPIImpl.CACHE_TTL_FIELD, "0");
		contentAsset.setProperty(HTMLPageAssetAPIImpl.TEMPLATE_FIELD, template.getIdentifier());
		contentAsset.setLanguageId(langId);
		contentAsset.setFolder(ftest.getInode());
		contentAsset=APILocator.getContentletAPI().checkin(contentAsset, user, false);
		APILocator.getContentletAPI().publish(contentAsset, user, false);

		//copy page in same folder
		APILocator.getContentletAPI().copyContentlet(contentAsset, ftest, user, false);
		
		IHTMLPage page = APILocator.getHTMLPageAssetAPI().getPageByPath(ftest.getPath()+pageStr, demo, APILocator.getLanguageAPI().getDefaultLanguage().getId(), false);
		Assert.assertTrue(page != null && page.getTitle().contains(pageStr));
		page = APILocator.getHTMLPageAssetAPI().getPageByPath(ftest.getPath()+pageStr+"_COPY", demo, langId, false);
		Assert.assertTrue(page != null && page.getTitle().contains(pageStr));

		/*
		 * Test 2
		 * Copy folder (located at the second level) with a page, menu link, image asset and empty folder
		 */
		Folder ftest1 = APILocator.getFolderAPI().createFolders(ftest.getPath()+"/fcopy1", demo, user, false);
		Folder ftest2 = APILocator.getFolderAPI().createFolders(ftest1.getPath()+"/fcopy2", demo, user, false);
		Folder ftest3 = APILocator.getFolderAPI().createFolders(ftest1.getPath()+"/fcopy3", demo, user, false);

		/*adding page with content*/
		String page1Str="mypage1";
		contentAsset=new Contentlet();
		contentAsset.setStructureInode(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
		contentAsset.setHost(demo.getIdentifier());
		contentAsset.setProperty(HTMLPageAssetAPIImpl.FRIENDLY_NAME_FIELD, page1Str);
		contentAsset.setProperty(HTMLPageAssetAPIImpl.URL_FIELD, page1Str);
		contentAsset.setProperty(HTMLPageAssetAPIImpl.TITLE_FIELD, page1Str);
		contentAsset.setProperty(HTMLPageAssetAPIImpl.CACHE_TTL_FIELD, "0");
		contentAsset.setProperty(HTMLPageAssetAPIImpl.TEMPLATE_FIELD, template.getIdentifier());
		contentAsset.setFolder(ftest1.getInode());
		contentAsset=APILocator.getContentletAPI().checkin(contentAsset, user, false);
		APILocator.getContentletAPI().publish(contentAsset, user, false);

		/*Adding content*/
		String title="test1";
		Contentlet contentAsset2=new Contentlet();
		Structure st = StructureFactory.getStructureByVelocityVarName("webPageContent");
		contentAsset2.setStructureInode(st.getInode());
		contentAsset2.setHost(demo.getIdentifier());
		contentAsset2.setProperty("title", title);
		contentAsset2.setLanguageId(langId);
		contentAsset2.setProperty("body", title);
		contentAsset2.setFolder(ftest1.getInode());
		contentAsset2=APILocator.getContentletAPI().checkin(contentAsset2, user, false);
		APILocator.getContentletAPI().publish(contentAsset2, user, false);
		Container container =null;
		List<Container> containers = APILocator.getContainerAPI().findContainersForStructure(st.getInode());
		for(Container c : containers){
			if(c.getTitle().equals("Large Column (lg-1)")){
				container=c;
				break;
			}
		}
		/*Relate content to page*/
		MultiTree m = new MultiTree(contentAsset.getIdentifier(), container.getIdentifier(), contentAsset2.getIdentifier());
		MultiTreeFactory.saveMultiTree(m);

		/*Adding file asset to folder fcopy1*/
		File destFile = testFolder.newFile(LOGO_GIF_1);
		FileUtil.write(destFile, "helloworld");

		Contentlet contentAsset3=new Contentlet();
		st = StructureFactory.getStructureByVelocityVarName("FileAsset");
		contentAsset3.setStructureInode(st.getInode());
		contentAsset3.setHost(demo.getIdentifier());
		contentAsset3.setProperty(FileAssetAPI.FILE_NAME_FIELD, LOGO_GIF_1);
		contentAsset3.setProperty(FileAssetAPI.BINARY_FIELD, destFile);
		contentAsset3.setLanguageId(langId);
		contentAsset3.setProperty(FileAssetAPI.TITLE_FIELD, LOGO_GIF_1);
		contentAsset3.setFolder(ftest1.getInode());
		contentAsset3=APILocator.getContentletAPI().checkin(contentAsset3, user, false);
		APILocator.getContentletAPI().publish(contentAsset3, user, false);

		/*Adding a link*/
		String linkStr="link1";
  		Link link = new Link();
		link.setTitle(linkStr);
		link.setFriendlyName(linkStr);
		link.setParent(ftest1.getInode());
		link.setTarget("_blank");
		link.setModUser(user.getUserId());
		page = APILocator.getHTMLPageAssetAPI().getPageByPath("/about-us/locations/index", demo, langId, true);

  		Identifier internalLinkIdentifier = APILocator.getIdentifierAPI().findFromInode(page.getIdentifier());
		link.setLinkType(Link.LinkType.INTERNAL.toString());
		link.setInternalLinkIdentifier(internalLinkIdentifier.getId());
		link.setProtocal("http://");

		StringBuffer myURL = new StringBuffer();
		if(InodeUtils.isSet(internalLinkIdentifier.getHostId())) {
			myURL.append(demo.getHostname());
		}
		myURL.append(internalLinkIdentifier.getURI());
		link.setUrl(myURL.toString());
		WebAssetFactory.createAsset(link, user.getUserId(), ftest1);
		APILocator.getVersionableAPI().setLive(link);
		 
		/*Adding page and file asset to folder fcopy2*/
		destFile = testFolder.newFile(LOGO_GIF_2);
		FileUtil.write(destFile, "helloworld");

		Contentlet contentAsset4=new Contentlet();
		st = StructureFactory.getStructureByVelocityVarName("FileAsset");
		contentAsset4.setStructureInode(st.getInode());
		contentAsset4.setHost(demo.getIdentifier());
		contentAsset4.setProperty(FileAssetAPI.FILE_NAME_FIELD, LOGO_GIF_2);
		contentAsset4.setProperty(FileAssetAPI.BINARY_FIELD, destFile);
		contentAsset4.setLanguageId(langId);
		contentAsset4.setProperty(FileAssetAPI.TITLE_FIELD, LOGO_GIF_2);
		contentAsset4.setFolder(ftest2.getInode());
		contentAsset4=APILocator.getContentletAPI().checkin(contentAsset4, user, false);
		APILocator.getContentletAPI().publish(contentAsset4, user, false);

		String pageStr2="page2";
		Contentlet contentAsset5=new Contentlet();
		contentAsset5.setStructureInode(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
		contentAsset5.setHost(demo.getIdentifier());
		contentAsset5.setProperty(HTMLPageAssetAPIImpl.FRIENDLY_NAME_FIELD, pageStr2);
		contentAsset5.setProperty(HTMLPageAssetAPIImpl.URL_FIELD, pageStr2);
		contentAsset5.setProperty(HTMLPageAssetAPIImpl.TITLE_FIELD, pageStr2);
		contentAsset5.setProperty(HTMLPageAssetAPIImpl.CACHE_TTL_FIELD, "0");
		contentAsset5.setProperty(HTMLPageAssetAPIImpl.TEMPLATE_FIELD, template.getIdentifier());
		contentAsset5.setLanguageId(langId);
		contentAsset5.setFolder(ftest2.getInode());
		contentAsset5=APILocator.getContentletAPI().checkin(contentAsset5, user, false);
		APILocator.getContentletAPI().publish(contentAsset5, user, false);
		
		/*Adding page and menu link to folder fcopy3*/
		String linkStr2="link2";
  		Link link2 = new Link();
		link2.setTitle(linkStr2);
		link2.setFriendlyName(linkStr2);
		link2.setParent(ftest3.getInode());
		link2.setTarget("_blank");
		link2.setModUser(user.getUserId());
		page = APILocator.getHTMLPageAssetAPI().getPageByPath("/about-us/locations/index", demo, langId, true);

  		internalLinkIdentifier = APILocator.getIdentifierAPI().findFromInode(page.getIdentifier());
		link2.setLinkType(Link.LinkType.INTERNAL.toString());
		link2.setInternalLinkIdentifier(internalLinkIdentifier.getId());
		link2.setProtocal("http://");

		myURL = new StringBuffer();
		if(InodeUtils.isSet(internalLinkIdentifier.getHostId())) {
			myURL.append(demo.getHostname());
		}
		myURL.append(internalLinkIdentifier.getURI());
		link2.setUrl(myURL.toString());
		WebAssetFactory.createAsset(link2, user.getUserId(), ftest3);
		APILocator.getVersionableAPI().setLive(link2);

		String pageStr3="page3";
		Contentlet contentAsset6=new Contentlet();
		contentAsset6.setStructureInode(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
		contentAsset6.setHost(demo.getIdentifier());
		contentAsset6.setProperty(HTMLPageAssetAPIImpl.FRIENDLY_NAME_FIELD, pageStr3);
		contentAsset6.setProperty(HTMLPageAssetAPIImpl.URL_FIELD, pageStr3);
		contentAsset6.setProperty(HTMLPageAssetAPIImpl.TITLE_FIELD, pageStr3);
		contentAsset6.setProperty(HTMLPageAssetAPIImpl.CACHE_TTL_FIELD, "0");
		contentAsset6.setProperty(HTMLPageAssetAPIImpl.TEMPLATE_FIELD, template.getIdentifier());
		contentAsset6.setLanguageId(langId);
		contentAsset6.setFolder(ftest3.getInode());
		contentAsset6=APILocator.getContentletAPI().checkin(contentAsset6, user, false);
		APILocator.getContentletAPI().publish(contentAsset6, user, false);
		
		String title3="test3";
		Contentlet contentAsset7=new Contentlet();
		st = StructureFactory.getStructureByVelocityVarName("webPageContent");
		contentAsset7.setStructureInode(st.getInode());
		contentAsset7.setHost(demo.getIdentifier());
		contentAsset7.setProperty("title", title3);
		contentAsset7.setLanguageId(langId);
		contentAsset7.setProperty("body", title3);
		contentAsset7.setFolder(ftest1.getInode());
		contentAsset7=APILocator.getContentletAPI().checkin(contentAsset7, user, false);
		APILocator.getContentletAPI().publish(contentAsset7, user, false);
		
		/*Relate content to page*/
		MultiTree m2 = new MultiTree(contentAsset6.getIdentifier(), container.getIdentifier(), contentAsset7.getIdentifier());
		MultiTreeFactory.saveMultiTree(m2);
		
		/*Copy folder*/
		Thread.sleep(2000);
		Folder fcopydest = APILocator.getFolderAPI().createFolders("/folderCopyDestinationTest"+System.currentTimeMillis(), demo, user, false);
		APILocator.getFolderAPI().copy(ftest1, fcopydest, user, false);
		
		/*validate that the folderfcopy1 exist*/
		Folder  newftest1 = APILocator.getFolderAPI().findFolderByPath(fcopydest.getPath()+ftest1.getName(), demo, user, false);
		Assert.assertTrue("Folder ("+ftest1.getName()+") wasn't moved", newftest1 != null);

		/*validate that folder page, page content, file asset, link and sub folders were copied*/
		page = APILocator.getHTMLPageAssetAPI().getPageByPath(newftest1.getPath()+page1Str, demo, langId, false);
		Assert.assertTrue(page != null && page.getTitle().contains(pageStr));

		List<MultiTree> mt= MultiTreeFactory.getMultiTree(page.getIdentifier());
		Assert.assertTrue(mt.size() ==1 && mt.get(0).getParent2().equals(container.getIdentifier()) && mt.get(0).getChild().equals(contentAsset2.getIdentifier()) );
		Thread.sleep(3000);
		List<FileAsset> files = APILocator.getFileAssetAPI().findFileAssetsByFolder(newftest1, user, false);
		Assert.assertTrue(files.size()==1 && files.get(0).getTitle().equals(LOGO_GIF_1));
		
		List<Link> links = APILocator.getMenuLinkAPI().findFolderMenuLinks(newftest1);
		Assert.assertTrue(links.size()==1 && links.get(0).getTitle().equals(linkStr));

		Folder  newftest2 = APILocator.getFolderAPI().findFolderByPath(newftest1.getPath()+ftest2.getName(), demo, user, false);
		Assert.assertTrue("Folder ("+ftest2.getName()+") wasn't moved", newftest2 != null);
		
		/*validate page and file asset in folder fcopy2*/
		files = APILocator.getFileAssetAPI().findFileAssetsByFolder(newftest2, user, false);
		Assert.assertTrue(files.size()==1 && files.get(0).getTitle().equals(LOGO_GIF_2));
		
		page = APILocator.getHTMLPageAssetAPI().getPageByPath(newftest2.getPath()+pageStr2, demo, langId, false);
		Assert.assertTrue(page != null && page.getTitle().contains(pageStr2));		
		
		Folder  newftest3 = APILocator.getFolderAPI().findFolderByPath(newftest1.getPath()+ftest3.getName(), demo, user, false);
		Assert.assertTrue("Folder ("+ftest3.getName()+") wasn't moved", newftest3 != null);
		
		/*validate page and menu link in folder fcopy3*/
		links = APILocator.getMenuLinkAPI().findFolderMenuLinks(newftest3);
		Assert.assertTrue(links.size()==1 && links.get(0).getTitle().equals(linkStr2));
		
		page = APILocator.getHTMLPageAssetAPI().getPageByPath(newftest3.getPath()+pageStr3, demo, langId, false);
		Assert.assertTrue(page != null && page.getTitle().contains(pageStr3));
		
		mt= MultiTreeFactory.getMultiTree(page.getIdentifier());
		Assert.assertTrue(mt.size() ==1 && mt.get(0).getParent2().equals(container.getIdentifier()) && mt.get(0).getChild().equals(contentAsset7.getIdentifier()) );
		
	}

	/**
	 * Test delete folders with multilingua pages 
	 * @throws Exception
	 */
	@Test
	public void delete() throws Exception {
		User user = APILocator.getUserAPI().getSystemUser();
		Host demo = APILocator.getHostAPI().findByName("demo.dotcms.com", user, false);
		long langIdUS = APILocator.getLanguageAPI().getDefaultLanguage().getId();
		long langIdES= APILocator.getLanguageAPI().getLanguage("es", "ES").getId();
		
		String folderPath = "/folderDeleteSourceTest"+System.currentTimeMillis();
		Folder ftest = APILocator.getFolderAPI().createFolders(folderPath, demo, user, false);
		//adding page
		String pageStr ="mypage";
		List<Template> templates = APILocator.getTemplateAPI().findTemplatesAssignedTo(demo);
		Template template =null;
		for(Template temp: templates){
			if(temp.getTitle().equals("Quest - 1 Column")){
				template=temp;
				break;
			}
		}
		/*create page content multilingual */
		Contentlet contentAsset=new Contentlet();
		contentAsset.setStructureInode(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
		contentAsset.setHost(demo.getIdentifier());
		contentAsset.setProperty(HTMLPageAssetAPIImpl.FRIENDLY_NAME_FIELD, pageStr);
		contentAsset.setProperty(HTMLPageAssetAPIImpl.URL_FIELD, pageStr);
		contentAsset.setProperty(HTMLPageAssetAPIImpl.TITLE_FIELD, pageStr);
		contentAsset.setProperty(HTMLPageAssetAPIImpl.CACHE_TTL_FIELD, "0");
		contentAsset.setProperty(HTMLPageAssetAPIImpl.TEMPLATE_FIELD, template.getIdentifier());
		contentAsset.setLanguageId(langIdUS);
		contentAsset.setFolder(ftest.getInode());
		contentAsset=APILocator.getContentletAPI().checkin(contentAsset, user, false);
		APILocator.getContentletAPI().publish(contentAsset, user, false);
		
		contentAsset.setLanguageId(langIdES);
		contentAsset.setInode(null);
		contentAsset=APILocator.getContentletAPI().checkin(contentAsset, user, false);
		APILocator.getContentletAPI().publish(contentAsset, user, false);
		
		APILocator.getFolderAPI().delete(ftest, user, false);
		
		/*validate that the folder and pages were deleted*/
		Folder  folder = APILocator.getFolderAPI().findFolderByPath(folderPath, demo, user, false);
		Assert.assertTrue(folder.getInode() == null);
	
		IHTMLPage page = APILocator.getHTMLPageAssetAPI().getPageByPath(folderPath+"/"+pageStr, demo, langIdUS, false);
		Assert.assertTrue(page == null);
		
		page = APILocator.getHTMLPageAssetAPI().getPageByPath(folderPath+"/"+pageStr, demo, langIdES, false);
		Assert.assertTrue(page == null);
	}

}
