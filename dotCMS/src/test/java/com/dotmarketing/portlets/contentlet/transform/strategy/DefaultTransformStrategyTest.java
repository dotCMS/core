package com.dotmarketing.portlets.contentlet.transform.strategy;

import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.BINARIES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.api.APIProvider;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.storage.model.Metadata;
import com.dotmarketing.image.focalpoint.FocalPointAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultTransformStrategyTest {

    private static final String FIELD_VAR = "fileField";
    private static final String META_KEY = FIELD_VAR + "MetaData";

    /**
     * Custom metadata is persisted in the backing map under the {@link Metadata#CUSTOM_PROP_PREFIX}
     * ("dot:") prefix; {@code Metadata.getCustomMeta()} strips that prefix when exposing it.
     */
    private static final String CUSTOM_FOCAL_POINT_KEY =
            Metadata.CUSTOM_PROP_PREFIX + FocalPointAPI.FOCAL_POINT;

    /**
     * Invokes the private {@code addBinaries} method in isolation so the focal-point behavior can be
     * exercised without standing up the rest of the transform pipeline.
     */
    @SuppressWarnings("unchecked")
    private void invokeAddBinaries(final DefaultTransformStrategy strategy,
            final Contentlet contentlet, final Map<String, Object> map) throws Exception {
        final Method addBinaries = DefaultTransformStrategy.class.getDeclaredMethod(
                "addBinaries", Contentlet.class, Map.class, Set.class);
        addBinaries.setAccessible(true);
        addBinaries.invoke(strategy, contentlet, map, Set.of(BINARIES));
    }

    private Contentlet mockContentletWithBinary(final Metadata metadata) throws Exception {
        final Field field = Mockito.mock(BinaryField.class);
        Mockito.when(field.variable()).thenReturn(FIELD_VAR);

        final ContentType contentType = Mockito.mock(ContentType.class);
        Mockito.when(contentType.fields(BinaryField.class)).thenReturn(List.of(field));

        final Contentlet contentlet = Mockito.mock(Contentlet.class);
        Mockito.when(contentlet.getContentType()).thenReturn(contentType);
        Mockito.when(contentlet.isFileAsset()).thenReturn(false);
        Mockito.when(contentlet.getIdentifier()).thenReturn("identifier-1");
        Mockito.when(contentlet.getInode()).thenReturn("inode-1");
        Mockito.when(contentlet.getBinaryMetadata(FIELD_VAR)).thenReturn(metadata);
        return contentlet;
    }

    /**
     * When the binary's custom metadata carries a focal point, it must be surfaced under
     * {@code {field}MetaData.focalPoint} on the REST read path so the image editor can re-seed its
     * marker. Regression coverage for <a href="https://github.com/dotCMS/core/issues/36067">#36067</a>.
     */
    @Test
    public void testAddBinaries_whenCustomMetaHasFocalPoint_surfacesItInMetaDataMap()
            throws Exception {

        final Map<String, Serializable> fieldsMeta = new HashMap<>();
        fieldsMeta.put("name", "image.png");
        fieldsMeta.put(CUSTOM_FOCAL_POINT_KEY, "0.25,0.75");
        final Metadata metadata = new Metadata(FIELD_VAR, fieldsMeta);

        final APIProvider toolBox = Mockito.mock(APIProvider.class);
        final DefaultTransformStrategy strategy = new DefaultTransformStrategy(toolBox);
        final Contentlet contentlet = mockContentletWithBinary(metadata);

        final Map<String, Object> map = new HashMap<>();
        invokeAddBinaries(strategy, contentlet, map);

        assertTrue("Expected the field MetaData entry to be present", map.containsKey(META_KEY));
        @SuppressWarnings("unchecked")
        final Map<String, Serializable> metaMap = (Map<String, Serializable>) map.get(META_KEY);
        assertEquals("Focal point from custom metadata must be exposed under the focalPoint key",
                "0.25,0.75", metaMap.get(FocalPointAPI.FOCAL_POINT));
    }

    /**
     * When the binary has no focal point in its custom metadata, the read path must default the
     * {@code focalPoint} entry to "0.0" rather than omit it, matching the GraphQL/Velocity view.
     */
    @Test
    public void testAddBinaries_whenCustomMetaHasNoFocalPoint_defaultsToZero()
            throws Exception {

        final Map<String, Serializable> fieldsMeta = new HashMap<>();
        fieldsMeta.put("name", "image.png");
        final Metadata metadata = new Metadata(FIELD_VAR, fieldsMeta);

        final APIProvider toolBox = Mockito.mock(APIProvider.class);
        final DefaultTransformStrategy strategy = new DefaultTransformStrategy(toolBox);
        final Contentlet contentlet = mockContentletWithBinary(metadata);

        final Map<String, Object> map = new HashMap<>();
        invokeAddBinaries(strategy, contentlet, map);

        assertTrue("Expected the field MetaData entry to be present", map.containsKey(META_KEY));
        @SuppressWarnings("unchecked")
        final Map<String, Serializable> metaMap = (Map<String, Serializable>) map.get(META_KEY);
        assertEquals("focalPoint must default to 0.0 when absent from custom metadata",
                "0.0", metaMap.get(FocalPointAPI.FOCAL_POINT));
    }
}
