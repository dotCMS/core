package com.dotcms.contenttype.business.uniquefields.extratable;

import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.StringUtils;
import net.bytebuddy.utility.RandomString;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.CONTENTLET_IDS_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.CONTENT_TYPE_ID_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.FIELD_VALUE_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.FIELD_VARIABLE_NAME_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.LANGUAGE_ID_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.SITE_ID_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.UNIQUE_PER_SITE_ATTR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UniqueFieldDataBaseUtilTest {

    @BeforeClass
    //TODO: Remove this when the whole change is done
    public static void init (){
        try {
            new DotConnect().setSQL("CREATE TABLE IF NOT EXISTS unique_fields (" +
                    "unique_key_val VARCHAR(64) PRIMARY KEY," +
                    "supporting_values JSONB" +
                    " )").loadObjectResults();
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Method to test: {@link UniqueFieldDataBaseUtil#insertWithHash(String, Map)}
     * When: Called the method with the right parameters
     * Should: Insert a register in the unique_fields table
     */
    @Test
    public void insert() throws DotDataException, IOException {
        final RandomString randomStringGenerator = new RandomString();

        final String hash = StringUtils.hashText("This is a test " + System.currentTimeMillis());

        final Map<String, Object> supportingValues = Map.of(
                CONTENT_TYPE_ID_ATTR, randomStringGenerator.nextString(),
                FIELD_VARIABLE_NAME_ATTR, randomStringGenerator.nextString(),
                FIELD_VALUE_ATTR, randomStringGenerator.nextString(),
                LANGUAGE_ID_ATTR, randomStringGenerator.nextString(),
                SITE_ID_ATTR, randomStringGenerator.nextString(),
                UNIQUE_PER_SITE_ATTR, true,
                CONTENTLET_IDS_ATTR, CollectionsUtils.list( randomStringGenerator.nextString() )
        );

        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();

        uniqueFieldDataBaseUtil.insertWithHash(hash, supportingValues);

        final List<Map<String, Object>> results = new DotConnect().setSQL("SELECT * FROM unique_fields WHERE unique_key_val = ?")
                .addParam(hash).loadObjectResults();

        assertFalse(results.isEmpty());

        final List<Map<String, Object>> hashResults = results.stream()
                .filter(result -> result.get("unique_key_val").equals(hash))
                .collect(Collectors.toList());

        assertEquals(1, hashResults.size());
        assertEquals(supportingValues, JsonUtil.getJsonFromString(hashResults.get(0).get("supporting_values").toString()));
    }

    /**
     * Method to test: {@link UniqueFieldDataBaseUtil#insertWithHash(String, Map)}
     * When: Called the method with a 'unique_key_val' duplicated
     * Should: Throw a {@link java.sql.SQLException}
     */
    @Test
    public void tryToInsertDuplicated() throws DotDataException {
        final RandomString randomStringGenerator = new RandomString();

        final String hash = StringUtils.hashText("This is a test " + System.currentTimeMillis());

        final Map<String, Object> supportingValues_1 = Map.of(
                CONTENT_TYPE_ID_ATTR, randomStringGenerator.nextString(),
                FIELD_VARIABLE_NAME_ATTR, randomStringGenerator.nextString(),
                FIELD_VALUE_ATTR, randomStringGenerator.nextString(),
                LANGUAGE_ID_ATTR, randomStringGenerator.nextString(),
                SITE_ID_ATTR, randomStringGenerator.nextString(),
                UNIQUE_PER_SITE_ATTR, true,
                CONTENTLET_IDS_ATTR, "['" + randomStringGenerator.nextString() + "']"
        );

        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();
        uniqueFieldDataBaseUtil.insertWithHash(hash, supportingValues_1);

        final Map<String, Object> supportingValues_2 = Map.of(
                CONTENT_TYPE_ID_ATTR, randomStringGenerator.nextString(),
                FIELD_VARIABLE_NAME_ATTR, randomStringGenerator.nextString(),
                FIELD_VALUE_ATTR, randomStringGenerator.nextString(),
                LANGUAGE_ID_ATTR, randomStringGenerator.nextString(),
                SITE_ID_ATTR, randomStringGenerator.nextString(),
                UNIQUE_PER_SITE_ATTR, true,
                CONTENTLET_IDS_ATTR, CollectionsUtils.list( randomStringGenerator.nextString())
        );

        try {
            uniqueFieldDataBaseUtil.insertWithHash(hash, supportingValues_2);

            throw new AssertionError("Exception expected");
        } catch (DotDataException e) {
            assertTrue(e.getMessage().startsWith("ERROR: duplicate key value violates unique constraint \"unique_fields_pkey\""));
        }
    }


}
