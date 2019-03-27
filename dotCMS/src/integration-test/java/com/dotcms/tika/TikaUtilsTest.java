package com.dotcms.tika;

import com.dotmarketing.exception.DotDataException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author nollymar
 */
public class TikaUtilsTest{

    @Test
    public void testGetConfiguredMetadataFields() throws DotDataException {
        final TikaUtils tikaUtils = new TikaUtils();
        final Set<String>  fields = tikaUtils.getConfiguredMetadataFields();

        Assert.assertNotNull(fields);
        Assert.assertTrue(!fields.isEmpty());
    }

    @Test
    public void test_FilterMetadataFields_WhenMapEmpty_ReturnsEmptyMap() throws DotDataException {
        final TikaUtils tikaUtils = new TikaUtils();
        final Map<String, Object> metaMap = new HashMap<>();
        final Set<String> fields  = new HashSet<>();
        fields.add("width");
        tikaUtils.filterMetadataFields(metaMap, fields);

        Assert.assertNotNull(metaMap);
        Assert.assertTrue(metaMap.isEmpty());
    }

    @Test
    public void test_FilterMetadataFields_WhenFieldsArrayIsEmpty_DoesNotModifyTheMap() throws DotDataException {
        final TikaUtils tikaUtils = new TikaUtils();
        final Map<String, Object> metaMap = new HashMap<>();

        metaMap.put("content", "Test to filter metadata fields");
        metaMap.put("width", "300px");

        tikaUtils.filterMetadataFields(metaMap, null);

        Assert.assertNotNull(metaMap);
        Assert.assertTrue(metaMap.size() == 2);
        Assert.assertTrue(metaMap.containsKey("width") && metaMap.containsKey("content"));
    }

    @Test
    public void test_FilterMetadataFields_WhenFieldExistsInMap_ReturnsMapWithTheField() throws DotDataException {
        final TikaUtils tikaUtils = new TikaUtils();
        final Set<String> fields  = new HashSet<>();
        final Map<String, Object> metaMap = new HashMap<>();

        fields.add("width");
        fields.add("size");

        metaMap.put("content", "Test to filter metadata fields");
        metaMap.put("width", "300px");
        tikaUtils.filterMetadataFields(metaMap, fields);

        Assert.assertNotNull(metaMap);
        Assert.assertTrue(metaMap.size() == 1);
        Assert.assertTrue(metaMap.containsKey("width"));
    }

    @Test
    public void test_FilterMetadataFields_WhenFieldMatchesRegex_ReturnsMapWithTheField() throws DotDataException {
        final TikaUtils tikaUtils = new TikaUtils();
        final Map<String, Object> metaMap = new HashMap<>();
        final Set<String> fields  = new HashSet<>();

        fields.add("wid.*");
        fields.add("size");

        metaMap.put("content", "Test to filter metadata fields");
        metaMap.put("width", "300px");
        tikaUtils.filterMetadataFields(metaMap, fields);

        Assert.assertNotNull(metaMap);
        Assert.assertTrue(metaMap.size() == 1);
        Assert.assertTrue(metaMap.containsKey("width"));
    }

    @Test
    public void test_FilterMetadataFields_WhenFieldIsWildcard_ReturnsMapWithAllFields() throws DotDataException {
        final TikaUtils tikaUtils = new TikaUtils();
        final Map<String, Object> metaMap = new HashMap<>();
        final Set<String> fields  = new HashSet<>();

        fields.add("*");

        metaMap.put("content", "Test to filter metadata fields");
        metaMap.put("width", "300px");
        tikaUtils.filterMetadataFields(metaMap, fields);

        Assert.assertNotNull(metaMap);
        Assert.assertTrue(metaMap.size() == 2);
        Assert.assertTrue(metaMap.containsKey("content"));
        Assert.assertTrue(metaMap.containsKey("width"));
    }
}
