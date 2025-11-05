package com.dotmarketing.factories;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.enterprise.achecker.utility.Utility;
import com.dotcms.experiments.model.ExperimentVariant;
import com.dotcms.rendering.velocity.directive.ParseContainer;
import com.dotcms.rendering.velocity.services.PageLoader;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.util.DotPreconditions;
import com.dotcms.util.transform.TransformerLocator;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.cache.MultiTreeCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.Params;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.LayoutChanges;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
import io.vavr.control.Try;

import java.util.*;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.dotcms.variant.VariantAPI.DEFAULT_VARIANT;


/**
 * This class provides utility routines to interact with the Multi-Tree structures in the system. A
 * Multi-Tree represents the relationship between a Legacy or Content Page, a container, and a
 * contentlet.
 * <p>
 * Therefore, the content of a page can be described as the sum of several Multi-Tree records which
 * represent each piece of information contained in it.
 * </p>
 * 
 * @author will
 */
public class MultiTreeAPIImpl implements MultiTreeAPI {

    private static Lazy<Boolean> deleteOrphanedContentsFromContainer =
            Lazy.of(() -> Config.getBooleanProperty("DELETE_ORPHANED_CONTENTS_FROM_CONTAINER", true));
    private static final String SELECT_MULTITREES_BY_VARIANT = "SELECT * FROM multi_tree WHERE variant_id = ?";
    private final Lazy<MultiTreeCache> multiTreeCache = Lazy.of(CacheLocator::getMultiTreeCache);
    private static final ObjectMapper jsonMapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();

    private static final String DELETE_ALL_MULTI_TREE_RELATED_TO_IDENTIFIER_SQL =
            "delete from multi_tree where child = ? or parent1 = ? or parent2 = ?";
    private static final String DELETE_SQL = "delete from multi_tree where parent1=? and parent2=? and child=? and  relation_type = ? and personalization = ? and variant_id = ?";
    private static final String DELETE_SQL_PERSONALIZATION_PER_PAGE = "delete from multi_tree where parent1=? and personalization = ? AND variant_id = ?";
    private static final String DELETE_ALL_MULTI_TREE_SQL = "delete from multi_tree where parent1=? AND relation_type != ? AND variant_id = ?";
    private static final String DELETE_ALL_MULTI_TREE_SQL_BY_RELATION_AND_PERSONALIZATION = "delete from multi_tree where parent1=? AND relation_type != ? and personalization = ? and variant_id = ?";
    private static final String DELETE_ALL_MULTI_TREE_SQL_BY_RELATION_AND_PERSONALIZATION_PER_LANGUAGE_NOT_SQL =
            "delete from multi_tree where variant_id = ? and relation_type != ? and personalization = ? and multi_tree.parent1 = ?  and " +
                    "child in (select distinct identifier from contentlet,multi_tree where multi_tree.child = contentlet.identifier and multi_tree.parent1 = ? and language_id = ?)";
    private static final String SELECT_COUNT_MULTI_TREE_BY_RELATION_PERSONALIZATION_PAGE_CONTAINER_AND_CHILD =
            "select count(*) cc from multi_tree where relation_type = ? and personalization = ? and " +
                    "multi_tree.parent1 = ? and multi_tree.parent2 = ? and multi_tree.child = ? and variant_id = ?";

    private static final String DELETE_ALL_MULTI_TREE_SQL_BY_RELATION_AND_PERSONALIZATION_PER_LANGUAGE_SQL =
            "delete from multi_tree where relation_type != ? and personalization = ? and multi_tree.parent1 = ?  and child in (%s)";
    private static final String SELECT_MULTI_TREE_BY_LANG =
            "select distinct contentlet.identifier from contentlet,multi_tree where multi_tree.child = contentlet.identifier and multi_tree.parent1 = ? and language_id = ? and variant_id = ?";

    private static final String UPDATE_MULTI_TREE_PERSONALIZATION = "update multi_tree set personalization = ? where personalization = ?";
    private static final String SELECT_SQL = "select * from multi_tree where parent1 = ? and parent2 = ? and child = ? and  relation_type = ? and personalization = ? and variant_id = ?";

    private static final String jsonb = DbConnectionFactory.isPostgres() ? "?" : "?::jsonb";
    private static final String INSERT_SQL = "insert into multi_tree (parent1, parent2, child, relation_type, tree_order, personalization, variant_id, style_properties) values (?,?,?,?,?,?,?," + jsonb + ")  ";

    private static final String SELECT_BY_PAGE = "select * from multi_tree where parent1 = ? order by tree_order";
    private static final String SELECT_BY_PAGE_AND_PERSONALIZATION = "select * from multi_tree where parent1 = ? and personalization = ? and variant_id = ? order by tree_order";
    private static final String SELECT_UNIQUE_PERSONALIZATION = "select distinct(personalization) from multi_tree";

    private static final String SELECT_BY_ONE_PARENT = "select * from multi_tree where (parent1 = ? or parent2 = ?) and variant_id = ? order by tree_order"; // search by page id or container id
    private static final String SELECT_BY_TWO_PARENTS = "select * from multi_tree where parent1 = ? and parent2 = ? and variant_id = 'DEFAULT'  order by tree_order";
    private static final String SELECT_ALL = "select * from multi_tree  ";
    private static final String SELECT_BY_CHILD = "select * from multi_tree where child = ? and variant_id = 'DEFAULT' order by parent1, parent2, relation_type ";

    private static final String SELECT_BY_PARENTS_AND_RELATIONS =
            " select * from multi_tree where parent1 = ? and parent2 = ? and relation_type = ? and personalization = ? and variant_id = 'DEFAULT' order by tree_order";
    private static final String SELECT_BY_CONTAINER_AND_STRUCTURE = "SELECT mt.* FROM multi_tree mt JOIN contentlet c "
            + " ON c.identifier = mt.child WHERE mt.parent2 = ? AND c.structure_inode = ? ";
    private static final String SELECT_CONTENTLET_REFERENCES = "SELECT COUNT(*) FROM multi_tree WHERE child = ?";
    private static final String SELECT_CHILD_BY_PARENT = "SELECT child FROM multi_tree WHERE multi_tree.parent1 = ? AND relation_type != ?";
    private static final String SELECT_CHILD_BY_PARENT_RELATION_PERSONALIZATION_VARIANT =
            SELECT_CHILD_BY_PARENT + " AND personalization = ? AND variant_id = ? ";
    private static final String SELECT_CHILD_BY_PARENT_RELATION_PERSONALIZATION_VARIANT_LANGUAGE =
            SELECT_CHILD_BY_PARENT_RELATION_PERSONALIZATION_VARIANT + " AND child IN (SELECT DISTINCT identifier FROM contentlet, multi_tree " +
                    "WHERE multi_tree.child = contentlet.identifier AND multi_tree.parent1 = ? AND language_id = ?)";

    @WrapInTransaction
    @Override
    public void deleteMultiTree(final MultiTree mTree) throws DotDataException {
        Logger.info(this, String.format("Deleting MultiTree: %s", mTree));
        _dbDelete(mTree);
        this.multiTreeCache.get().removeContentletReferenceCount(mTree.getContentlet());
        updateHTMLPageVersionTS(mTree.getHtmlPage(), mTree.getVariantId());
        refreshPageInCache(mTree.getHtmlPage(), mTree.getVariantId());
    }

    @WrapInTransaction
    @Override
    public void deleteMultiTreesRelatedToIdentifier(final String pageIdentifier) throws DotDataException {

        final List<MultiTree> pagesRelatedList = getMultiTreesByPage(pageIdentifier);

        new DotConnect().setSQL(DELETE_ALL_MULTI_TREE_RELATED_TO_IDENTIFIER_SQL).addParam(pageIdentifier).addParam(pageIdentifier)
                .addParam(pageIdentifier).loadResult();

        if (UtilMethods.isSet(pagesRelatedList)) {
            for (final MultiTree multiTree : pagesRelatedList) {
                this.multiTreeCache.get().removeContentletReferenceCount(multiTree.getContentlet());
                updateHTMLPageVersionTS(multiTree.getHtmlPage());
                refreshPageInCache(multiTree.getHtmlPage(), multiTree.getVariantId());
            }
        }
    }

    @WrapInTransaction
    @Override
    public void deleteMultiTree(final List<MultiTree> mTree) throws DotDataException {
        for (MultiTree tree : mTree) {
            deleteMultiTree(tree);
        }
    }

