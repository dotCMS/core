package com.dotmarketing.portlets.folders.business;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.*;
import com.dotmarketing.cache.FolderCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPIImpl;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.links.business.MenuLinkAPI;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FolderAPITest {//24 contentlets

	private final static String LOGO_GIF_1 = "logo.gif";
	private final static String LOGO_GIF_2 = "logo2.gif";
	private static IdentifierAPI identifierAPI;
	private static UserAPI userAPI;
	private static HostAPI hostAPI;
	private static FolderAPI folderAPI;
	private static LanguageAPI languageAPI;
	private static ContentletAPI contentletAPI;
	private static VersionableAPI versionableAPI;
	private static ContainerAPI containerAPI;
	private static MenuLinkAPI menuLinkAPI;
	private static HTMLPageAssetAPI htmlPageAssetAPI;
	private static FileAssetAPI fileAssetAPI;
	private static TemplateAPI templateAPI;
	private static ContentTypeAPI contentTypeAPI;
	private static FolderCache fc;
	private static User user;
	private static Host host;
	private static long langId;

	@BeforeClass
    public static void prepare () throws Exception {
    	
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
		identifierAPI    = APILocator.getIdentifierAPI();
		userAPI          = APILocator.getUserAPI();
		hostAPI          = APILocator.getHostAPI();
		folderAPI        = APILocator.getFolderAPI();
		languageAPI      = APILocator.getLanguageAPI();
		contentletAPI    = APILocator.getContentletAPI();
		htmlPageAssetAPI = APILocator.getHTMLPageAssetAPI();
		versionableAPI   = APILocator.getVersionableAPI();
		containerAPI     = APILocator.getContainerAPI();
		menuLinkAPI      = APILocator.getMenuLinkAPI();
		htmlPageAssetAPI = APILocator.getHTMLPageAssetAPI();
		fileAssetAPI     = APILocator.getFileAssetAPI();
		templateAPI      = APILocator.getTemplateAPI();

		user   = userAPI.getSystemUser();
		host   = hostAPI.findByName("demo.dotcms.com", user, false);
		langId = languageAPI.getDefaultLanguage().getId();
		fc     = CacheLocator.getFolderCache();

		contentTypeAPI   = APILocator.getContentTypeAPI(user);

    }

	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();



	@Test
	public void renameFolder() throws Exception {

		final Folder ftest = folderAPI
				.createFolders("/folderTest"+System.currentTimeMillis(), host, user, false);
		final Folder ftest1 = folderAPI
				.createFolders(ftest.getPath()+"/ff1", host, user, false);
		final Folder ftest2 = folderAPI
				.createFolders(ftest.getPath()+"/ff1/ff2", host, user, false);
		final Folder ftest3 = folderAPI
				.createFolders(ftest.getPath()+"/ff1/ff2/ff3", host, user, false);

		Assert.assertTrue(folderAPI
				.renameFolder(ftest, "folderTestXX"+System.currentTimeMillis(), user, false));

		// make sure the rename is properly propagated on children (that's done in a db trigger)

        final Identifier ident  = identifierAPI.loadFromDb(ftest.getVersionId());
        final Identifier ident1 = identifierAPI.loadFromDb(ftest1.getVersionId());
        final Identifier ident2 = identifierAPI.loadFromDb(ftest2.getVersionId());
        final Identifier ident3 = identifierAPI.loadFromDb(ftest3.getVersionId());

		Assert.assertTrue(ident.getAssetName().startsWith("foldertestxx"));//After 4.1 the asset_name is saved lowercase
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

		
		//create folders and assets
		Folder ftest = folderAPI
				.createFolders("/folderMoveSourceTest"+System.currentTimeMillis(), host, user, false);
		Folder ftest1 = folderAPI
				.createFolders(ftest.getPath()+"/ff1", host, user, false);

		//adding page
		final String page0Str ="page0";
		final List<Template> templates = templateAPI.findTemplatesAssignedTo(host);
		Template template =null;
		for(final Template temp: templates){
			if(temp.getTitle().equals("Quest - 1 Column")){
				template=temp;
				break;
			}
		}
		Contentlet contentAsset1=new Contentlet();
		contentAsset1.setStructureInode(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
		contentAsset1.setHost(host.getIdentifier());
		contentAsset1.setProperty(HTMLPageAssetAPIImpl.FRIENDLY_NAME_FIELD, page0Str);
		contentAsset1.setProperty(HTMLPageAssetAPIImpl.URL_FIELD, page0Str);
		contentAsset1.setProperty(HTMLPageAssetAPIImpl.TITLE_FIELD, page0Str);
		contentAsset1.setProperty(HTMLPageAssetAPIImpl.CACHE_TTL_FIELD, "0");
		contentAsset1.setProperty(HTMLPageAssetAPIImpl.TEMPLATE_FIELD, template.getIdentifier());
		contentAsset1.setLanguageId(langId);
		contentAsset1.setFolder(ftest1.getInode());
		contentAsset1 = contentletAPI.checkin(contentAsset1, user, false);
		contentletAPI.publish(contentAsset1, user, false);
		
		/*adding menu link*/
		final String linkStr="link";
  		final Link link = new Link();
		link.setTitle(linkStr);
		link.setFriendlyName(linkStr);
		link.setParent(ftest1.getInode());
		link.setTarget("_blank");
		link.setModUser(user.getUserId());
		IHTMLPage page = htmlPageAssetAPI
				.getPageByPath("/about-us/locations/index", host, langId, true);

  		Identifier internalLinkIdentifier = identifierAPI.find(page.getIdentifier());
		link.setLinkType(Link.LinkType.INTERNAL.toString());
		link.setInternalLinkIdentifier(internalLinkIdentifier.getId());
		link.setProtocal("http://");

		StringBuffer myURL = new StringBuffer();
		if(InodeUtils.isSet(internalLinkIdentifier.getHostId())) {
			myURL.append(host.getHostname());
		}
		myURL.append(internalLinkIdentifier.getURI());
		link.setUrl(myURL.toString());
		WebAssetFactory.createAsset(link, user.getUserId(), ftest1);
		versionableAPI.setLive(link);

		Folder ftest2 = folderAPI
				.createFolders(ftest.getPath()+"/ff1/ff2", host, user, false);

		//adding page
		final String page1Str="page1";
		Contentlet contentAsset2=new Contentlet();
		contentAsset2.setStructureInode(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
		contentAsset2.setHost(host.getIdentifier());
		contentAsset2.setProperty(HTMLPageAssetAPIImpl.FRIENDLY_NAME_FIELD, page1Str);
		contentAsset2.setProperty(HTMLPageAssetAPIImpl.URL_FIELD, page1Str);
		contentAsset2.setProperty(HTMLPageAssetAPIImpl.TITLE_FIELD, page1Str);
		contentAsset2.setProperty(HTMLPageAssetAPIImpl.CACHE_TTL_FIELD, "0");
		contentAsset2.setProperty(HTMLPageAssetAPIImpl.TEMPLATE_FIELD, template.getIdentifier());
		contentAsset2.setLanguageId(langId);
		contentAsset2.setFolder(ftest2.getInode());
		contentAsset2 = contentletAPI.checkin(contentAsset2, user, false);
		contentletAPI.publish(contentAsset2, user, false);

		/*Adding menu link*/
		final String linkStr2="link2";
  		final Link link2 = new Link();
		link2.setTitle(linkStr2);
		link2.setFriendlyName(linkStr2);
		link2.setParent(ftest2.getInode());
		link2.setTarget("_blank");
		link2.setModUser(user.getUserId());
		page = htmlPageAssetAPI
				.getPageByPath("/about-us/locations/index", host, langId, true);

  		internalLinkIdentifier = identifierAPI.findFromInode(page.getIdentifier());
		link2.setLinkType(Link.LinkType.INTERNAL.toString());
		link2.setInternalLinkIdentifier(internalLinkIdentifier.getId());
		link2.setProtocal("http://");

		myURL = new StringBuffer();
		if(InodeUtils.isSet(internalLinkIdentifier.getHostId())) {
			myURL.append(host.getHostname());
		}
		myURL.append(internalLinkIdentifier.getURI());
		link2.setUrl(myURL.toString());
		WebAssetFactory.createAsset(link2, user.getUserId(), ftest2);
		versionableAPI.setLive(link2);
		
		/*Adding file asset to folder */
        final String fileTitle = "testMove.txt";
        final File destFile = testFolder.newFile(fileTitle);
		FileUtil.write(destFile, "helloworld");

		Contentlet contentAsset3=new Contentlet();
		Structure st = StructureFactory.getStructureByVelocityVarName("FileAsset");
		contentAsset3.setStructureInode(st.getInode());
		contentAsset3.setHost(host.getIdentifier());
		contentAsset3.setProperty(FileAssetAPI.FILE_NAME_FIELD, fileTitle);
		contentAsset3.setProperty(FileAssetAPI.BINARY_FIELD, destFile);
		contentAsset3.setLanguageId(langId);
		contentAsset3.setProperty(FileAssetAPI.TITLE_FIELD, fileTitle);
		contentAsset3.setFolder(ftest2.getInode());
		contentAsset3= contentletAPI.checkin(contentAsset3, user, false);
		contentletAPI.publish(contentAsset3, user, false);
				
		//adding folder
		Folder ftest3 = folderAPI
				.createFolders(ftest.getPath()+"/ff1/ff3", host, user, false);

		//adding page
		final String page2Str ="page2";
		Contentlet contentAsset4=new Contentlet();
		contentAsset4.setStructureInode(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
		contentAsset4.setHost(host.getIdentifier());
		contentAsset4.setProperty(HTMLPageAssetAPIImpl.FRIENDLY_NAME_FIELD, page2Str);
		contentAsset4.setProperty(HTMLPageAssetAPIImpl.URL_FIELD, page2Str);
		contentAsset4.setProperty(HTMLPageAssetAPIImpl.TITLE_FIELD, page2Str);
		contentAsset4.setProperty(HTMLPageAssetAPIImpl.CACHE_TTL_FIELD, "0");
		contentAsset4.setProperty(HTMLPageAssetAPIImpl.TEMPLATE_FIELD, template.getIdentifier());
		contentAsset4.setLanguageId(langId);
		contentAsset4.setFolder(ftest3.getInode());
		contentAsset4 = contentletAPI.checkin(contentAsset4, user, false);
		contentletAPI.publish(contentAsset4, user, false);
		
		/*adding page content*/
		final String title="movetest2";
		Contentlet contentAsset5=new Contentlet();
		st = StructureFactory.getStructureByVelocityVarName("webPageContent");
		contentAsset5.setStructureInode(st.getInode());
		contentAsset5.setHost(host.getIdentifier());
		contentAsset5.setProperty("title", title);
		contentAsset5.setLanguageId(langId);
		contentAsset5.setProperty("body", title);
		contentAsset5.setFolder(ftest1.getInode());
		contentAsset5= contentletAPI.checkin(contentAsset5, user, false);
		contentletAPI.publish(contentAsset5, user, false);
		Container container =null;
		final List<Container> containers = containerAPI.findContainersForStructure(st.getInode());
		for(final Container c : containers){
			if(c.getTitle().equals("Large Column (lg-1)")){
				container=c;
				break;
			}
		}
		/*Relate content to page*/
		final MultiTree m = new MultiTree(contentAsset4.getIdentifier(), container.getIdentifier(), contentAsset2.getIdentifier());
		MultiTreeFactory.saveMultiTree(m);
		

		final Folder destinationftest = folderAPI
				.createFolders("/folderMoveDestinationTest"+System.currentTimeMillis(), host, user, false);

		//moving folder and assets
		Thread.sleep(2000);
		folderAPI.move(ftest1, destinationftest, user, false);
				
		//validate that the folder and assets were moved
		final Folder  newftest1 = folderAPI
				.findFolderByPath(destinationftest.getPath()+ftest1.getName(), host, user, false);
		Assert.assertTrue("Folder ("+ftest1.getName()+") wasn't moved", newftest1 != null);

		List<Link> links = menuLinkAPI.findFolderMenuLinks(newftest1);
		Assert.assertTrue(links.size()==1 && links.get(0).getTitle().equals(linkStr));
		
		List<IHTMLPage> pages = htmlPageAssetAPI
				.getLiveHTMLPages(newftest1,user, false);
		Assert.assertTrue(pages.size() == 1 && pages.get(0).getTitle().equals(page0Str));
		
		final Folder  newftest2 = folderAPI
				.findFolderByPath(newftest1.getPath()+ftest2.getName(), host, user, false);
		Assert.assertNotNull(newftest2);
		
		pages = htmlPageAssetAPI.getLiveHTMLPages(newftest2, user, false);
		Assert.assertTrue(pages.size() == 1 && pages.get(0).getTitle().equals(page1Str));
		
		links = menuLinkAPI.findFolderMenuLinks(newftest2);
		Assert.assertTrue(links.size()==1 && links.get(0).getTitle().equals(linkStr2));

		final List<FileAsset> files = fileAssetAPI
				.findFileAssetsByFolder(newftest2, user, false);
		Assert.assertTrue(files.size()==1 && files.get(0).getTitle().equals(fileTitle));
				
		final Folder  newftest3 = folderAPI
				.findFolderByPath(newftest1.getPath()+ftest3.getName(), host, user, false);
		Assert.assertNotNull(newftest3);
		
		pages = htmlPageAssetAPI.getLiveHTMLPages(newftest3,user, false);
		Assert.assertTrue(pages.size() == 1 && pages.get(0).getTitle().equals(page2Str));		
		
		final List<MultiTree> mt= MultiTreeFactory.getMultiTrees(pages.get(0).getIdentifier());
		Assert.assertTrue(mt.size() ==1 && mt.get(0).getParent2().equals(container.getIdentifier()) && mt.get(0).getChild().equals(contentAsset2.getIdentifier()) );

		contentletAPI.archive(contentAsset1,user,false);
		contentletAPI.archive(contentAsset2,user,false);
		contentletAPI.archive(contentAsset3,user,false);
		contentletAPI.archive(contentAsset4,user,false);
		contentletAPI.archive(contentAsset5,user,false);
		contentletAPI.delete(contentAsset1,user,false);
		contentletAPI.delete(contentAsset2,user,false);
		contentletAPI.delete(contentAsset3,user,false);
		contentletAPI.delete(contentAsset4,user,false);
		contentletAPI.delete(contentAsset5,user,false);


	}

	/**
	 * Test move folders with subfolders 
	 * @throws Exception
	 */
	@SuppressWarnings("resource")
	@Test
	public void copy() throws Exception {

		/*
		 * Test 1:
		 * copy a page in the same folder
		 */
		Folder ftest = folderAPI
				.createFolders("/folderCopySourceTest"+System.currentTimeMillis(), host, user, false);
		//adding page
		final String pageStr ="mypage";
		List<Template> templates = templateAPI.findTemplatesAssignedTo(host);
		Template template =null;
		for(Template temp: templates){
			if(temp.getTitle().equals("Quest - 1 Column")){
				template=temp;
				break;
			}
		}
		Contentlet contentAsset1=new Contentlet();
		contentAsset1.setStructureInode(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
		contentAsset1.setHost(host.getIdentifier());
		contentAsset1.setProperty(HTMLPageAssetAPIImpl.FRIENDLY_NAME_FIELD, pageStr);
		contentAsset1.setProperty(HTMLPageAssetAPIImpl.URL_FIELD, pageStr);
		contentAsset1.setProperty(HTMLPageAssetAPIImpl.TITLE_FIELD, pageStr);
		contentAsset1.setProperty(HTMLPageAssetAPIImpl.CACHE_TTL_FIELD, "0");
		contentAsset1.setProperty(HTMLPageAssetAPIImpl.TEMPLATE_FIELD, template.getIdentifier());
		contentAsset1.setLanguageId(langId);
		contentAsset1.setFolder(ftest.getInode());
		contentAsset1 = contentletAPI.checkin(contentAsset1, user, false);
		contentletAPI.publish(contentAsset1, user, false);

		//copy page in same folder
		Contentlet contentAsset1Copy = contentletAPI.copyContentlet(contentAsset1, ftest, user, false);
		
		IHTMLPage page = htmlPageAssetAPI
				.getPageByPath(ftest.getPath()+pageStr, host, languageAPI
				.getDefaultLanguage().getId(), false);
		Assert.assertTrue(page != null && page.getTitle().contains(pageStr));
		page = htmlPageAssetAPI
				.getPageByPath(ftest.getPath()+pageStr+"_COPY", host, langId, false);
		Assert.assertTrue(page != null && page.getTitle().contains(pageStr));

		/*
		 * Test 2
		 * Copy folder (located at the second level) with a page, menu link, image asset and empty folder
		 */
		Folder ftest1 = folderAPI
				.createFolders(ftest.getPath()+"/fcopy1", host, user, false);
		Folder ftest2 = folderAPI
				.createFolders(ftest1.getPath()+"/fcopy2", host, user, false);
		Folder ftest3 = folderAPI
				.createFolders(ftest1.getPath()+"/fcopy3", host, user, false);

		/*adding page with content*/
		final String page1Str="mypage1";
		Contentlet contentAsset2=new Contentlet();
		contentAsset2.setStructureInode(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
		contentAsset2.setHost(host.getIdentifier());
		contentAsset2.setProperty(HTMLPageAssetAPIImpl.FRIENDLY_NAME_FIELD, page1Str);
		contentAsset2.setProperty(HTMLPageAssetAPIImpl.URL_FIELD, page1Str);
		contentAsset2.setProperty(HTMLPageAssetAPIImpl.TITLE_FIELD, page1Str);
		contentAsset2.setProperty(HTMLPageAssetAPIImpl.CACHE_TTL_FIELD, "0");
		contentAsset2.setProperty(HTMLPageAssetAPIImpl.TEMPLATE_FIELD, template.getIdentifier());
		contentAsset2.setFolder(ftest1.getInode());
		contentAsset2 = contentletAPI.checkin(contentAsset2, user, false);
		contentletAPI.publish(contentAsset2, user, false);

		/*Adding content*/
		final String title="test1";
		Contentlet contentAsset3=new Contentlet();
		Structure st = StructureFactory.getStructureByVelocityVarName("webPageContent");
		contentAsset3.setStructureInode(st.getInode());
		contentAsset3.setHost(host.getIdentifier());
		contentAsset3.setProperty("title", title);
		contentAsset3.setLanguageId(langId);
		contentAsset3.setProperty("body", title);
		contentAsset3.setFolder(ftest1.getInode());
		contentAsset3= contentletAPI.checkin(contentAsset3, user, false);
		contentletAPI.publish(contentAsset3, user, false);
		Container container =null;
		List<Container> containers = containerAPI.findContainersForStructure(st.getInode());
		for(Container c : containers){
			if(c.getTitle().equals("Large Column (lg-1)")){
				container=c;
				break;
			}
		}
		/*Relate content to page*/
		MultiTree m = new MultiTree(contentAsset2.getIdentifier(), container.getIdentifier(), contentAsset3.getIdentifier());
		MultiTreeFactory.saveMultiTree(m);

		/*Adding file asset to folder fcopy1*/
		File destFile = testFolder.newFile(LOGO_GIF_1);
		FileUtil.write(destFile, "helloworld");

		Contentlet contentAsset4=new Contentlet();
		st = StructureFactory.getStructureByVelocityVarName("FileAsset");
		contentAsset4.setStructureInode(st.getInode());
		contentAsset4.setHost(host.getIdentifier());
		contentAsset4.setProperty(FileAssetAPI.FILE_NAME_FIELD, LOGO_GIF_1);
		contentAsset4.setProperty(FileAssetAPI.BINARY_FIELD, destFile);
		contentAsset4.setLanguageId(langId);
		contentAsset4.setProperty(FileAssetAPI.TITLE_FIELD, LOGO_GIF_1);
		contentAsset4.setFolder(ftest1.getInode());
		contentAsset4 = contentletAPI.checkin(contentAsset4, user, false);
		contentletAPI.publish(contentAsset4, user, false);

		/*Adding a link*/
		final String linkStr="link1";
  		Link link = new Link();
		link.setTitle(linkStr);
		link.setFriendlyName(linkStr);
		link.setParent(ftest1.getInode());
		link.setTarget("_blank");
		link.setModUser(user.getUserId());
		page = htmlPageAssetAPI
				.getPageByPath("/about-us/locations/index", host, langId, true);

  		Identifier internalLinkIdentifier = identifierAPI.findFromInode(page.getIdentifier());
		link.setLinkType(Link.LinkType.INTERNAL.toString());
		link.setInternalLinkIdentifier(internalLinkIdentifier.getId());
		link.setProtocal("http://");

		StringBuffer myURL = new StringBuffer();
		if(InodeUtils.isSet(internalLinkIdentifier.getHostId())) {
			myURL.append(host.getHostname());
		}
		myURL.append(internalLinkIdentifier.getURI());
		link.setUrl(myURL.toString());
		WebAssetFactory.createAsset(link, user.getUserId(), ftest1);
		versionableAPI.setLive(link);
		 
		/*Adding page and file asset to folder fcopy2*/
		destFile = testFolder.newFile(LOGO_GIF_2);
		FileUtil.write(destFile, "helloworld");

		Contentlet contentAsset5=new Contentlet();
		st = StructureFactory.getStructureByVelocityVarName("FileAsset");
		contentAsset5.setStructureInode(st.getInode());
		contentAsset5.setHost(host.getIdentifier());
		contentAsset5.setProperty(FileAssetAPI.FILE_NAME_FIELD, LOGO_GIF_2);
		contentAsset5.setProperty(FileAssetAPI.BINARY_FIELD, destFile);
		contentAsset5.setLanguageId(langId);
		contentAsset5.setProperty(FileAssetAPI.TITLE_FIELD, LOGO_GIF_2);
		contentAsset5.setFolder(ftest2.getInode());
		contentAsset5= contentletAPI.checkin(contentAsset5, user, false);
		contentletAPI.publish(contentAsset5 , user, false);

		final String pageStr2="page2";
		Contentlet contentAsset6=new Contentlet();
		contentAsset6.setStructureInode(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
		contentAsset6.setHost(host.getIdentifier());
		contentAsset6.setProperty(HTMLPageAssetAPIImpl.FRIENDLY_NAME_FIELD, pageStr2);
		contentAsset6.setProperty(HTMLPageAssetAPIImpl.URL_FIELD, pageStr2);
		contentAsset6.setProperty(HTMLPageAssetAPIImpl.TITLE_FIELD, pageStr2);
		contentAsset6.setProperty(HTMLPageAssetAPIImpl.CACHE_TTL_FIELD, "0");
		contentAsset6.setProperty(HTMLPageAssetAPIImpl.TEMPLATE_FIELD, template.getIdentifier());
		contentAsset6.setLanguageId(langId);
		contentAsset6.setFolder(ftest2.getInode());
		contentAsset6 = contentletAPI.checkin(contentAsset6, user, false);
		contentletAPI.publish(contentAsset6, user, false);
		
		/*Adding page and menu link to folder fcopy3*/
		String linkStr2="link2";
  		Link link2 = new Link();
		link2.setTitle(linkStr2);
		link2.setFriendlyName(linkStr2);
		link2.setParent(ftest3.getInode());
		link2.setTarget("_blank");
		link2.setModUser(user.getUserId());
		page = htmlPageAssetAPI
				.getPageByPath("/about-us/locations/index", host, langId, true);

  		internalLinkIdentifier = identifierAPI.findFromInode(page.getIdentifier());
		link2.setLinkType(Link.LinkType.INTERNAL.toString());
		link2.setInternalLinkIdentifier(internalLinkIdentifier.getId());
		link2.setProtocal("http://");

		myURL = new StringBuffer();
		if(InodeUtils.isSet(internalLinkIdentifier.getHostId())) {
			myURL.append(host.getHostname());
		}
		myURL.append(internalLinkIdentifier.getURI());
		link2.setUrl(myURL.toString());
		WebAssetFactory.createAsset(link2, user.getUserId(), ftest3);
		versionableAPI.setLive(link2);

		final String pageStr3="page3";
		Contentlet contentAsset7=new Contentlet();
		contentAsset7.setStructureInode(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
		contentAsset7.setHost(host.getIdentifier());
		contentAsset7.setProperty(HTMLPageAssetAPIImpl.FRIENDLY_NAME_FIELD, pageStr3);
		contentAsset7.setProperty(HTMLPageAssetAPIImpl.URL_FIELD, pageStr3);
		contentAsset7.setProperty(HTMLPageAssetAPIImpl.TITLE_FIELD, pageStr3);
		contentAsset7.setProperty(HTMLPageAssetAPIImpl.CACHE_TTL_FIELD, "0");
		contentAsset7.setProperty(HTMLPageAssetAPIImpl.TEMPLATE_FIELD, template.getIdentifier());
		contentAsset7.setLanguageId(langId);
		contentAsset7.setFolder(ftest3.getInode());
		contentAsset7 = contentletAPI.checkin(contentAsset7, user, false);
		contentletAPI.publish(contentAsset7, user, false);
		
		final String title3="test3";
		Contentlet contentAsset8=new Contentlet();
		st = StructureFactory.getStructureByVelocityVarName("webPageContent");
		contentAsset8.setStructureInode(st.getInode());
		contentAsset8.setHost(host.getIdentifier());
		contentAsset8.setProperty("title", title3);
		contentAsset8.setLanguageId(langId);
		contentAsset8.setProperty("body", title3);
		contentAsset8.setFolder(ftest1.getInode());
		contentAsset8 = contentletAPI.checkin(contentAsset8, user, false);
		contentletAPI.publish(contentAsset8, user, false);
		
		/*Relate content to page*/
		final MultiTree m2 = new MultiTree(contentAsset7.getIdentifier(), container.getIdentifier(), contentAsset8.getIdentifier());
		MultiTreeFactory.saveMultiTree(m2);
		
		/*Copy folder*/
		Thread.sleep(2000);
		final Folder fcopydest = folderAPI
				.createFolders("/folderCopyDestinationTest"+System.currentTimeMillis(), host, user, false);
		folderAPI.copy(ftest1, fcopydest, user, false);
		
		/*validate that the folderfcopy1 exist*/
		Folder  newftest1 = folderAPI
				.findFolderByPath(fcopydest.getPath()+ftest1.getName(), host, user, false);
		Assert.assertTrue("Folder ("+ftest1.getName()+") wasn't moved", newftest1 != null);

		/*validate that folder page, page content, file asset, link and sub folders were copied*/
		page = htmlPageAssetAPI
				.getPageByPath(newftest1.getPath()+page1Str, host, langId, false);
		Assert.assertTrue(page != null && page.getTitle().contains(pageStr));

		List<MultiTree> mt= MultiTreeFactory.getMultiTrees(page.getIdentifier());
		Assert.assertTrue(mt.size() ==1 && mt.get(0).getParent2().equals(container.getIdentifier()) && mt.get(0).getChild().equals(contentAsset3.getIdentifier()) );
		Thread.sleep(3000);
		List<FileAsset> files = fileAssetAPI
				.findFileAssetsByFolder(newftest1, user, false);
		Assert.assertTrue(files.size()==1 && files.get(0).getTitle().equals(LOGO_GIF_1));
		
		List<Link> links = menuLinkAPI.findFolderMenuLinks(newftest1);
		Assert.assertTrue(links.size()==1 && links.get(0).getTitle().equals(linkStr));

		Folder  newftest2 = folderAPI
				.findFolderByPath(newftest1.getPath()+ftest2.getName(), host, user, false);
		Assert.assertTrue("Folder ("+ftest2.getName()+") wasn't moved", newftest2 != null);
		
		/*validate page and file asset in folder fcopy2*/
		files = fileAssetAPI.findFileAssetsByFolder(newftest2, user, false);
		Assert.assertTrue(files.size()==1 && files.get(0).getTitle().equals(LOGO_GIF_2));
		
		page = htmlPageAssetAPI
				.getPageByPath(newftest2.getPath()+pageStr2, host, langId, false);
		Assert.assertTrue(page != null && page.getTitle().contains(pageStr2));		
		
		Folder  newftest3 = folderAPI
				.findFolderByPath(newftest1.getPath()+ftest3.getName(), host, user, false);
		Assert.assertTrue("Folder ("+ftest3.getName()+") wasn't moved", newftest3 != null);
		
		/*validate page and menu link in folder fcopy3*/
		links = menuLinkAPI.findFolderMenuLinks(newftest3);
		Assert.assertTrue(links.size()==1 && links.get(0).getTitle().equals(linkStr2));
		
		page = htmlPageAssetAPI
				.getPageByPath(newftest3.getPath()+pageStr3, host, langId, false);
		Assert.assertTrue(page != null && page.getTitle().contains(pageStr3));
		
		mt= MultiTreeFactory.getMultiTrees(page.getIdentifier());
		Assert.assertTrue(mt.size() ==1 && mt.get(0).getParent2().equals(container.getIdentifier()) && mt.get(0).getChild().equals(contentAsset8.getIdentifier()) );

		for(Contentlet contentlet : folderAPI.getLiveContent(newftest1,user,false)){
			contentletAPI.archive(contentlet,user,false);
			contentletAPI.delete(contentlet,user,false);
		}

		for(Contentlet contentlet : folderAPI.getLiveContent(newftest2,user,false)){
			contentletAPI.archive(contentlet,user,false);
			contentletAPI.delete(contentlet,user,false);
		}

		for(Contentlet contentlet : folderAPI.getLiveContent(newftest3,user,false)){
			contentletAPI.archive(contentlet,user,false);
			contentletAPI.delete(contentlet,user,false);
		}

		contentletAPI.archive(contentAsset1,user,false);
		contentletAPI.archive(contentAsset2,user,false);
		contentletAPI.archive(contentAsset3,user,false);
		contentletAPI.archive(contentAsset4,user,false);
		contentletAPI.archive(contentAsset5,user,false);
		contentletAPI.archive(contentAsset6,user,false);
		contentletAPI.archive(contentAsset7,user,false);
		contentletAPI.archive(contentAsset8,user,false);
		contentletAPI.archive(contentAsset1Copy,user,false);


		contentletAPI.delete(contentAsset1,user,false);
		contentletAPI.delete(contentAsset2,user,false);
		contentletAPI.delete(contentAsset3,user,false);
		contentletAPI.delete(contentAsset4,user,false);
		contentletAPI.delete(contentAsset5,user,false);
		contentletAPI.delete(contentAsset6,user,false);
		contentletAPI.delete(contentAsset7,user,false);
		contentletAPI.delete(contentAsset8,user,false);
		contentletAPI.delete(contentAsset1Copy,user,false);
		
	}

	/**
	 * Test delete folders with multilingua pages 
	 * @throws Exception
	 */
	@Test
	public void delete() throws Exception {
		final long langIdES= languageAPI.getLanguage("es", "ES").getId();

		final String folderPath = "/folderDeleteSourceTest"+System.currentTimeMillis();
		final Folder ftest = folderAPI.createFolders(folderPath, host, user, false);

		final String folderIdentifier = ftest.getIdentifier();

		//adding page
		String pageStr ="mypage";
		List<Template> templates = templateAPI.findTemplatesAssignedTo(host);
		Template template =null;
		for(Template temp: templates){
			if(temp.getTitle().equals("Quest - 1 Column")){
				template=temp;
				break;
			}
		}
		/*create page content multilingual */
		Contentlet contentAsset1=new Contentlet();
		contentAsset1.setStructureInode(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
		contentAsset1.setHost(host.getIdentifier());
		contentAsset1.setProperty(HTMLPageAssetAPIImpl.FRIENDLY_NAME_FIELD, pageStr);
		contentAsset1.setProperty(HTMLPageAssetAPIImpl.URL_FIELD, pageStr);
		contentAsset1.setProperty(HTMLPageAssetAPIImpl.TITLE_FIELD, pageStr);
		contentAsset1.setProperty(HTMLPageAssetAPIImpl.CACHE_TTL_FIELD, "0");
		contentAsset1.setProperty(HTMLPageAssetAPIImpl.TEMPLATE_FIELD, template.getIdentifier());
		contentAsset1.setLanguageId(langId);
		contentAsset1.setFolder(ftest.getInode());
		contentAsset1 = contentletAPI.checkin(contentAsset1, user, false);
		contentletAPI.publish(contentAsset1, user, false);

		contentAsset1.setLanguageId(langIdES);
		contentAsset1.setInode(null);
		contentAsset1.setIndexPolicy(IndexPolicy.FORCE);
		contentAsset1 = contentletAPI.checkin(contentAsset1, user, false);
		contentletAPI.publish(contentAsset1, user, false);
		
		folderAPI.delete(ftest, user, false);

		/*validate that the folder and pages were deleted*/
		final Folder  folder = folderAPI.findFolderByPath(folderPath, host, user, false);
		Assert.assertTrue(folder.getInode() == null);
	
		IHTMLPage page = htmlPageAssetAPI
				.getPageByPath(folderPath+"/"+pageStr, host, langId, false);
		Assert.assertTrue(page == null);
		
		page = htmlPageAssetAPI
				.getPageByPath(folderPath+"/"+pageStr, host, langIdES, false);
		Assert.assertTrue(page == null);

		final Identifier dbFolderIdentifier = identifierAPI.find(folderIdentifier);
		Assert.assertTrue(dbFolderIdentifier == null || !UtilMethods.isSet(dbFolderIdentifier.getId()));
	}

	@Test
	public void deleteFolderWithContentTypeInIt() throws DotDataException, DotSecurityException {
		final String folderPath = "/folder_with_content"+System.currentTimeMillis();
		ContentType contentType = null;
		Folder folder = null;
		try {
			folder = folderAPI.createFolders(folderPath, host, user, false);

			contentType = ContentTypeBuilder
					.builder(BaseContentType.getContentTypeClass(BaseContentType.CONTENT.ordinal()))
					.description("Content Type For Folder").folder(folder.getInode())
					.host(folder.getHostId()).name("ContentTypeForFolder")
					.owner(APILocator.systemUser().toString()).build();
			contentType = contentTypeAPI.save(contentType);

			folderAPI.delete(folder, user, false);

			folder = folderAPI.findFolderByPath(folderPath, host, user, false);
			Assert.assertTrue(folder.getInode() == null);

			contentType = contentTypeAPI.find(contentType.inode());
			Assert.assertTrue(contentType != null);
			Assert.assertTrue(contentType.host().equals(host.getIdentifier()));
			Assert.assertTrue(contentType.folder().equals(FolderAPI.SYSTEM_FOLDER));
		}finally{
			if (contentType != null){
				contentTypeAPI.delete(contentType);
			}

			if (folder != null && folder.getInode() != null){
				folderAPI.delete(folder, user, false);
			}
		}
	}

	@Test
	public void testFindSubFolders() throws DotDataException, DotSecurityException {
		String folderPath1, folderPath2;
		Folder folder1 , folder2;

		folder1     = null;
		folder2     = null;
		folderPath1 = "/folder1"+System.currentTimeMillis();
		folderPath2 = folderPath1 + "/folder2";

		try{
			folder1 = folderAPI.createFolders(folderPath1, host, user, false);
			folder2 = folderAPI.createFolders(folderPath2, host, user, false);

			folder2.setOwner("folder2's owner");

			folderAPI.save(folder2, user, false);
			final List<Folder> folders = folderAPI.findSubFolders(folder1, false);

			Assert.assertNotNull(folders);
			Assert.assertTrue(folders.size() == 1);

			Folder result = folders.get(0);
			Assert.assertEquals(folder2.getInode(), result.getInode());
			Assert.assertTrue(
					result.getOwner() != null && result.getOwner().equals(folder2.getOwner()));
		}finally{
			if (folder2 != null){
				folderAPI.delete(folder2, user, false);
			}

			if (folder1 != null){
				folderAPI.delete(folder1, user, false);
			}
		}
	}

	@Test
	public void testFindSubFoldersByHost() throws DotDataException, DotSecurityException {
		final List<Folder> folders = folderAPI.findSubFolders(host, false);

		Assert.assertNotNull(folders);
		Assert.assertFalse(folders.isEmpty());
	}

	@Test
	public void testFindFoldersByHost() throws DotDataException, DotSecurityException {
		final List<Folder> folders = folderAPI.findFoldersByHost(host, user, false);

		Assert.assertNotNull(folders);
		Assert.assertFalse(folders.isEmpty());
	}

	@Test
	public void testFindThemes() throws DotDataException, DotSecurityException {
		final List<Folder> folders = folderAPI.findThemes(host, user, false);

		Assert.assertNotNull(folders);
		Assert.assertFalse(folders.isEmpty());
	}

	@Test
	public void testFindFolderByPath() throws DotDataException, DotSecurityException {
		String folderPath;
		Folder folder;

		folder     = null;
		folderPath = "/folder"+System.currentTimeMillis();

		try{
			folder = folderAPI.createFolders(folderPath, host, user, false);
			folder.setOwner("folder's owner");

			folderAPI.save(folder, user, false);

			fc.removeFolder(folder, identifierAPI.find(folder));
			Folder result = folderAPI.findFolderByPath(folderPath, host, user,false);

			Assert.assertNotNull(result);
			Assert.assertEquals(folder.getInode(), result.getInode());

			Assert.assertTrue(
					result.getOwner() != null && result.getOwner().equals(folder.getOwner()));
		}finally{

			if (folder != null){
				folderAPI.delete(folder, user, false);
			}
		}
	}

	@Test
	public void testFindSubFoldersTitleSort() throws DotDataException, DotSecurityException {
		String folderPath1, folderPath2, folderPath3;
		Folder folder1 , folder2, folder3;

		folder1     = null;
		folder2     = null;
		folder3     = null;
		folderPath1 = "/folder1"+System.currentTimeMillis();
		folderPath2 = folderPath1 + "/folder2";
		folderPath3 = folderPath1 + "/folder3";

		try{
			folder1 = folderAPI.createFolders(folderPath1, host, user, false);
			folder2 = folderAPI.createFolders(folderPath2, host, user, false);
			folder3 = folderAPI.createFolders(folderPath3, host, user, false);

			List<Folder> folders = folderAPI.findSubFoldersTitleSort(folder1, user, false);

			Assert.assertNotNull(folders);
			Assert.assertTrue(folders.size() == 2);
			Assert.assertEquals(folder2.getInode(), folders.get(0).getInode());
		}finally{
			if (folder3 != null){
				folderAPI.delete(folder3, user, false);
			}

			if (folder2 != null){
				folderAPI.delete(folder2, user, false);
			}

			if (folder1 != null){
				folderAPI.delete(folder1, user, false);
			}
		}
	}

	@Test
	public void testGetLinks() throws Exception {
		Container container     = null;
		Contentlet contentAsset = null;
		Folder ftest            = null;
		Link link               = null;
		Template template       = null;

		Host host;
		Identifier internalLinkIdentifier;
		long id;
		String containerName, linkStr, page0Str, userId;
		StringBuffer myURL;

		host    = hostAPI.findDefaultHost(user, false);
		id      = System.currentTimeMillis();
		linkStr = "link" + id;
		userId  = user.getUserId();

		try {
			//Create new folder
			ftest = folderAPI.createFolders("/folderTest" + id, host, user, false);
			ftest.setOwner(userId);
			folderAPI.save(ftest, user, false);

			/**
			 * Create new container
			 */
			container     = new Container();
			containerName = "container" + id;

			container.setFriendlyName(containerName);
			container.setTitle(containerName);
			container.setOwner(userId);
			container.setMaxContentlets(5);
			container.setPreLoop("preloop code");
			container.setPostLoop("postloop code");

			List<ContainerStructure> csList = new ArrayList<ContainerStructure>();
			ContainerStructure cs = new ContainerStructure();
			cs.setStructureId(
					CacheLocator.getContentTypeCache().getStructureByVelocityVarName("SimpleWidget")
							.getInode());
			cs.setCode("<div><h3>content $!{title}</h3><p>$!{body}</p></div>");
			csList.add(cs);
			container = containerAPI.save(container, csList, host, user, false);
			PublishFactory.publishAsset(container, user, false, false);

			/**
			 * Create new template
			 */
			String templateBody  = "<html><body> #parseContainer('" + container.getIdentifier()
					+ "') </body></html>";
			String templateTitle = "template" + id;

			//Create template
			template = new Template();
			template.setTitle(templateTitle);
			template.setBody(templateBody);
			template.setOwner(user.getUserId());
			template.setDrawedBody(templateBody);
			template = templateAPI.saveTemplate(template, host, user, false);
			PublishFactory.publishAsset(template, user, false, false);

			//Create new page
			page0Str     = "page" + id;
			contentAsset = new Contentlet();
			contentAsset
					.setContentTypeId(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
			contentAsset.setHost(host.getIdentifier());
			contentAsset.setProperty(HTMLPageAssetAPIImpl.FRIENDLY_NAME_FIELD, page0Str);
			contentAsset.setProperty(HTMLPageAssetAPIImpl.URL_FIELD, page0Str);
			contentAsset.setProperty(HTMLPageAssetAPIImpl.TITLE_FIELD, page0Str);
			contentAsset.setProperty(HTMLPageAssetAPIImpl.CACHE_TTL_FIELD, "0");
			contentAsset.setProperty(HTMLPageAssetAPIImpl.TEMPLATE_FIELD, template.getIdentifier());
			contentAsset.setLanguageId(languageAPI.getDefaultLanguage().getId());
			contentAsset.setFolder(ftest.getInode());
			contentAsset = contentletAPI.checkin(contentAsset, user, false);
			contentletAPI.publish(contentAsset, user, false);

			internalLinkIdentifier = identifierAPI.findFromInode(contentAsset.getIdentifier());

			myURL = new StringBuffer();
			if (InodeUtils.isSet(internalLinkIdentifier.getHostId())) {
				myURL.append(host.getHostname());
			}
			myURL.append(internalLinkIdentifier.getURI());

			link = new Link();
			link.setTitle(linkStr);
			link.setFriendlyName(linkStr);
			link.setParent(ftest.getInode());
			link.setTarget("_blank");
			link.setOwner(userId);
			link.setModUser(userId);
			link.setLinkType(Link.LinkType.INTERNAL.toString());
			link.setInternalLinkIdentifier(internalLinkIdentifier.getId());
			link.setProtocal("http://");
			link.setUrl(myURL.toString());
			WebAssetFactory.createAsset(link, userId, ftest);

			List<Link> links = folderAPI.getLinks(ftest, user, false);

			Assert.assertNotNull(links);
			Assert.assertFalse(links.isEmpty());
			Assert.assertEquals(link.getIdentifier(), links.get(0).getIdentifier());
			Assert.assertEquals(link.getInode(), links.get(0).getInode());
		} finally {

			if (contentAsset != null && contentAsset.getInode() != null) {
				contentletAPI.archive(contentAsset, user, false);
				contentletAPI.delete(contentAsset, user, false);
			}

			if (ftest != null) {
				folderAPI.delete(ftest, user, false);
			}

			if (container != null && container.getInode() != null) {
				containerAPI.delete(container, user, false);
			}

			if (template != null && template.getInode() != null) {
				templateAPI.delete(template, user, false);
			}

		}

	}
}
