package com.dotcms.uuid.shorty;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.CategoryDataGen;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.LinkDataGen;
import com.dotcms.datagen.RelationshipDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.UUIDGenerator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.vavr.Tuple2;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for the Shorty ID API class.
 * 
 * @author 
 * @since Oct 10, 2016
 */
@RunWith(DataProviderRunner.class)
public class LegacyShortyIdApiTest {

    static List<String> fourOhFours = new ArrayList<>();
    static ShortyIdAPI shortyIdAPI;
    static List<String[]> expectedIds = null;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    	IntegrationTestInitService.getInstance().init();

        shortyIdAPI = new LegacyShortyIdAPIImpl();
        getExpectedUUIDs();

        for (int i = 0; i < 10; i++) {
            fourOhFours.add(RandomStringUtils.randomAlphanumeric(ShortyIdAPIImpl.MINIMUM_SHORTY_ID_LENGTH));
        }


    }
    
    final static String GET_INODE = "SELECT inode FROM inode WHERE type = ? AND inode <> 'SYSTEM_FOLDER'";
	final static String GET_ID_CONTAINERS = "SELECT identifier FROM dot_containers";
    final static String GET_ID_CONTENTLETS = "SELECT identifier FROM contentlet WHERE identifier <> 'SYSTEM_HOST'";
	final static String GET_ID_LINKS = "SELECT identifier FROM links";
	final static String GET_ID_TEMPLATES = "SELECT identifier FROM template";
	final static String GET_ID_FOLDERS = "SELECT identifier FROM folder";

    /**
	 * This utility method reads actual data from the DB in order to get valid
	 * information for testing purposes.
	 * 
	 * @throws DotDataException
	 *             An error occurred when reading the test data.
	 */
    private static void getExpectedUUIDs() throws DotDataException, DotSecurityException {

		final DotConnect dc = new DotConnect();
		Builder<String[]> builder = ImmutableList.<String[]>builder();

		//Create 2 Containers, and save the inode and the identifier of them
        new ContainerDataGen().nextPersisted();
        new ContainerDataGen().nextPersisted();
        dc.setSQL(GET_INODE, 2);
        dc.addParam("containers");
        List<Map<String, Object>> res = dc.loadObjectResults();
        //builder.add(new String[] { res.get(1).get("inode").toString(), "inode", "containers" });
		dc.setSQL(GET_ID_CONTAINERS, 2);
		res = dc.loadObjectResults();
		//builder.add(new String[] { res.get(1).get("identifier").toString(), "identifier", "containers" });

        //Create 2 Contentlets, and save the inode and the identifier of them
        final ContentType contentGenericType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find("webPageContent");
        new ContentletDataGen(contentGenericType.id())
                .setProperty("title", "TestContent")
                .setProperty("body", "TestBody").nextPersisted();
        new ContentletDataGen(contentGenericType.id())
                .setProperty("title", "TestContent")
                .setProperty("body", "TestBody").nextPersisted();
		dc.setSQL(GET_ID_CONTENTLETS, 2);
		res = dc.loadObjectResults();
        //builder.add(new String[] { res.get(1).get("identifier").toString(), "identifier", "contentlet" });

        dc.setSQL(GET_INODE, 2);
        dc.addParam("contentlet");
        res = dc.loadObjectResults();
        //builder.add(new String[] { res.get(1).get("inode").toString(), "inode", "contentlet" });

        //Create 2 Folders, and save the inode and the identifier of them
        new FolderDataGen().nextPersisted();
        new FolderDataGen().nextPersisted();
		dc.setSQL(GET_ID_FOLDERS, 2);
		res = dc.loadObjectResults();
        //builder.add(new String[] { res.get(1).get("identifier").toString(), "identifier", "folder" });

        dc.setSQL(GET_INODE, 2);
        dc.addParam("folder");
        res = dc.loadObjectResults();
        builder.add(new String[] { res.get(1).get("inode").toString(), "inode", "folder" });

        //Create 2 Links, and save the inode and the identifier of them
        new LinkDataGen().nextPersisted();
        new LinkDataGen().nextPersisted();
		dc.setSQL(GET_ID_LINKS, 2);
		res = dc.loadObjectResults();
		//builder.add(new String[] { res.get(1).get("identifier").toString(), "identifier", "links" });

        dc.setSQL(GET_INODE, 2);
        dc.addParam("links");
        res = dc.loadObjectResults();
        //builder.add(new String[] { res.get(1).get("inode").toString(), "inode", "links" });

        //Create 2 Templates, and save the inode and the identifier of them
        new TemplateDataGen().nextPersisted();
        new TemplateDataGen().nextPersisted();
		dc.setSQL(GET_ID_TEMPLATES, 2);
		res = dc.loadObjectResults();
		//builder.add(new String[] { res.get(1).get("identifier").toString(), "identifier", "template" });

		dc.setSQL(GET_INODE, 2);
		dc.addParam("template");
		res = dc.loadObjectResults();
		//builder.add(new String[] { res.get(1).get("inode").toString(), "inode", "template" });

        //Create 2 Categories, and save the inode of them
        new CategoryDataGen().nextPersisted();
        new CategoryDataGen().nextPersisted();
		dc.setSQL(GET_INODE, 2);
		dc.addParam("category");
		res = dc.loadObjectResults();
		//builder.add(new String[] { res.get(1).get("inode").toString(), "inode", "category" });

        //Create 2 Fields in a new Content Type, and save the inode of them
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        new FieldDataGen().contentTypeId(contentType.id()).nextPersisted();
        new FieldDataGen().contentTypeId(contentType.id()).nextPersisted();
		dc.setSQL(GET_INODE, 2);
		dc.addParam("field");
		res = dc.loadObjectResults();
		//builder.add(new String[] { res.get(1).get("inode").toString(), "inode", "field" });

        //Create 2 Relationships, and save the inode of them
        final Relationship relationship1 = new RelationshipDataGen(true).nextPersisted();
        final Relationship relationship2 = new RelationshipDataGen(true).nextPersisted();
		//builder.add(new String[] { relationship2.getInode(), "inode", "relationship" });
		expectedIds = builder.build();
	}

	@DataProvider
    public static Object[] dataProviderStringToShortify(){
        return new Tuple2[]{
                //String To Shorty, expected
                new Tuple2("",""),
                new Tuple2("1234","1234"),
                new Tuple2("12345","12345"),
                new Tuple2("12345-6","123456"),
                new Tuple2("12345-67","1234567"),
                new Tuple2("12345-678","12345678"),
                new Tuple2("12345-6789","123456789"),
                new Tuple2("12345-6789-0","1234567890"),
                new Tuple2("12345-6789-01","1234567890"),
                new Tuple2("12345-6789-012","1234567890"),
                new Tuple2("12345-6789-011-12","1234567890"),

        };
    }

    /**
     * Method to test: {@link ShortyIdAPI#shortify(String)}
     * Given Scenario: Shortify the String (will trim the - and leave it at 10 char)
     * ExpectedResult: success all shortify
     */
    @Test
    @UseDataProvider("dataProviderStringToShortify")
    public void test_shortify(final Tuple2<String,String> testCase) {
        Assert.assertEquals(shortyIdAPI.shortify(testCase._1), testCase._2);
    }

    /**
     * Given Scenario: For each of datatypes in the expectedUUIDs, shortify the UUID and then
     *                  get {@link ShortyId} using the uuid-shorty as key.
     * ExpectedResult: should get the same object(full uuid, type and subType), regardless using the uuid shorty
     */
    @Test
    public void testShortyLookup() {
        for (final String[] values : expectedIds) {
            final String[] val = values;
            final String uuidShorty = shortyIdAPI.shortify(val[0]);
            final ShortType shortType = ShortType.fromString(val[1]);
            final ShortType shortSubType = ShortType.fromString(val[2]);
            final Optional<ShortyId> opt = shortyIdAPI.getShorty(uuidShorty);

            assertTrue(opt.isPresent());

            final ShortyId shorty = opt.get();
            assertEquals(val[0], shorty.longId);
            assertEquals(shortType, shorty.type);
            assertEquals(shortSubType, shorty.subType);
        }
    }

    /**
     * Given Scenario: For each of datatypes in the expectedUUIDs, shortify the UUID and then
     *                  call getShorty using the uuid-shorty as key (do this a few times), after the first
     *                  time should be calling the cache instead of the DB, check the dbHits value it shouldn't
     *                  change.
     * ExpectedResult: DBhits should be the same for the first time than for the 4th time, since should be
     *                  using cache.
     */
    @Test
    public void testShortyCache() {
        // Call get to load Cache
        for (final String[] values : expectedIds) {
            final String key = shortyIdAPI.shortify(values[0]);
            shortyIdAPI.getShorty(key);
        }

        //Get Hits to the DB
        final long dbHits = shortyIdAPI.getDbHits();

        //Hit the getShorty a few more times, to check if is hitting cache or DB
        for (int i = 0; i < 3; i++) {
            for (final String[] values : expectedIds) {
                final String key = shortyIdAPI.shortify(values[0]);
                shortyIdAPI.getShorty(key);
            }
        }

        //Get Hits to the DB
        final long dbHits2 = shortyIdAPI.getDbHits();

        //Check that the amount of hits to the DB has not increase, since it should be hitting the cache
        assertEquals(dbHits,dbHits2);
    }

    @DataProvider
    public static Object[] dataProviderValidShorty(){
        return new Tuple2[]{
                //String to check if is valid, is valid?
                new Tuple2("asdd-1234-asdasda-asda",true),
                new Tuple2("asd87-234-214",true),
                new Tuple2("asd87-234:-251",false),
                new Tuple2("asd87-234;-251",false),
                new Tuple2("asd87-234\"-251",false),
                new Tuple2("asd87-234*-251",false),
                new Tuple2("asd87_232_251",false),
                new Tuple2(null,false),
                new Tuple2("asd81",false)
        };
    }

    /**
     * Given Scenario: Check that the UUID are valid to shorties
     * ExpectedResult: Some characters are not valid in shorties.
     */
    @Test
    @UseDataProvider("dataProviderValidShorty")
    public void testValidShorty(final Tuple2<String,Boolean> testCase) {
            try {
                shortyIdAPI.validShorty(testCase._1);
                assertTrue(testCase._2);
            } catch (Exception e) {
                if (e instanceof ShortyException) {
                    assertFalse(testCase._2);
                } else {
                    assertFalse("Another exception was thrown: " + e.getMessage() , true);
                }
            }
    }

    /**
     * Given Scenario: Check that is calling the Cache for 404 Shorties
     * ExpectedResult: DBhits should be the same for the first time than for the 4th time, since should be
     *                  using cache.
     */
    @Test
    public void test404Cache() {
        // Call get to load Cache
        for (final String key : fourOhFours) {
            shortyIdAPI.getShorty(key);
        }

        //Get Hits to the DB
        final long dbHits = shortyIdAPI.getDbHits();

        //Hit the getShorty a few more times, to check if is hitting cache or DB
        for (int i = 0; i < 3; i++) {
            for (final String key : fourOhFours) {
                shortyIdAPI.getShorty(key);
            }
        }

        //Get Hits to the DB
        final long dbHits2 = shortyIdAPI.getDbHits();

        //Check that the amount of hits to the DB has not increase, since it should be hitting the cache
        assertEquals(dbHits,dbHits2);
    }

    /**
     * Given Scenario: Hit the getShorty to get 404 cache with an UUID that not belong to any content, link the UUID
     *                  to a content and hit again the getShorty method with the same UUID.
     * ExpectedResult: the first time we hit we should get an empty value, after linking the UUID we should the ShortyId
     */
    @Test
    public void test404CacheInvalidation() throws Exception{

        final String uuid = UUIDGenerator.generateUuid();
        final String uuidShorty = shortyIdAPI.shortify(uuid);
        assertFalse(shortyIdAPI.getShorty(uuid).isPresent());
        assertFalse(shortyIdAPI.getShorty(uuidShorty).isPresent());

        final Contentlet contentlet = TestDataUtils.getGenericContentContent(false,
                APILocator.getLanguageAPI().getDefaultLanguage().getId());
        APILocator.getIdentifierAPI().createNew(contentlet, new SiteDataGen().nextPersisted(), uuid);

        assertTrue(shortyIdAPI.getShorty(uuid).isPresent());
        assertTrue(shortyIdAPI.getShorty(uuidShorty).isPresent());

    }

    /**
     * Given Scenario: Modify the MINIMUM_SHORTY_ID_LENGTH property to create a longer Shorty
     * ExpectedResult: shorty with the length of the MINIMUM_SHORTY_ID_LENGTH should be created.
     */
    @Test
    public void test_shortify_modify_MINIMUM_SHORTY_ID_LENGTH_success() {

        final String uuid = UUIDGenerator.generateUuid();
        final int defaultMINIMUM_SHORTY_ID_LENGTH = ShortyIdAPIImpl.MINIMUM_SHORTY_ID_LENGTH;

        ShortyIdAPIImpl.MINIMUM_SHORTY_ID_LENGTH = 15;
        String uuidShorty = shortyIdAPI.shortify(uuid);
        assertTrue("Length: " + uuidShorty.length(),uuidShorty.length()==15);
        ShortyIdAPIImpl.MINIMUM_SHORTY_ID_LENGTH = defaultMINIMUM_SHORTY_ID_LENGTH;
    }
}
