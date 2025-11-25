package com.dotmarketing.cache;

import static com.dotcms.util.CollectionsUtils.list;
import static graphql.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.MultiTreeDataGen;
import com.dotcms.datagen.StructureDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.VariantDataGen;
import com.dotcms.experiments.business.ExperimentsAPI;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.ExperimentVariant;
import com.dotcms.experiments.model.TrafficProportion;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.factories.PersonalizedContentlet;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import graphql.AssertException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;

public class MultiTreeCacheTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    private Map<String, Object> testStyleProperties() {
        Map<String, Object> styleProperties = new HashMap<>();
        styleProperties.put("backgroundColor", "red");
        styleProperties.put("fontSize", "16px");
        styleProperties.put("padding", "10px");
        return styleProperties;
    }

    /**
     * Method to test: {@link MultiTreeCache#putPageMultiTrees(String, String, boolean, Table)} and {@link MultiTreeCache#getPageMultiTrees(String, String, boolean)}
     * When: put a {@link MultiTree} collections with live equals true and DEFAULT Variant
     * Should: be able to get this collection with live equals true and DEFAULT Variant
     */
    @Test
    public void putAndGetLiveTrue(){
        final MultiTreeCache multiTreeCache = CacheLocator.getMultiTreeCache();

        final Template template = new TemplateDataGen().body("body").nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template).nextPersisted();

        final Structure structure_1 = new StructureDataGen().nextPersisted();
        final Container container_1 = new ContainerDataGen().maxContentlets(1).withStructure(structure_1, "").nextPersisted();

        final Structure structure_2 = new StructureDataGen().nextPersisted();
        final Container container_2 = new ContainerDataGen().maxContentlets(1).withStructure(structure_2, "").nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType.id()).nextPersisted();

        final MultiTree multiTree_1 = new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container_1)
                .setContentlet(contentlet_1)
                .nextPersisted();

        final MultiTree multiTree_2 = new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container_2)
                .setContentlet(contentlet_2)
                .nextPersisted();

        final Table<String, String, Set<PersonalizedContentlet>> multiTrees =  HashBasedTable.create();

        final Set<PersonalizedContentlet> contentlets_1 = new HashSet<>();
        contentlets_1.add(new PersonalizedContentlet(multiTree_1.getContentlet(),
                multiTree_1.getPersonalization(), 1, testStyleProperties()));

        final Set<PersonalizedContentlet> contentlets_2 = new HashSet<>();
        contentlets_2.add(new PersonalizedContentlet(multiTree_2.getContentlet(),
                multiTree_2.getPersonalization(), 1, testStyleProperties()));

        multiTrees.put(multiTree_1.getContainer(), multiTree_1.getContainerAsID(), contentlets_1);
        multiTrees.put(multiTree_2.getContainer(), multiTree_2.getContainerAsID(), contentlets_2);

        multiTreeCache.putPageMultiTrees(page.getIdentifier(), VariantAPI.DEFAULT_VARIANT.name(), true, multiTrees);

        final Optional<Table<String, String, Set<PersonalizedContentlet>>> pageMultiTrees = multiTreeCache.getPageMultiTrees(
                page.getIdentifier(), VariantAPI.DEFAULT_VARIANT.name(), true);

        check(container_1, container_2, contentlet_1, contentlet_2, pageMultiTrees);

        final Optional<Table<String, String, Set<PersonalizedContentlet>>> pageMultiTrees1 = multiTreeCache.getPageMultiTrees(
                page.getIdentifier(), VariantAPI.DEFAULT_VARIANT.name(), false);

        assertFalse(pageMultiTrees1.isPresent());
    }

    /**
     * Method to test: {@link MultiTreeCache#putPageMultiTrees(String, String, boolean, Table)} and {@link MultiTreeCache#getPageMultiTrees(String, String, boolean)}
     * When: put a {@link MultiTree} collections with live equals true and a specific variant
     * Should: be able to get this collection with live equals true
     */
    @Test
    public void putAndGetLiveTrueWithVariant(){
        final Variant variant = new VariantDataGen().nextPersisted();
        final MultiTreeCache multiTreeCache = CacheLocator.getMultiTreeCache();

        final Template template = new TemplateDataGen().body("body").nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template).nextPersisted();

        final Structure structure_1 = new StructureDataGen().nextPersisted();
        final Container container_1 = new ContainerDataGen().maxContentlets(1).withStructure(structure_1, "").nextPersisted();

        final Structure structure_2 = new StructureDataGen().nextPersisted();
        final Container container_2 = new ContainerDataGen().maxContentlets(1).withStructure(structure_2, "").nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType.id()).nextPersisted();

        final MultiTree multiTree_1 = new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container_1)
                .setContentlet(contentlet_1)
                .nextPersisted();

        final MultiTree multiTree_2 = new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container_2)
                .setContentlet(contentlet_2)
                .nextPersisted();

        final Table<String, String, Set<PersonalizedContentlet>> multiTrees =  HashBasedTable.create();

        final Set<PersonalizedContentlet> contentlets_1 = new HashSet<>();
        contentlets_1.add(new PersonalizedContentlet(multiTree_1.getContentlet(),
                multiTree_1.getPersonalization(), 1, testStyleProperties()));

        final Set<PersonalizedContentlet> contentlets_2 = new HashSet<>();
        contentlets_2.add(new PersonalizedContentlet(multiTree_2.getContentlet(),
                multiTree_2.getPersonalization(), 1, testStyleProperties()));

        multiTrees.put(multiTree_1.getContainer(), multiTree_1.getContainerAsID(), contentlets_1);
        multiTrees.put(multiTree_2.getContainer(), multiTree_2.getContainerAsID(), contentlets_2);

        multiTreeCache.putPageMultiTrees(page.getIdentifier(), variant.name(), true, multiTrees);

        final Optional<Table<String, String, Set<PersonalizedContentlet>>> pageMultiTrees = multiTreeCache.getPageMultiTrees(
                page.getIdentifier(), variant.name(), true);

        check(container_1, container_2, contentlet_1, contentlet_2, pageMultiTrees);

        final Optional<Table<String, String, Set<PersonalizedContentlet>>> pageMultiTrees1 = multiTreeCache.getPageMultiTrees(
                page.getIdentifier(), VariantAPI.DEFAULT_VARIANT.name(), false);

        assertFalse(pageMultiTrees1.isPresent());

        final Optional<Table<String, String, Set<PersonalizedContentlet>>> pageMultiTrees2 = multiTreeCache.getPageMultiTrees(
                page.getIdentifier(), variant.name(), false);

        assertFalse(pageMultiTrees2.isPresent());
    }

    /**
     * Method to test: {@link MultiTreeCache#putPageMultiTrees(String, String, boolean, Table)} and {@link MultiTreeCache#getPageMultiTrees(String, String, boolean)}
     * When: put a {@link MultiTree} collections with live equals false
     * Should: be able to get this collection with live equals false
     */
    @Test
    public void putAndGetLiveFalse(){
        final MultiTreeCache multiTreeCache = CacheLocator.getMultiTreeCache();

        final Template template = new TemplateDataGen().body("body").nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template).nextPersisted();

        final Structure structure_1 = new StructureDataGen().nextPersisted();
        final Container container_1 = new ContainerDataGen().maxContentlets(1).withStructure(structure_1, "").nextPersisted();

        final Structure structure_2 = new StructureDataGen().nextPersisted();
        final Container container_2 = new ContainerDataGen().maxContentlets(1).withStructure(structure_2, "").nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType.id()).nextPersisted();

        final MultiTree multiTree_1 = new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container_1)
                .setContentlet(contentlet_1)
                .nextPersisted();

        final MultiTree multiTree_2 = new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container_2)
                .setContentlet(contentlet_2)
                .nextPersisted();

        final Table<String, String, Set<PersonalizedContentlet>> multiTrees =  HashBasedTable.create();

        final Set<PersonalizedContentlet> contentlets_1 = new HashSet<>();
        contentlets_1.add(new PersonalizedContentlet(multiTree_1.getContentlet(),
                multiTree_1.getPersonalization(), 1, testStyleProperties()));

        final Set<PersonalizedContentlet> contentlets_2 = new HashSet<>();
        contentlets_2.add(new PersonalizedContentlet(multiTree_2.getContentlet(),
                multiTree_2.getPersonalization(), 1, testStyleProperties()));

        multiTrees.put(multiTree_1.getContainer(), multiTree_1.getContainerAsID(), contentlets_1);
        multiTrees.put(multiTree_2.getContainer(), multiTree_2.getContainerAsID(), contentlets_2);

        multiTreeCache.putPageMultiTrees(page.getIdentifier(), VariantAPI.DEFAULT_VARIANT.name(), false, multiTrees);

        final Optional<Table<String, String, Set<PersonalizedContentlet>>> pageMultiTrees = multiTreeCache.getPageMultiTrees(
                page.getIdentifier(), VariantAPI.DEFAULT_VARIANT.name(), false);

        check(container_1, container_2, contentlet_1, contentlet_2, pageMultiTrees);

        final Optional<Table<String, String, Set<PersonalizedContentlet>>> pageMultiTrees1 = multiTreeCache.getPageMultiTrees(
                page.getIdentifier(), VariantAPI.DEFAULT_VARIANT.name(), true);

        assertFalse(pageMultiTrees1.isPresent());
    }

    /**
     * Method to test: {@link MultiTreeCache#putPageMultiTrees(String, String, boolean, Table)} and {@link MultiTreeCache#getPageMultiTrees(String, String, boolean)}
     * When: put a {@link MultiTree} collections with live equals false and a specific {@link Variant}
     * Should: be able to get this collection with live equals false
     */
    @Test
    public void putAndGetLiveFalseWithVariant(){
        final Variant variant = new VariantDataGen().nextPersisted();
        final MultiTreeCache multiTreeCache = CacheLocator.getMultiTreeCache();

        final Template template = new TemplateDataGen().body("body").nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template).nextPersisted();

        final Structure structure_1 = new StructureDataGen().nextPersisted();
        final Container container_1 = new ContainerDataGen().maxContentlets(1).withStructure(structure_1, "").nextPersisted();

        final Structure structure_2 = new StructureDataGen().nextPersisted();
        final Container container_2 = new ContainerDataGen().maxContentlets(1).withStructure(structure_2, "").nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType.id()).nextPersisted();

        final MultiTree multiTree_1 = new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container_1)
                .setContentlet(contentlet_1)
                .nextPersisted();

        final MultiTree multiTree_2 = new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container_2)
                .setContentlet(contentlet_2)
                .nextPersisted();

        final Table<String, String, Set<PersonalizedContentlet>> multiTrees =  HashBasedTable.create();

        final Set<PersonalizedContentlet> contentlets_1 = new HashSet<>();
        contentlets_1.add(new PersonalizedContentlet(multiTree_1.getContentlet(),
                multiTree_1.getPersonalization(), 1, testStyleProperties()));

        final Set<PersonalizedContentlet> contentlets_2 = new HashSet<>();
        contentlets_2.add(new PersonalizedContentlet(multiTree_2.getContentlet(),
                multiTree_2.getPersonalization(), 1, testStyleProperties()));

        multiTrees.put(multiTree_1.getContainer(), multiTree_1.getContainerAsID(), contentlets_1);
        multiTrees.put(multiTree_2.getContainer(), multiTree_2.getContainerAsID(), contentlets_2);

        multiTreeCache.putPageMultiTrees(page.getIdentifier(), variant.name(), false, multiTrees);

        final Optional<Table<String, String, Set<PersonalizedContentlet>>> pageMultiTrees = multiTreeCache.getPageMultiTrees(
                page.getIdentifier(), variant.name(), false);

        check(container_1, container_2, contentlet_1, contentlet_2, pageMultiTrees);

        final Optional<Table<String, String, Set<PersonalizedContentlet>>> pageMultiTrees1 = multiTreeCache.getPageMultiTrees(
                page.getIdentifier(), VariantAPI.DEFAULT_VARIANT.name(), true);

        assertFalse(pageMultiTrees1.isPresent());

        final Optional<Table<String, String, Set<PersonalizedContentlet>>> pageMultiTrees2 = multiTreeCache.getPageMultiTrees(
                page.getIdentifier(), variant.name(), true);

        assertFalse(pageMultiTrees2.isPresent());
    }

    /**
     * Method to test: {@link MultiTreeCache#removePageMultiTrees(String, String, boolean)} (String)}
     * When: put a {@link MultiTree} collections and later remove it
     * Should: return empty
     */
    @Test
    public void remove(){
        final MultiTreeCache multiTreeCache = CacheLocator.getMultiTreeCache();

        final Template template = new TemplateDataGen().body("body").nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template).nextPersisted();

        final Structure structure_1 = new StructureDataGen().nextPersisted();
        final Container container_1 = new ContainerDataGen().maxContentlets(1).withStructure(structure_1, "").nextPersisted();

        final Structure structure_2 = new StructureDataGen().nextPersisted();
        final Container container_2 = new ContainerDataGen().maxContentlets(1).withStructure(structure_2, "").nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType.id()).nextPersisted();

        final MultiTree multiTree_1 = new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container_1)
                .setContentlet(contentlet_1)
                .nextPersisted();

        final MultiTree multiTree_2 = new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container_2)
                .setContentlet(contentlet_2)
                .nextPersisted();

        final Table<String, String, Set<PersonalizedContentlet>> multiTrees =  HashBasedTable.create();

        final Set<PersonalizedContentlet> contentlets_1 = new HashSet<>();
        contentlets_1.add(new PersonalizedContentlet(multiTree_1.getContentlet(), multiTree_1.getPersonalization(), 1,testStyleProperties()));

        final Set<PersonalizedContentlet> contentlets_2 = new HashSet<>();
        contentlets_2.add(new PersonalizedContentlet(multiTree_2.getContentlet(), multiTree_2.getPersonalization(), 1,testStyleProperties()));

        multiTrees.put(multiTree_1.getContainer(), multiTree_1.getContainerAsID(), contentlets_1);
        multiTrees.put(multiTree_2.getContainer(), multiTree_2.getContainerAsID(), contentlets_2);

        multiTreeCache.putPageMultiTrees(page.getIdentifier(), VariantAPI.DEFAULT_VARIANT.name(), false, multiTrees);

        final Optional<Table<String, String, Set<PersonalizedContentlet>>> pageMultiTrees = multiTreeCache.getPageMultiTrees(
                page.getIdentifier(), VariantAPI.DEFAULT_VARIANT.name(), false);

        check(container_1, container_2, contentlet_1, contentlet_2, pageMultiTrees);

        multiTreeCache.removePageMultiTrees(page.getIdentifier(), VariantAPI.DEFAULT_VARIANT.name(), false);

        final Optional<Table<String, String, Set<PersonalizedContentlet>>> pageMultiTrees_1 = multiTreeCache.getPageMultiTrees(
                page.getIdentifier(), VariantAPI.DEFAULT_VARIANT.name(), false);
        assertFalse(pageMultiTrees_1.isPresent());
    }

    /**
     * Method to test: {@link MultiTreeCache#removePageMultiTrees(String, String, boolean)} (String)}
     * When: put a {@link MultiTree} collections and later remove it to a specific {@link Variant}
     * Should: return empty
     */
    @Test
    public void removeWithVariant(){
        final Variant variant = new VariantDataGen().nextPersisted();
        final MultiTreeCache multiTreeCache = CacheLocator.getMultiTreeCache();

        final Template template = new TemplateDataGen().body("body").nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template).nextPersisted();

        final Structure structure_1 = new StructureDataGen().nextPersisted();
        final Container container_1 = new ContainerDataGen().maxContentlets(1).withStructure(structure_1, "").nextPersisted();

        final Structure structure_2 = new StructureDataGen().nextPersisted();
        final Container container_2 = new ContainerDataGen().maxContentlets(1).withStructure(structure_2, "").nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType.id()).nextPersisted();

        final MultiTree multiTree_1 = new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container_1)
                .setContentlet(contentlet_1)
                .nextPersisted();

        final MultiTree multiTree_2 = new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container_2)
                .setContentlet(contentlet_2)
                .nextPersisted();

        final Table<String, String, Set<PersonalizedContentlet>> multiTrees =  HashBasedTable.create();

        final Set<PersonalizedContentlet> contentlets_1 = new HashSet<>();
        contentlets_1.add(new PersonalizedContentlet(multiTree_1.getContentlet(), multiTree_1.getPersonalization(), 1,testStyleProperties()));

        final Set<PersonalizedContentlet> contentlets_2 = new HashSet<>();
        contentlets_2.add(new PersonalizedContentlet(multiTree_2.getContentlet(), multiTree_2.getPersonalization(), 1,testStyleProperties()));

        multiTrees.put(multiTree_1.getContainer(), multiTree_1.getContainerAsID(), contentlets_1);
        multiTrees.put(multiTree_2.getContainer(), multiTree_2.getContainerAsID(), contentlets_2);

        multiTreeCache.putPageMultiTrees(page.getIdentifier(), VariantAPI.DEFAULT_VARIANT.name(), false, multiTrees);
        multiTreeCache.putPageMultiTrees(page.getIdentifier(), variant.name(), false, multiTrees);

        multiTreeCache.removePageMultiTrees(page.getIdentifier(), variant.name(), false);

        final Optional<Table<String, String, Set<PersonalizedContentlet>>> pageMultiTrees_1 = multiTreeCache.getPageMultiTrees(
                page.getIdentifier(), variant.name(), false);
        assertFalse(pageMultiTrees_1.isPresent());

        final Optional<Table<String, String, Set<PersonalizedContentlet>>> pageMultiTrees_2 = multiTreeCache.getPageMultiTrees(
                page.getIdentifier(), VariantAPI.DEFAULT_VARIANT.name(), false);
        assertTrue(pageMultiTrees_2.isPresent());
    }

    /**
     * Method to test: {@link MultiTreeCache#removePageMultiTrees(String, String )} (String)}
     * When: put a {@link MultiTree} collections and later remove it
     * Should: return empty
     */
    @Test
    public void removePageMultiTrees(){
        final MultiTreeCache multiTreeCache = CacheLocator.getMultiTreeCache();

        final Template template = new TemplateDataGen().body("body").nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template).nextPersisted();

        final Structure structure_1 = new StructureDataGen().nextPersisted();
        final Container container_1 = new ContainerDataGen().maxContentlets(1).withStructure(structure_1, "").nextPersisted();

        final Structure structure_2 = new StructureDataGen().nextPersisted();
        final Container container_2 = new ContainerDataGen().maxContentlets(1).withStructure(structure_2, "").nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType.id()).nextPersisted();

        final MultiTree multiTree_1 = new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container_1)
                .setContentlet(contentlet_1)
                .nextPersisted();

        final MultiTree multiTree_2 = new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container_2)
                .setContentlet(contentlet_2)
                .nextPersisted();

        final Table<String, String, Set<PersonalizedContentlet>> multiTrees =  HashBasedTable.create();

        final Set<PersonalizedContentlet> contentlets_1 = new HashSet<>();
        contentlets_1.add(new PersonalizedContentlet(multiTree_1.getContentlet(),
                multiTree_1.getPersonalization(), 1, testStyleProperties()));

        final Set<PersonalizedContentlet> contentlets_2 = new HashSet<>();
        contentlets_2.add(new PersonalizedContentlet(multiTree_2.getContentlet(),
                multiTree_2.getPersonalization(), 1, null));

        multiTrees.put(multiTree_1.getContainer(), multiTree_1.getContainerAsID(), contentlets_1);
        multiTrees.put(multiTree_2.getContainer(), multiTree_2.getContainerAsID(), contentlets_2);

        multiTreeCache.putPageMultiTrees(page.getIdentifier(), VariantAPI.DEFAULT_VARIANT.name(), false, multiTrees);
        multiTreeCache.putPageMultiTrees(page.getIdentifier(), VariantAPI.DEFAULT_VARIANT.name(), true, multiTrees);
        multiTreeCache.removePageMultiTrees(page.getIdentifier(), VariantAPI.DEFAULT_VARIANT.name() );

        final Optional<Table<String, String, Set<PersonalizedContentlet>>> pageMultiTrees_1 = multiTreeCache.getPageMultiTrees(
                page.getIdentifier(), VariantAPI.DEFAULT_VARIANT.name(), false);
        assertFalse(pageMultiTrees_1.isPresent());

        final Optional<Table<String, String, Set<PersonalizedContentlet>>> pageMultiTrees_2 = multiTreeCache.getPageMultiTrees(
                page.getIdentifier(), VariantAPI.DEFAULT_VARIANT.name(), true);
        assertFalse(pageMultiTrees_2.isPresent());
    }

    private void check(Container container_1, Container container_2, Contentlet contentlet_1,
            Contentlet contentlet_2,
            Optional<Table<String, String, Set<PersonalizedContentlet>>> pageMultiTrees) {
        assertTrue(pageMultiTrees.isPresent());
        assertEquals(2, pageMultiTrees.get().size());

        for (final String containerId : pageMultiTrees.get().rowKeySet()) {
            final List<String> list = pageMultiTrees.get().row(containerId).values().stream()
                    .flatMap(personalizedContentlets -> personalizedContentlets.stream())
                    .map(personalizedContentlet -> personalizedContentlet.getContentletId())
                    .collect(Collectors.toList());

            if (containerId.equals(container_1.getIdentifier())) {
                assertEquals(1, list.size());
                assertTrue(list.contains(contentlet_1.getIdentifier()));
            } else if (containerId.equals(container_2.getIdentifier())) {
                assertEquals(1, list.size());
                assertTrue(list.contains(contentlet_2.getIdentifier()));
            } else {
                throw new AssertException("Container not expected");
            }
        }
    }

    /**
     * Method to test: {@link MultiTreeCache#putPageMultiTrees(String, String, boolean, Table)} and {@link MultiTreeCache#getPageMultiTrees(String, String, boolean)}
     * When: When you put MultiTress inside a Cache
     * Should:
     * - Get them with the Get method.
     * - Don't get them anymore after remove them.
     */
    @Test
    public void putAndRemove(){
        final MultiTreeCache multiTreeCache = CacheLocator.getMultiTreeCache();

        final String pageId = RandomStringUtils.random(20);

        final Table<String, String, Set<PersonalizedContentlet>> multiTreesLiveDefault =  mock(Table.class);
        final Table<String, String, Set<PersonalizedContentlet>> multiTreesWorkingDefault =  mock(Table.class);
        final Table<String, String, Set<PersonalizedContentlet>> multiTreesLiveSpecificVariant =  mock(Table.class);
        final Table<String, String, Set<PersonalizedContentlet>> multiTreesWorkingSpecificVariant =  mock(Table.class);

        final String specificVariantName = "Specific Variant";

        final boolean present_1 = Stream.of(
                        multiTreeCache.getPageMultiTrees(pageId, VariantAPI.DEFAULT_VARIANT.name(), true),
                        multiTreeCache.getPageMultiTrees(pageId, VariantAPI.DEFAULT_VARIANT.name(), false),
                        multiTreeCache.getPageMultiTrees(pageId, specificVariantName, true),
                        multiTreeCache.getPageMultiTrees(pageId, specificVariantName, false)
                )
                .filter(optional -> optional.isPresent())
                .findFirst()
                .isPresent();

        if (present_1) {
            throw new AssertException("Value Not Expected");
        }

        multiTreeCache.putPageMultiTrees(pageId, VariantAPI.DEFAULT_VARIANT.name(), true, multiTreesLiveDefault);
        multiTreeCache.putPageMultiTrees(pageId, VariantAPI.DEFAULT_VARIANT.name(), false, multiTreesWorkingDefault);

        multiTreeCache.putPageMultiTrees(pageId, specificVariantName, true, multiTreesLiveSpecificVariant);
        multiTreeCache.putPageMultiTrees(pageId, specificVariantName, false, multiTreesWorkingSpecificVariant);

        assertEquals(multiTreesLiveDefault, multiTreeCache.getPageMultiTrees(
                pageId, VariantAPI.DEFAULT_VARIANT.name(), true).orElseThrow());

        assertEquals(multiTreesWorkingDefault, multiTreeCache.getPageMultiTrees(
                pageId, VariantAPI.DEFAULT_VARIANT.name(), false).orElseThrow());
        assertEquals(multiTreesLiveSpecificVariant, multiTreeCache.getPageMultiTrees(
                pageId, specificVariantName, true).orElseThrow());
        assertEquals(multiTreesWorkingSpecificVariant, multiTreeCache.getPageMultiTrees(
                pageId, specificVariantName, false).orElseThrow());

        multiTreeCache.removePageMultiTrees(pageId, VariantAPI.DEFAULT_VARIANT.name(), true);
        multiTreeCache.removePageMultiTrees(pageId, VariantAPI.DEFAULT_VARIANT.name(), false);

        multiTreeCache.removePageMultiTrees(pageId, specificVariantName, true);
        multiTreeCache.removePageMultiTrees(pageId, specificVariantName, false);

        assertFalse(multiTreeCache.getPageMultiTrees(pageId, VariantAPI.DEFAULT_VARIANT.name(), true).isPresent());

        assertFalse(multiTreeCache.getPageMultiTrees(pageId, VariantAPI.DEFAULT_VARIANT.name(), false).isPresent());
        assertFalse(multiTreeCache.getPageMultiTrees(pageId, specificVariantName, true).isPresent());
        assertFalse(multiTreeCache.getPageMultiTrees(pageId, specificVariantName, false).isPresent());
    }

    /**
     * Method to test: {@link MultiTreeCache#removePageMultiTrees(String)}
     * When: When you put MultiTress inside a Cache for differenet Variants
     * Should: Remove all the MultiTrees for all the variants
     */
    @Test
    public void putAndRemoveAll() throws DotDataException {

        final String pageId = RandomStringUtils.random(20);

        final Table<String, String, Set<PersonalizedContentlet>> multiTreesLiveDefault =  mock(Table.class);
        final Table<String, String, Set<PersonalizedContentlet>> multiTreesWorkingDefault =  mock(Table.class);
        final Table<String, String, Set<PersonalizedContentlet>> multiTreesLiveSpecificVariant =  mock(Table.class);
        final Table<String, String, Set<PersonalizedContentlet>> multiTreesWorkingSpecificVariant =  mock(Table.class);

        final String specificVariantName = "Specific Variant";

        final Experiment experiment = mock(Experiment.class);
        final TrafficProportion trafficProportion = mock(TrafficProportion.class);

        when(experiment.trafficProportion()).thenReturn(trafficProportion);
        final SortedSet<ExperimentVariant> variants = new TreeSet<>(Comparator.comparingInt(ExperimentVariant::hashCode));

        final ExperimentVariant variant_1 = mock(ExperimentVariant.class);
        when(variant_1.id()).thenReturn(VariantAPI.DEFAULT_VARIANT.name());

        final ExperimentVariant variant_2 = mock(ExperimentVariant.class);
        when(variant_2.id()).thenReturn(specificVariantName);

        variants.add(variant_1);
        variants.add(variant_2);

        when(trafficProportion.variants()).thenReturn(variants);

        final ExperimentsAPI experimentsAPI = mock(ExperimentsAPI.class);
        when(experimentsAPI.listActive(pageId)).thenReturn(list(experiment));

        final MultiTreeCache multiTreeCache = new MultiTreeCache(experimentsAPI);

        final boolean present_1 = Stream.of(
                        multiTreeCache.getPageMultiTrees(pageId, VariantAPI.DEFAULT_VARIANT.name(), true),
                        multiTreeCache.getPageMultiTrees(pageId, VariantAPI.DEFAULT_VARIANT.name(), false),
                        multiTreeCache.getPageMultiTrees(pageId, specificVariantName, true),
                        multiTreeCache.getPageMultiTrees(pageId, specificVariantName, false)
                )
                .filter(optional -> optional.isPresent())
                .findFirst()
                .isPresent();

        if (present_1) {
            throw new AssertException("Value Not Expected");
        }

        multiTreeCache.putPageMultiTrees(pageId, VariantAPI.DEFAULT_VARIANT.name(), true, multiTreesLiveDefault);
        multiTreeCache.putPageMultiTrees(pageId, VariantAPI.DEFAULT_VARIANT.name(), false, multiTreesWorkingDefault);

        multiTreeCache.putPageMultiTrees(pageId, specificVariantName, true, multiTreesLiveSpecificVariant);
        multiTreeCache.putPageMultiTrees(pageId, specificVariantName, false, multiTreesWorkingSpecificVariant);

        assertEquals(multiTreesLiveDefault, multiTreeCache.getPageMultiTrees(
                pageId, VariantAPI.DEFAULT_VARIANT.name(), true).orElseThrow());

        assertEquals(multiTreesWorkingDefault, multiTreeCache.getPageMultiTrees(
                pageId, VariantAPI.DEFAULT_VARIANT.name(), false).orElseThrow());
        assertEquals(multiTreesLiveSpecificVariant, multiTreeCache.getPageMultiTrees(
                pageId, specificVariantName, true).orElseThrow());
        assertEquals(multiTreesWorkingSpecificVariant, multiTreeCache.getPageMultiTrees(
                pageId, specificVariantName, false).orElseThrow());


        multiTreeCache.removePageMultiTrees(pageId);

        assertFalse(multiTreeCache.getPageMultiTrees(pageId, VariantAPI.DEFAULT_VARIANT.name(), true).isPresent());

        assertFalse(multiTreeCache.getPageMultiTrees(pageId, VariantAPI.DEFAULT_VARIANT.name(), false).isPresent());
        assertFalse(multiTreeCache.getPageMultiTrees(pageId, specificVariantName, true).isPresent());
        assertFalse(multiTreeCache.getPageMultiTrees(pageId, specificVariantName, false).isPresent());

    }
}
