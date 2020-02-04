package com.dotmarketing.factories;


import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.rendering.velocity.services.PageLoader;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotcms.util.transform.TransformerLocator;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.Params;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.business.ContainerFinderByIdOrPathStrategy;
import com.dotmarketing.portlets.containers.business.LiveContainerFinderByIdOrPathStrategyResolver;
import com.dotmarketing.portlets.containers.business.WorkingContainerFinderByIdOrPathStrategyResolver;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import io.vavr.control.Try;
import org.apache.bcel.generic.NEW;
import org.apache.commons.lang.StringUtils;

import java.sql.SQLException;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;


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



    private static final String DELETE_ALL_MULTI_TREE_RELATED_TO_IDENTIFIER_SQL =
            "delete from multi_tree where child = ? or parent1 = ? or parent2 = ?";
    private static final String DELETE_SQL = "delete from multi_tree where parent1=? and parent2=? and child=? and  relation_type = ? and personalization = ?";
    private static final String DELETE_SQL_PERSONALIZATION_PER_PAGE = "delete from multi_tree where parent1=? and personalization = ?";
    private static final String DELETE_ALL_MULTI_TREE_SQL = "delete from multi_tree where parent1=? AND relation_type != ?";
    private static final String DELETE_ALL_MULTI_TREE_SQL_BY_RELATION_AND_PERSONALIZATION = "delete from multi_tree where parent1=? AND relation_type != ? and personalization = ?";
    private static final String UPDATE_MULTI_TREE_PERSONALIZATION = "update multi_tree set personalization = ? where personalization = ?";
    private static final String SELECT_SQL = "select * from multi_tree where parent1 = ? and parent2 = ? and child = ? and  relation_type = ? and personalization = ?";

    private static final String INSERT_SQL = "insert into multi_tree (parent1, parent2, child, relation_type, tree_order, personalization) values (?,?,?,?,?,?)  ";

    private static final String SELECT_BY_PAGE = "select * from multi_tree where parent1 = ? order by tree_order";
    private static final String SELECT_BY_PAGE_AND_PERSONALIZATION = "select * from multi_tree where parent1 = ? and personalization = ? order by tree_order";
    private static final String SELECT_UNIQUE_PERSONALIZATION_PER_PAGE = "select distinct(personalization) from multi_tree where parent1 = ?";
    private static final String SELECT_UNIQUE_PERSONALIZATION = "select distinct(personalization) from multi_tree";
    private static final String SELECT_BY_ONE_PARENT = "select * from multi_tree where parent1 = ? or parent2 = ? order by tree_order"; // search by page id or container id
    private static final String SELECT_BY_TWO_PARENTS = "select * from multi_tree where parent1 = ? and parent2 = ?  order by tree_order";
    private static final String SELECT_ALL = "select * from multi_tree  ";
    private static final String SELECT_BY_CHILD = "select * from multi_tree where child = ?  order by parent1, parent2, relation_type ";
    private static final String SELECT_BY_PARENTS_AND_RELATIONS =
            " select * from multi_tree where parent1 = ? and parent2 = ? and relation_type = ? and personalization = ? order by tree_order";

    private static final String SELECT_BY_CONTAINER_AND_STRUCTURE = "SELECT mt.* FROM multi_tree mt JOIN contentlet c "
            + " ON c.identifier = mt.child WHERE mt.parent2 = ? AND c.structure_inode = ? ";


    @WrapInTransaction
    @Override
    public void deleteMultiTree(final MultiTree mTree) throws DotDataException {
        Logger.info(this, String.format("Deleting MutiTree: %s", mTree));
        _dbDelete(mTree);
        updateHTMLPageVersionTS(mTree.getHtmlPage());
        refreshPageInCache(mTree.getHtmlPage());
    }

    @WrapInTransaction
    @Override
    public void deleteMultiTreesRelatedToIdentifier(final String pageIdentifier) throws DotDataException {

        final List<MultiTree> pagesRelatedList = getMultiTreesByPage(pageIdentifier);

        new DotConnect().setSQL(DELETE_ALL_MULTI_TREE_RELATED_TO_IDENTIFIER_SQL).addParam(pageIdentifier).addParam(pageIdentifier)
                .addParam(pageIdentifier).loadResult();

        if (UtilMethods.isSet(pagesRelatedList)) {
            for (final MultiTree multiTree : pagesRelatedList) {
                updateHTMLPageVersionTS(multiTree.getHtmlPage());
                refreshPageInCache(multiTree.getHtmlPage());
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

        final DotConnect db =
                new DotConnect().setSQL(SELECT_SQL).addParam(htmlPage).addParam(container)
                        .addParam(childContent).addParam(containerInstance)
                        .addParam(personalization);
        db.loadResult();

        return TransformerLocator.createMultiTreeTransformer(db.loadObjectResults()).findFirst();

    }

    @Override
    public MultiTree getMultiTree(final String htmlPage, final String container, final String childContent, final String containerInstance)
            throws DotDataException {

        return this.getMultiTree(htmlPage, container, childContent, containerInstance, MultiTree.DOT_PERSONALIZATION_DEFAULT);
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

    @CloseDBIfOpened
    @Override
    public java.util.List<MultiTree> getMultiTrees(final String parentInode) throws DotDataException {

        final DotConnect db = new DotConnect().setSQL(SELECT_BY_ONE_PARENT).addParam(parentInode).addParam(parentInode);
        return TransformerLocator.createMultiTreeTransformer(db.loadObjectResults()).asList();
    }

    @CloseDBIfOpened
    @Override
    public Set<String> getPersonalizationsForPage(final String pageID) throws DotDataException {
        IHTMLPage pageId=APILocator.getHTMLPageAssetAPI().fromContentlet(APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(pageID));
        return getPersonalizationsForPage(pageId);
    }
    
    @CloseDBIfOpened
    @Override
    public Set<String> getPersonalizationsForPage(final IHTMLPage page) throws DotDataException {
        final Set<String> personas=new HashSet<>();

        final Table<String, String, Set<PersonalizedContentlet>> pageContents = Try.of(()-> getPageMultiTrees(page, false)).getOrElseThrow(e->new DotRuntimeException(e));

        for (final String containerId : pageContents.rowKeySet()) {
            for (final String uniqueId : pageContents.row(containerId).keySet()) {
                pageContents.get(containerId, uniqueId).forEach(p->personas.add(p.getPersonalization()));
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
                                                       final String newPersonalization)  throws DotDataException {

        List<MultiTree> multiTrees = null;
        final ImmutableList.Builder<MultiTree> personalizedContainerListBuilder =
                new ImmutableList.Builder<>();

        final List<MultiTree> basedMultiTreeList = this.getMultiTreesByPersonalizedPage(pageId, basePersonalization);

        if (null != basedMultiTreeList) {

            basedMultiTreeList.forEach(multiTree ->
                    personalizedContainerListBuilder.add(MultiTree.personalized(multiTree, newPersonalization)));

            multiTrees = personalizedContainerListBuilder.build();
            this.saveMultiTrees(multiTrees);
        }

        return multiTrees;
    } // copyPersonalizationForPage.


    @WrapInTransaction
    @Override
    public void deletePersonalizationForPage(final String pageId, final String personalization) throws DotDataException {

        Logger.debug(this, "Removing personalization for: " + pageId +
                                ", personalization: " + personalization);

        new DotConnect().setSQL(DELETE_SQL_PERSONALIZATION_PER_PAGE)
                .addParam(pageId)
                .addParam(personalization)
                .loadResult();

        updateHTMLPageVersionTS(pageId);
        refreshPageInCache(pageId);
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
    public List<MultiTree> getMultiTreesByPersonalizedPage(final String pageId, final String personalization) throws DotDataException {

        return TransformerLocator.createMultiTreeTransformer(
                new DotConnect().setSQL(SELECT_BY_PAGE_AND_PERSONALIZATION)
                        .addParam(pageId).addParam(personalization).loadObjectResults())
                .asList();
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
        Logger.debug(this, () -> String.format("Saving MutiTree: %s", mTree));
        _reorder(mTree);
        updateHTMLPageVersionTS(mTree.getHtmlPage());
        refreshPageInCache(mTree.getHtmlPage());

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
        }

        final MultiTree mTree = mTrees.get(0);
        updateHTMLPageVersionTS(mTree.getHtmlPage());
        refreshPageInCache(mTree.getHtmlPage());
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

        Logger.info(this, String.format(
                "Overriding MutiTrees: pageId -> %s personalization -> %s multiTrees-> %s ",
                pageId, personalization, multiTrees));

        if (multiTrees == null) {

            throw new DotDataException("empty list passed in");
        }

        Logger.debug(MultiTreeAPIImpl.class, ()->String.format("Saving page's content: %s", multiTrees));

        final DotConnect db = new DotConnect();
        db.setSQL(DELETE_ALL_MULTI_TREE_SQL_BY_RELATION_AND_PERSONALIZATION)
                .addParam(pageId)
                .addParam(ContainerUUID.UUID_DEFAULT_VALUE)
                .addParam(personalization).loadResult();

        if (!multiTrees.isEmpty()) {

            final List<Params> insertParams = Lists.newArrayList();
            final Set<String> newContainers = new HashSet<>();

            for (final MultiTree tree : multiTrees) {
                insertParams
                        .add(new Params(pageId, tree.getContainerAsID(), tree.getContentlet(),
                                tree.getRelationType(), tree.getTreeOrder(), tree.getPersonalization()));
                newContainers.add(tree.getContainer());
            }

            db.executeBatch(INSERT_SQL, insertParams);
        }

        updateHTMLPageVersionTS(pageId);
        refreshPageInCache(pageId);
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
                        .removePageMultiTrees(pageId);
            }
        }
    }

    /**
     * Save a collection of {@link MultiTree} and link them with a page, Also delete all the
     * {@link MultiTree} linked previously with the page.
     *
     * @param pageId Page's identifier
     * @param mTrees
     * @throws DotDataException
     */
    @Override
    @WrapInTransaction
    public void saveMultiTrees(final String pageId, final List<MultiTree> mTrees) throws DotDataException {
        Logger.debug(this, () -> String
                .format("Saving MutiTrees: pageId -> %s multiTrees-> %s", pageId, mTrees));
        if (mTrees == null) {
            throw new DotDataException("empty list passed in");
        }

        Logger.debug(MultiTreeAPIImpl.class, ()->String.format("Saving page's content: %s", mTrees));

        final DotConnect db = new DotConnect();
        db.setSQL(DELETE_ALL_MULTI_TREE_SQL).addParam(pageId).addParam(ContainerUUID.UUID_DEFAULT_VALUE).loadResult();

        if (!mTrees.isEmpty()) {
            final List<Params> insertParams = Lists.newArrayList();
            final Set<String> newContainers = new HashSet<>();

            for (final MultiTree tree : mTrees) {
                insertParams
                        .add(new Params(pageId, tree.getContainerAsID(), tree.getContentlet(),
                                tree.getRelationType(), tree.getTreeOrder(), tree.getPersonalization()));
                newContainers.add(tree.getContainer());
            }

            db.executeBatch(INSERT_SQL, insertParams);
        }

        updateHTMLPageVersionTS(pageId);
        refreshPageInCache(pageId);
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

        Logger.debug(this, () -> String.format("_dbInsert -> Saving MutiTree: %s", multiTree));

        new DotConnect().setSQL(INSERT_SQL).addParam(multiTree.getHtmlPage()).addParam(multiTree.getContainerAsID()).addParam(multiTree.getContentlet())
                .addParam(multiTree.getRelationType()).addParam(multiTree.getTreeOrder()).addObject(multiTree.getPersonalization()).loadResult();
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
    private void updateHTMLPageVersionTS(final String pageId) throws DotDataException {
        final List<ContentletVersionInfo> contentletVersionInfos =
                APILocator.getVersionableAPI().findContentletVersionInfos(pageId);
        for (final ContentletVersionInfo versionInfo : contentletVersionInfos) {
            if (versionInfo != null) {
                versionInfo.setVersionTs(new Date());
                APILocator.getVersionableAPI().saveContentletVersionInfo(versionInfo);
            }
        }
    }


    private void refreshPageInCache(final String pageIdentifier) throws DotDataException {

        CacheLocator.getMultiTreeCache()
                .removePageMultiTrees(pageIdentifier);

        final Set<String> inodeSet = new HashSet<>();
        final List<ContentletVersionInfo> contentletVersionInfos = APILocator.getVersionableAPI()
                .findContentletVersionInfos(pageIdentifier);

        for (final ContentletVersionInfo versionInfo : contentletVersionInfos) {

            inodeSet.add(versionInfo.getWorkingInode());
            if (versionInfo.getLiveInode() != null) {
                inodeSet.add(versionInfo.getLiveInode());
            }
        }

        try {
            final List<Contentlet> contentlets = APILocator.getContentletAPIImpl()
                    .findContentlets(Lists.newArrayList(inodeSet));

            final PageLoader pageLoader = new PageLoader();

            for (final Contentlet pageContent : contentlets) {

                final IHTMLPage htmlPage = APILocator.getHTMLPageAssetAPI().fromContentlet(pageContent);

                pageLoader.invalidate(htmlPage, PageMode.EDIT_MODE);
                pageLoader.invalidate(htmlPage, PageMode.PREVIEW_MODE);
            }
        } catch (DotStateException | DotSecurityException e) {

            Logger.warn(MultiTreeAPIImpl.class, "unable to refresh page cache:" + e.getMessage());
        }
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
            } catch (SQLException e) {
                throw new DotDataException(e);
            }
        }
    }

    @CloseDBIfOpened
    @Override
    public Table<String, String, Set<PersonalizedContentlet>> getPageMultiTrees(final IHTMLPage page, final boolean liveMode)
            throws DotDataException, DotSecurityException {

        final String multiTreeCacheKey = page.getIdentifier();
        final Optional<Table<String, String, Set<PersonalizedContentlet>>> pageContentsOpt =
                CacheLocator.getMultiTreeCache().getPageMultiTrees(multiTreeCacheKey, liveMode);
        
        if(pageContentsOpt.isPresent()) {
            return pageContentsOpt.get();
        }

        final Table<String, String, Set<PersonalizedContentlet>> pageContents = HashBasedTable.create();
        final List<MultiTree> multiTrees    = this.getMultiTrees(page.getIdentifier());
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
                contentlet = contentletAPI.findContentletByIdentifierAnyLanguage(multiTree.getContentlet());
            } catch (DotDataException  | DotContentletStateException e) {
                Logger.debug(this.getClass(), "invalid contentlet on multitree:" + multiTree
                        + ", msg: " + e.getMessage(), e);
                Logger.warn(this.getClass(), "invalid contentlet on multitree:" + multiTree);
            }

            if (contentlet != null) {

                final Set<PersonalizedContentlet> myContents = pageContents.contains(containerId, multiTree.getRelationType())
                        ? pageContents.get(containerId, multiTree.getRelationType())
                        : new LinkedHashSet<>();
                long byPersonaCount  = myContents.stream().filter(p->p.getPersonalization().equals(personalization)).count();
                if (container != null && byPersonaCount < container.getMaxContentlets()) {

                    myContents.add(new PersonalizedContentlet(multiTree.getContentlet(), personalization));
                }

                pageContents.put(containerId, multiTree.getRelationType(), myContents);
            }
        }

        this.addEmptyContainers(page, pageContents, liveMode);
        
        CacheLocator.getMultiTreeCache().putPageMultiTrees(multiTreeCacheKey, liveMode, pageContents);
        return pageContents;
    }

    /**
     * Returns the list of Containers from the drawn layout of a given Template.
     *
     * @param page The {@link IHTMLPage} object using the {@link Template} which holds the Containers.
     *
     * @return The list of {@link ContainerUUID} objects.
     *
     * @throws DotSecurityException The internal APIs are not allowed to return data for the specified user.
     * @throws DotDataException     The information for the Template could not be accessed.
     */
    private List<ContainerUUID> getDrawedLayoutContainerUUIDs (final IHTMLPage page) throws DotSecurityException, DotDataException {

        final TemplateLayout layout =
                DotTemplateTool.themeLayout(page.getTemplateId(), APILocator.systemUser(), false);
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
                        this.getDrawedLayoutContainerUUIDs(page):
                        APILocator.getTemplateAPI().getContainersUUIDFromDrawTemplateBody(template.getBody());
            } catch (final Exception e) {
                Logger.error(this, String.format("An error occurred when retrieving empty Containers from page with " +
                        "ID '%s' in liveMode '%s': %s", page.getIdentifier(), liveMode, e.getMessage()), e);
                return;
            }

            for (final ContainerUUID containerUUID : containersUUID) {

                Container container = null;
                try {
                    // this read path or id.
                    container = liveMode ? this.getLiveContainerById(containerUUID.getIdentifier(), APILocator.systemUser(), template):
                            this.getWorkingContainerById(containerUUID.getIdentifier(), APILocator.systemUser(), template);

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
    private boolean doesPageContentsHaveContainer(
            final Table<String, String, Set<PersonalizedContentlet>> pageContents,
            final ContainerUUID containerUUID,
            final Container container) {

        if(pageContents.contains(container.getIdentifier(), containerUUID.getUUID())){
            return true;
        } else if (ContainerUUID.UUID_LEGACY_VALUE.equals(containerUUID.getUUID())) {
            boolean pageContenstContains = pageContents.contains(containerUUID.getIdentifier(), ContainerUUID.UUID_START_VALUE);

            if (!pageContenstContains && container instanceof FileAssetContainer) {
                pageContenstContains = pageContents.contains(container.getIdentifier(), ContainerUUID.UUID_START_VALUE);
            }

            return pageContenstContains;
        } else {
            return false;
        }
    }

    private Container getLiveContainerById(final String containerIdOrPath, final User user, final Template template) throws NotFoundInDbException {

        final LiveContainerFinderByIdOrPathStrategyResolver strategyResolver =
                LiveContainerFinderByIdOrPathStrategyResolver.getInstance();
        final Optional<ContainerFinderByIdOrPathStrategy> strategy           = strategyResolver.get(containerIdOrPath);

        return this.geContainerById(containerIdOrPath, user, template, strategy, strategyResolver.getDefaultStrategy());
    }

    private Container getWorkingContainerById(final String containerIdOrPath, final User user, final Template template) throws NotFoundInDbException {

        final WorkingContainerFinderByIdOrPathStrategyResolver strategyResolver =
                WorkingContainerFinderByIdOrPathStrategyResolver.getInstance();
        final Optional<ContainerFinderByIdOrPathStrategy> strategy           = strategyResolver.get(containerIdOrPath);

        return this.geContainerById(containerIdOrPath, user, template, strategy, strategyResolver.getDefaultStrategy());
    }

    private Container geContainerById(final String containerIdOrPath, final User user, final Template template,
                                      final Optional<ContainerFinderByIdOrPathStrategy> strategy,
                                      final ContainerFinderByIdOrPathStrategy defaultContainerFinderByIdOrPathStrategy) throws NotFoundInDbException  {

        final Supplier<Host> resourceHostSupplier = Sneaky.sneaked(()->APILocator.getTemplateAPI().getTemplateHost(template));

        return strategy.isPresent()?
                strategy.get().apply(containerIdOrPath, user, false, resourceHostSupplier):
                defaultContainerFinderByIdOrPathStrategy.apply(containerIdOrPath, user, false, resourceHostSupplier);
    }
}
