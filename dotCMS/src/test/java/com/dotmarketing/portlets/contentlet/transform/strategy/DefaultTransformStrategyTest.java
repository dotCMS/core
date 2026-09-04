package com.dotmarketing.portlets.contentlet.transform.strategy;

import static com.dotmarketing.portlets.contentlet.model.Contentlet.MOD_USER_NAME_KEY;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.BINARIES;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.VERSION_INFO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.dotcms.api.APIProvider;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.storage.model.Metadata;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.image.focalpoint.FocalPointAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.liferay.portal.model.User;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private static final String FILE_ASSET_META_KEY = FileAssetAPI.BINARY_FIELD + "MetaData";

    /**
     * Mocks an {@link APIProvider} whose {@code identifierAPI} resolves to an asset name, so the
     * FileAsset branch of {@code addBinaries} can build its binary links. The field is injected via
     * reflection because the real {@code APIProvider.Builder} eagerly resolves APILocator defaults,
     * which is not available in a pure unit test.
     */
    private APIProvider toolBoxResolvingAssetName(final String assetName) throws Exception {
        final Identifier identifier = Mockito.mock(Identifier.class);
        Mockito.when(identifier.getAssetName()).thenReturn(assetName);
        final IdentifierAPI identifierAPI = Mockito.mock(IdentifierAPI.class);
        Mockito.when(identifierAPI.find(Mockito.anyString())).thenReturn(identifier);

        final APIProvider toolBox = Mockito.mock(APIProvider.class);
        final java.lang.reflect.Field field = APIProvider.class.getDeclaredField("identifierAPI");
        field.setAccessible(true);
        field.set(toolBox, identifierAPI);
        return toolBox;
    }

    private Contentlet mockFileAssetWithBinary(final Metadata metadata) throws Exception {
        final Field field = Mockito.mock(BinaryField.class);
        Mockito.when(field.variable()).thenReturn(FileAssetAPI.BINARY_FIELD);

        final ContentType contentType = Mockito.mock(ContentType.class);
        Mockito.when(contentType.fields(BinaryField.class)).thenReturn(List.of(field));

        final Contentlet contentlet = Mockito.mock(Contentlet.class);
        Mockito.when(contentlet.getContentType()).thenReturn(contentType);
        Mockito.when(contentlet.isFileAsset()).thenReturn(true);
        Mockito.when(contentlet.getIdentifier()).thenReturn("identifier-1");
        Mockito.when(contentlet.getInode()).thenReturn("inode-1");
        Mockito.when(contentlet.getBinaryMetadata(FileAssetAPI.BINARY_FIELD)).thenReturn(metadata);
        return contentlet;
    }

    /**
     * A legacy FileAsset's {@code fileAsset} binary field must also surface its focal point under
     * {@code fileAssetMetaData.focalPoint}, mirroring the dotAsset path, so the image editor can
     * re-seed the marker when a File/Image field references a FileAsset. Regression coverage for
     * <a href="https://github.com/dotCMS/core/issues/36363">#36363</a>.
     */
    @Test
    public void testAddBinaries_fileAsset_whenCustomMetaHasFocalPoint_surfacesItInMetaDataMap()
            throws Exception {

        final Map<String, Serializable> fieldsMeta = new HashMap<>();
        fieldsMeta.put("name", "image.png");
        fieldsMeta.put(CUSTOM_FOCAL_POINT_KEY, "0.76,0.13");
        final Metadata metadata = new Metadata(FileAssetAPI.BINARY_FIELD, fieldsMeta);

        final DefaultTransformStrategy strategy =
                new DefaultTransformStrategy(toolBoxResolvingAssetName("image.png"));
        final Contentlet contentlet = mockFileAssetWithBinary(metadata);

        final Map<String, Object> map = new HashMap<>();
        invokeAddBinaries(strategy, contentlet, map);

        assertTrue("Expected fileAssetMetaData to be surfaced for a FileAsset",
                map.containsKey(FILE_ASSET_META_KEY));
        @SuppressWarnings("unchecked")
        final Map<String, Serializable> metaMap =
                (Map<String, Serializable>) map.get(FILE_ASSET_META_KEY);
        assertEquals("Focal point must be exposed under the focalPoint key for a FileAsset",
                "0.76,0.13", metaMap.get(FocalPointAPI.FOCAL_POINT));
    }

    /**
     * A FileAsset with no focal point in its custom metadata still surfaces the
     * {@code focalPoint} entry defaulting to "0.0", matching the dotAsset path.
     */
    @Test
    public void testAddBinaries_fileAsset_whenCustomMetaHasNoFocalPoint_defaultsToZero()
            throws Exception {

        final Map<String, Serializable> fieldsMeta = new HashMap<>();
        fieldsMeta.put("name", "image.png");
        final Metadata metadata = new Metadata(FileAssetAPI.BINARY_FIELD, fieldsMeta);

        final DefaultTransformStrategy strategy =
                new DefaultTransformStrategy(toolBoxResolvingAssetName("image.png"));
        final Contentlet contentlet = mockFileAssetWithBinary(metadata);

        final Map<String, Object> map = new HashMap<>();
        invokeAddBinaries(strategy, contentlet, map);

        assertTrue("Expected fileAssetMetaData to be surfaced for a FileAsset",
                map.containsKey(FILE_ASSET_META_KEY));
        @SuppressWarnings("unchecked")
        final Map<String, Serializable> metaMap =
                (Map<String, Serializable>) map.get(FILE_ASSET_META_KEY);
        assertEquals("focalPoint must default to 0.0 when absent from custom metadata",
                "0.0", metaMap.get(FocalPointAPI.FOCAL_POINT));
    }

    // --- resolveModUserName (issue #37186, User Story 2: no repeated resolution within one row)

    /**
     * Invokes the private {@code resolveModUserName} method in isolation, mirroring
     * {@link #invokeAddBinaries} above.
     */
    private String invokeResolveModUserName(final DefaultTransformStrategy strategy,
            final Contentlet contentlet, final Map<String, Object> map) throws Exception {
        final Method resolveModUserName = DefaultTransformStrategy.class.getDeclaredMethod(
                "resolveModUserName", Contentlet.class, Map.class);
        resolveModUserName.setAccessible(true);
        return (String) resolveModUserName.invoke(strategy, contentlet, map);
    }

    private APIProvider toolBoxWithMockUserAPI(final UserAPI userAPI) throws Exception {
        final APIProvider toolBox = Mockito.mock(APIProvider.class);
        final java.lang.reflect.Field field = APIProvider.class.getDeclaredField("userAPI");
        field.setAccessible(true);
        field.set(toolBox, userAPI);
        return toolBox;
    }

    /**
     * When {@code addAuditProperties} already resolved modUser for this row (its value is
     * already in the map under {@code MOD_USER_NAME_KEY}), {@code addVersionProperties} must
     * reuse it rather than calling {@code loadUserById} a second time for the same id.
     */
    @Test
    public void resolveModUserName_reusesAlreadyResolvedName_doesNotCallLoadUserByIdAgain()
            throws Exception {
        final UserAPI userAPI = Mockito.mock(UserAPI.class);
        final DefaultTransformStrategy strategy =
                new DefaultTransformStrategy(toolBoxWithMockUserAPI(userAPI));

        final Contentlet contentlet = Mockito.mock(Contentlet.class);
        Mockito.when(contentlet.getModUser()).thenReturn("user-1");

        final Map<String, Object> map = new HashMap<>();
        map.put(MOD_USER_NAME_KEY, "Ada Lovelace"); // already resolved by addAuditProperties

        final String result = invokeResolveModUserName(strategy, contentlet, map);

        assertEquals("Ada Lovelace", result);
        verify(userAPI, never()).loadUserById(Mockito.anyString());
    }

    /**
     * When nothing resolved modUser yet (e.g. COMMON_PROPS wasn't requested for this transform),
     * {@code addVersionProperties} must still resolve it itself — exactly once.
     */
    @Test
    public void resolveModUserName_notYetResolved_resolvesExactlyOnce() throws Exception {
        final UserAPI userAPI = Mockito.mock(UserAPI.class);
        final User user = Mockito.mock(User.class);
        Mockito.when(user.getFullName()).thenReturn("Grace Hopper");
        Mockito.when(userAPI.loadUserById("user-2")).thenReturn(user);

        final DefaultTransformStrategy strategy =
                new DefaultTransformStrategy(toolBoxWithMockUserAPI(userAPI));

        final Contentlet contentlet = Mockito.mock(Contentlet.class);
        Mockito.when(contentlet.getModUser()).thenReturn("user-2");

        final Map<String, Object> map = new HashMap<>(); // nothing resolved it yet

        final String result = invokeResolveModUserName(strategy, contentlet, map);

        assertEquals("Grace Hopper", result);
        verify(userAPI, times(1)).loadUserById("user-2");
    }
}
