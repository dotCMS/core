package com.dotcms.tika;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import static org.junit.Assert.*;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.felix.framework.OSGISystem;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * @author nollymar
 */
public class TikaUtilsTest{

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }
    
    
    
    String[] bundles = {
            "plugins/commons-io-2.11.0.jar",
            "plugins/tika-core-2.7.0.jar",
            "plugins/org.osgi.dto-1.1.1.jar",
            "plugins/org.apache.felix.scr-2.2.6.jar",
           // "plugins/slf4j-simple-1.7.25.jar",
            "plugins/slf4j-api-2.0.6.jar",
            "plugins/tika-bundle-standard-2.6.0.jar",

            
    };
    
    
    
    
    @Test
    public void testBundleLoaded() throws Exception {
        
        BundleContext bundlecontext = OSGISystem.getInstance().getFelixFramework().getBundleContext();
        


        for(String bundle : bundles) {
            File f = new File("/Users/will/git/dotcms/tomcat9/webapps/ROOT/WEB-INF/felix-system/" + bundle);
            Bundle myBundle = bundlecontext.installBundle(f.toURL().toString());
            System.out.println("starging:" +f.getName());
            try {
                myBundle.start();
            }
            catch(Throwable t) {
                Logger.error(getClass(), t.getMessage(), t);
            }
            
        }
        
        
        boolean hasCore = false, hasBundle = false;
        for (Bundle b : OSGISystem.getInstance().getBundles()) {
            if ("org.apache.tika.core".equals(b.getSymbolicName())) {
                hasCore = true;
                assertEquals("Core not activated", Bundle.ACTIVE, b.getState());
            }
            if ("org.apache.tika.bundle-standard".equals(b.getSymbolicName())) {
                hasBundle = true;
                assertEquals("Bundle not activated", Bundle.ACTIVE, b.getState());
            }
        }
        assertTrue("Core bundle not found", hasCore);
        assertTrue("Bundle bundle not found", hasBundle);
    }
    
    
    
    
    
    
    
    @Test
    public void testGetConfiguredMetadataFields() throws DotDataException {
        final Set<String>  fields = TikaUtils.getConfiguredMetadataFields();
        Assert.assertNotNull(fields);
        Assert.assertTrue(!fields.isEmpty());
    }

    @Test
    public void test_FilterMetadataFields_WhenMapEmpty_ReturnsEmptyMap() throws DotDataException {
        final Map<String, Object> metaMap = new HashMap<>();
        final Set<String> fields  = new HashSet<>();
        fields.add("width");
        TikaUtils.filterMetadataFields(metaMap, fields);

        Assert.assertNotNull(metaMap);
        Assert.assertTrue(metaMap.isEmpty());
    }

    @Test
    public void test_FilterMetadataFields_WhenFieldsArrayIsEmpty_DoesNotModifyTheMap() throws DotDataException {
        final Map<String, Object> metaMap = new HashMap<>();
        metaMap.put("content", "Test to filter metadata fields");
        metaMap.put("width", "300px");

        TikaUtils.filterMetadataFields(metaMap, null);

        Assert.assertNotNull(metaMap);
        Assert.assertEquals(2, metaMap.size());
        Assert.assertTrue(metaMap.containsKey("width") && metaMap.containsKey("content"));
    }

    @Test
    public void test_FilterMetadataFields_WhenFieldExistsInMap_ReturnsMapWithTheField() throws DotDataException {
        final Set<String> fields  = new HashSet<>();
        final Map<String, Object> metaMap = new HashMap<>();

        fields.add("width");
        fields.add("size");

        metaMap.put("content", "Test to filter metadata fields");
        metaMap.put("width", "300px");
        TikaUtils.filterMetadataFields(metaMap, fields);

        Assert.assertNotNull(metaMap);
        Assert.assertEquals(1, metaMap.size());
        Assert.assertTrue(metaMap.containsKey("width"));
    }

    @Test
    public void test_FilterMetadataFields_WhenFieldMatchesRegex_ReturnsMapWithTheField() throws DotDataException {

        final Map<String, Object> metaMap = new HashMap<>();
        final Set<String> fields  = new HashSet<>();

        fields.add("wid.*");
        fields.add("size");

        metaMap.put("content", "Test to filter metadata fields");
        metaMap.put("width", "300px");
        TikaUtils.filterMetadataFields(metaMap, fields);

        Assert.assertNotNull(metaMap);
        Assert.assertEquals(1, metaMap.size());
        Assert.assertTrue(metaMap.containsKey("width"));
    }

    @Test
    public void test_FilterMetadataFields_WhenFieldIsWildcard_ReturnsMapWithAllFields() throws DotDataException {

        final Map<String, Object> metaMap = new HashMap<>();
        final Set<String> fields  = new HashSet<>();

        fields.add("*");

        metaMap.put("content", "Test to filter metadata fields");
        metaMap.put("width", "300px");
        TikaUtils.filterMetadataFields(metaMap, fields);

        Assert.assertNotNull(metaMap);
        Assert.assertEquals(2, metaMap.size());
        Assert.assertTrue(metaMap.containsKey("content"));
        Assert.assertTrue(metaMap.containsKey("width"));
    }
}
