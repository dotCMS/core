package com.dotmarketing.factories;


import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.rendering.velocity.services.PageLoader;
import com.dotcms.util.transform.TransformerLocator;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.Params;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.Lists;
import com.liferay.util.StringPool;
import org.apache.commons.lang.StringUtils;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.list;

@Deprecated
/**
 * Slated for removal in > 5.1
 * Use  {@link com.dotmarketing.factories.MultiTreeAPI}
 * e.g. APILocator.getMultiTreeAPI().getMultiTreesByParent(String id)
 */
public class MultiTreeFactory {

    static final String DELETE_ALL_MULTI_TREE_RELATED_TO_IDENTIFIER_SQL
            = "delete from multi_tree where child = ? or parent1 = ? or parent2 = ?";
    static final String DELETE_SQL = "delete from multi_tree where parent1=? and parent2=? and child=? and  relation_type = ?";
    static final String DELETE_ALL_MULTI_TREE_SQL = "delete from multi_tree where parent1=? AND relation_type != ?";
    static final String SELECT_SQL =
            "select * from multi_tree where parent1 = ? and parent2 = ? and child = ? and  relation_type = ?";

    static final String INSERT_SQL =
            "insert into multi_tree (parent1, parent2, child, relation_type, tree_order ) values (?,?,?,?,?)  ";

    static final String SELECT_BY_PAGE        = "select * from multi_tree where parent1 = ? order by tree_order";
    static final String SELECT_BY_ONE_PARENT  = "select * from multi_tree where parent1 = ? or parent2 = ? order by tree_order";
    static final String SELECT_BY_TWO_PARENTS = "select * from multi_tree where parent1 = ? and parent2 = ?  order by tree_order";
    static final String SELECT_ALL = "select * from multi_tree  ";
    static final String SELECT_BY_CHILD = "select * from multi_tree where child = ?  order by parent1, parent2, relation_type ";
    static final String SELECT_BY_PARENTS_AND_RELATIONS =
            " select * from multi_tree where parent1 = ? and parent2 = ? and relation_type = ? order by tree_order";

    private static final String SELECT_BY_CONTAINER_AND_STRUCTURE = "SELECT mt.* FROM multi_tree mt JOIN contentlet c "
            + " ON c.identifier = mt.child WHERE mt.parent2 = ? AND c.structure_inode = ? ";



    static final String UPDATE_RELATION_TYPE_SQL = "UPDATE multi_tree SET relation_type = ? WHERE parent1 = ? and parent2 = ? and relation_type = ?";

    static final String UPDATE_PAGE_UUID_TO_DEFAULT_SQL = "UPDATE multi_tree SET relation_type = ? WHERE parent1 = ?";
    static final String UPDATE_PAGE_CONTAINER_UUID_TO_DEFAULT_SQL = "UPDATE multi_tree SET relation_type = ? WHERE parent1 = ? and parent2 = ?";

    public static void deleteMultiTree(final MultiTree mTree) throws DotDataException {
        _dbDelete(mTree);
        updateHTMLPageVersionTS(mTree.getHtmlPage());
        refreshPageInCache(mTree.getHtmlPage());
    }

    public static void deleteMultiTreesRelatedToIdentifier(final String identifier) throws DotDataException {

        final List<MultiTree> pagesRelatedList = getMultiTreesByPage(identifier);

        new DotConnect().setSQL(DELETE_ALL_MULTI_TREE_RELATED_TO_IDENTIFIER_SQL)
                .addParam(identifier)
                .addParam(identifier)
                .addParam(identifier)
                .loadResult();

        if (UtilMethods.isSet(pagesRelatedList)) {
            for(final MultiTree multiTree : pagesRelatedList) {
                updateHTMLPageVersionTS(multiTree.getHtmlPage());
                refreshPageInCache(multiTree.getHtmlPage());
            }
        }
    }



    public static void deleteMultiTree(final List<MultiTree> mTree) throws DotDataException {
        for(MultiTree tree : mTree) {
            deleteMultiTree(tree);
        }
    }
    
    public static void deleteMultiTreeByParent(String pageOrContainer) throws DotDataException {
        deleteMultiTree(getMultiTrees(pageOrContainer));
    }
    
    public static void deleteMultiTreeByChild(String contentIdentifier) throws DotDataException {
        deleteMultiTree(getMultiTreesByChild(contentIdentifier));
    }

