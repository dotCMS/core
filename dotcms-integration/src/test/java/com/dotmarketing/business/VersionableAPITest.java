package com.dotmarketing.business;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.StructureDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.VariantDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;

import java.util.List;
import java.util.Random;

import graphql.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


import java.util.Optional;

import static org.junit.Assert.*;

/**
 * Created by Erick Gonzalez
 */
public class VersionableAPITest {
	
	private static User user;
	private static Host host;
	@BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        
        user = APILocator.getUserAPI().getSystemUser();
        host = APILocator.getHostAPI().findDefaultHost(user, false);
	}
	
	private HTMLPageAsset createHTMLPage() throws Exception{
		//Create HTMLPage
		String ext="."+Config.getStringProperty("VELOCITY_PAGE_EXTENSION");
				
		Template template = new TemplateDataGen().nextPersisted();
		        
		Folder folder = APILocator.getFolderAPI().createFolders("/testingVersionable", host, user, false);
				
		return new HTMLPageDataGen(folder,template).nextPersisted();
	}
	@Test
	public void testFindPreviousVersion() throws Exception {

		//Create Contentlet
		final Structure structure = new StructureDataGen().nextPersisted();
		final Contentlet contentletPrevious = new ContentletDataGen(structure.getInode()).nextPersisted();


		final Contentlet checkoutContentlet = APILocator.getContentletAPI()
				.checkout(contentletPrevious.getInode(), APILocator.systemUser(), false);

		final String inode = checkoutContentlet.getInode();
		APILocator.getContentletAPI().copyProperties(checkoutContentlet, contentletPrevious.getMap());
		checkoutContentlet.setInode(inode);
		checkoutContentlet.setStringProperty("name", "New Name");
		APILocator.getContentletAPI().checkin(checkoutContentlet, APILocator.systemUser(), false);

		try {
			//Call Versionable
			final Optional<Versionable> previousVersion = APILocator.getVersionableAPI()
					.findPreviousVersion(contentletPrevious.getIdentifier());

			assertNotNull(previousVersion);
			assertTrue(previousVersion.isPresent());
			final Contentlet recoveryPreviousContentlet =  APILocator.getContentletAPI().find
					(previousVersion.get().getInode(), APILocator.systemUser(), false);

			assertNotNull(recoveryPreviousContentlet);
			assertEquals(recoveryPreviousContentlet.getStringProperty("name"),
					contentletPrevious.getStringProperty("name"));
		}finally {
			StructureDataGen.remove(structure);
		}

	}

	@Test
	public void testFindWorkingVersionHTMLPage() throws Exception{
		HTMLPageAsset page = createHTMLPage();
        
        //Call Versionable
        Contentlet verAPI = APILocator.getContentletAPI().findContentletByIdentifier(page.getIdentifier(), false, page.getLanguageId(), user, false);
        
        //Check Same HTMLPage
        assertEquals(verAPI.getTitle(),page.getTitle());
        assertEquals(verAPI.getInode(),page.getInode());
        
        //Delete Template, Folder, HTMLPage
        Folder folder = APILocator.getHTMLPageAssetAPI().getParentFolder(page);
        Template template = APILocator.getHTMLPageAssetAPI().getTemplate(page, false);
        HTMLPageDataGen.remove(page);
        APILocator.getFolderAPI().delete(folder, user, false);
	}
	
	@Test
	public void testFindWorkingVersionContainer() throws Exception{
		//Create Container
		Structure structure = new StructureDataGen().nextPersisted();
		Container container = new ContainerDataGen().withStructure(structure, "").nextPersisted();
        
        //Call Versionable
        Versionable verAPI = APILocator.getVersionableAPI().findWorkingVersion(container.getIdentifier(), user, false);
        
        //Check Same Container
        assertEquals(verAPI.getTitle(),container.getTitle());
        assertEquals(verAPI.getInode(),container.getInode());
        
        //Delete Container
        APILocator.getContainerAPI().delete(container, user, false);
        APILocator.getStructureAPI().delete(structure, user);
	}
	
	@Test
	public void testFindWorkingVersionTemplate() throws Exception{
		//Create Template
		Template template = new TemplateDataGen().nextPersisted();
        
        //Call Versionable
        Versionable verAPI = APILocator.getVersionableAPI().findWorkingVersion(template.getIdentifier(), user, false);
        
        //Check same Template
        assertEquals(verAPI.getTitle(),template.getTitle());
        assertEquals(verAPI.getInode(),template.getInode());
        
        //Delete Template
        APILocator.getTemplateAPI().delete(template, user, false);
	}

	@Test(expected = DotDataException.class)
	public void testFindWorkingVersionContentlet() throws Exception{
		//Create Contentlet
		Structure structure = new StructureDataGen().nextPersisted();
		Contentlet contentlet = new ContentletDataGen(structure.getInode()).nextPersisted();

		try {
			//Call Versionable
			Versionable verAPI = APILocator.getVersionableAPI()
					.findWorkingVersion(contentlet.getIdentifier(), user, false);
		}finally {
			StructureDataGen.remove(structure);
		}

	}
	
	@Test
	public void testFindLiveVersionHTMLPage() throws Exception{
		HTMLPageAsset page = createHTMLPage();
        
        APILocator.getVersionableAPI().setLive(page);
        
        //Call Versionable
        Contentlet verAPI = APILocator.getContentletAPI().findContentletByIdentifier(page.getIdentifier(), true, page.getLanguageId(), user, false);
        
        //Check Same HTMLPage
        assertEquals(verAPI.getTitle(),page.getTitle());
        assertEquals(verAPI.getInode(),page.getInode());
        
        //Delete Template, Folder, HTMLPage
        Folder folder = APILocator.getHTMLPageAssetAPI().getParentFolder(page);
        Template template = APILocator.getHTMLPageAssetAPI().getTemplate(page, false);
        HTMLPageDataGen.remove(page);
        APILocator.getFolderAPI().delete(folder, user, false);
	}
	
	@Test
	public void testFindLiveVersionContainer() throws Exception{
		//Create Container
		Structure structure = new StructureDataGen().nextPersisted();
		Container container = new ContainerDataGen().withStructure(structure, "").nextPersisted();
        
        APILocator.getVersionableAPI().setLive(container);
        
        //Call Versionable
        Versionable verAPI = APILocator.getVersionableAPI().findLiveVersion(container.getIdentifier(), user, false);
        
        //Check Same Container
        assertEquals(verAPI.getTitle(),container.getTitle());
        assertEquals(verAPI.getInode(),container.getInode());
        
        //Delete Container
        APILocator.getContainerAPI().delete(container, user, false);
        APILocator.getStructureAPI().delete(structure, user);
	}
	
	@Test
	public void testFindLiveVersionTemplate() throws Exception{
		//Create Template
		Template template = new TemplateDataGen().nextPersisted();
        APILocator.getVersionableAPI().setLive(template);
        
        //Call Versionable
        Versionable verAPI = APILocator.getVersionableAPI().findLiveVersion(template.getIdentifier(), user, false);
        
        //Check same Template
        assertEquals(verAPI.getTitle(),template.getTitle());
        assertEquals(verAPI.getInode(),template.getInode());
        
        //Delete Template
        APILocator.getTemplateAPI().delete(template, user, false);
	}

	@Test(expected = DotDataException.class)
	public void testFindLiveVersionContentlet() throws Exception{
		//Create Contentlet
		Structure structure = new StructureDataGen().nextPersisted();
		Contentlet contentlet = new ContentletDataGen(structure.getInode()).nextPersisted();
        try {
			//Call Versionable
			Versionable verAPI = APILocator.getVersionableAPI()
					.findLiveVersion(contentlet.getIdentifier(), user, false);
		}finally {
        	StructureDataGen.remove(structure);
		}
	}

	/**
	 * Method to test: {@link VersionableAPI#isWorking(Versionable)}
	 * Test Case: Invoking {@link VersionableAPI#isWorking(Versionable)} using a host with an invalid language
	 * Expected Results: It should return the host because the language should be ignored
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@Test
	public void testIsWorkingHost() throws DotDataException, DotSecurityException {
		final Host host = new SiteDataGen().nextPersisted();
		host.setLanguageId(new Random().nextLong());
		assertTrue(APILocator.getVersionableAPI().isWorking(host));
	}

	/**
	 * Method to test: {@link VersionableAPI#isLive(Versionable)}
	 * Test Case: Invoking {@link VersionableAPI#isLive(Versionable)} using a host with an invalid language
	 * Expected Results: It should return the host because the language should be ignored
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@Test
	public void testIsLiveHost() throws DotDataException, DotSecurityException {
		final Host host = new SiteDataGen().nextPersisted();
		host.setLanguageId(new Random().nextLong());
		assertTrue(APILocator.getVersionableAPI().isLive(host));
	}

	/**
	 * Methods to test: {@link VersionableAPI#isLocked(Versionable)} and {@link VersionableAPI#setLocked(Versionable, boolean, User)}
	 * Test Case: Invoking {@link VersionableAPI#isLive(Versionable)} and {@link VersionableAPI#setLocked(Versionable, boolean, User)}
	 * using a host with an invalid language
	 * Expected Results: They should return the host because the language should be ignored
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@Test
	public void testSetLockedHost() throws DotDataException, DotSecurityException {
		final Host host = new SiteDataGen().nextPersisted();
		host.setLanguageId(new Random().nextLong());
		assertFalse(APILocator.getVersionableAPI().isLocked(host));
		APILocator.getVersionableAPI().setLocked(host, true, user);
		assertTrue(APILocator.getVersionableAPI().isLocked(host));
	}

	/**
	 * Method to test: {@link Contentlet#isWorking()} and {@link Contentlet#isLive()}
	 * When:
	 * - Create a {@link Contentlet} but not publish it, the {@link Contentlet#isWorking()} should return
	 * true and {@link Contentlet#isLive()} should return false.
	 * - Publish a {@link Contentlet}, the {@link Contentlet#isWorking()} should return
	 * true and {@link Contentlet#isLive()} should return true.
	 * - Create a new version od a {@link Contentlet} but not publish it, the {@link Contentlet#isWorking()} should return
	 * true and {@link Contentlet#isLive()} should return false.
	 *
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@Test
	public void setWorkingWithDefaultVariant() throws DotDataException, DotSecurityException {
		final ContentType contentType = new ContentTypeDataGen().nextPersisted();
		final Contentlet contentlet = new ContentletDataGen(contentType).nextPersisted();

		assertTrue(contentlet.isWorking());
		assertFalse(contentlet.isLive());

		ContentletDataGen.publish(contentlet);

		assertTrue(contentlet.isWorking());
		assertTrue(contentlet.isLive());

		final Contentlet checkout = ContentletDataGen.checkout(contentlet);
		final Contentlet checkin = ContentletDataGen.checkin(checkout);

		assertTrue(checkin.isWorking());
		assertFalse(checkin.isLive());
	}

	/**
	 * Method to test: {@link Contentlet#isWorking()} and {@link Contentlet#isLive()}
	 * When:
	 * - Create a {@link Contentlet} but not publish it, the {@link Contentlet#isWorking()} should return
	 * true and {@link Contentlet#isLive()} should return false.
	 * - Publish a {@link Contentlet}, the {@link Contentlet#isWorking()} should return
	 * true and {@link Contentlet#isLive()} should return true.
	 * - Create a new version od a {@link Contentlet} but not publish it, the {@link Contentlet#isWorking()} should return
	 * true and {@link Contentlet#isLive()} should return false.
	 * - Create a new version for a specific {@link com.dotcms.variant.model.Variant} and repeat all the previous steps
	 *
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@Test
	public void setWorkingWithAnotherVariant() throws DotDataException, DotSecurityException {
		final ContentType contentType = new ContentTypeDataGen().nextPersisted();
		final Contentlet contentlet = new ContentletDataGen(contentType).nextPersisted();

		assertTrue(contentlet.isWorking());
		assertFalse(contentlet.isLive());

		ContentletDataGen.publish(contentlet);

		assertTrue(contentlet.isWorking());
		assertTrue(contentlet.isLive());

		final Contentlet checkout = ContentletDataGen.checkout(contentlet);
		final Contentlet checkin = ContentletDataGen.checkin(checkout);

		assertTrue(checkin.isWorking());
		assertFalse(checkin.isLive());

		final Variant variant = new VariantDataGen().nextPersisted();
		final Contentlet contentletCheckout_2 = ContentletDataGen.checkout(checkin);
		contentletCheckout_2.setVariantId(variant.name());
		final Contentlet checkinWithVariant = ContentletDataGen.checkin(contentletCheckout_2);

		assertTrue(checkinWithVariant.isWorking());
		assertFalse(checkinWithVariant.isLive());

		ContentletDataGen.publish(checkinWithVariant);

		assertTrue(checkinWithVariant.isWorking());
		assertTrue(checkinWithVariant.isLive());

		final Contentlet contentletWithVariantCheckout = ContentletDataGen.checkout(checkinWithVariant);
		final Contentlet contentletWithVariantChecking = ContentletDataGen.checkin(contentletWithVariantCheckout);

		assertTrue(contentletWithVariantChecking.isWorking());
		assertFalse(contentletWithVariantChecking.isLive());
	}

	/**
	 * Method to test: {@link VersionableAPI#findAllByVariant(Variant)} (Variant)}
	 * When: Create a {@link Contentlet}, a {@link com.dotcms.variant.model.Variant} and a {@link com.dotmarketing.portlets.languagesmanager.model.Language}
	 * and Create a version of this {@link Contentlet} for the specific and DEFAULT {@link com.dotcms.variant.model.Variant}
	 * in different {@link com.dotmarketing.portlets.languagesmanager.model.Language}'s
	 * Should: Return the {@link Contentlet} for all the versions for specific {@link com.dotcms.variant.model.Variant}
	 */
	@Test
	public void getAllByVariants() throws DotDataException {
		final Language language = new LanguageDataGen().nextPersisted();
		final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();

		final Variant specificVariant = new VariantDataGen().nextPersisted();
		final Variant defaultVariant = VariantAPI.DEFAULT_VARIANT;

		final ContentType contentType = new ContentTypeDataGen().nextPersisted();
		final Contentlet contentletDefaultVariantSpecificLang = new ContentletDataGen(contentType)
				.languageId(language.getId())
				.nextPersisted();

		final Contentlet contentletDefaultVariantDefaultLang = new ContentletDataGen(contentType)
				.languageId(defaultLanguage.getId())
				.nextPersisted();

		final Contentlet contentletSpecificVariantSpecificLang = new ContentletDataGen(contentType)
				.languageId(language.getId())
				.variant(specificVariant)
				.nextPersisted();

		final Contentlet contentletSpecificVariantDefaultLang = new ContentletDataGen(contentType)
				.languageId(defaultLanguage.getId())
				.variant(specificVariant)
				.nextPersisted();

		final List<ContentletVersionInfo> allByVariant = APILocator.getVersionableAPI()
				.findAllByVariant(specificVariant);

		assertEquals(2, allByVariant.size());

		allByVariant.forEach(contentlet -> {
			assertEquals(specificVariant.name(), contentlet.getVariant());

			assertTrue(
			contentletSpecificVariantDefaultLang.getInode().equals(contentlet.getWorkingInode()) ||
						contentletSpecificVariantSpecificLang.getInode().equals(contentlet.getWorkingInode())
			);
		});

	}

	/**
	 * Method to test: {@link VersionableAPI#findAllByVariant(Variant)}
	 * When: Create a {@link Contentlet}, a {@link com.dotcms.variant.model.Variant} and a {@link com.dotmarketing.portlets.languagesmanager.model.Language}
	 * and Create a version of this {@link Contentlet} for the specific and DEFAULT {@link com.dotcms.variant.model.Variant}
	 * in different {@link com.dotmarketing.portlets.languagesmanager.model.Language}'s
	 * and PUBLISH THEM
	 * Should: Return the {@link Contentlet} for all the versions for specific {@link com.dotcms.variant.model.Variant}
	 */
	@Test
	public void getAllByVariantsLIVE() throws DotDataException {
		final Language language = new LanguageDataGen().nextPersisted();
		final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();

		final Variant specificVariant = new VariantDataGen().nextPersisted();
		final Variant defaultVariant = VariantAPI.DEFAULT_VARIANT;

		final ContentType contentType = new ContentTypeDataGen().nextPersisted();
		final Contentlet contentletDefaultVariantSpecificLang = new ContentletDataGen(contentType)
				.languageId(language.getId())
				.nextPersistedAndPublish();

		final Contentlet contentletDefaultVariantDefaultLang = new ContentletDataGen(contentType)
				.languageId(defaultLanguage.getId())
				.nextPersistedAndPublish();

		final Contentlet contentletSpecificVariantSpecificLang = new ContentletDataGen(contentType)
				.languageId(language.getId())
				.variant(specificVariant)
				.nextPersistedAndPublish();

		final Contentlet contentletSpecificVariantDefaultLang = new ContentletDataGen(contentType)
				.languageId(defaultLanguage.getId())
				.variant(specificVariant)
				.nextPersistedAndPublish();

		final List<ContentletVersionInfo> allByVariant = APILocator.getVersionableAPI()
				.findAllByVariant(specificVariant);

		assertEquals(2, allByVariant.size());

		allByVariant.forEach(contentlet -> {
			assertEquals(specificVariant.name(), contentlet.getVariant());

			assertTrue(
					contentletSpecificVariantDefaultLang.getInode().equals(contentlet.getLiveInode()) ||
							contentletSpecificVariantSpecificLang.getInode().equals(contentlet.getLiveInode())
			);
		});

	}

	/**
	 * Method to test: {@link VersionableAPI#findAllByVariant(Variant)}
	 * When: Create a {@link com.dotcms.variant.model.Variant} but not create any {@link Contentlet}'s version
	 * into it.
	 * Should: Return empty Collection
	 */
	@Test
	public void getEmptyVariant() throws DotDataException {

		final Variant specificVariant = new VariantDataGen().nextPersisted();

		final List<ContentletVersionInfo> allByVariant = APILocator.getVersionableAPI()
				.findAllByVariant(specificVariant);

		assertTrue(allByVariant.isEmpty());
	}

	/**
	 * Method to test: {@link VersionableAPI#isWorking(Versionable)}
	 * When: Create a  {@link Contentlet} but do not save it, call the isWorking
	 * Should: should false
	 */
	@Test
	public void test_is_working_with_non_persisted_contentlet() throws DotDataException, DotSecurityException {

		final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser()).find("webPageContent");
		final Contentlet myContentlet = new ContentletDataGen(contentType).next();

		Assert.assertFalse(myContentlet.isWorking());
	}

	/**
	 * Method to test: {@link VersionableAPI#isWorking(Versionable)}
	 * When: Create a  {@link Contentlet} and save it, call the isWorking
	 * Should: should true
	 */
	@Test
	public void test_is_working_with_persisted_contentlet() throws DotDataException, DotSecurityException {

		final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser()).find("webPageContent");
		final Contentlet myContentlet = new ContentletDataGen(contentType)
				.setProperty("title","Test").setProperty("body","Test Body")
				.nextPersisted();

		Assert.assertTrue(myContentlet.isWorking());
	}

	/**
	 * Method to test: {@link VersionableAPI#isWorking(Versionable)}
	 * When: Create a  {@link Contentlet} and save it, call the isWorking
	 * Next, create another languages.
	 * Next, create a version but in that new language, call the isWorking
	 * Should: should false
	 */
	@Test
	public void test_is_working_with_persisted_in_diff_lang_contentlet() throws DotDataException, DotSecurityException {

		final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser()).find("webPageContent");
		final Contentlet myContentlet = new ContentletDataGen(contentType)
				.setProperty("title","Test").setProperty("body","Test Body")
				.nextPersisted();
		final String countryCode = "it";
		final String languageCode = "it";
		final Language languageIt = APILocator.getLanguageAPI().getLanguage(languageCode, countryCode)==null?
				new LanguageDataGen().countryCode(countryCode).languageCode(languageCode).nextPersisted():
				APILocator.getLanguageAPI().getLanguage(languageCode, countryCode);

		final Contentlet myContentletIt = new ContentletDataGen(contentType)
				.setProperty("title","Test").setProperty("body","Test Body")
				.languageId(languageIt.getId())
				.next();

		Assert.assertTrue(myContentlet.isWorking());

		myContentletIt.setIdentifier(myContentlet.getIdentifier());

		Assert.assertFalse(myContentletIt.isWorking());
	}

	/**
	 * Method to test: {@link VersionableAPI#isWorking(Versionable)}
	 * When: Create a  {@link Contentlet} and save it, call the isWorking
	 * Next, create another languages.
	 * Next, create a version but in that new language, call the isWorking
	 * Additionally calls the {@link VersionableAPI#hasWorkingVersionInAnyOtherLanguage(Versionable, long)} that may return true
	 */
	@Test
	public void test_is_working_with_persisted_in_any_lang_contentlet() throws DotDataException, DotSecurityException {

		final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser()).find("webPageContent");
		final Contentlet myContentlet = new ContentletDataGen(contentType)
				.setProperty("title","Test").setProperty("body","Test Body")
				.nextPersisted();
		final String countryCode = "it";
		final String languageCode = "it";
		final Language languageIt = APILocator.getLanguageAPI().getLanguage(languageCode, countryCode)==null?
				new LanguageDataGen().countryCode(countryCode).languageCode(languageCode).nextPersisted():
				APILocator.getLanguageAPI().getLanguage(languageCode, countryCode);

		final Contentlet myContentletIt = new ContentletDataGen(contentType)
				.setProperty("title","Test").setProperty("body","Test Body")
				.languageId(languageIt.getId())
				.next();

		Assert.assertTrue(myContentlet.isWorking());

		myContentletIt.setIdentifier(myContentlet.getIdentifier());

		Assert.assertFalse(myContentletIt.isWorking());

		Assert.assertTrue(APILocator.getVersionableAPI().hasWorkingVersionInAnyOtherLanguage(myContentletIt, languageIt.getId()));
	}
}
