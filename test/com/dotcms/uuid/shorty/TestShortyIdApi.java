package com.dotcms.uuid.shorty;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.repackage.org.apache.commons.lang.RandomStringUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.ConfigTestHelper;

public class TestShortyIdApi {

    List<String[]> expecteds = ImmutableList.<String[]>builder()
            .add(new String[] {"f6b80fab-8671-4764-af0d-79b716725ffd", "inode", "containers"})
            .add(new String[] {"fc193c82-8c32-4abe-ba8a-49522328c93e", "identifier", "containers"})


            .add(new String[] {"ff9d9f72-3650-4bca-9e0f-6ce18150d51e", "identifier", "contentlet"})
            .add(new String[] {"ffa0b494-cbb2-4634-b747-8795b5995d74", "identifier", "folder"})
            .add(new String[] {"1aeb328b-57b5-46e8-9eec-2cb2b4c24953", "identifier", "htmlpage"})
            .add(new String[] {"b12f30da-0f0c-4376-88a0-a17d1ffe39f9", "identifier", "links"})
            .add(new String[] {"fdb3f906-e9c4-46c4-b7e4-148201271d04", "identifier", "template"})



            .add(new String[] {"ffe266d8-0683-45f3-9b08-3d7804ae3280", "inode", "contentlet"})
            .add(new String[] {"975964f8-ef1f-451a-aa23-10a44c39ce98", "inode", "htmlpage"})
            .add(new String[] {"fe654925-f011-487c-b5db-d7cb4ed2553a", "inode", "template"})

            .add(new String[] {"ede13663-ff06-4d01-ab99-a8976e995010", "inode", "category"})
            .add(new String[] {"fff86d86-7908-4922-aaed-a3e8b0c6aae4", "inode", "field"})
            .add(new String[] {"008dab22-8bc3-4eb2-93a0-79e3d5d0a4ab", "inode", "folder"})
            .add(new String[] {"b1bb57cf-6f98-486d-a3b9-83f96d3dab22", "inode", "links"})
            .add(new String[] {"a2446e0c-0e8f-4308-8b17-9d0e907aaff5", "inode", "relationship"})
            .add(new String[] {"868c07ca-c2af-4668-8fe8-75d5b48bd6a5", "inode", "virtual_link"})



            .build();


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
        new com.dotmarketing.util.TestingJndiDatasource().init();
        ConfigTestHelper._setupFakeTestingContext();
        APILocator.getBundleAPI();
        CacheLocator.init();

        for (int i = 0; i < 10; i++) {
            fourOhFours.add(RandomStringUtils.randomAlphanumeric(ShortyIdAPIImpl.MINIMUM_SHORTY_ID_LENGTH));
        }
    }

    @Before
    public void setUp() throws Exception {}

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
                for (String[] values : expecteds) {
                    val = values;
                    key = val[0].substring(0, ShortyIdAPIImpl.MINIMUM_SHORTY_ID_LENGTH);
                    shortType = ShortType.fromString(val[1]);
                    shortSubType = ShortType.fromString(val[2]);
                    Optional<ShortyId> opt = APILocator.getShortyAPI().getShorty(key);
                    shorty = opt.get();

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
        for (String[] values : expecteds) {
            val = values;
            String key = val[0].substring(0, ShortyIdAPIImpl.MINIMUM_SHORTY_ID_LENGTH);
            Optional<ShortyId> opt = api.getShorty(key);
        }

        long dbQueries = api.getDbHits();

        for (int i = 0; i < 10; i++) {

            for (String[] values : expecteds) {
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
        for (String[] values : expecteds) {
            val = values;
            String key = val[0].substring(0, ShortyIdAPIImpl.MINIMUM_SHORTY_ID_LENGTH);
            Optional<ShortyId> opt = api.getShorty(key);
        }


        long dbQueries3 = api.getDbHits();
        assert (dbQueries3 == dbQueries2 + expecteds.size());
    }



    @Test
    public void testValidShorty() {


        String[] invalids = new String[] {"!", ":", ";", "\"", "'", "*", "_", null};
        String[] valids = new String[] {"-", "a", "4", "9", "A", "Z"};
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

        for (String[] x : expecteds) {
            String noDashes = x[0].replaceAll("-", "");
            String y = api.uuidIfy(noDashes);
            assert (x[0].equals(y));

        }
    }


    @Test
    public void testLongerShorties() {

        ShortyIdAPI api = APILocator.getShortyAPI();
        for (String[] x : expecteds) {
            String noDashes = x[0].replaceAll("-", "");
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