    /**
     * Deletes multi-tree relationship given a MultiTree object. It also updates the version_ts of
     * all versions of the htmlpage passed in (multiTree.parent1)
     *
     * @param multiTree
     * @throws DotDataException
     * @throws DotSecurityException
     * 
     */
    private static void _dbDelete(final MultiTree multiTree) throws DotDataException {

        final DotConnect db = new DotConnect().setSQL(DELETE_SQL)
            .addParam(multiTree.getHtmlPage())
            .addParam(multiTree.getContainer())
            .addParam(multiTree.getContentlet())
            .addParam(multiTree.getRelationType());
        db.loadResult();

    }



    public static MultiTree getMultiTree(Identifier htmlPage, Identifier container, Identifier childContent, String relationType)
            throws DotDataException {
        return getMultiTree(htmlPage.getId(), container.getId(), childContent.getId(), relationType);
    }


    public static MultiTree getMultiTree(String htmlPage, String container, String childContent, String relationType)
            throws DotDataException {

        DotConnect db = new DotConnect().setSQL(SELECT_SQL)
            .addParam(htmlPage)
            .addParam(container)
            .addParam(childContent)
            .addParam(relationType);
        db.loadResult();

        return TransformerLocator.createMultiTreeTransformer(db.loadObjectResults()).findFirst();

    }
    /**
     * Use {@link #getMultiTree(String, String, String, String)} This method does not use the unique id specified 
     * in the #parseContainer code
     * @param htmlPage
     * @param container
     * @param childContent
     * @throws DotDataException
     */
    @Deprecated
    public static MultiTree getMultiTree(String htmlPage, String container, String childContent)
            throws DotDataException {

        DotConnect db = new DotConnect().setSQL(SELECT_SQL)
            .addParam(htmlPage)
            .addParam(container)
            .addParam(childContent)
            .addParam(Container.LEGACY_RELATION_TYPE);
        db.loadResult();

        return TransformerLocator.createMultiTreeTransformer(db.loadObjectResults()).findFirst();

    }
    public static java.util.List<MultiTree> getMultiTrees(Identifier parent) throws DotDataException {
        return getMultiTrees(parent.getId());
    }

    public static java.util.List<MultiTree> getMultiTrees(Identifier htmlPage, Identifier container) throws DotDataException {
        return getMultiTrees(htmlPage.getId(), container.getId());
    }

    public static java.util.List<MultiTree> getMultiTrees(String parentInode) throws DotDataException {

        DotConnect db = new DotConnect().setSQL(SELECT_BY_ONE_PARENT)
                .addParam(parentInode)
                .addParam(parentInode);
        return TransformerLocator.createMultiTreeTransformer(db.loadObjectResults()).asList();

    }

    /**
     * Returns the tree with the pages that matched on the parent1
     * @param parentInode
     * @return
     * @throws DotDataException
     */
    public static java.util.List<MultiTree> getMultiTreesByPage(final String parentInode) throws DotDataException {

        final DotConnect db = new DotConnect().setSQL(SELECT_BY_PAGE)
                .addParam(parentInode);
        return TransformerLocator.createMultiTreeTransformer(db.loadObjectResults()).asList();

    }

    public static java.util.List<MultiTree> getAllMultiTrees() {
        try {
            DotConnect db = new DotConnect().setSQL(SELECT_ALL);

            return TransformerLocator.createMultiTreeTransformer(db.loadObjectResults()).asList();

        } catch (Exception e) {
            Logger.error(MultiTreeFactory.class, "getMultiTree failed:" + e, e);
            throw new DotRuntimeException(e.toString());
        }
    }

    public static java.util.List<MultiTree> getMultiTrees(String htmlPage, String container, String relationType) {
        try {

            DotConnect db = new DotConnect().setSQL(SELECT_BY_PARENTS_AND_RELATIONS)
                .addParam(htmlPage)
                .addParam(container)
                .addParam(relationType);
            return TransformerLocator.createMultiTreeTransformer(db.loadObjectResults()).asList();
        } catch (Exception e) {
            Logger.error(MultiTreeFactory.class, "getMultiTree failed:" + e, e);
            throw new DotRuntimeException(e.toString());
        }
    }