    @WrapInTransaction
    @Override
    public void deleteMultiTreeByParent(final String pageOrContainer) throws DotDataException {
        deleteMultiTree(getMultiTrees(pageOrContainer));
    }

    @WrapInTransaction
    @Override
    public void deleteMultiTreeByChild(final String contentIdentifier) throws DotDataException {
        deleteMultiTree(getMultiTreesByChild(contentIdentifier));
    }

    @WrapInTransaction
    @Override
    public void deleteMultiTree(final String pageId, final String variant)throws DotDataException {

        this.getMultiTreeByVariantWithoutFallback(pageId, variant)
                .forEach(multiTree -> this.multiTreeCache.get()
                        .removeContentletReferenceCount(multiTree.getContentlet()));

        new DotConnect().setSQL("DELETE FROM multi_tree WHERE parent1 = ? AND variant_id = ?")
                .addParam(pageId)
                .addParam(variant)
                .loadResult();

        refreshPageInCache(pageId, variant);
    }

    /**
     * Deletes multi-tree relationship given a MultiTree object. It also updates the version_ts of all
     * versions of the htmlpage passed in (multiTree.parent1)
     *
     * @param multiTree
     * @throws DotDataException
     * @throws DotSecurityException
     * 
     */
    private void _dbDelete(final MultiTree multiTree) throws DotDataException {

        new DotConnect().setSQL(DELETE_SQL)
                .addParam(multiTree.getHtmlPage())
                .addParam(multiTree.getContainerAsID())
                .addParam(multiTree.getContentlet())
                .addParam(multiTree.getRelationType())
                .addObject(multiTree.getPersonalization())
                .addParam(multiTree.getVariantId())
                .loadResult();
    }

    @CloseDBIfOpened
    @Override
    public MultiTree getMultiTree(final Identifier htmlPage, final Identifier container, final Identifier childContent,
            final String containerInstance) throws DotDataException {
        return getMultiTree(htmlPage.getId(), container.getId(), childContent.getId(), containerInstance);
    }

    @CloseDBIfOpened
    @Override
    public MultiTree getMultiTree(final String htmlPage, final String container, final String childContent,
                                  final String containerInstance, final String personalization)
            throws DotDataException {

        return this.getMultiTree(htmlPage, container, childContent, containerInstance, personalization, VariantAPI.DEFAULT_VARIANT.name());

    }

    @Override
    public MultiTree getMultiTree(final String htmlPage, final String container, final String childContent, final String containerInstance)
            throws DotDataException {

        return this.getMultiTree(htmlPage, container, childContent, containerInstance, MultiTree.DOT_PERSONALIZATION_DEFAULT, VariantAPI.DEFAULT_VARIANT.name());
    }

    @Override
    public MultiTree getMultiTree(final String htmlPage, final String container, final String childContent,
            final String containerInstance,  final String personalization, final String variantId)
            throws DotDataException {
        final DotConnect db =
                new DotConnect().setSQL(SELECT_SQL).addParam(htmlPage).addParam(container)
                        .addParam(childContent).addParam(containerInstance)
                        .addParam(personalization).addParam(variantId);
        db.loadResult();

        return TransformerLocator.createMultiTreeTransformer(db.loadObjectResults()).findFirst();

    }

    /**
     * Use {@link #getMultiTree(String, String, String, String)} This method does not use the unique id
     * specified in the #parseContainer code
     * 
     * @param htmlPage
     * @param container
     * @param childContent
     * @throws DotDataException
     */
    @Override
    @Deprecated
    public MultiTree getMultiTree(final String htmlPage, final String container, final String childContent) throws DotDataException {

        return this.getMultiTree(htmlPage, container, childContent, Container.LEGACY_RELATION_TYPE);
    }

    @Override
    public java.util.List<MultiTree> getMultiTrees(final Identifier parent) throws DotDataException {
        return getMultiTrees(parent.getId());
    }

    @Override
    public java.util.List<MultiTree> getMultiTrees(final Identifier htmlPage, final Identifier container) throws DotDataException {
        return getMultiTrees(htmlPage.getId(), container.getId());
    }

    @Override
    public java.util.List<MultiTree> getMultiTrees(final String parentInode) throws DotDataException {
        return getMultiTreesByVariant(parentInode, VariantAPI.DEFAULT_VARIANT.name());
    }

    @CloseDBIfOpened
    public List<MultiTree> getMultiTreesByVariant(final String parentId, final String variantName) throws DotDataException {

        List<MultiTree> loadObjectResults = getMultiTreeByVariantWithoutFallback(parentId, variantName);

        if (!UtilMethods.isSet(loadObjectResults)) {
            if (!VariantAPI.DEFAULT_VARIANT.name().equals(variantName)) {
                loadObjectResults = getMultiTreeByVariantWithoutFallback(parentId, VariantAPI.DEFAULT_VARIANT.name());
            }
        }

        return loadObjectResults;
    }

    private List<MultiTree> getMultiTreeByVariantWithoutFallback(final String parentId, final String variantName)
            throws DotDataException {

        List<Map<String, Object>> loadObjectResults = new DotConnect().setSQL(SELECT_BY_ONE_PARENT)
                .addParam(parentId)
                .addParam(parentId)
                .addParam(variantName)
                .loadObjectResults();

        if (!UtilMethods.isSet(loadObjectResults)) {
            if (!VariantAPI.DEFAULT_VARIANT.name().equals(variantName)) {
                loadObjectResults = new DotConnect().setSQL(SELECT_BY_ONE_PARENT)
                        .addParam(parentId)
                        .addParam(parentId)
                        .addParam(VariantAPI.DEFAULT_VARIANT.name())
                        .loadObjectResults();
            }
        }

        return TransformerLocator.createMultiTreeTransformer(loadObjectResults).asList();
    }

    @CloseDBIfOpened
    @Override
    public Set<String> getPersonalizationsForPage(final String pageID) throws DotDataException {
        IHTMLPage pageId = APILocator.getHTMLPageAssetAPI()
                .fromContentlet(APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(pageID));
        return getPersonalizationsForPage(pageId);
    }
    
    @CloseDBIfOpened
    @Override
    public Set<String> getPersonalizationsForPage(final IHTMLPage page) throws DotDataException {
        return getPersonalizationsForPage(page, VariantAPI.DEFAULT_VARIANT.name());
    }

    @CloseDBIfOpened
    @Override
    public Set<String> getPersonalizationsForPage(final IHTMLPage page, final String variantName) throws DotDataException{
        final Set<String> personalizationsForPagVariant = getPersonalizationsForPageInner(page, variantName);
        final Set<String> personalizationsForPagDefault = Collections.emptySet();

        return Stream.concat(personalizationsForPagVariant.stream(),
                        personalizationsForPagDefault.stream())
                .collect(Collectors.toSet());
    }

    @CloseDBIfOpened
    public Set<String> getPersonalizationsForPageInner(final IHTMLPage page, final String variantName) throws DotDataException{
        final Set<String> personas = new HashSet<>();

        final Table<String, String, Set<PersonalizedContentlet>> pageContents = Try.of(
                        ()-> getPageMultiTrees(page, variantName, false))
                .getOrElseThrow(e->new DotRuntimeException(e));

        for (final String containerId : pageContents.rowKeySet()) {
            for (final String uniqueId : pageContents.row(containerId).keySet()) {
                pageContents.get(containerId, uniqueId)
                        .forEach(p->personas.add(p.getPersonalization()));
            }
        }

        return personas;
    }

    @CloseDBIfOpened
    @Override
    public Set<String> getPersonalizations () throws DotDataException {

        final Set<String> personalizationSet = new HashSet<>();
        final  List<Map<String, Object>>  personalizationMaps =
                new DotConnect().setSQL(SELECT_UNIQUE_PERSONALIZATION).loadObjectResults();

        for (final Map<String, Object> personalizationMap : personalizationMaps) {

            personalizationSet.add(personalizationMap.values()
                    .stream().findFirst().orElse(StringPool.BLANK).toString());
        }

        return personalizationSet;
    }

