package com.dotcms.publishing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

public class BundlerUtilTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }


    /**
     * <b>Method to test:</b> {@link BundlerUtil#jsonToObject(File, TypeReference)}<br></br>
     * <b>Given Scenario:</b> A map is serialized to JSON. Then, the JSON was deserialized and results compared<br></br>
     * <b>ExpectedResult:</b> The original map is returned after the serialization/deserialization process
     * @throws IOException
     */
    @Test
    public void test_jsonToObject_when_typeReference() throws IOException {
        final Map<String, Integer> myInitialMap = Map.of("firstItem", 1, "secondItem", 2);

        //The collection is serialized to a JSON file
        final File tempFile = File.createTempFile("testJsonToObject", ".json");
        BundlerUtil.writeObject(myInitialMap, tempFile);

        //The collection is deserialized
        final Map<String, Integer> result = BundlerUtil.jsonToObject(tempFile, new TypeReference<>() {});
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1, result.get("firstItem").intValue());
        assertEquals(2, result.get("secondItem").intValue());
    }


    /**
     * <b>Method to test:</b> {@link BundlerUtil#jsonToObject(File, TypeReference)}<br></br>
     * <b>Given Scenario:</b> A map is serialized to JSON. Then, the JSON was deserialized and results
     * compared<br></br>
     * <b>ExpectedResult:</b> The original map is returned after the serialization/deserialization process
     *
     * @throws IOException
     */
    @Test
    public void test_xml_to_object() throws IOException {
        final Map<String, Integer> myInitialMap = Map.of("firstItem", 1, "secondItem", 2);

        //The collection is serialized to a JSON file
        final File tempFile = File.createTempFile("testJsonToObject", ".xml");
        BundlerUtil.writeObject(myInitialMap, tempFile);

        //The collection is deserialized
        final Map<String, Integer> result = BundlerUtil.readObject(tempFile, Map.class);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1, result.get("firstItem").intValue());
        assertEquals(2, result.get("secondItem").intValue());
    }

}
