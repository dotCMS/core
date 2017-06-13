package com.dotcms.uuid.shorty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.repackage.com.google.common.collect.ImmutableList.Builder;
import com.dotcms.repackage.org.apache.commons.lang.RandomStringUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;

/**
 * Integration tests for the Shorty ID API class.
 * 
 * @author 
 * @since Oct 10, 2016
 */
public class TestShortyIdApi {

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
    
    final String GET_INODE = "SELECT inode FROM inode WHERE type = ?";
	final String GET_ID_CONTAINERS = "SELECT identifier FROM dot_containers";
	final String GET_ID_CONTENTLETS = "SELECT identifier FROM contentlet";
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
	private void getExpectedIds() throws DotDataException {
		final DotConnect dc = new DotConnect();
		Builder<String[]> builder = ImmutableList.<String[]>builder();

		dc.setSQL(GET_INODE, 2);
		dc.addParam("containers");
		List<Map<String, Object>> res = dc.loadObjectResults();
		builder.add(new String[] { res.get(1).get("inode").toString(), "inode", "containers" });
		dc.setSQL(GET_ID_CONTAINERS, 2);
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(1).get("identifier").toString(), "identifier", "containers" });

		dc.setSQL(GET_ID_CONTENTLETS, 2);
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(1).get("identifier").toString(), "identifier", "contentlet" });
		dc.setSQL(GET_ID_FOLDERS, 2);
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(1).get("identifier").toString(), "identifier", "folder" });
		dc.setSQL(GET_ID_LINKS, 2);
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(1).get("identifier").toString(), "identifier", "links" });
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
		
		dc.setSQL(GET_INODE, 2);
		dc.addParam("category");
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(1).get("inode").toString(), "inode", "category" });
		dc.setSQL(GET_INODE, 2);
		dc.addParam("field");
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(1).get("inode").toString(), "inode", "field" });
		dc.setSQL(GET_INODE, 2);
		dc.addParam("folder");
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(1).get("inode").toString(), "inode", "folder" });
		dc.setSQL(GET_INODE, 2);
		dc.addParam("links");
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(1).get("inode").toString(), "inode", "links" });
		dc.setSQL(GET_INODE, 2);
		dc.addParam("relationship");
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(1).get("inode").toString(), "inode", "relationship" });
		dc.setSQL(GET_INODE, 2);
		dc.addParam("virtual_link");
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(1).get("inode").toString(), "inode", "virtual_link" });
		this.expectedIds = builder.build();
	}

	/**
	 * This utility method reads actual data from the DB in order to get valid
	 * information for testing purposes.
	 * 
	 * @throws DotDataException
	 *             An error occurred when reading the test data.
	 */
	private void getExpectedIdsFromStarter() throws DotDataException {
		final DotConnect dc = new DotConnect();
		Builder<String[]> builder = ImmutableList.<String[]>builder();

		dc.setSQL(GET_INODE, 1);
		dc.addParam("containers");
		List<Map<String, Object>> res = dc.loadObjectResults();
		builder.add(new String[] { res.get(0).get("inode").toString(), "inode", "containers" });
		dc.setSQL(GET_ID_CONTAINERS, 1);
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(0).get("identifier").toString(), "identifier", "containers" });

		dc.setSQL(GET_ID_CONTENTLETS, 2);
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(0).get("identifier").toString(), "identifier", "contentlet" });
		builder.add(new String[] { res.get(1).get("identifier").toString(), "identifier", "contentlet" });
		dc.setSQL(GET_INODE, 2);
		dc.addParam("contentlet");
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(0).get("inode").toString(), "inode", "contentlet" });
		builder.add(new String[] { res.get(1).get("inode").toString(), "inode", "contentlet" });

		dc.setSQL(GET_INODE, 2);
		dc.addParam("folder");
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(0).get("inode").toString(), "inode", "folder" });
		builder.add(new String[] { res.get(1).get("inode").toString(), "inode", "folder" });

		dc.setSQL(GET_INODE, 1);
		dc.addParam("links");
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(0).get("inode").toString(), "inode", "links" });
		dc.setSQL(GET_ID_LINKS, 1);
		res = dc.loadObjectResults();
		builder.add(new String[] { res.get(0).get("identifier").toString(), "identifier", "links" });

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
                    key = val[0].substring(0, ShortyIdAPIImpl.MINIMUM_SHORTY_ID_LENGTH);
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
            String key = val[0].substring(0, ShortyIdAPIImpl.MINIMUM_SHORTY_ID_LENGTH);
            Optional<ShortyId> opt = api.getShorty(key);
        }

        long dbQueries = api.getDbHits();

        for (int i = 0; i < 10; i++) {

            for (String[] values : expectedIds) {
                val = values;
                String key = val[0].substring(0, ShortyIdAPIImpl.MINIMUM_SHORTY_ID_LENGTH);
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
            String key = val[0].substring(0, ShortyIdAPIImpl.MINIMUM_SHORTY_ID_LENGTH);
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

}