    @WrapInTransaction
    @Override
    public Set<String> cleanUpUnusedPersonalization(final Predicate<String> personalizationFilter) throws DotDataException {

        final Set<Params> personalizationToRemoveParamsSet = new HashSet<>();
        final Set<String> personalizationToRemoveSet       = new HashSet<>();
        final Set<String> currentPersonalizationSet        = this.getPersonalizations();

        currentPersonalizationSet.stream()
                .filter(personalizationFilter)
                .forEach(personalization -> {

                    personalizationToRemoveParamsSet.add(new Params(personalization));
                    personalizationToRemoveSet.add(personalization);
                });

        if (!personalizationToRemoveParamsSet.isEmpty()) {

            new DotConnect().executeBatch(
                    "DELETE FROM multi_tree where personalization = ?",
                    personalizationToRemoveParamsSet);
        }

        return personalizationToRemoveSet;
    }

    @WrapInTransaction
    @Override
    public List<MultiTree> copyPersonalizationForPage (final String pageId,
                                                       final String basePersonalization,
                                                       final String newPersonalization,
                                                       final String targetVariantName )  throws DotDataException {

        List<MultiTree> multiTrees = null;
        final ImmutableList.Builder<MultiTree> personalizedContainerListBuilder =
                new ImmutableList.Builder<>();

        List<MultiTree> basedMultiTreeList = this.getMultiTreesByPersonalizedPage(pageId,
                basePersonalization, targetVariantName);

        if (!UtilMethods.isSet(basedMultiTreeList)) {
            basedMultiTreeList = this.getMultiTreesByPersonalizedPage(pageId,
                    basePersonalization, VariantAPI.DEFAULT_VARIANT.name());
        }

        if (null != basedMultiTreeList) {

            basedMultiTreeList.forEach(multiTree -> personalizedContainerListBuilder.add(
                        MultiTree.buildMultitree(multiTree, targetVariantName, newPersonalization))
            );

            multiTrees = personalizedContainerListBuilder.build();
            this.saveMultiTrees(multiTrees);
        }

        return multiTrees;
    } // copyPersonalizationForPage.

    @WrapInTransaction
    @Override
    public void deletePersonalizationForPage(final String pageId, final String personalization,
            final String variantName) throws DotDataException {

        Logger.debug(this, "Removing personalization for: " + pageId +
                                ", personalization: " + personalization);
        final List<MultiTree> pageMultiTrees = this.getMultiTreesByPersonalizedPage(pageId,
                personalization, variantName);

        new DotConnect().setSQL(DELETE_SQL_PERSONALIZATION_PER_PAGE)
                .addParam(pageId)
                .addParam(personalization)
                .addParam(variantName)
                .loadResult();
        pageMultiTrees.forEach(multiTree -> this.multiTreeCache.get().removeContentletReferenceCount(multiTree.getContentlet()));
        updateHTMLPageVersionTS(pageId);
        refreshPageInCache(pageId, VariantAPI.DEFAULT_VARIANT.name());
    } // deletePersonalizationForPage.

    /**
     * Returns the trees associated to a page and personalization
     *
     * @param pageId String page id
     * @param personalization String personalization to find
     * @return List of MultiTree
     * @throws DotDataException
     */
    @CloseDBIfOpened
    @Override
    public List<MultiTree> getMultiTreesByPersonalizedPage(final String pageId,
            final String personalization, final String variantName) throws DotDataException {

        return TransformerLocator.createMultiTreeTransformer(
                new DotConnect().setSQL(SELECT_BY_PAGE_AND_PERSONALIZATION)
                        .addParam(pageId)
                        .addParam(personalization)
                        .addParam(variantName)
                        .loadObjectResults()
                ).asList();
    }

    /**
     * Returns the tree with the pages that matched on the parent1
     * 
     * @param parentInode
     * @return
     * @throws DotDataException
     */
    @CloseDBIfOpened
    @Override
    public List<MultiTree> getMultiTreesByPage(final String parentInode) throws DotDataException {

        final DotConnect db = new DotConnect().setSQL(SELECT_BY_PAGE).addParam(parentInode);
        return TransformerLocator.createMultiTreeTransformer(db.loadObjectResults()).asList();
    }

    @CloseDBIfOpened
    @Override
    public List<MultiTree> getAllMultiTrees() {
        try {
            final DotConnect db = new DotConnect().setSQL(SELECT_ALL);

            return TransformerLocator.createMultiTreeTransformer(db.loadObjectResults()).asList();
        } catch (Exception e) {
            Logger.error(MultiTreeAPIImpl.class, "getMultiTree failed:" + e, e);
            throw new DotRuntimeException(e);
        }
    }

    @Override
    public List<MultiTree> getMultiTrees(final String htmlPage, final String container, final String containerInstance) {

        return this.getMultiTrees(htmlPage, container, containerInstance, MultiTree.DOT_PERSONALIZATION_DEFAULT);
    }

    @CloseDBIfOpened
    @Override
    public List<MultiTree> getMultiTrees(final String htmlPage, final String container,
                                         final String containerInstance, final String personalization) {

        try {

            return TransformerLocator.createMultiTreeTransformer(new DotConnect().setSQL(SELECT_BY_PARENTS_AND_RELATIONS)
                    .addParam(htmlPage).addParam(container).addParam(containerInstance).addObject(personalization).loadObjectResults()).asList();
        } catch (Exception e) {
            Logger.error(MultiTreeAPIImpl.class, "getMultiTree failed:" + e, e);
            throw new DotRuntimeException(e);
        }
    }

    @Override
    public List<MultiTree> getMultiTrees(final IHTMLPage htmlPage, final Container container) throws DotDataException {
        return getMultiTrees(htmlPage.getIdentifier(), container.getIdentifier());
    }

    @CloseDBIfOpened
    @Override
    public List<MultiTree> getMultiTrees(final String htmlPage, final String container) throws DotDataException {

        final DotConnect db = new DotConnect().setSQL(SELECT_BY_TWO_PARENTS).addParam(htmlPage).addParam(container);
        return TransformerLocator.createMultiTreeTransformer(db.loadObjectResults()).asList();

    }

    @Override
    public List<MultiTree> getMultiTrees(final IHTMLPage htmlPage, final Container container, final String containerInstance) {
        return getMultiTrees(htmlPage.getIdentifier(), container.getIdentifier(), containerInstance);
    }

    @Override
    public List<MultiTree> getContainerMultiTrees(final String containerIdentifier) throws DotDataException {
        return getMultiTrees(containerIdentifier);
    }

    @CloseDBIfOpened
    @Override
    public List<MultiTree> getMultiTreesByChild(final String contentIdentifier) throws DotDataException {

        final DotConnect db = new DotConnect().setSQL(SELECT_BY_CHILD).addParam(contentIdentifier);

        return TransformerLocator.createMultiTreeTransformer(db.loadObjectResults()).asList();

    }

    /**
     * Get a list of MultiTree for Contentlets using a specific Structure and specific Container
     * 
     * @param containerIdentifier
     * @param structureInode
     * @return List of MultiTree
     */
    @CloseDBIfOpened
    @Override
    public List<MultiTree> getContainerStructureMultiTree(final String containerIdentifier, final String structureInode) {
        try {
            final DotConnect dc =
                    new DotConnect().setSQL(SELECT_BY_CONTAINER_AND_STRUCTURE).addParam(containerIdentifier).addParam(structureInode);

            return TransformerLocator.createMultiTreeTransformer(dc.loadObjectResults()).asList();

        } catch (DotDataException e) {
            Logger.error(MultiTreeAPIImpl.class, "getContainerStructureMultiTree failed:" + e, e);
            throw new DotRuntimeException(e.toString());
        }
    }

    @Override
    @WrapInTransaction
    public void saveMultiTree(final MultiTree mTree) throws DotDataException {
        Logger.debug(this, () -> String.format("Saving MultiTree: %s", mTree));
        _dbUpsert(mTree);
        this.multiTreeCache.get().removeContentletReferenceCount(mTree.getContentlet());
        updateHTMLPageVersionTS(mTree.getHtmlPage(), mTree.getVariantId());
        refreshPageInCache(mTree.getHtmlPage(), mTree.getVariantId());

    }

    @Override
    @WrapInTransaction
    public void saveMultiTreeAndReorder(final MultiTree mTree) throws DotDataException {
        Logger.debug(this, () -> String.format("Saving MultiTree and Reordering: %s", mTree));
        _reorder(mTree);
        updateHTMLPageVersionTS(mTree.getHtmlPage(), mTree.getVariantId());
        refreshPageInCache(mTree.getHtmlPage(), mTree.getVariantId());

    }

