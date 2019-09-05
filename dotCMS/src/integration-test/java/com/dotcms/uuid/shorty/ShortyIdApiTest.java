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
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UUIDGenerator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.liferay.portal.model.User;

import static org.junit.Assert.assertEquals;

import com.liferay.portal.util.WebKeys;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the Shorty ID API class.
 * 
 * @author 
 * @since Oct 10, 2016
 */
public class ShortyIdApiTest {

    static List<String> fourOhFours = new ArrayList<>();

    /**
     * | fc193c82-8c32-4abe-ba8a-49522328c93e | containers | | ff9d9f72-3650-4bca-9e0f-6ce18150d51e
     * | contentlet | | ffa0b494-cbb2-4634-b747-8795b5995d74 | folder | |
     * 1aeb328b-57b5-46e8-9eec-2cb2b4c24953 | htmlpage | | b12f30da-0f0c-4376-88a0-a17d1ffe39f9 |
     * links | | fdb3f906-e9c4-46c4-b7e4-148201271d04 | template | |
     * fba1b937-5c06-40a0-94c6-a830425d3875 | identifer |
     **/

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    	IntegrationTestInitService.getInstance().init();
        APILocator.getBundleAPI();

        for (int i = 0; i < 10; i++) {
            fourOhFours.add(RandomStringUtils.randomAlphanumeric(ShortyIdAPIImpl.MINIMUM_SHORTY_ID_LENGTH));
        }
    }

    protected List<String[]> expectedIdsFromStarter = null;
    protected List<String[]> expectedIds = null;
    
    final String GET_INODE = "SELECT inode FROM inode WHERE type = ? AND inode <> 'SYSTEM_FOLDER'";
	final String GET_ID_CONTAINERS = "SELECT identifier FROM dot_containers";
    final String GET_ID_CONTENTLETS = "SELECT identifier FROM contentlet WHERE identifier <> 'SYSTEM_HOST'";
	final String GET_ID_LINKS = "SELECT identifier FROM links";
	final String GET_ID_TEMPLATES = "SELECT identifier FROM template";
	final String GET_ID_FOLDERS = "SELECT identifier FROM folder";
    
    @Before
    public void setUp() throws Exception {
    	getExpectedIds();
    	getExpectedIdsFromStarter();
    }

    /**
	 * This utility method reads actual data from the DB in order to get valid
	 * information for testing purposes.
	 * 
	 * @throws DotDataException
	 *             An error occurred when reading the test data.
	 */
    private void getExpectedIds() throws DotDataException, DotSecurityException {

        final ContentType contentGenericType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find("webPageContent");

		final DotConnect dc = new DotConnect();
		Builder<String[]> builder = ImmutableList.<String[]>builder();

        new ContainerDataGen().nextPersisted();
        new ContainerDataGen().nextPersisted();
		dc.setSQL(GET_INODE, 2);
		dc.addParam("containers");
		List<Map<String, Object>> res = dc.loadObjectResults();
		builder.add(new String[] { res.get(1).get("inode").toString(), "inode", "containers" });
		dc.setSQL(GET_ID_CONTAINERS, 2);
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(1).get("identifier").toString(), "identifier", "containers" });

        new ContentletDataGen(contentGenericType.id())
                .setProperty("title", "TestContent")
                .setProperty("body", "TestBody").nextPersisted();
        new ContentletDataGen(contentGenericType.id())
                .setProperty("title", "TestContent")
                .setProperty("body", "TestBody").nextPersisted();
		dc.setSQL(GET_ID_CONTENTLETS, 2);
		res = dc.loadObjectResults();
        builder.add(new String[] { res.get(1).get("identifier").toString(), "identifier", "contentlet" });

        new FolderDataGen().nextPersisted();
        new FolderDataGen().nextPersisted();
		dc.setSQL(GET_ID_FOLDERS, 2);
		res = dc.loadObjectResults();
        builder.add(new String[] { res.get(1).get("identifier").toString(), "identifier", "folder" });

        new LinkDataGen().nextPersisted();
        new LinkDataGen().nextPersisted();
		dc.setSQL(GET_ID_LINKS, 2);
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(1).get("identifier").toString(), "identifier", "links" });

        new TemplateDataGen().nextPersisted();
        new TemplateDataGen().nextPersisted();
		dc.setSQL(GET_ID_TEMPLATES, 2);
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(1).get("identifier").toString(), "identifier", "template" });
		
		dc.setSQL(GET_INODE, 2);
		dc.addParam("contentlet");
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(1).get("inode").toString(), "inode", "contentlet" });
		dc.setSQL(GET_INODE, 2);
		dc.addParam("template");
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(1).get("inode").toString(), "inode", "template" });

        new CategoryDataGen().nextPersisted();
        new CategoryDataGen().nextPersisted();
		dc.setSQL(GET_INODE, 2);
		dc.addParam("category");
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(1).get("inode").toString(), "inode", "category" });

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        new FieldDataGen().contentTypeId(contentType.id()).nextPersisted();
        new FieldDataGen().contentTypeId(contentType.id()).nextPersisted();
		dc.setSQL(GET_INODE, 2);
		dc.addParam("field");
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(1).get("inode").toString(), "inode", "field" });

        new FolderDataGen().nextPersisted();
        new FolderDataGen().nextPersisted();
		dc.setSQL(GET_INODE, 2);
		dc.addParam("folder");
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(1).get("inode").toString(), "inode", "folder" });
		dc.setSQL(GET_INODE, 2);
		dc.addParam("links");
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(1).get("inode").toString(), "inode", "links" });

        new RelationshipDataGen(true).nextPersisted();
        new RelationshipDataGen(true).nextPersisted();
		dc.setSQL(GET_INODE, 2);
		dc.addParam("relationship");
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(1).get("inode").toString(), "inode", "relationship" });
		this.expectedIds = builder.build();
	}

	/**
	 * This utility method reads actual data from the DB in order to get valid
	 * information for testing purposes.
	 * 
	 * @throws DotDataException
	 *             An error occurred when reading the test data.
	 */
    private void getExpectedIdsFromStarter() throws DotDataException, DotSecurityException {

        final ContentType contentGenericType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find("webPageContent");

		final DotConnect dc = new DotConnect();
		Builder<String[]> builder = ImmutableList.<String[]>builder();

        new ContainerDataGen().nextPersisted();
        new ContainerDataGen().nextPersisted();
		dc.setSQL(GET_INODE, 1);
		dc.addParam("containers");
		List<Map<String, Object>> res = dc.loadObjectResults();
		builder.add(new String[] { res.get(0).get("inode").toString(), "inode", "containers" });
		dc.setSQL(GET_ID_CONTAINERS, 1);
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(0).get("identifier").toString(), "identifier", "containers" });

        new ContentletDataGen(contentGenericType.id())
                .setProperty("title", "TestContent")
                .setProperty("body", "TestBody").nextPersisted();
        new ContentletDataGen(contentGenericType.id())
                .setProperty("title", "TestContent")
                .setProperty("body", "TestBody").nextPersisted();
        dc.setSQL(GET_ID_CONTENTLETS, 2);
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(0).get("identifier").toString(), "identifier", "contentlet" });
		builder.add(new String[] { res.get(1).get("identifier").toString(), "identifier", "contentlet" });

		dc.setSQL(GET_INODE, 2);
		dc.addParam("contentlet");
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(0).get("inode").toString(), "inode", "contentlet" });
		builder.add(new String[] { res.get(1).get("inode").toString(), "inode", "contentlet" });

        new FolderDataGen().nextPersisted();
        new FolderDataGen().nextPersisted();
        dc.setSQL(GET_INODE, 2);
		dc.addParam("folder");
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(0).get("inode").toString(), "inode", "folder" });
		builder.add(new String[] { res.get(1).get("inode").toString(), "inode", "folder" });

        new LinkDataGen().nextPersisted();
        new LinkDataGen().nextPersisted();
		dc.setSQL(GET_INODE, 1);
		dc.addParam("links");
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(0).get("inode").toString(), "inode", "links" });
		dc.setSQL(GET_ID_LINKS, 1);
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(0).get("identifier").toString(), "identifier", "links" });

        new TemplateDataGen().nextPersisted();
        new TemplateDataGen().nextPersisted();
		dc.setSQL(GET_ID_TEMPLATES, 1);
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(0).get("identifier").toString(), "identifier", "template" });
		dc.setSQL(GET_INODE, 1);
		dc.addParam("template");
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(0).get("inode").toString(), "inode", "template" });
		this.expectedIdsFromStarter = builder.build();
	}

    @Test
    public void testShortify() {

	    final String nullString = null;
        Assert.assertEquals(APILocator.getShortyAPI().shortify(nullString), nullString);

        final String emptyString = "";
        Assert.assertEquals(APILocator.getShortyAPI().shortify(emptyString), emptyString);

        final String emptyString2 = "            ";
        Assert.assertEquals(APILocator.getShortyAPI().shortify(emptyString2), emptyString2);

        final String shortId4 = "1234";
        Assert.assertEquals(APILocator.getShortyAPI().shortify(shortId4), shortId4);

        final String shortId5 = "12345";
        Assert.assertEquals(APILocator.getShortyAPI().shortify(shortId5), shortId5);

        final String shortId6 = "12345-6";
        Assert.assertEquals(APILocator.getShortyAPI().shortify(shortId6), "123456");

        final String shortId7 = "12345-67";
        Assert.assertEquals(APILocator.getShortyAPI().shortify(shortId7), "1234567");

        final String shortId10 = "12345-6789-0";
        Assert.assertEquals(APILocator.getShortyAPI().shortify(shortId10), "1234567890");

        final String shortId12 = "12345-6789-012";
        Assert.assertEquals(APILocator.getShortyAPI().shortify(shortId12), "1234567890");

        final String shortId14 = "12345-6789-012-11";
        Assert.assertEquals(APILocator.getShortyAPI().shortify(shortId14), "1234567890");
    }

	@Test
    public void testShortyLookup() {
        ShortyId shorty = null;
        ShortType shortType = null;
        ShortType shortSubType = null;
        String[] val = null;
        ShortyIdAPI api = APILocator.getShortyAPI();
        String key = null;
        // run 10 times
        for (int i = 0; i < 10; i++) {
            try {
                for (String[] values : expectedIdsFromStarter) {
                    val = values;
                    key = api.shortify(val[0]);
                    shortType = ShortType.fromString(val[1]);
                    shortSubType = ShortType.fromString(val[2]);
                    Optional<ShortyId> opt = APILocator.getShortyAPI().getShorty(key);
                    shorty = opt.get();

                    System.out.println(val[0] + " == " + shorty.longId);
                    System.out.println(shortType + " == " + shorty.type);
                    System.out.println(shortSubType + " == " + shorty.subType);

                    assert (shorty.longId.equals(val[0]));
                    assert (shortType == shorty.type);
                    assert (shortSubType == shorty.subType);
                }
            } catch (Throwable t) {

                System.out.println("val[0]:" + val[0]);
                System.out.println("val[1]:" + val[1]);
                System.out.println("val[2]:" + val[2]);


                System.out.println("bad shorty:" + key);
                System.out.println("looking shortType:" + shortType);
                System.out.println("looking shortSubType:" + shortSubType);
                throw t;
            }
        }
    }

    @Test
    public void testShortyCache() {
        ShortyIdAPI api = APILocator.getShortyAPI();

        String[] val = null;

        // load cache
        for (String[] values : expectedIds) {
            val = values;
            String key = api.shortify(val[0]);
            Optional<ShortyId> opt = api.getShorty(key);
        }

        long dbQueries = api.getDbHits();

        for (int i = 0; i < 10; i++) {

            for (String[] values : expectedIds) {
                val = values;
                String key = api.shortify(val[0]);
                Optional<ShortyId> opt = api.getShorty(key);
            }
        }

        // test that we are loading from cache
        long dbQueries2 = api.getDbHits();
        assert (dbQueries2 == dbQueries);

        CacheLocator.getCacheAdministrator().flushAll();

        // load cache
        for (String[] values : expectedIds) {
            val = values;
            String key = api.shortify(val[0]);
            Optional<ShortyId> opt = api.getShorty(key);
        }

        long dbQueries3 = api.getDbHits();
        assert (dbQueries3 == dbQueries2 + expectedIds.size());
    }

    @Test
    public void testValidShorty() {

        String[] invalids = new String[] {"!", ":", ";", "\"", "'", "*", "_", null};
        ShortyIdAPI api = APILocator.getShortyAPI();
        int runs = 10;

        for (String x : invalids) {
            try {
                api.validShorty(x);
                assert (false);
            } catch (Exception e) {
                if (e instanceof ShortyException) {
                    assert (true);
                } else {
                    assert (false);
                }

            }
        }

        for (int i = 0; i < runs; i++) {
            String x = RandomStringUtils.randomAlphanumeric(ShortyIdAPIImpl.MINIMUM_SHORTY_ID_LENGTH);
            try {
                api.validShorty(x);
                assert (true);
            } catch (Exception e) {
                assert (false);
            }
        }
    }

    @Test
    public void test404Cache() {
        ShortyIdAPI api = APILocator.getShortyAPI();

        // load cache
        for (String key : fourOhFours) {
            Optional<ShortyId> opt = api.getShorty(key);
        }

        long dbQueries = api.getDbHits();

        for (int i = 0; i < 10; i++) {

            for (String key : fourOhFours) {
                Optional<ShortyId> opt = api.getShorty(key);
            }
        }

        // test that we are loading from cache
        long dbQueries2 = api.getDbHits();
        assert (dbQueries2 == dbQueries);

        CacheLocator.getCacheAdministrator().flushAll();

        for (String key : fourOhFours) {
            Optional<ShortyId> opt = api.getShorty(key);
        }

        long dbQueries3 = api.getDbHits();
        assert (dbQueries3 == dbQueries2 + fourOhFours.size());
    }

    @Test
    public void testIdentifier404CacheInvalidation() throws Exception{
        
        
        String uuid = UUIDGenerator.generateUuid();
        String testUuid = uuid.replace("-","");
        ShortyIdAPI api = APILocator.getShortyAPI();
        assert(!api.getShorty(uuid).isPresent());
        assert(!api.getShorty(testUuid).isPresent());
        
        for(int i=testUuid.length();i>ShortyIdAPIImpl.MINIMUM_SHORTY_ID_LENGTH;i--) {
            String test=testUuid.substring(0, i);
            assert(!api.getShorty(test).isPresent());
        }
        
        
        

        ContentType type = new ContentTypeDataGen().nextPersisted();
        Contentlet con = new ContentletDataGen(type.id()).next();
        con.setInode(UUIDGenerator.generateUuid());
        Host host =APILocator.systemHost();
        Identifier id = APILocator.getIdentifierAPI().createNew(con, host, uuid);
        
        
        assert(api.getShorty(uuid).isPresent());
        assert(api.getShorty(testUuid).isPresent());
        
        for(int i=testUuid.length();i>ShortyIdAPIImpl.MINIMUM_SHORTY_ID_LENGTH;i--) {
            String test=testUuid.substring(0, i);
            assert(api.getShorty(test).isPresent());
        }
        
    }
    
    @Test
    public void testContentlet404CacheInvalidation() throws Exception{
        
        
        String uuid = UUIDGenerator.generateUuid();
        String testUuid = uuid.replace("-","");
        ShortyIdAPI api = APILocator.getShortyAPI();
        assert(!api.getShorty(uuid).isPresent());
        assert(!api.getShorty(testUuid).isPresent());
        
        for(int i=testUuid.length();i>ShortyIdAPIImpl.MINIMUM_SHORTY_ID_LENGTH;i--) {
            String test=testUuid.substring(0, i);
            assert(!api.getShorty(test).isPresent());
        }
        
        
        

        ContentType type = new ContentTypeDataGen().nextPersisted();
        Contentlet con = new ContentletDataGen(type.id()).next();
        con.setInode(uuid);
        con.setProperty(Contentlet.DONT_VALIDATE_ME, true);
        con = APILocator.getContentletAPI().checkin(con, APILocator.systemUser(), false);

        assert(con.getInode().equals(uuid));
        assert(api.getShorty(uuid).isPresent());
        assert(api.getShorty(testUuid).isPresent());
        
        for(int i=testUuid.length();i>ShortyIdAPIImpl.MINIMUM_SHORTY_ID_LENGTH;i--) {
            String test=testUuid.substring(0, i);
            assert(api.getShorty(test).isPresent());
        }
        
    }
    
    
    
    @Test
    public void testUuidIfy() {

        ShortyIdAPI api = APILocator.getShortyAPI();
        for (String x : fourOhFours) {
            String y = api.uuidIfy(x);
            assert (y.indexOf('-') == 8);
        }

        for (String[] x : expectedIds) {
            String noDashes = x[0].replaceAll("-", "");
            String y = api.uuidIfy(noDashes);
            assert (x[0].equals(y));
        }
    }

    @Test
    public void testLongerShorties() {

        ShortyIdAPI api = APILocator.getShortyAPI();
        for (String[] expected : expectedIdsFromStarter) {
            String noDashes = expected[0].replaceAll("-", "");
            for ( int i = ShortyIdAPIImpl.MINIMUM_SHORTY_ID_LENGTH; i < 30; i++ ) {
                String key = noDashes.substring(0, i);
                Optional<ShortyId> opt = APILocator.getShortyAPI().getShorty(key);
                try {
                    assert (opt.isPresent());
                } catch (Throwable t) {
                    System.out.println("key is empty:" + key);
                    throw t;
                }
            }
        }
    }
    
    @Test
    public void testTempShorties() throws DotSecurityException, IOException {

        ShortyIdAPI api = APILocator.getShortyAPI();
        User systemUser = APILocator.systemUser();
        String testingFileName = "TESTING.PNG";
        final HttpServletRequest request = new MockHttpRequest("localhost", "/api/v1/tempResource").request();
        request.setAttribute(WebKeys.USER,systemUser);
        DotTempFile temp =  APILocator.getTempFileAPI().createEmptyTempFile(testingFileName,request);

        new FileOutputStream(temp.file).close();
        assertEquals(temp.id, api.shortify(temp.id));
        
        
        ShortyId shorty = api.getShorty(temp.id).get();
        assertEquals(temp.id, shorty.longId);
        assertEquals(temp.id, shorty.shortId);
        assert(ShortType.TEMP_FILE == shorty.type);
        assert(ShortType.TEMP_FILE == shorty.subType);
    }
    
    

}