    public static java.util.List<MultiTree> getMultiTrees(IHTMLPage htmlPage, Container container) throws DotDataException {
        return getMultiTrees(htmlPage.getIdentifier(), container.getIdentifier());
    }


    public static java.util.List<MultiTree> getMultiTrees(String htmlPage, String container) throws DotDataException {

        DotConnect db = new DotConnect().setSQL(SELECT_BY_TWO_PARENTS)
            .addParam(htmlPage)
            .addParam(container);
        return TransformerLocator.createMultiTreeTransformer(db.loadObjectResults()).asList();

    }

    public static java.util.List<MultiTree> getMultiTrees(IHTMLPage htmlPage, Container container, String relationType) {
        return getMultiTrees(htmlPage.getIdentifier(), container.getIdentifier(), relationType);
    }


    public static java.util.List<MultiTree> getContainerMultiTrees(String containerIdentifier) throws DotDataException {
        return getMultiTrees(containerIdentifier);
    }


    public static java.util.List<MultiTree> getMultiTreesByChild(String contentIdentifier) throws DotDataException {

        DotConnect db = new DotConnect().setSQL(SELECT_BY_CHILD)
            .addParam(contentIdentifier);

        return TransformerLocator.createMultiTreeTransformer(db.loadObjectResults()).asList();

    }

    /**
     * Get a list of MultiTree for Contentlets using a specific Structure and specific Container
     * @param containerIdentifier
     * @param structureInode
     * @return List of MultiTree
     */
    public static List<MultiTree> getContainerStructureMultiTree(final String containerIdentifier, final String structureInode) {
        try {
            final DotConnect dc = new DotConnect()
                .setSQL(SELECT_BY_CONTAINER_AND_STRUCTURE)
                .addParam(containerIdentifier)
                .addParam(structureInode);

            return TransformerLocator.createMultiTreeTransformer(dc.loadObjectResults()).asList();

        } catch (DotDataException e) {
            Logger.error(MultiTreeFactory.class, "getContainerStructureMultiTree failed:" + e, e);
            throw new DotRuntimeException(e.toString());
        }
    }

