package com.dotmarketing.portlets.folders.business;

import static org.junit.Assert.assertEquals;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.cache.FolderCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
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
import com.dotmarketing.portlets.folders.exception.InvalidFolderNameException;
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
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
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
	public static void prepare() throws Exception {

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
		host = new SiteDataGen().nextPersisted();
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

		final Identifier ident = identifierAPI.loadFromDb(ftest.getVersionId());
		final Identifier ident1 = identifierAPI.loadFromDb(ftest1.getVersionId());
		final Identifier ident2 = identifierAPI.loadFromDb(ftest2.getVersionId());
		final Identifier ident3 = identifierAPI.loadFromDb(ftest3.getVersionId());

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
	public void move_to_existing_destination() throws Exception {

		//create folders and assets
		final Folder baseFolder = folderAPI
				.createFolders("/folderMoveSourceTest"+System.currentTimeMillis(), host, user, false);
		String postfixName = String.valueOf(System.currentTimeMillis()).substring(0, 8);
		Folder folderTest1 = folderAPI
				.createFolders(baseFolder.getPath()+"/ff1"+postfixName, host, user, false);

		//adding page
		final String page0Name ="page0";
		final Template template = new TemplateDataGen().nextPersisted();

		Contentlet contentAsset1 = new Contentlet();
		contentAsset1.setStructureInode(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
		contentAsset1.setHost(host.getIdentifier());
		contentAsset1.setProperty(HTMLPageAssetAPIImpl.FRIENDLY_NAME_FIELD, page0Name);
		contentAsset1.setProperty(HTMLPageAssetAPIImpl.URL_FIELD, page0Name);
		contentAsset1.setProperty(HTMLPageAssetAPIImpl.TITLE_FIELD, page0Name);
		contentAsset1.setProperty(HTMLPageAssetAPIImpl.CACHE_TTL_FIELD, "0");
		contentAsset1.setProperty(HTMLPageAssetAPIImpl.TEMPLATE_FIELD, template.getIdentifier());
		contentAsset1.setLanguageId(langId);
		contentAsset1.setFolder(folderTest1.getInode());
		contentAsset1.setIndexPolicy(IndexPolicy.FORCE);
		contentAsset1 = contentletAPI.checkin(contentAsset1, user, false);
		contentletAPI.publish(contentAsset1, user, false);

		//adding page
		final String page1Name="page1";
		Contentlet contentAsset2=new Contentlet();
		contentAsset2.setStructureInode(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
		contentAsset2.setHost(host.getIdentifier());
		contentAsset2.setProperty(HTMLPageAssetAPIImpl.FRIENDLY_NAME_FIELD, page1Name);
		contentAsset2.setProperty(HTMLPageAssetAPIImpl.URL_FIELD, page1Name);
		contentAsset2.setProperty(HTMLPageAssetAPIImpl.TITLE_FIELD, page1Name);
		contentAsset2.setProperty(HTMLPageAssetAPIImpl.CACHE_TTL_FIELD, "0");
		contentAsset2.setProperty(HTMLPageAssetAPIImpl.TEMPLATE_FIELD, template.getIdentifier());
		contentAsset2.setLanguageId(langId);
		contentAsset2.setFolder(folderTest1.getInode());
		contentAsset2.setIndexPolicy(IndexPolicy.FORCE);
		contentAsset2 = contentletAPI.checkin(contentAsset2, user, false);
		contentletAPI.publish(contentAsset2, user, false);

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
		contentAsset4.setFolder(folderTest1.getInode());
		contentAsset4.setIndexPolicy(IndexPolicy.FORCE);
		contentAsset4 = contentletAPI.checkin(contentAsset4, user, false);
		contentletAPI.publish(contentAsset4, user, false);

		final Folder destinationftest = folderAPI
				.createFolders("/application", host, user, false);

		//moving folder and assets
		Thread.sleep(2000);
		folderAPI.move(folderTest1.getIdentifier(), destinationftest.getInode(), user, false);

		//validate that the folder and assets were moved
		final Folder  newftest1 = folderAPI
				.findFolderByPath(destinationftest.getPath()+folderTest1.getName(), host, user, false);
		Assert.assertTrue("Folder ("+folderTest1.getName()+") wasn't moved", newftest1 != null);

		final List<IHTMLPage> pages = htmlPageAssetAPI.getLiveHTMLPages(newftest1,user, false);
		Assert.assertEquals(3, pages.size());
		Assert.assertTrue(pages.stream().anyMatch(page -> page.getName().equals(page0Name)));

		contentletAPI.destroy(contentAsset1, user, false);
		contentletAPI.destroy(contentAsset2, user, false);
		contentletAPI.destroy(contentAsset4, user, false);
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
		Template template = new TemplateDataGen().nextPersisted();

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

		Contentlet pageContentlet = TestDataUtils.getPageContent(true, langId);
		IHTMLPage page = APILocator.getHTMLPageAssetAPI().fromContentlet(pageContentlet);

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

		pageContentlet = TestDataUtils.getPageContent(true, langId);
		page = APILocator.getHTMLPageAssetAPI().fromContentlet(pageContentlet);

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

		Container container = new ContainerDataGen()
				.nextPersisted();
		/*Relate content to page*/
		final MultiTree m = new MultiTree(contentAsset4.getIdentifier(), container.getIdentifier(), contentAsset2.getIdentifier());
		APILocator.getMultiTreeAPI().saveMultiTree(m);

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

		final List<MultiTree> mt= APILocator.getMultiTreeAPI().getMultiTrees(pages.get(0).getIdentifier());
		Assert.assertTrue(mt.size() ==1 && mt.get(0).getParent2().equals(container.getIdentifier()) && mt.get(0).getChild().equals(contentAsset2.getIdentifier()) );
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
		Template template = new TemplateDataGen().nextPersisted();

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

		Container container = new ContainerDataGen().nextPersisted();
		/*Relate content to page*/
		MultiTree m = new MultiTree(contentAsset2.getIdentifier(), container.getIdentifier(), contentAsset3.getIdentifier());
		APILocator.getMultiTreeAPI().saveMultiTree(m);

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

		Contentlet pageContentlet = TestDataUtils.getPageContent(true, langId);
		page = APILocator.getHTMLPageAssetAPI().fromContentlet(pageContentlet);

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

		pageContentlet = TestDataUtils.getPageContent(true, langId);
		page = APILocator.getHTMLPageAssetAPI().fromContentlet(pageContentlet);

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
		APILocator.getMultiTreeAPI().saveMultiTree(m2);

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

		List<MultiTree> mt= APILocator.getMultiTreeAPI().getMultiTrees(page.getIdentifier());
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

		mt= APILocator.getMultiTreeAPI().getMultiTrees(page.getIdentifier());
		Assert.assertTrue(mt.size() ==1 && mt.get(0).getParent2().equals(container.getIdentifier()) && mt.get(0).getChild().equals(contentAsset8.getIdentifier()) );

	}

	/**
	 * Test delete folders with multilingua pages
	 * @throws Exception
	 */
	@Test
	public void delete() throws Exception {
		final long langIdES = TestDataUtils.getSpanishLanguage().getId();

		final String folderPath = "/folderDeleteSourceTest"+System.currentTimeMillis();
		final Folder ftest = folderAPI.createFolders(folderPath, host, user, false);

		final String folderIdentifier = ftest.getIdentifier();

		//adding page
		String pageStr ="mypage";
		Template template = new TemplateDataGen().nextPersisted();

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
		final Host newHost = new SiteDataGen().nextPersisted();
		final String folderPath = "/folder"+System.currentTimeMillis();

		final Folder folder = folderAPI.createFolders(folderPath, newHost, user, false);
		folder.setOwner("folder's owner");

		folderAPI.save(folder, user, false);

		final List<Folder> folders = folderAPI.findSubFolders(newHost, false);

		Assert.assertNotNull(folders);
		Assert.assertFalse(folders.isEmpty());
	}

	@Test
	public void testFindFoldersByHost() throws DotDataException, DotSecurityException {
		final Host newHost = new SiteDataGen().nextPersisted();
		final String folderPath = "/folder"+System.currentTimeMillis();

		final Folder folder = folderAPI.createFolders(folderPath, newHost, user, false);
		folder.setOwner("folder's owner");

		folderAPI.save(folder, user, false);

		final List<Folder> folders = folderAPI.findFoldersByHost(newHost, user, false);

		Assert.assertNotNull(folders);
		Assert.assertFalse(folders.isEmpty());
	}

	@Test
	public void testFindThemes() throws DotDataException, DotSecurityException {

		//Create a test theme folder
		final String themeFolderPath = "/application/themes/testTheme" + System.currentTimeMillis();
		folderAPI.createFolders(themeFolderPath, host, user, false);

		final List<Folder> folders = folderAPI.findThemes(host, user, false);

		Assert.assertNotNull(folders);
		Assert.assertFalse(folders.isEmpty());
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

		String containerName, linkStr, page0Str, userId;

		Host host = hostAPI.findDefaultHost(user, false);
		long id = System.currentTimeMillis();
		linkStr = "link" + id;
		userId = user.getUserId();

		final ContentType widgetContentType = TestDataUtils.getWidgetLikeContentType();

		//Create new folder
		Folder ftest = folderAPI.createFolders("/folderTest" + id, host, user, false);
		ftest.setOwner(userId);
		folderAPI.save(ftest, user, false);

		/**
		 * Create new container
		 */
		Container container = new Container();
		containerName = "container" + id;

		container.setFriendlyName(containerName);
		container.setTitle(containerName);
		container.setOwner(userId);
		container.setMaxContentlets(5);
		container.setPreLoop("preloop code");
		container.setPostLoop("postloop code");

		List<ContainerStructure> csList = new ArrayList<ContainerStructure>();
		ContainerStructure cs = new ContainerStructure();
		cs.setStructureId(widgetContentType.inode());
		cs.setCode("<div><h3>content $!{title}</h3><p>$!{body}</p></div>");
		csList.add(cs);
		container = containerAPI.save(container, csList, host, user, false);
		PublishFactory.publishAsset(container, user, false, false);

		/**
		 * Create new template
		 */
		String templateBody = "<html><body> #parseContainer('" + container.getIdentifier()
				+ "') </body></html>";
		String templateTitle = "template" + id;

		//Create template
		Template template = new Template();
		template.setTitle(templateTitle);
		template.setBody(templateBody);
		template.setOwner(user.getUserId());
		template.setDrawedBody(templateBody);
		template = templateAPI.saveTemplate(template, host, user, false);
		PublishFactory.publishAsset(template, user, false, false);

		//Create new page
		page0Str = "page" + id;
		Contentlet contentAsset = new Contentlet();
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

		Identifier internalLinkIdentifier = identifierAPI
				.findFromInode(contentAsset.getIdentifier());

		StringBuffer myURL = new StringBuffer();
		if (InodeUtils.isSet(internalLinkIdentifier.getHostId())) {
			myURL.append(host.getHostname());
		}
		myURL.append(internalLinkIdentifier.getURI());

		Link link = new Link();
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
	}

	@DataProvider
	public static Object[] reservedFolderNames() {
		return FolderFactoryImpl.reservedFolderNames.toArray();
	}

	@Test(expected = InvalidFolderNameException.class)
	@UseDataProvider("reservedFolderNames")
	public void testSave_BlacklistedName_ShouldFail(final String reservedName)
			throws DotDataException, DotSecurityException {
		Identifier newIdentifier=null;
		try {
			final Folder invalidFolder = new FolderDataGen().name(reservedName).next();
			newIdentifier = identifierAPI.createNew(invalidFolder, host);
			invalidFolder.setIdentifier(newIdentifier.getId());
			folderAPI.save(invalidFolder, APILocator.systemUser(), false);
		} finally {
			identifierAPI.delete(newIdentifier);
		}
	}

	@UseDataProvider("reservedFolderNames")
	public void testCopyToFolder_BlacklistedName_ShouldSucceed(final String reservedName)
			throws DotDataException, DotSecurityException, IOException {

		Identifier newIdentifier = null;

		try {
			final Folder invalidFolder = new FolderDataGen().name(reservedName).next();
			newIdentifier = identifierAPI.createNew(invalidFolder, host);
			invalidFolder.setIdentifier(newIdentifier.getId());

			final Folder newFolder = new FolderDataGen().nextPersisted();

			folderAPI.copy(invalidFolder, newFolder, APILocator.systemUser(), false);
		} finally {
			identifierAPI.delete(newIdentifier);
		}
	}

	@Test(expected = InvalidFolderNameException.class)
	@UseDataProvider("reservedFolderNames")
	public void testCopyToHost_BlacklistedName_ShouldFail(final String reservedName)
			throws DotDataException, DotSecurityException, IOException {
		final Folder invalidFolder = new FolderDataGen().name(reservedName).next();
		final Identifier newIdentifier = identifierAPI.createNew(invalidFolder, host);
		invalidFolder.setIdentifier(newIdentifier.getId());
		final Host newHost = new SiteDataGen().nextPersisted();
		folderAPI.copy(invalidFolder, newHost, APILocator.systemUser(), false);
	}

	@Test(expected = InvalidFolderNameException.class)
	@UseDataProvider("reservedFolderNames")
	public void testRename_BlacklistedName_ShouldFail(final String reservedName)
			throws DotDataException, DotSecurityException {

		Identifier newIdentifier = null;

		try {
			final Folder folder = new FolderDataGen()
					.name("testFolderRename" + System.currentTimeMillis()).next();
			newIdentifier = identifierAPI.createNew(folder, host);
			folder.setIdentifier(newIdentifier.getId());
			folderAPI.save(folder, APILocator.systemUser(), false);

			folderAPI.renameFolder(folder, reservedName, user, false);
		} finally {
			if(newIdentifier!=null) {
				identifierAPI.delete(newIdentifier);
			}
		}
	}

	@Test(expected = InvalidFolderNameException.class)
	@UseDataProvider("reservedFolderNames")
	public void testCreateFolders_AtRootLevel_BlacklistedName_ShouldFail(final String reservedName)
			throws DotDataException, DotSecurityException {
		final String foldersString = "/" + reservedName;
		folderAPI.createFolders(foldersString,host,user,false);
	}

	@Test(expected = InvalidFolderNameException.class)
	@UseDataProvider("reservedFolderNames")
	public void testSave_BlacklistedName_NotAtRootLevel_ShouldSucceed(final String reservedName)
			throws DotDataException, DotSecurityException {
		Identifier newIdentifier = null;

		try {
			final Folder parentFolder = new FolderDataGen().next();
			final Folder invalidFolder = new FolderDataGen().parent(parentFolder)
					.name(reservedName).next();
			newIdentifier = identifierAPI.createNew(invalidFolder, host);
			invalidFolder.setIdentifier(newIdentifier.getId());
			folderAPI.save(invalidFolder, APILocator.systemUser(), false);
		} finally {
			identifierAPI.delete(newIdentifier);
		}
	}

	@Test(expected = InvalidFolderNameException.class)
	@UseDataProvider("reservedFolderNames")
	public void testRename_BlacklistedName_NotAtRootLevel_ShouldSucceed(final String reservedName)
			throws DotDataException, DotSecurityException {
		Identifier newIdentifier = null;

		try {
			final Folder parentFolder = new FolderDataGen().next();
			final Folder folder = new FolderDataGen().parent(parentFolder)
					.name("testFolderRename" + System.currentTimeMillis()).next();
			newIdentifier = identifierAPI.createNew(folder, host);
			folder.setIdentifier(newIdentifier.getId());
			folderAPI.save(folder, APILocator.systemUser(), false);

			folderAPI.renameFolder(folder,reservedName,user,false);
		} finally {
			identifierAPI.delete(newIdentifier);
		}
	}

	@UseDataProvider("reservedFolderNames")
	public void testCreateFolders_NotAtRootLevel_BlacklistedName_ShouldSucceed(final String reservedName)
			throws DotDataException, DotSecurityException {
		Folder folder = null;
		try {
			final String foldersString = "/testFolders/" + reservedName;
			folder = folderAPI.createFolders(foldersString, host, user, false);
		} finally {
			if(folder!=null) {
				APILocator.getFolderAPI().delete(folder, APILocator.systemUser(), false);
			}
		}
	}

	/**
	 * Method to test: findFolderByPath in the FolderAPI
	 * Given Scenario: Create a folder and get the folder using the path and the admin user.
	 * ExpectedResult: The folder created.
	 *
	 */
	@Test
	public void test_findFolderByPath_Admin_success() throws DotDataException, DotSecurityException {
		final Host newHost = new SiteDataGen().nextPersisted();
		final String folderPath = "/folder"+System.currentTimeMillis();

		final Folder folder = folderAPI.createFolders(folderPath, newHost, user, false);
		folder.setOwner("folder's owner");

		folderAPI.save(folder, user, false);

		fc.removeFolder(folder, identifierAPI.find(folder));
		final Folder folderByPath = folderAPI.findFolderByPath(folderPath, newHost, user,false);

		Assert.assertNotNull(folderByPath);
		Assert.assertEquals(folder.getInode(), folderByPath.getInode());
		Assert.assertEquals(folder.getOwner(), folderByPath.getOwner());
	}

	/**
	 * Method to test: findFolderByPath in the FolderAPI
	 * Given Scenario: call the findFolderByPath but the host is null
	 * ExpectedResult: null
	 *
	 */
	@Test
	public void test_findFolderByPath_Admin_HostisNull_returnNullFolder() throws DotDataException, DotSecurityException {

		final Folder folderByPath = folderAPI.findFolderByPath("/", (String) null, user,false);

		Assert.assertNull(folderByPath);
	}

	/**
	 * Method to test: findFolderByPath in the FolderAPI
	 * Given Scenario: call the findFolderByPath but the path is null
	 * ExpectedResult: null
	 *
	 */
	@Test
	public void test_findFolderByPath_Admin_PathisNull_returnNullFolder() throws DotDataException, DotSecurityException {

		final Folder folderByPath = folderAPI.findFolderByPath(null, host, user,false);

		Assert.assertNull(folderByPath);
	}

	/**
	 * Method to test: findFolderByPath in the FolderAPI
	 * Given Scenario: Create a folder using the admin user, now using a limited user try to get
	 * the created folder by path, but the user does not have permissions over the host
	 * ExpectedResult: DotSecurityException.class, since the user does not have permissions over the host
	 *
	 */
	@Test (expected = DotSecurityException.class)
	public void test_findFolderByPath_UserNoPermissionsOverHost_returnDotSecurityException() throws DotDataException, DotSecurityException {
		final Host newHost = new SiteDataGen().nextPersisted();
		final long currentTime = System.currentTimeMillis();
		final String folderPath = "/folder"+currentTime;

		final Folder folder = folderAPI.createFolders(folderPath, newHost, user, false);
		folder.setOwner("folder's owner");

		folderAPI.save(folder, user, false);
		fc.removeFolder(folder, identifierAPI.find(folder));

		final User limitedUser = new UserDataGen().nextPersisted();

		folderAPI.findFolderByPath(folderPath, newHost, limitedUser,false);
	}

	/**
	 * Method to test: findFolderByPath in the FolderAPI
	 * Given Scenario: Create a folder using the admin user, now using a limited user try to get
	 * the created folder by path, but the user does not have permissions over the folder
	 * ExpectedResult: DotSecurityException.class, since the user does not have permissions over the folder
	 *
	 */
	@Test (expected = DotSecurityException.class)
	public void test_findFolderByPath_UserNoPermissionsOverFolder_returnDotSecurityException() throws DotDataException, DotSecurityException {
		final Host newHost = new SiteDataGen().nextPersisted();
		final long currentTime = System.currentTimeMillis();
		final String folderPath = "/folder"+currentTime;

		final Folder folder = folderAPI.createFolders(folderPath, newHost, user, false);
		folder.setOwner("folder's owner");

		folderAPI.save(folder, user, false);
		fc.removeFolder(folder, identifierAPI.find(folder));

		final User limitedUser = new UserDataGen().nextPersisted();

		//Give Permissions Over the Host
		final Permission permissions = new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
				newHost.getPermissionId(),
				APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId(),
				PermissionAPI.PERMISSION_READ, true);
		APILocator.getPermissionAPI().save(permissions, newHost, user, false);

		folderAPI.findFolderByPath(folderPath, newHost, limitedUser,false);
	}

	/**
	 * Method to test: findFolderByPath in the FolderAPI
	 * Given Scenario: Create a folder using the admin user, now using a limited user try to get
	 * the created folder by path, user has permissions over the folder and the host
	 * ExpectedResult: the requested folder
	 *
	 */
	@Test
	public void test_findFolderByPath_UserWithPermissionsOverFolderAndHost_success() throws DotDataException, DotSecurityException {
		final Host newHost = new SiteDataGen().nextPersisted();
		final long currentTime = System.currentTimeMillis();
		final String folderPath = "/folder"+currentTime;

		final Folder folder = folderAPI.createFolders(folderPath, newHost, user, false);
		folder.setOwner("folder's owner");

		folderAPI.save(folder, user, false);
		fc.removeFolder(folder, identifierAPI.find(folder));

		final User limitedUser = new UserDataGen().nextPersisted();

		//Give Permissions Over the Host
		Permission permissions = new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
				newHost.getPermissionId(),
				APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId(),
				PermissionAPI.PERMISSION_READ, true);
		APILocator.getPermissionAPI().save(permissions, newHost, user, false);
		//Give Permissions Over the Folder
		permissions = new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
				folder.getPermissionId(),
				APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId(),
				PermissionAPI.PERMISSION_READ, true);
		APILocator.getPermissionAPI().save(permissions, folder, user, false);

		final Folder folderByPath = folderAPI.findFolderByPath(folderPath, newHost, limitedUser,false);

		Assert.assertNotNull(folderByPath);
		Assert.assertEquals(folder.getInode(), folderByPath.getInode());
		Assert.assertEquals(folder.getOwner(), folderByPath.getOwner());
	}

	
	
    /**
     * this method tests that when you create a folder with a default file type and then create folders
     * underneith it, that the children folders inherit the parent's default file type. This is
     * especially used when creating folders via webdav
     * 
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void test_folders_inherit_the_filetypes_of_their_parents() throws DotDataException, DotSecurityException {
        final Host newHost = new SiteDataGen().nextPersisted();
        final long currentTime = System.currentTimeMillis();

        final ContentType fileAssetType = contentTypeAPI.find(FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME);
        ContentType newFileAssetType = ContentTypeBuilder
                        .builder(fileAssetType)
                        .id(null)
                        .variable("fileAsset"  + currentTime)
                        .name("fileAsset"  + currentTime)
                        .build();
        
        newFileAssetType =  contentTypeAPI.save(newFileAssetType);
        

        
        assertEquals(newFileAssetType.variable(),"fileAsset"  + currentTime);
        assert(newFileAssetType.id()!=null);
        
        final Folder parentFolder = new FolderDataGen().defaultFileType(newFileAssetType.id()).site(newHost).nextPersisted();
        assertEquals(parentFolder.getDefaultFileType(), newFileAssetType.id());
        
        
        final String folderPath = parentFolder.getPath() + "folder1/folder2/folder3";

        folderAPI.createFolders(folderPath, newHost, user, false);

        // /path/folder1
        Folder folder1 = folderAPI.findFolderByPath(parentFolder.getPath() + "/folder1", newHost, user, false);
        assert(folder1!=null);
        assertEquals(folder1.getDefaultFileType(), newFileAssetType.id());
        
        // /path/folder2
        Folder folder2 = folderAPI.findFolderByPath(parentFolder.getPath() + "/folder1/folder2", newHost, user, false);
        assert(folder2!=null);
        assertEquals(folder2.getDefaultFileType(), newFileAssetType.id());
        
        // /path/folder3
        Folder folder3 = folderAPI.findFolderByPath(parentFolder.getPath() + "/folder1/folder2/folder3", newHost, user, false);
        assert(folder3!=null);
        assertEquals(folder3.getDefaultFileType(), newFileAssetType.id());        
    }
	
	
	
	
}

