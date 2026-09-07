package com.dotmarketing.portlets.contentlet.transform.strategy;

import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.AVOID_MAP_SUFFIX_FOR_VIEWS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.api.APIProvider;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.mockito.Mockito;

public class BinaryViewStrategyTest {

    /**
     * Regression test for <a href="https://github.com/dotCMS/core/issues/35188">Issue #35188</a>.
     * <p>
     * When a binary field has no uploaded file (metadata is {@code null}), the instance
     * {@code transform} method must NOT write {@code emptyMap()} into the output map under the
     * bare field variable key (the key used when {@code AVOID_MAP_SUFFIX_FOR_VIEWS} is set).
     * <p>
     * Before the fix, {@code emptyMap()} was unconditionally placed in the contentlet's backing
     * map, poisoning the field slot. A subsequent {@code DefaultTransformStrategy.addBinaries}
     * call would then invoke {@code Contentlet.getBinary()}, which tried to cast the
     * {@code emptyMap()} to {@code File}, throwing a {@code ClassCastException} wrapped as
     * {@code DotDataException: null}.
     */
    @Test
    public void testTransform_whenBinaryMetadataIsNull_doesNotPutEmptyMapIntoOutputMap()
            throws Exception {

        final APIProvider toolBox = Mockito.mock(APIProvider.class);
        final BinaryViewStrategy strategy = new BinaryViewStrategy(toolBox);

        final Field field = Mockito.mock(BinaryField.class);
        Mockito.when(field.variable()).thenReturn("productImage");

        final ContentType contentType = Mockito.mock(ContentType.class);
        Mockito.when(contentType.fields(BinaryField.class)).thenReturn(List.of(field));

        final Contentlet contentlet = Mockito.mock(Contentlet.class);
        Mockito.when(contentlet.getContentType()).thenReturn(contentType);
        Mockito.when(contentlet.getBinaryMetadata("productImage")).thenReturn(null);

        final Map<String, Object> outputMap = new HashMap<>();
        strategy.transform(contentlet, outputMap, Set.of(AVOID_MAP_SUFFIX_FOR_VIEWS), null);

        assertFalse(
                "emptyMap() must NOT be written to the output map for a binary field with no "
                        + "uploaded file — it poisons the contentlet backing map and causes "
                        + "ClassCastException in Contentlet.getBinary()",
                outputMap.containsKey("productImage"));
    }

    /**
     * Verifies that the static {@code transform(Field, Contentlet)} helper returns an empty map
     * (the sentinel value) when the contentlet has no metadata for the field. The instance method
     * uses the emptiness of this result to decide whether to write to the output map.
     */
    @Test
    public void testStaticTransform_whenBinaryMetadataIsNull_returnsEmptyMap() throws Exception {
        final Field field = Mockito.mock(BinaryField.class);
        Mockito.when(field.variable()).thenReturn("productImage");

        final Contentlet contentlet = Mockito.mock(Contentlet.class);
        Mockito.when(contentlet.getBinaryMetadata("productImage")).thenReturn(null);

        final Map<String, Object> result = BinaryViewStrategy.transform(field, contentlet);

        assertTrue("Static transform must return emptyMap() when metadata is null",
                result.isEmpty());
    }
}