    @WrapInTransaction
    public static void saveMultiTree(MultiTree mTree) throws DotDataException {

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
    @WrapInTransaction
    public static void saveMultiTrees(final List<MultiTree> mTrees) throws DotDataException {
        if (mTrees == null || mTrees.isEmpty())
            throw new DotDataException("empty list passed in");
        int i = 0;
        for (MultiTree tree : mTrees) {
            _dbUpsert(tree.setTreeOrder(i++));
        }

        final MultiTree mTree = mTrees.get(0);
        updateHTMLPageVersionTS(mTree.getHtmlPage());
        refreshPageInCache(mTree.getHtmlPage());

    }

    /**
     * Save a collection of {@link MultiTree} and link them with a page, Also delete all the {@link MultiTree} linked
     * previously with the page.
     *
     * @param pageId Page's identifier
     * @param mTrees
     * @throws DotDataException
     */
    @WrapInTransaction
    public static void saveMultiTrees(final String pageId, final List<MultiTree> mTrees) throws DotDataException {
        if (mTrees == null) {
            throw new DotDataException("empty list passed in");
        }

        Logger.debug(MultiTreeFactory.class, String.format("Saving page's content: %s", mTrees));

        final DotConnect db = new DotConnect();

        db.setSQL(DELETE_ALL_MULTI_TREE_SQL)
            .addParam(pageId)
            .addParam(ContainerUUID.UUID_DEFAULT_VALUE)
            .loadResult();

        if (!mTrees.isEmpty()) {
            final List<Params> insertParams = list();
            final Set<String> newContainers = new HashSet<>();

            for (final MultiTree tree : mTrees) {
                insertParams.add(new Params(pageId, tree.getContainer(), tree.getContentlet(), tree.getRelationType(),
                        tree.getTreeOrder()));
                newContainers.add(tree.getContainer());
            }

            db.executeBatch(INSERT_SQL, insertParams);
        }

        updateHTMLPageVersionTS(pageId);
        refreshPageInCache(pageId);
    }

    @CloseDBIfOpened
    protected static List<String> getContainersId(String pageId) throws DotDataException {
        return MultiTreeFactory.getMultiTrees(pageId).stream()
                .map(multiTree -> multiTree.getContainer())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private static void _dbUpsert(final MultiTree mtree) throws DotDataException {

        _dbDelete(mtree);
        _dbInsert(mtree);

    }

    private static void _reorder(final MultiTree tree) throws DotDataException {

        List<MultiTree> trees = getMultiTrees(tree.getHtmlPage(), tree.getContainer(), tree.getRelationType());
        trees = trees.stream()
            .filter(rowTree -> !rowTree.equals(tree))
            .collect(Collectors.toList());
        int maxOrder = (tree.getTreeOrder() > trees.size()) ? trees.size() : tree.getTreeOrder();
        trees.add(maxOrder, tree);

        saveMultiTrees(trees);

    }



    private static void _dbInsert(final MultiTree o) throws DotDataException {
        new DotConnect().setSQL(INSERT_SQL)
            .addParam(o.getHtmlPage())
            .addParam(o.getContainer())
            .addParam(o.getContentlet())
            .addParam(o.getRelationType())
            .addParam(o.getTreeOrder())
            .loadResult();
    }




    /**
     * Update the version_ts of all versions of the HTML Page with the given id. If a MultiTree
     * Object has been added or deleted from this page, its version_ts value needs to be updated so
     * it can be included in future Push Publishing tasks
     * 
     * @param id The HTMLPage Identifier to pass in
     * @throws DotContentletStateException
     * @throws DotDataException
     * @throws DotSecurityException
     * 
     */
    private static void updateHTMLPageVersionTS(final String id) throws DotDataException {
        List<ContentletVersionInfo> infos = APILocator.getVersionableAPI()
            .findContentletVersionInfos(id);
        for (ContentletVersionInfo versionInfo : infos) {
            if (versionInfo != null) {
                versionInfo.setVersionTs(new Date());
                APILocator.getVersionableAPI()
                    .saveContentletVersionInfo(versionInfo);
            }
        }
    }

	

    private static void refreshPageInCache(final String pageIdentifier) throws DotDataException {
        Set<String> inodes = new HashSet<String>();
        List<ContentletVersionInfo> infos = APILocator.getVersionableAPI()
            .findContentletVersionInfos(pageIdentifier);
        for (ContentletVersionInfo versionInfo : infos) {
            inodes.add(versionInfo.getWorkingInode());
            if (versionInfo.getLiveInode() != null) {
                inodes.add(versionInfo.getLiveInode());
            }
        }
        try {
            List<Contentlet> contentlets = APILocator.getContentletAPIImpl()
                .findContentlets(Lists.newArrayList(inodes));

            PageLoader pageLoader = new PageLoader();

            for (Contentlet pageContent : contentlets) {
                IHTMLPage htmlPage = APILocator.getHTMLPageAssetAPI()
                    .fromContentlet(pageContent);

                pageLoader.invalidate(htmlPage, PageMode.EDIT_MODE);
                pageLoader.invalidate(htmlPage, PageMode.PREVIEW_MODE);
            }
        } catch (DotStateException | DotSecurityException e) {
            Logger.warn(MultiTreeFactory.class, "unable to refresh page cache:" + e.getMessage());
        }
    }

    /**
     * {link {@link #saveMultiTree(MultiTree)} The multitree does not respect language
     * @deprecated
     * @param multiTree
     * @param lang
     * @throws DotDataException
     */
    @Deprecated
    public static void saveMultiTree(MultiTree multiTree, long lang) throws DotDataException {
        saveMultiTree(multiTree);
    }

    @WrapInTransaction
    public static void updateMultiTree(final String pageId, final String containerId, final String oldRelationType,
                                       final String newRelationType) throws DotDataException {

        new DotConnect().setSQL(UPDATE_RELATION_TYPE_SQL)
                .addParam(newRelationType)
                .addParam(pageId)
                .addParam(containerId)
                .addParam(oldRelationType)
                .loadResult();
    }

    /**
     * Removes the references to these inodes on child and parents.
     * @param inodes
     * @throws SQLException
     */
    public static void deleteMultiTreesForInodes(final List<String> inodes) throws  SQLException {

        final String sInodeIds = StringUtils.join(inodes, StringPool.COMMA);
        new DotConnect().
                executeStatement("delete from multi_tree where child in (" + sInodeIds
                + ") or parent1 in (" + sInodeIds + ") or parent2 in (" + sInodeIds + ")");
    }
}
