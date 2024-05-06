package com.dotmarketing.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.content.elasticsearch.business.ESContentletAPIImpl;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.VariantDataGen;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.Test;

public class VersionableFactoryImplTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link VersionableFactoryImpl#createContentletVersionInfo(Identifier, long, String)}
     * When: Call the method
     * Should: Create a {@link com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo} in the
     * DEFAULT Variant
     */
    @Test
     public void createContentletVersionInfoWithDefaultVariant()
            throws DotDataException, DotSecurityException {

        final Language language = new LanguageDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType).nextPersisted();

        final Identifier identifier = APILocator.getIdentifierAPI()
                .find(contentlet.getIdentifier());

        final ContentletVersionInfo contentletVersionInfo = FactoryLocator.getVersionableFactory()
                .createContentletVersionInfo(identifier, language.getId(), contentlet.getInode());

        assertEquals(contentlet.getIdentifier(), contentletVersionInfo.getIdentifier());
        assertEquals(language.getId(), contentletVersionInfo.getLang());
        assertEquals("DEFAULT", contentletVersionInfo.getVariant());
        assertEquals(contentlet.getInode(), contentletVersionInfo.getWorkingInode());

        final ArrayList results = new DotConnect().setSQL(
                        "select * from contentlet_version_info where identifier =? AND lang = ?")
                .addParam(identifier.getId())
                .addParam(language.getId())
                .loadResults();

        assertEquals(1, results.size());

        assertEquals(contentlet.getIdentifier(), ((Map) results.get(0)).get("identifier"));
        assertEquals(language.getId(), Long.parseLong(((Map) results.get(0)).get("lang").toString()));
        assertEquals("DEFAULT", ((Map) results.get(0)).get("variant_id"));
        assertEquals(contentlet.getInode(), ((Map) results.get(0)).get("working_inode"));
    }

    /**
     * Method to test: {@link VersionableFactoryImpl#createContentletVersionInfo(Identifier, long, String, String)}
     * When: Call the method
     * Should: Create a {@link com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo}
     */
    @Test
    public void createContentletVersionInfoWithNotDefaultVariant()
            throws DotDataException, DotSecurityException {
        final Variant variant = new VariantDataGen().nextPersisted();
        final Language language = new LanguageDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType).nextPersisted();

        final Identifier identifier = APILocator.getIdentifierAPI()
                .find(contentlet.getIdentifier());

        final ContentletVersionInfo contentletVersionInfo = FactoryLocator.getVersionableFactory()
                .createContentletVersionInfo(identifier, language.getId(), contentlet.getInode(),
                        variant.name());

        assertEquals(contentlet.getIdentifier(), contentletVersionInfo.getIdentifier());
        assertEquals(language.getId(), contentletVersionInfo.getLang());
        assertEquals(variant.name(), contentletVersionInfo.getVariant());

        final ArrayList results = new DotConnect().setSQL(
                        "select * from contentlet_version_info where identifier =? AND lang = ?")
                .addParam(identifier.getId())
                .addParam(language.getId())
                .loadResults();

        assertEquals(1, results.size());

        assertEquals(contentlet.getIdentifier(), ((Map) results.get(0)).get("identifier"));
        assertEquals(language.getId(), Long.parseLong(((Map) results.get(0)).get("lang").toString()));
        assertEquals(variant.name(), ((Map) results.get(0)).get("variant_id"));
        assertEquals(contentlet.getInode(), ((Map) results.get(0)).get("working_inode"));
    }

    /**
     * Method to test: {@link VersionableFactoryImpl#getContentletVersionInfo(String, long)} )}
     * When: Create a new {@link ContentletVersionInfo}
     * Should: return it when you look for it
     */
    @Test
    public void findContentletVersionInfo() throws DotDataException {
        final Language language = new LanguageDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType).nextPersisted();

        final Identifier identifier = APILocator.getIdentifierAPI()
                .find(contentlet.getIdentifier());

        FactoryLocator.getVersionableFactory()
                .createContentletVersionInfo(identifier, language.getId(), contentlet.getInode());

        final Optional<ContentletVersionInfo> contentletVersionInfo = FactoryLocator.getVersionableFactory()
                .getContentletVersionInfo(contentlet.getIdentifier(), language.getId());

        assertTrue(contentletVersionInfo.isPresent());

        assertEquals(contentlet.getIdentifier(), contentletVersionInfo.get().getIdentifier());
        assertEquals(language.getId(), contentletVersionInfo.get().getLang());
        assertEquals(VariantAPI.DEFAULT_VARIANT.name(), contentletVersionInfo.get().getVariant());
        assertEquals(contentlet.getInode(), contentletVersionInfo.get().getWorkingInode());

        final ContentletVersionInfo contentletVersionInfoFromCache = CacheLocator.getIdentifierCache()
                .getContentVersionInfo(contentlet.getIdentifier(),
                        language.getId(), VariantAPI.DEFAULT_VARIANT.name());

        assertEquals(contentlet.getIdentifier(), contentletVersionInfoFromCache.getIdentifier());
        assertEquals(language.getId(), contentletVersionInfoFromCache.getLang());
        assertEquals(VariantAPI.DEFAULT_VARIANT.name(), contentletVersionInfoFromCache.getVariant());


        final ContentletVersionInfo contentletVersionInfoFromCache_2 = CacheLocator.getIdentifierCache()
                .getContentVersionInfo(contentlet.getIdentifier(), language.getId());

        assertEquals(contentlet.getIdentifier(), contentletVersionInfoFromCache_2.getIdentifier());
        assertEquals(language.getId(), contentletVersionInfoFromCache_2.getLang());
        assertEquals(VariantAPI.DEFAULT_VARIANT.name(), contentletVersionInfoFromCache_2.getVariant());
    }

    /**
     * Method to test: {@link VersionableFactoryImpl#findContentletVersionInfoInDB(String, long)} )}
     * When: Create a new {@link ContentletVersionInfo}
     * Should: return it when you look for it
     */
    @Test
    public void findContentletVersionInfoInDB() throws DotDataException {
        final Language language = new LanguageDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType).nextPersisted();

        final Identifier identifier = APILocator.getIdentifierAPI()
                .find(contentlet.getIdentifier());

        FactoryLocator.getVersionableFactory()
                .createContentletVersionInfo(identifier, language.getId(), contentlet.getInode());

        final Optional<ContentletVersionInfo> contentletVersionInfo = FactoryLocator.getVersionableFactory()
                .findContentletVersionInfoInDB(contentlet.getIdentifier(), language.getId());

        assertTrue(contentletVersionInfo.isPresent());

        assertEquals(contentlet.getIdentifier(), contentletVersionInfo.get().getIdentifier());
        assertEquals(language.getId(), contentletVersionInfo.get().getLang());
        assertEquals(VariantAPI.DEFAULT_VARIANT.name(), contentletVersionInfo.get().getVariant());
        assertEquals(contentlet.getInode(), contentletVersionInfo.get().getWorkingInode());
    }

    /**
     * Method to test: {@link VersionableFactoryImpl#findContentletVersionInfoInDB(String, long)} )}
     * When: Create a new {@link ContentletVersionInfo}
     * Should: return it when you look for it
     */
    @Test
    public void findContentletVersionInfoWithVariant() throws DotDataException {
        final Variant variant = new VariantDataGen().nextPersisted();
        final Language language = new LanguageDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType).nextPersisted();

        final Identifier identifier = APILocator.getIdentifierAPI()
                .find(contentlet.getIdentifier());

        FactoryLocator.getVersionableFactory()
                .createContentletVersionInfo(identifier, language.getId(), contentlet.getInode(), VariantAPI.DEFAULT_VARIANT.name());

        FactoryLocator.getVersionableFactory()
                .createContentletVersionInfo(identifier, language.getId(), contentlet.getInode(), variant.name());

        final Optional<ContentletVersionInfo> contentletVersionInfo = FactoryLocator.getVersionableFactory()
                .getContentletVersionInfo(contentlet.getIdentifier(), language.getId(), variant.name());

        assertTrue(contentletVersionInfo.isPresent());

        assertEquals(contentlet.getIdentifier(), contentletVersionInfo.get().getIdentifier());
        assertEquals(language.getId(), contentletVersionInfo.get().getLang());
        assertEquals(variant.name(), contentletVersionInfo.get().getVariant());
        assertEquals(contentlet.getInode(), contentletVersionInfo.get().getWorkingInode());


        final ContentletVersionInfo contentletVersionInfoFromCache = CacheLocator.getIdentifierCache()
                .getContentVersionInfo(contentlet.getIdentifier(),
                        language.getId(), variant.name());

        assertEquals(contentlet.getIdentifier(), contentletVersionInfoFromCache.getIdentifier());
        assertEquals(language.getId(), contentletVersionInfoFromCache.getLang());
        assertEquals(variant.name(), contentletVersionInfoFromCache.getVariant());
    }

    /**
     * Method to test: {@link VersionableFactoryImpl#findAllContentletVersionInfos(String, String)}
     * When: Create several {@link ContentletVersionInfo} for DEFAULT variant and a specific variant
     * Should: return just to a specific variant
     */
    @Test
    public void findAllContentletVersionInfoWithVariant() throws DotDataException {
        final Variant variant = new VariantDataGen().nextPersisted();
        final Language language = new LanguageDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType).nextPersisted();

        final Identifier identifier_1 = APILocator.getIdentifierAPI()
                .find(contentlet_1.getIdentifier());

        final Identifier identifier_2 = APILocator.getIdentifierAPI()
                .find(contentlet_2.getIdentifier());

        FactoryLocator.getVersionableFactory()
                .createContentletVersionInfo(identifier_1, language.getId(), contentlet_1.getInode(), VariantAPI.DEFAULT_VARIANT.name());

        FactoryLocator.getVersionableFactory()
                .createContentletVersionInfo(identifier_1, language.getId(), contentlet_1.getInode(), variant.name());

        FactoryLocator.getVersionableFactory()
                .createContentletVersionInfo(identifier_2, language.getId(), contentlet_1.getInode(), VariantAPI.DEFAULT_VARIANT.name());

        FactoryLocator.getVersionableFactory()
                .createContentletVersionInfo(identifier_2, language.getId(), contentlet_1.getInode(), variant.name());

        final List<ContentletVersionInfo> allContentletVersionInfos = FactoryLocator.getVersionableFactory()
                .findAllContentletVersionInfos(contentlet_1.getIdentifier(), variant.name());

        assertEquals(1, allContentletVersionInfos.size());

        assertEquals(allContentletVersionInfos.get(0).getVariant(), variant.name());
        assertEquals(allContentletVersionInfos.get(0).getWorkingInode(), contentlet_1.getInode());
        assertEquals(allContentletVersionInfos.get(0).getLang(), language.getId());
    }

    /**
     * Method to test: {@link VersionableFactoryImpl#findContentletVersionInfoInDB(String, long), String} )}
     * When: Create a new {@link ContentletVersionInfo}
     * Should: return it when you look for it
     */
    @Test
    public void findContentletVersionInfoInDBWithVariant() throws DotDataException {
        final Variant variant = new VariantDataGen().nextPersisted();
        final Language language = new LanguageDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType).nextPersisted();

        final Identifier identifier = APILocator.getIdentifierAPI()
                .find(contentlet.getIdentifier());

        FactoryLocator.getVersionableFactory().createContentletVersionInfo(identifier,
                language.getId(), contentlet.getInode(), variant.name());


        FactoryLocator.getVersionableFactory()
                .createContentletVersionInfo(identifier, language.getId(), contentlet.getInode(), VariantAPI.DEFAULT_VARIANT.name());

        final Optional<ContentletVersionInfo> contentletVersionInfo = FactoryLocator.getVersionableFactory()
                .findContentletVersionInfoInDB(contentlet.getIdentifier(), language.getId(), variant.name());

        assertTrue(contentletVersionInfo.isPresent());

        assertEquals(contentlet.getIdentifier(), contentletVersionInfo.get().getIdentifier());
        assertEquals(language.getId(), contentletVersionInfo.get().getLang());
        assertEquals(variant.name(), contentletVersionInfo.get().getVariant());
        assertEquals(contentlet.getInode(), contentletVersionInfo.get().getWorkingInode());
    }

    /**
     * Method to test: {@link VersionableFactoryImpl#getContentletVersionInfo(String, long)} )}
     * When: Create a new {@link ContentletVersionInfo} and Update it directly into Database, after that get it again
     * Should: return the original version without the direct Database changes because it must get it from cache
     */
    @Test
    public void findContentletVersionInfoCache() throws DotDataException {
        final Language language = new LanguageDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType).nextPersisted();

        final Identifier identifier = APILocator.getIdentifierAPI()
                .find(contentlet.getIdentifier());

        FactoryLocator.getVersionableFactory()
                .createContentletVersionInfo(identifier, language.getId(), contentlet.getInode());

        final ContentletVersionInfo contentletVersionInfoBefore = FactoryLocator.getVersionableFactory()
                .getContentletVersionInfo(contentlet.getIdentifier(), language.getId())
                .orElseThrow(() -> new AssertionError("ContentletVersioNinfo expected"));

        assertFalse(contentletVersionInfoBefore.isDeleted());

        new DotConnect()
                .setSQL("UPDATE contentlet_version_info SET deleted = ? WHERE identifier = ? AND variant_id = ? AND lang = ?")
                .addParam(true)
                .addParam(contentlet.getIdentifier())
                .addParam(VariantAPI.DEFAULT_VARIANT.name())
                .addParam(language.getId())
                .loadResult();

        final ContentletVersionInfo contentletVersionInfoAfter = FactoryLocator.getVersionableFactory()
                .getContentletVersionInfo(contentlet.getIdentifier(), language.getId())
                .orElseThrow(() -> new AssertionError("ContentletVersioNinfo expected"));

        assertFalse(contentletVersionInfoAfter.isDeleted());

        final ArrayList<Map> results = new DotConnect()
                .setSQL("SELECT deleted FROM contentlet_version_info WHERE identifier = ? AND variant_id = ? AND lang = ?")
                .addParam(contentlet.getIdentifier())
                .addParam(VariantAPI.DEFAULT_VARIANT.name())
                .addParam(language.getId())
                .loadResults();

        assertFalse(results.isEmpty());

        final Map map = results.get(0);
        assertTrue(ConversionUtils.toBooleanFromDb(map.get("deleted").toString()));

    }

    /**
     * Method to test: {@link VersionableFactoryImpl#getContentletVersionInfo(String, long)} )}
     * When: Create a new {@link ContentletVersionInfo} and Update it directly into Database, after that get it again
     * Should: return the original version without the direct Database changes because it must get it from cache
     */
    @Test
    public void findContentletVersionInfoCacheWithVariant() throws DotDataException {
        final Variant variant = new VariantDataGen().nextPersisted();
        final Language language = new LanguageDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType).nextPersisted();

        final Identifier identifier = APILocator.getIdentifierAPI()
                .find(contentlet.getIdentifier());

        FactoryLocator.getVersionableFactory()
                .createContentletVersionInfo(identifier, language.getId(), contentlet.getInode(), variant.name());

        final ContentletVersionInfo contentletVersionInfoBefore = FactoryLocator.getVersionableFactory()
                .getContentletVersionInfo(contentlet.getIdentifier(), language.getId(),
                        variant.name())
                .orElseThrow(() -> new AssertionError("ContentletVersioNinfo expected"));

        assertFalse(contentletVersionInfoBefore.isDeleted());

        new DotConnect()
                .setSQL("UPDATE contentlet_version_info SET deleted = ? WHERE identifier = ? AND variant_id = ? AND lang = ?")
                .addParam(true)
                .addParam(contentlet.getIdentifier())
                .addParam(variant.name())
                .addParam(language.getId())
                .loadResult();

        final ContentletVersionInfo contentletVersionInfoAfter = FactoryLocator.getVersionableFactory()
                .getContentletVersionInfo(contentlet.getIdentifier(), language.getId(), variant.name())
                .orElseThrow(() -> new AssertionError("ContentletVersioNinfo expected"));

        assertFalse(contentletVersionInfoAfter.isDeleted());

        final ArrayList<Map> results = new DotConnect()
                .setSQL("SELECT deleted FROM contentlet_version_info WHERE identifier = ? AND variant_id = ? AND lang = ?")
                .addParam(contentlet.getIdentifier())
                .addParam(variant.name())
                .addParam(language.getId())
                .loadResults();

        assertFalse(results.isEmpty());

        final Map map = results.get(0);
        assertTrue(ConversionUtils.toBooleanFromDb(map.get("deleted").toString()));

    }

    /**
     * Method to test: {@link VersionableFactory#findAnyContentletVersionInfoAnyVariant(String, boolean)} (String)}
     * When: The contentlet had just one version not in the DEFAULT variant
     * Should: return the {@link ContentletVersionInfo} anyway
     *
     * @throws WebAssetException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void findContentletByIdentifierAnyLanguageAndVariant() throws DotDataException {
        final Variant variant = new VariantDataGen().nextPersisted();
        final Language language = new LanguageDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType)
                .languageId(language.getId())
                .host(host)
                .variant(variant)
                .nextPersisted();

        final ContentletVersionInfo anyContentletVersionInfoAnyVariant = FactoryLocator.getVersionableFactory()
                .findAnyContentletVersionInfoAnyVariant(contentlet.getIdentifier(), false)
                .orElseThrow();

        assertNotNull(anyContentletVersionInfoAnyVariant);
        assertEquals(contentlet.getIdentifier(), anyContentletVersionInfoAnyVariant.getIdentifier());
        assertEquals(contentlet.getInode(), anyContentletVersionInfoAnyVariant.getWorkingInode());


    }

}