    /**
     * Saves a multi-tree
     * <ol>
     * <li>The identifier of the Content Page.</li>
     * <li>The identifier of the container in the page.</li>
     * <li>The identifier of the contentlet itself.</li>
     * <li>The type of content relation.</li>
     * <li>The order in which this construct is added to the database.</li>
     * </ol>
     * 
     * @param mTrees - The multi-tree structure.
     * @throws DotDataException
     */
    @Override
    @WrapInTransaction
    public void saveMultiTrees(final List<MultiTree> mTrees) throws DotDataException {
        if (mTrees == null || mTrees.isEmpty()) {
            throw new DotDataException("empty list passed in");
        }

        int i = 0;
        for (final MultiTree tree : mTrees) {
            _dbUpsert(tree.setTreeOrder(i++));
            this.multiTreeCache.get().removeContentletReferenceCount(tree.getContentlet());
        }

        final MultiTree mTree = mTrees.get(0);
        updateHTMLPageVersionTS(mTree.getHtmlPage(), mTree.getVariantId());
        refreshPageInCache(mTree.getHtmlPage(), mTree.getVariantId());
    }

    /**
     * Save a collection of {@link MultiTree} and link them with a page, Also delete all the
     * {@link MultiTree} linked previously with the page.
     *
     * @param pageId Page's identifier
     * @param multiTrees
     * @throws DotDataException
     */
    @Override
    @WrapInTransaction
    public void overridesMultitreesByPersonalization(final String pageId,
                                                     final String personalization,
                                                     final List<MultiTree> multiTrees) throws DotDataException {

        this.overridesMultitreesByPersonalization(pageId, personalization, multiTrees,
                Optional.empty(), VariantAPI.DEFAULT_VARIANT.name());  // no lang passed, will deletes everything
    }

    /**
     * Save a collection of {@link MultiTree} and link them with a page, Also delete all the
     * {@link MultiTree} linked previously with the page.
     *
     * @param pageId {@link String} Page's identifier
     * @param personalization {@link String} personalization token
     * @param multiTrees {@link List} of {@link MultiTree} to safe
     * @param languageIdOpt {@link Optional} {@link Long}   optional language, if present will deletes only the contentlets that have a version on this language.
     *                                        Since it is by identifier, when deleting for instance in spanish, will remove the english and any other lang version too.
     * @throws DotDataException 
     */
    @Override
    @WrapInTransaction
    public void overridesMultitreesByPersonalization(final String pageId,
                                                    final String personalization,
                                                    final List<MultiTree> multiTrees,
                                                    final Optional<Long> languageIdOpt,
                                                    final String variantId) throws DotDataException {

        Logger.info(this, String.format(
                "Overriding MultiTrees: pageId -> %s personalization -> %s multiTrees-> %s ",
                pageId, personalization, multiTrees));

        if (multiTrees == null) {

            throw new DotDataException("empty list passed in");
        }

        Logger.debug(MultiTreeAPIImpl.class, ()->String.format("Saving page's content: %s", multiTrees));
        Set<String> originalContentletIds = new HashSet<>();
        final DotConnect db = new DotConnect();
        if (languageIdOpt.isPresent()) {
            if (DbConnectionFactory.isMySql()) {
                deleteMultiTreeToMySQL(pageId, personalization, languageIdOpt, variantId);
           } else {
                originalContentletIds = this.getOriginalContentlets(pageId, ContainerUUID.UUID_DEFAULT_VALUE,
                        personalization, variantId, languageIdOpt.get());
                db.setSQL(DELETE_ALL_MULTI_TREE_SQL_BY_RELATION_AND_PERSONALIZATION_PER_LANGUAGE_NOT_SQL)
                        .addParam(variantId)
                        .addParam(ContainerUUID.UUID_DEFAULT_VALUE)
                        .addParam(personalization)
                        .addParam(pageId)
                        .addParam(pageId)
                        .addParam(languageIdOpt.get())
                        .loadResult();
            }
        } else {
            originalContentletIds = this.getOriginalContentlets(pageId, ContainerUUID.UUID_DEFAULT_VALUE,
                    personalization, variantId);
            db.setSQL(DELETE_ALL_MULTI_TREE_SQL_BY_RELATION_AND_PERSONALIZATION)
                    .addParam(pageId)
                    .addParam(ContainerUUID.UUID_DEFAULT_VALUE)
                    .addParam(personalization)
                    .addParam(variantId)
                    .loadResult();
        }

        if (!multiTrees.isEmpty()) {

            copyMultiTree(pageId, multiTrees);
        }
        this.refreshContentletReferenceCount(originalContentletIds, multiTrees);
        updateHTMLPageVersionTS(pageId, variantId);

        refreshPageInCache(pageId, variantId);
    }

    public void copyMultiTree(final String pageId, final List<MultiTree> multiTrees) throws DotDataException {
        copyMultiTree(pageId, multiTrees, null);
    }


    /**
     * Copy a collection of {@link MultiTree} but to a different {@link Variant}.
     *
     * @param pageId {@link String} Page's identifier
     * @param multiTrees {@link List} of {@link MultiTree} to copy
     * @param variantName {@link String} name of the variant to copy to
     * @throws DotDataException
     */
    @Override
    @WrapInTransaction
    public void copyMultiTree(final String pageId, final List<MultiTree> multiTrees,
            final String variantName) throws DotDataException {

        DotPreconditions.notNull(multiTrees, () -> "multiTrees can't be null");

        final DotConnect db = new DotConnect();
        final List<Params> insertParams = Lists.newArrayList();

        for (final MultiTree tree : multiTrees) {
            final String copiedMultiTreeVariantId =
                    UtilMethods.isSet(variantName) ? variantName : tree.getVariantId();

            //This is for checking if the content we are trying to add is already added into the container
            db.setSQL(SELECT_COUNT_MULTI_TREE_BY_RELATION_PERSONALIZATION_PAGE_CONTAINER_AND_CHILD)
                    .addParam(tree.getRelationType())
                    .addParam(tree.getPersonalization())
                    .addParam(pageId)
                    .addParam(tree.getContainerAsID())
                    .addParam(tree.getContentlet())
                    .addParam(copiedMultiTreeVariantId);
            final int contentExist = Integer.parseInt(db.loadObjectResults().get(0).get("cc").toString());
            if(contentExist != 0){
                final String contentletTitle = APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(tree.getContentlet()).getTitle();
                final String errorMsg = String.format("Content '%s' [ %s ] has already been added to Container " +
                                                              "'%s'", contentletTitle, tree.getContentlet(),
                        tree.getContainer());
                Logger.debug(MultiTreeAPIImpl.class, errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            final String stylePropertiesJson = serializeStyleProperties(tree.getStyleProperties());

            insertParams
                    .add(new Params(pageId, tree.getContainerAsID(), tree.getContentlet(),
                            tree.getRelationType(), tree.getTreeOrder(), tree.getPersonalization(),
                            copiedMultiTreeVariantId, stylePropertiesJson));
        }
        db.executeBatch(INSERT_SQL, insertParams);
    }

    @Override
    public void overridesMultitreesByPersonalization(String pageId,
            String personalization, List<MultiTree> multiTrees,
            Optional<Long> languageIdOpt) throws DotDataException {
        overridesMultitreesByPersonalization(pageId, personalization, multiTrees,
                languageIdOpt, VariantAPI.DEFAULT_VARIANT.name());
    }

    private void deleteMultiTreeToMySQL(
            final String pageId,
            final String personalization,
            final Optional<Long> languageIdOpt, final String variantId) throws DotDataException {
        final DotConnect db = new DotConnect();

        final List<String> multiTreesId = db.setSQL(SELECT_MULTI_TREE_BY_LANG)
            .addParam(pageId)
            .addParam(languageIdOpt.get())
            .addParam(variantId)
            .loadObjectResults()
            .stream()
            .map(map -> String.format("'%s'", map.get("identifier")))
            .collect(Collectors.toList());

        if (!multiTreesId.isEmpty()) {

            db.setSQL(String.format(DELETE_ALL_MULTI_TREE_SQL_BY_RELATION_AND_PERSONALIZATION_PER_LANGUAGE_SQL, Utility.joinList(",", multiTreesId)))
                    .addParam(ContainerUUID.UUID_DEFAULT_VALUE)
                    .addParam(personalization)
                    .addParam(pageId)
                    .loadResult();
        }
    }

    @Override
    @WrapInTransaction
    public void updatePersonalization(final String currentPersonalization, final String newPersonalization) throws DotDataException {

        Logger.info(this, "Updating the personalization: " + currentPersonalization +
                                        " to " + newPersonalization);
        final List<Map<String, Object>> currentPersonalizationPages =
                new DotConnect().setSQL("select parent1 from multi_tree where personalization = ?")
                .addObject(currentPersonalization).loadObjectResults();

        new DotConnect().setSQL(UPDATE_MULTI_TREE_PERSONALIZATION)
                .addParam(newPersonalization)
                .addParam(currentPersonalization).loadResult();

        if (UtilMethods.isSet(currentPersonalizationPages)) {

            Logger.info(this, "The personalization: " + currentPersonalization + ", has been changed to : " + newPersonalization +
                    " all related pages multitrees will be invalidated: " + currentPersonalizationPages.size());
            for (final Map<String, Object> pageMap : currentPersonalizationPages) {

                final String pageId = (String) pageMap.get("parent1");
                Logger.info(this, "Invalidating the multitrees for the page: " + pageId);
                CacheLocator.getMultiTreeCache()
                        .removePageMultiTrees(pageId, VariantAPI.DEFAULT_VARIANT.name());
            }
        }
    }

    /**
     * Saves a collection of {@link MultiTree} objects and links them to an HTML Page. It deletes all the
     * {@link MultiTree} previously linked with the page before adding the new data.
     *
     * @param pageId The Page's identifier.
     * @param mTrees The list of {@link MultiTree} objects that make up the HTML Page contents.
     *
     * @throws DotDataException The list of MultiTree entries is null.
     */
    @Override
    @WrapInTransaction
    public void saveMultiTrees(final String pageId, final String variantName, final List<MultiTree> mTrees) throws DotDataException {

        DotPreconditions.isTrue(mTrees.stream().filter(mTree -> !mTree.getVariantId().equals(variantName)).count() == 0,
                () -> "All the MultiTree must have the variantName: " + variantName);

        Logger.debug(this, () -> String
                .format("Saving MultiTrees: pageId -> %s multiTrees-> %s", pageId, mTrees));
        if (mTrees == null) {
            throw new DotDataException(String.format("MultiTree list for page ID '%s' cannot be null", pageId));
        }

        Logger.debug(MultiTreeAPIImpl.class, ()->String.format("Saving page's content: %s", mTrees));
        final Set<String> originalContents = this.getOriginalContentlets(pageId, ContainerUUID.UUID_DEFAULT_VALUE);
        final DotConnect db = new DotConnect();
        db.setSQL(DELETE_ALL_MULTI_TREE_SQL)
                .addParam(pageId)
                .addParam(ContainerUUID.UUID_DEFAULT_VALUE)
                .addParam(variantName)
                .loadResult();

        if (!mTrees.isEmpty()) {
            final List<Params> insertParams = Lists.newArrayList();
            for (final MultiTree tree : mTrees) {
                final String stylePropertiesJson = serializeStyleProperties(tree.getStyleProperties());

                insertParams
                        .add(new Params(pageId, tree.getContainerAsID(), tree.getContentlet(),
                                tree.getRelationType(), tree.getTreeOrder(), tree.getPersonalization(), tree.getVariantId(), stylePropertiesJson));
            }

            db.executeBatch(INSERT_SQL, insertParams);
        }

        this.refreshContentletReferenceCount(originalContents, mTrees);
        updateHTMLPageVersionTS(pageId, variantName);

        refreshPageInCache(pageId, variantName);
    }

    @Override
    public List<String> getContainersId(final String pageId) throws DotDataException {
        return this.getMultiTrees(pageId).stream().map(multiTree -> multiTree.getContainer()).distinct().sorted()
                .collect(Collectors.toList());
    }


    private void _dbUpsert(final MultiTree mtree) throws DotDataException {
        _dbDelete(mtree);
        _dbInsert(mtree);

    }

    private void _reorder(final MultiTree treeInput) throws DotDataException {

        final MultiTree tree = this.checkPersonalization(treeInput);
        List<MultiTree> trees = getMultiTrees(tree.getHtmlPage(), tree.getContainerAsID(), tree.getRelationType(), tree.getPersonalization());
        trees = trees.stream().filter(rowTree -> !rowTree.equals(tree)).collect(Collectors.toList());
        int maxOrder = (tree.getTreeOrder() > trees.size()) ? trees.size() : tree.getTreeOrder();
        trees.add(maxOrder, tree);

        saveMultiTrees(trees);
    }

    private MultiTree checkPersonalization(final MultiTree tree) {

        return null != tree && null != tree.getPersonalization()?
                tree: MultiTree.personalized(tree, MultiTree.DOT_PERSONALIZATION_DEFAULT);
    }

    private void _dbInsert(final MultiTree multiTree) throws DotDataException {

        Logger.debug(this, () -> String.format("_dbInsert -> Saving MultiTree: %s", multiTree));

        final String stylePropertiesJson = serializeStyleProperties(multiTree.getStyleProperties());

        new DotConnect().setSQL(INSERT_SQL).addParam(multiTree.getHtmlPage())
                .addParam(multiTree.getContainerAsID()).addParam(multiTree.getContentlet())
                .addParam(multiTree.getRelationType()).addParam(multiTree.getTreeOrder())
                .addObject(multiTree.getPersonalization()).addParam(multiTree.getVariantId())
                .addParam(stylePropertiesJson)
                .loadResult();
    }

    /**
     * Serializes styleProperties Map to JSON string for database storage.
     * Returns null if the map is null or empty.
     *
     * @param styleProperties Map of style properties
     * @return JSON string or null
     */
    private String serializeStyleProperties(final Map<String, Object> styleProperties) {
        return Try.of(() -> UtilMethods.isSet(styleProperties)
                        ? jsonMapper.writeValueAsString(styleProperties)
                        : null)
                .onFailure(e -> Logger.error(this, "Error serializing style properties: " + e.getMessage(), e))
                .getOrNull();
    }

    /**
     * Update the version_ts of all versions of the HTML Page with the given id. If a MultiTree Object
     * has been added or deleted from this page, its version_ts value needs to be updated so it can be
     * included in future Push Publishing tasks
     * 
     * @param pageId The HTMLPage Identifier to pass in
     * @throws DotContentletStateException
     * @throws DotDataException
     * @throws DotSecurityException
     * 
     */
    private void updateHTMLPageVersionTS(final String pageId, final String variantName) throws DotDataException {
        final List<ContentletVersionInfo> contentletVersionInfos =
                APILocator.getVersionableAPI().findContentletVersionInfos(pageId, variantName);
        for (final ContentletVersionInfo versionInfo : contentletVersionInfos) {
            if (versionInfo != null) {
                versionInfo.setVersionTs(new Date());
                APILocator.getVersionableAPI().saveContentletVersionInfo(versionInfo);
            }
        }
    }

    private void updateHTMLPageVersionTS(final String pageId) throws DotDataException {
        final List<Variant> variants = APILocator.getVariantAPI().getVariants();

        for (Variant variant : variants) {
            updateHTMLPageVersionTS(pageId, variant.name());
        }
    }

    private void refreshPageInCache(final String pageIdentifier) throws DotDataException {

            Stream.concat(getPageVariants(pageIdentifier).stream(), Stream.of(DEFAULT_VARIANT.name()))
                    .distinct()
                    .forEach(variantName -> {
                        try {
                            refreshPageInCacheInner(pageIdentifier, variantName);
                        } catch (DotDataException e) {
                            Logger.error(this, e.getMessage(), e);
                        }
                });
    }

    private Collection<String> getPageVariants(final String pageIdentifier) throws DotDataException {
        try{
            final Contentlet pageAsContent = APILocator.getContentletAPI()
                    .findContentletByIdentifierAnyLanguageAnyVariant(pageIdentifier);

            return APILocator.getExperimentsAPI().listActive(pageAsContent.getHost()).stream()
                    .flatMap(experiment -> experiment.trafficProportion().variants().stream())
                    .map(ExperimentVariant::id)
                    .collect(Collectors.toSet());
        } catch (DotContentletStateException e) {
            Logger.warn(this.getClass(), e.getMessage());
            return Collections.emptyList();
        }
    }


    private void refreshPageInCache(final String pageIdentifier, final String variantName) throws DotDataException {

        if (VariantAPI.DEFAULT_VARIANT.name().equals(variantName)) {
            refreshPageInCache(pageIdentifier);
        } else {
            refreshPageInCacheInner(pageIdentifier, variantName);
        }
    }

    private void refreshPageInCacheInner(final String pageIdentifier, final String variantName)
            throws DotDataException {

        CacheLocator.getMultiTreeCache()
                .removePageMultiTrees(pageIdentifier, variantName);

        final Collection<String> inodeSet = getInodes(pageIdentifier, variantName);

        try {
            final List<Contentlet> contentlets = APILocator.getContentletAPIImpl()
                    .findContentlets(Lists.newArrayList(inodeSet));

            final PageLoader pageLoader = new PageLoader();

            for (final Contentlet pageContent : contentlets) {

                final IHTMLPage htmlPage = APILocator.getHTMLPageAssetAPI().fromContentlet(pageContent);

                pageLoader.invalidate(htmlPage, variantName, PageMode.EDIT_MODE);
                pageLoader.invalidate(htmlPage, variantName, PageMode.PREVIEW_MODE);
                pageLoader.invalidate(htmlPage, variantName, PageMode.LIVE);
            }
        } catch (DotStateException | DotSecurityException e) {

            Logger.warn(MultiTreeAPIImpl.class, "unable to refresh page cache:" + e.getMessage());
        }
    }

    private Collection<String> getInodes(String pageIdentifier, String variantName)
            throws DotDataException {

        final List<ContentletVersionInfo> contentletVersionInfos = APILocator.getVersionableAPI()
                .findContentletVersionInfos(pageIdentifier, variantName);

        if (UtilMethods.isSet(contentletVersionInfos)) {
            return getInodes(contentletVersionInfos);
        } else {
           return  getInodes(APILocator.getVersionableAPI()
                   .findContentletVersionInfos(pageIdentifier, VariantAPI.DEFAULT_VARIANT.name()));
        }
    }

    private Collection<String> getInodes(final List<ContentletVersionInfo> contentletVersionInfos) {
        final Set<String> inodeSet = new HashSet<>();

        for (final ContentletVersionInfo versionInfo : contentletVersionInfos) {

            inodeSet.add(versionInfo.getWorkingInode());
            if (versionInfo.getLiveInode() != null) {
                inodeSet.add(versionInfo.getLiveInode());
            }
        }

        return inodeSet;
    }

    @WrapInTransaction
    @Override
    public void deleteMultiTreeByIdentifier(final Identifier identifier) throws DotDataException {

        final List<MultiTree> multiTrees = this.getMultiTrees(identifier);
        if (UtilMethods.isSet(multiTrees)) {

            for (final MultiTree multiTree : multiTrees) {
                this.deleteMultiTree(multiTree);
            }
        }
    }

    @WrapInTransaction
    @Override
    public void deleteMultiTreesForIdentifiers(final List<String> identifiers) throws DotDataException {

        if (UtilMethods.isSet(identifiers)) {

            try {

                final String sInodeIds = StringUtils.join(identifiers, StringPool.COMMA);
                new DotConnect().executeStatement("delete from multi_tree where child in (" + sInodeIds + ") or parent1 in (" + sInodeIds
                        + ") or parent2 in (" + sInodeIds + ")");
                identifiers.forEach(contentletId -> this.multiTreeCache.get().removeContentletReferenceCount(contentletId));
            } catch (final SQLException e) {
                throw new DotDataException(e);
            }
        }
    }

    @Override
    public Table<String, String, Set<PersonalizedContentlet>> getPageMultiTrees(final IHTMLPage page, final boolean liveMode)
            throws DotDataException, DotSecurityException {
        return getPageMultiTrees(page, VariantAPI.DEFAULT_VARIANT.name(), liveMode);
    }

    @CloseDBIfOpened
    @Override
    public Table<String, String, Set<PersonalizedContentlet>> getPageMultiTrees(final IHTMLPage page,
            final String variantName, final boolean liveMode) throws DotDataException, DotSecurityException {

        final String multiTreeCacheKey = page.getIdentifier();
        final Optional<Table<String, String, Set<PersonalizedContentlet>>> pageContentsOpt =
                CacheLocator.getMultiTreeCache().getPageMultiTrees(multiTreeCacheKey, variantName, liveMode);

        if(pageContentsOpt.isPresent()) {
            return pageContentsOpt.get();
        }

        final Table<String, String, Set<PersonalizedContentlet>> pageContents = HashBasedTable.create();
        final Collection<MultiTree> multiTrees = this.getMultiTreesByVariant(page.getIdentifier(),
                variantName);

        final ContainerAPI    containerAPI  = APILocator.getContainerAPI();
        final ContentletAPI   contentletAPI = APILocator.getContentletAPI();
        final User systemUser = APILocator.systemUser();

        for (final MultiTree multiTree : multiTrees) {

            Container container   = null;
            final String    containerId     = multiTree.getContainerAsID();
            final String    personalization = multiTree.getPersonalization();

            try {

                container = liveMode?
                        containerAPI.getLiveContainerById(containerId, systemUser, false):
                        containerAPI.getWorkingContainerById(containerId, systemUser, false);

                if (container == null && !liveMode) {
                    continue;
                }
            } catch (NotFoundInDbException e) {

                Logger.debug(this, e.getMessage(), e);
                continue;
            }

            Contentlet contentlet = null;
            try {
                contentlet = contentletAPI.findContentletByIdentifierAnyLanguageAnyVariant(multiTree.getContentlet());
            } catch (DotDataException  | DotContentletStateException e) {
                Logger.debug(this.getClass(), "invalid contentlet on multitree:" + multiTree
                        + ", msg: " + e.getMessage(), e);
                Logger.warn(this.getClass(), "invalid contentlet on multitree:" + multiTree);
            }

            if (contentlet != null) {

                final Set<PersonalizedContentlet> myContents = pageContents.contains(containerId, multiTree.getRelationType())
                        ? pageContents.get(containerId, multiTree.getRelationType())
                        : new LinkedHashSet<>();

                if (container != null) {

                    myContents.add(new PersonalizedContentlet(multiTree.getContentlet(), personalization, multiTree.getTreeOrder()));
                }

                pageContents.put(containerId, multiTree.getRelationType(), myContents);
            }
        }

        this.addEmptyContainers(page, pageContents, liveMode);
        
        CacheLocator.getMultiTreeCache().putPageMultiTrees(multiTreeCacheKey, variantName, liveMode, pageContents);
        return pageContents;
    }

    /**
     * Returns the list of Containers from the drawn layout of a given Template.
     *
     * @param template The {@link Template} which holds the Containers.
     *
     * @return The list of {@link ContainerUUID} objects.
     *
     * @throws DotSecurityException The internal APIs are not allowed to return data for the specified user.
     * @throws DotDataException     The information for the Template could not be accessed.
     */
    private List<ContainerUUID> getDrawedLayoutContainerUUIDs (final Template template) throws DotSecurityException, DotDataException {
        final TemplateLayout layout =
                DotTemplateTool.themeLayout(template.getInode(), APILocator.systemUser(), false);
        return APILocator.getTemplateAPI().getContainersUUID(layout);
    }

    /**
     * Traverses the {@link Template} from an HTML Page and retrieves the Containers that are currently empty, i.e.,
     * Containers that have no content in them.
     *
     * @param page         The {@link IHTMLPage} object that will be inspected.
     * @param pageContents The parts that make up the {@link IHTMLPage} object.
     * @param liveMode     If set to {@code true}, only the live version of the Containers will be retrieved. If set to
     *                     {@code} false, only the working version will be retrieved.
     *
     * @throws DotDataException     An error occurred qhen retrieving the required information from the data source.
     * @throws DotSecurityException The internal APIs are not allowed to return data for the specified user.
     */
    private void addEmptyContainers(final IHTMLPage page,
                                    final Table<String, String, Set<PersonalizedContentlet>> pageContents,
                                    final boolean liveMode)
            throws DotDataException, DotSecurityException {

        try {

            final List<ContainerUUID> containersUUID;
            final Template template =
                    APILocator.getTemplateAPI().findWorkingTemplate(page.getTemplateId(), APILocator.getUserAPI().getSystemUser(), false);
            try {
                containersUUID = template.isDrawed()?
                        this.getDrawedLayoutContainerUUIDs(template):
                        APILocator.getTemplateAPI().getContainersUUIDFromDrawTemplateBody(template.getBody());
            } catch (final Exception e) {
                Logger.error(this, String.format("An error occurred when retrieving empty Containers from page with " +
                        "ID '%s' in liveMode '%s': %s", page.getIdentifier(), liveMode, e.getMessage()), e);
                return;
            }

            for (final ContainerUUID containerUUID : containersUUID) {

                Container container = null;
                try {

                    final Optional<Container> optionalContainer =
                            APILocator.getContainerAPI().findContainer(containerUUID.getIdentifier(), APILocator.systemUser(), liveMode, false);

                    container = optionalContainer.isPresent() ? optionalContainer.get() : null;

                    if (container == null && !liveMode) {
                        continue;
                    }
                } catch (final NotFoundInDbException| DotRuntimeException e) {
                    Logger.debug(this, e.getMessage(), e);
                    continue;
                }

                if (!doesPageContentsHaveContainer(pageContents, containerUUID, container)) {
                    pageContents.put(container.getIdentifier(), containerUUID.getUUID(), new LinkedHashSet<>());
                }
            }
        } catch (final RuntimeException e) {
            Logger.error(this, String.format("An error occurred when retrieving empty Containers from page with ID " +
                    "'%s' in liveMode '%s': %s", page.getIdentifier(), liveMode, e.getMessage()), e);
        }
    }

    /**
     * Check if a container with the same id or path (in case of {@link FileAssetContainer}), exist into pageContents.
     * Also support legacy 'LEGACY_RELATION_TYPE' uuid value
     *
     * @param pageContents Table of the {@link MultiTree} into the page
     * @param containerUUID container's UUID link with the page
     * @param container container
     * @return true in case of the containerUUId is contains in pageContents
     */
    protected boolean doesPageContentsHaveContainer(
            final Table<String, String, Set<PersonalizedContentlet>> pageContents,
            final ContainerUUID containerUUID,
            final Container container) {

        if(pageContents.contains(container.getIdentifier(), containerUUID.getUUID())){
            return true;
        } else if(pageContents.contains(container.getIdentifier(), ParseContainer.PARSE_CONTAINER_UUID_PREFIX + containerUUID.getUUID())) {
            return true;
        } else if (ContainerUUID.UUID_LEGACY_VALUE.equals(containerUUID.getUUID())) {
            boolean pageContenstContains = pageContents.contains(containerUUID.getIdentifier(), ContainerUUID.UUID_START_VALUE)
                    ||pageContents.contains(containerUUID.getIdentifier(),
                    ParseContainer.PARSE_CONTAINER_UUID_PREFIX + ContainerUUID.UUID_START_VALUE) ;

            if (!pageContenstContains && container instanceof FileAssetContainer) {
                pageContenstContains = pageContents.contains(container.getIdentifier(), ContainerUUID.UUID_START_VALUE)
                    || pageContents.contains(container.getIdentifier(),
                        ParseContainer.PARSE_CONTAINER_UUID_PREFIX + ContainerUUID.UUID_START_VALUE);
            }

            return pageContenstContains;
        } else {
            return false;
        }
    }

    @Override
    public int getAllContentletReferencesCount(final String contentletId) throws DotDataException {
        final Optional<Integer> referenceCount = this.multiTreeCache.get().getContentletReferenceCount(contentletId);
        if (referenceCount.isPresent()) {
            return referenceCount.get();
        }
        final Long count = this.getContentletReferenceCountFromDB(contentletId);
        this.multiTreeCache.get().putContentletReferenceCount(contentletId, count.intValue());
        return count.intValue();
    }



    @Override
    @WrapInTransaction
    public List<MultiTree> getMultiTrees(final Variant variant) throws DotDataException {
        final List<Map<String, Object>> results = new DotConnect().setSQL(SELECT_MULTITREES_BY_VARIANT)
                .addParam(variant.name())
                .loadResults();

        return TransformerLocator.createMultiTreeTransformer(results).asList();
    }


    /**
     * Update the {@link MultiTree} according to the {@link LayoutChanges}.
     * The steps follows to update the MultiTree are:
     *
     * 1. Mark all the MultiTree to be updated. These are all the MultiTree on the pageIds and in the Containers that
     * were changed. These MultiTree are marked by turning their relation_type to a negative number.
     * We avoid using the -1 value because it's already used for orphan Contentlet. To calculate this new value,
     * the following function is used:
     *
     * <code></>(relation_type AS numeric * -1) - 1</code>
     *
     *  This allowed us to later update the MultiTree without taking care of the order in which the
     *  SQL Statements were executed.
     *
     *  2. Update the MultiTree to its new relation_type value according to the changes thar were applied,
     *  for this the UPDATE STATEMENT Syntax is:
     *
     *  UPDATE multi_tree SET relation_type = [temporal negative value]
     *  WHERE parent1 = [for each page] AND parent2 = [for each container] and relation_type = [new_value]
     *
     * These Update STATEMENT are executed using BATCH for performance reasons.
     *
     * 3. DELETE or mark as orphan the MultiTree on the Container that were removed.
     * The action taken (delete or mark as orphan) depends on whether the DELETE_ORPHANED_CONTENTS_FROM_CONTAINER
     * option is enabled or not.
     *
     * @param layoutChanges
     * @param pageIds
     *
     * @throws DotDataException
     */
    @Override
    @WrapInTransaction
    public void updateMultiTrees(final LayoutChanges layoutChanges, final Collection<String> pageIds,
                                 final String variantName )
            throws DotDataException {
        final List<Params> parametersToMark = new ArrayList<>();

        final boolean deleteOrphanedContents = deleteOrphanedContentsFromContainer.get();

        markMultiTreeToUpdate(layoutChanges, pageIds, parametersToMark, variantName);

        updateMarkedMultiTrees(layoutChanges, pageIds, variantName);

        if (deleteOrphanedContents) {
            removeMultiTrees(layoutChanges, pageIds, variantName);
        }

        pageIds.stream().forEach(pageId -> {
            CacheLocator.getMultiTreeCache().removePageMultiTrees(pageId);
            CacheLocator.getHTMLPageCache().remove(pageId);
        });
    }

    /**
     * DELETE or mark as orphan the MultiTree on the Container that were removed.
     * The action taken (delete or mark as orphan) depends on whether the DELETE_ORPHANED_CONTENTS_FROM_CONTAINER
     * option is enabled or not.
     *
     * These MultiTrees need to be marked first using the method {@link MultiTreeAPIImpl#markMultiTreeToUpdate(LayoutChanges, Collection, List)}
     *
     * @see MultiTreeAPIImpl#updateMultiTrees(LayoutChanges, Collection)
     *
     * @param layoutChanges
     * @param pageIds
     * @throws DotDataException
     */
    private static void removeMultiTrees(LayoutChanges layoutChanges, final Collection<String> pageIds,
                                         final String variantName) throws DotDataException {

        final List<Params> parametersToRemoved = new ArrayList<>();

        for (String identifier : pageIds) {
            parametersToRemoved.addAll(
                    layoutChanges.getAll().stream()
                            .filter(LayoutChanges.ContainerChanged::isRemove)
                            .map(changed -> new Params.Builder()
                                    .add(identifier, changed.getContainerId(), getMakValue(changed), variantName)
                                    .build()
                            )
                            .collect(Collectors.toList())
            );
        }

        if (!parametersToRemoved.isEmpty()) {
            new DotConnect().executeBatch("DELETE FROM multi_tree " +
                    "WHERE parent1 = ? AND parent2 = ? and relation_type = ? AND variant_id = ?", parametersToRemoved);
        }
    }

    /**
     * Update the MultiTree to its new relation_type value according to the changes thar were applied,
     *  for this the UPDATE STATEMENT Syntax is:
     *
     *  UPDATE multi_tree SET relation_type = [temporal negative value]
     *  WHERE parent1 = [for each page] AND parent2 = [for each container] and relation_type = [new_value]
     *
     * These Update STATEMENT are executed using BATCH for performance reasons.
     *
     * These MultiTrees need to be marked first using the method {@link MultiTreeAPIImpl#markMultiTreeToUpdate(LayoutChanges, Collection, List)}
     *
     * @see MultiTreeAPIImpl#updateMultiTrees(LayoutChanges, Collection)
     *
     * @param layoutChanges
     * @param pageIds
     * @throws DotDataException
     */
    private static void updateMarkedMultiTrees(final LayoutChanges layoutChanges, final Collection<String> pageIds,
                                               final String variantName) throws DotDataException {
        final boolean deleteOrphanedContents = deleteOrphanedContentsFromContainer.get();
        final List<Params> parametersToUpdate = new ArrayList<>();

        for (String identifier : pageIds) {
            parametersToUpdate.addAll(
                    layoutChanges.getAll().stream()
                            .filter(changed -> !deleteOrphanedContents || changed.isMoved())
                            .map(changed -> new Params.Builder()
                                    .add(String.valueOf(changed.getNewInstanceId()),
                                            identifier, changed.getContainerId(), getMakValue(changed), variantName)
                                    .build()
                            ).collect(Collectors.toList())
            );
        }

        new DotConnect().executeBatch("UPDATE multi_tree SET relation_type = ? " +
                "WHERE parent1 = ? AND parent2 = ? and relation_type = ? AND variant_id = ?", parametersToUpdate);
    }

    @NotNull
    private static String getMakValue(LayoutChanges.ContainerChanged changed) {
        return String.valueOf((Long.parseLong(changed.getOldInstanceId()) * -1) - 1);
    }

    /**
     * Mark all the MultiTree to be updated. These are all the MultiTree on the pageIds and in the Containers that
     * were changed. These MultiTree are marked by turning their relation_type to a negative number.
     * We avoid using the -1 value because it's already used for orphan Contentlet. To calculate this new value,
     * the following function is used:
     *
     * <code></>(relation_type AS numeric * -1) - 1</code>
     *
     *  This allowed us to later update the MultiTree without taking care of the order in which the
     *  SQL Statements were executed.
     *
     * @param layoutChanges
     * @param pageIds
     * @param parametersToMark
     * @throws DotDataException
     *
     * @see MultiTreeAPIImpl#updateMultiTrees(LayoutChanges, Collection)
     */
    private static void markMultiTreeToUpdate(final LayoutChanges layoutChanges, final Collection<String> pageIds,
                                              final List<Params> parametersToMark, final String variantName)
            throws DotDataException {

        for (String identifier : pageIds) {
            parametersToMark.addAll(
                    layoutChanges.getAll().stream()
                            .filter(changed -> !changed.isNew())
                            .map(changed -> new Params.Builder()
                                    .add(identifier, changed.getContainerId(), changed.getOldInstanceId(), variantName)
                                    .build()
                            ).collect(Collectors.toList())
            );
        }

        new DotConnect().executeBatch("UPDATE multi_tree SET relation_type = (CAST (relation_type AS numeric) * -1 )-1\n" +
                "WHERE parent1 = ? AND parent2 = ? AND relation_type = ? AND relation_type <> '-1' AND variant_id = ?", parametersToMark);
    }

    @CloseDBIfOpened
    private Long getContentletReferenceCountFromDB(final String contentletId) throws DotDataException {
        return ((Long) new DotConnect().setSQL(SELECT_CONTENTLET_REFERENCES).addParam(contentletId).loadObjectResults().get(0).get("count"));
    }

    /**
     * Returns the list of Contentlet IDs in an HTML Page before they're overwritten with the updated information. This
     * data is used to clear the page reference count ONLY on the Contentlets that were added or removed.
     *
     * @param pageId       The ID of the HTML Page whose original Contentlets will be retrieved.
     * @param relationType The relation type for the Multi-Trees.
     *
     * @return The list of Contentlet IDs.
     *
     * @throws DotDataException An error occurred when accessing the data source.
     */
    private Set<String> getOriginalContentlets(final String pageId, final String relationType) throws DotDataException {
        final List<Object> params = List.of(pageId, relationType);
        return this.getOriginalContentlets(SELECT_CHILD_BY_PARENT, params);
    }

    /**
     * Returns the list of Contentlet IDs in an HTML Page before they're overwritten with the updated information. This
     * data is used to clear the page reference count ONLY on the Contentlets that were added or removed.
     *
     * @param pageId          The ID of the HTML Page whose original Contentlets will be retrieved.
     * @param relationType    The relation type for the Multi-Trees.
     * @param personalization The Persona set for the Multi-Tree entry.
     * @param variantId       The ID of the selected Contentlet Variant.
     *
     * @return The list of Contentlet IDs.
     *
     * @throws DotDataException An error occurred when accessing the data source.
     */
    private Set<String> getOriginalContentlets(final String pageId, final String relationType, final String personalization,
                                               final String variantId) throws DotDataException {
        final List<Object> params = List.of(pageId,
                relationType,
                personalization,
                variantId);
        return this.getOriginalContentlets(SELECT_CHILD_BY_PARENT_RELATION_PERSONALIZATION_VARIANT, params);
    }

    /**
     * Returns the list of Contentlet IDs in an HTML Page before they're overwritten with the updated information. This
     * data is used to clear the page reference count ONLY on the Contentlets that were added or removed.
     *
     * @param pageId          The ID of the HTML Page whose original Contentlets will be retrieved.
     * @param relationType    The relation type for the Multi-Trees.
     * @param personalization The Persona set for the Multi-Tree entry.
     * @param variantId       The ID of the selected Contentlet Variant.
     * @param languageId      The Language ID of the Contentlets being updated.
     *
     * @return The list of Contentlet IDs.
     *
     * @throws DotDataException An error occurred when accessing the data source.
     */
    private Set<String> getOriginalContentlets(final String pageId, final String relationType, final String personalization,
                                               final String variantId, final Long languageId) throws DotDataException {
        final List<Object> params = List.of(pageId,
                relationType,
                personalization,
                variantId,
                pageId,
                languageId);
        return this.getOriginalContentlets(SELECT_CHILD_BY_PARENT_RELATION_PERSONALIZATION_VARIANT_LANGUAGE, params);
    }

    /**
     * Executes the specified SQL query with the provided parameters to get list of Contentlet IDs added to an HTML
     * Page.
     *
     * @param sqlQuery The SQL query.
     * @param params The parameters of the query.
     *
     * @return The list of Contentlet IDs.
     *
     * @throws DotDataException An error occurred when accessing the data source.
     */
    private Set<String> getOriginalContentlets(final String sqlQuery, final List<Object> params) throws DotDataException {
        final DotConnect db = new DotConnect().setSQL(sqlQuery);
        params.forEach(db::addParam);
        final List<Map<String, Object>> contentletData = db.loadObjectResults();
        return contentletData.stream().map(dataMap -> dataMap.get("child").toString()).collect(Collectors.toSet());
    }

    /**
     * Takes the list of Contentlet IDs present in an HTML Page before any change is made, and the list of
     * {@link MultiTree} objects representing the updated Contentlets. Then, compares both lists and determines what
     * specific Contentlets must have their page reference counter reset. This improves the overall performance as it
     * will avoid re-calculating the page reference on Contentlets that were not added or removed from any HTML Page at
     * all.
     *
     * @param originalContentletIds The list of Contentlet IDs that existed in the page before any change has been made.
     * @param multiTrees            The Multi-Tree objects representing added/removed Contentlets.
     */
    private void refreshContentletReferenceCount(final Set<String> originalContentletIds, final List<MultiTree> multiTrees) {
        if (!UtilMethods.isSet(multiTrees)) {
            originalContentletIds.forEach(id -> this.multiTreeCache.get().removeContentletReferenceCount(id));
        } else {
            final Set<String> updatedContentletIds = multiTrees.stream().map(MultiTree::getContentlet).collect(Collectors.toSet());
            final Set<String> modifiedIds = originalContentletIds.size() > updatedContentletIds.size() ?
                                                    originalContentletIds.stream().filter(id -> !updatedContentletIds.contains(id)).collect(Collectors.toSet()) :
                                                    updatedContentletIds.stream().filter(id -> !originalContentletIds.contains(id)).collect(Collectors.toSet());
            modifiedIds.forEach(id -> this.multiTreeCache.get().removeContentletReferenceCount(id));
        }
    }

    @VisibleForTesting
    public static void setDeleteOrphanedContentsFromContainer(final boolean newValue){
        deleteOrphanedContentsFromContainer = Lazy.of(() -> newValue);
    }
}
