package com.dotmarketing.factories;


import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.rendering.velocity.services.PageLoader;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotcms.util.transform.TransformerLocator;
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
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import org.apache.commons.lang.StringUtils;

import java.sql.SQLException;
import java.util.*;
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
    private static final String DELETE_SQL = "delete from multi_tree where parent1=? and parent2=? and child=? and  relation_type = ?";
    private static final String DELETE_ALL_MULTI_TREE_SQL = "delete from multi_tree where parent1=? AND relation_type != ?";
    private static final String SELECT_SQL = "select * from multi_tree where parent1 = ? and parent2 = ? and child = ? and  relation_type = ?";

    private static final String INSERT_SQL = "insert into multi_tree (parent1, parent2, child, relation_type, tree_order ) values (?,?,?,?,?)  ";

    private static final String SELECT_BY_PAGE = "select * from multi_tree where parent1 = ? order by tree_order";
    private static final String SELECT_BY_ONE_PARENT = "select * from multi_tree where parent1 = ? or parent2 = ? order by tree_order";
    private static final String SELECT_BY_TWO_PARENTS = "select * from multi_tree where parent1 = ? and parent2 = ?  order by tree_order";
    private static final String SELECT_ALL = "select * from multi_tree  ";
    private static final String SELECT_BY_CHILD = "select * from multi_tree where child = ?  order by parent1, parent2, relation_type ";
    private static final String SELECT_BY_PARENTS_AND_RELATIONS =
            " select * from multi_tree where parent1 = ? and parent2 = ? and relation_type = ? order by tree_order";

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
    public void deleteMultiTreesRelatedToIdentifier(final String identifier) throws DotDataException {

        final List<MultiTree> pagesRelatedList = getMultiTreesByPage(identifier);

        new DotConnect().setSQL(DELETE_ALL_MULTI_TREE_RELATED_TO_IDENTIFIER_SQL).addParam(identifier).addParam(identifier)
                .addParam(identifier).loadResult();

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

        final DotConnect db = new DotConnect().setSQL(DELETE_SQL).addParam(multiTree.getHtmlPage()).addParam(multiTree.getContainer())
                .addParam(multiTree.getContentlet()).addParam(multiTree.getRelationType());
        db.loadResult();

    }


    @CloseDBIfOpened
    @Override
    public MultiTree getMultiTree(final Identifier htmlPage, final Identifier container, final Identifier childContent,
            final String containerInstance) throws DotDataException {
        return getMultiTree(htmlPage.getId(), container.getId(), childContent.getId(), containerInstance);
    }

    @CloseDBIfOpened
    @Override
    public MultiTree getMultiTree(final String htmlPage, final String container, final String childContent, final String containerInstance)
            throws DotDataException {

        final DotConnect db =
                new DotConnect().setSQL(SELECT_SQL).addParam(htmlPage).addParam(container).addParam(childContent).addParam(containerInstance);
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
    @CloseDBIfOpened
    @Override
    @Deprecated
    public MultiTree getMultiTree(final String htmlPage, final String container, final String childContent) throws DotDataException {

        final DotConnect db = new DotConnect().setSQL(SELECT_SQL).addParam(htmlPage).addParam(container).addParam(childContent)
                .addParam(Container.LEGACY_RELATION_TYPE);
        db.loadResult();

        return TransformerLocator.createMultiTreeTransformer(db.loadObjectResults()).findFirst();

    }

    @CloseDBIfOpened
    @Override
    public java.util.List<MultiTree> getMultiTrees(final Identifier parent) throws DotDataException {
        return getMultiTrees(parent.getId());
    }

    @CloseDBIfOpened
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

    /**
     * Returns the tree with the pages that matched on the parent1
     * 
     * @param parentInode
     * @return
     * @throws DotDataException
     */
    @CloseDBIfOpened
    @Override
    public java.util.List<MultiTree> getMultiTreesByPage(final String parentInode) throws DotDataException {

        final DotConnect db = new DotConnect().setSQL(SELECT_BY_PAGE).addParam(parentInode);
        return TransformerLocator.createMultiTreeTransformer(db.loadObjectResults()).asList();

    }

    @CloseDBIfOpened
    @Override
    public java.util.List<MultiTree> getAllMultiTrees() {
        try {
            final DotConnect db = new DotConnect().setSQL(SELECT_ALL);

            return TransformerLocator.createMultiTreeTransformer(db.loadObjectResults()).asList();

        } catch (Exception e) {
            Logger.error(MultiTreeAPIImpl.class, "getMultiTree failed:" + e, e);
            throw new DotRuntimeException(e);
        }
    }

    @CloseDBIfOpened
    @Override
    public java.util.List<MultiTree> getMultiTrees(final String htmlPage, final String container, final String containerInstance) {
        try {

            final DotConnect db =
                    new DotConnect().setSQL(SELECT_BY_PARENTS_AND_RELATIONS).addParam(htmlPage).addParam(container).addParam(containerInstance);
            return TransformerLocator.createMultiTreeTransformer(db.loadObjectResults()).asList();
        } catch (Exception e) {
            Logger.error(MultiTreeAPIImpl.class, "getMultiTree failed:" + e, e);
            throw new DotRuntimeException(e);
        }
    }

    @CloseDBIfOpened
    @Override
    public java.util.List<MultiTree> getMultiTrees(final IHTMLPage htmlPage, final Container container) throws DotDataException {
        return getMultiTrees(htmlPage.getIdentifier(), container.getIdentifier());
    }

    @CloseDBIfOpened
    @Override
    public java.util.List<MultiTree> getMultiTrees(final String htmlPage, final String container) throws DotDataException {

        final DotConnect db = new DotConnect().setSQL(SELECT_BY_TWO_PARENTS).addParam(htmlPage).addParam(container);
        return TransformerLocator.createMultiTreeTransformer(db.loadObjectResults()).asList();

    }

    @CloseDBIfOpened
    @Override
    public java.util.List<MultiTree> getMultiTrees(final IHTMLPage htmlPage, final Container container, final String containerInstance) {
        return getMultiTrees(htmlPage.getIdentifier(), container.getIdentifier(), containerInstance);
    }

    @CloseDBIfOpened
    @Override
    public java.util.List<MultiTree> getContainerMultiTrees(final String containerIdentifier) throws DotDataException {
        return getMultiTrees(containerIdentifier);
    }

    @CloseDBIfOpened
    @Override
    public java.util.List<MultiTree> getMultiTreesByChild(final String contentIdentifier) throws DotDataException {

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
        Logger.info(this, String.format("Saving MutiTree: %s", mTree));
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
        if (mTrees == null || mTrees.isEmpty())
            throw new DotDataException("empty list passed in");
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
     * @param mTrees
     * @throws DotDataException
     */
    @Override
    @WrapInTransaction
    public void saveMultiTrees(final String pageId, final List<MultiTree> mTrees) throws DotDataException {
        Logger.info(this, String.format("Saving MutiTrees: pageId -> %s multiTrees-> %s", pageId, mTrees));
        if (mTrees == null) {
            throw new DotDataException("empty list passed in");
        }

        Logger.debug(MultiTreeAPIImpl.class, String.format("Saving page's content: %s", mTrees));

        final DotConnect db = new DotConnect();

        db.setSQL(DELETE_ALL_MULTI_TREE_SQL).addParam(pageId).addParam(ContainerUUID.UUID_DEFAULT_VALUE).loadResult();

        if (!mTrees.isEmpty()) {
            final List<Params> insertParams = Lists.newArrayList();
            final Set<String> newContainers = new HashSet<>();

            for (final MultiTree tree : mTrees) {
                insertParams
                        .add(new Params(pageId, tree.getContainer(), tree.getContentlet(), tree.getRelationType(), tree.getTreeOrder()));
                newContainers.add(tree.getContainer());
            }

            db.executeBatch(INSERT_SQL, insertParams);
        }

        updateHTMLPageVersionTS(pageId);
        refreshPageInCache(pageId);
    }

    @Override
    @CloseDBIfOpened
    public List<String> getContainersId(final String pageId) throws DotDataException {
        return this.getMultiTrees(pageId).stream().map(multiTree -> multiTree.getContainer()).distinct().sorted()
                .collect(Collectors.toList());
    }


    private void _dbUpsert(final MultiTree mtree) throws DotDataException {
        _dbDelete(mtree);
        _dbInsert(mtree);

    }

    private void _reorder(final MultiTree tree) throws DotDataException {

        List<MultiTree> trees = getMultiTrees(tree.getHtmlPage(), tree.getContainer(), tree.getRelationType());
        trees = trees.stream().filter(rowTree -> !rowTree.equals(tree)).collect(Collectors.toList());
        int maxOrder = (tree.getTreeOrder() > trees.size()) ? trees.size() : tree.getTreeOrder();
        trees.add(maxOrder, tree);

        saveMultiTrees(trees);

    }


    private void _dbInsert(final MultiTree o) throws DotDataException {
        new DotConnect().setSQL(INSERT_SQL).addParam(o.getHtmlPage()).addParam(o.getContainer()).addParam(o.getContentlet())
                .addParam(o.getRelationType()).addParam(o.getTreeOrder()).loadResult();
    }


    /**
     * Update the version_ts of all versions of the HTML Page with the given id. If a MultiTree Object
     * has been added or deleted from this page, its version_ts value needs to be updated so it can be
     * included in future Push Publishing tasks
     * 
     * @param id The HTMLPage Identifier to pass in
     * @throws DotContentletStateException
     * @throws DotDataException
     * @throws DotSecurityException
     * 
     */
    private void updateHTMLPageVersionTS(final String id) throws DotDataException {
        final List<ContentletVersionInfo> infos = APILocator.getVersionableAPI().findContentletVersionInfos(id);
        for (ContentletVersionInfo versionInfo : infos) {
            if (versionInfo != null) {
                versionInfo.setVersionTs(new Date());
                APILocator.getVersionableAPI().saveContentletVersionInfo(versionInfo);
            }
        }
    }


    private void refreshPageInCache(final String pageIdentifier) throws DotDataException {
        
        
        CacheLocator.getMultiTreeCache().removePageMultiTrees(pageIdentifier);
        final Set<String> inodes = new HashSet<String>();
        final List<ContentletVersionInfo> infos = APILocator.getVersionableAPI().findContentletVersionInfos(pageIdentifier);
        for (ContentletVersionInfo versionInfo : infos) {
            inodes.add(versionInfo.getWorkingInode());
            if (versionInfo.getLiveInode() != null) {
                inodes.add(versionInfo.getLiveInode());
            }
        }
        try {
            List<Contentlet> contentlets = APILocator.getContentletAPIImpl().findContentlets(Lists.newArrayList(inodes));

            PageLoader pageLoader = new PageLoader();

            for (Contentlet pageContent : contentlets) {
                IHTMLPage htmlPage = APILocator.getHTMLPageAssetAPI().fromContentlet(pageContent);

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
    public Table<String, String, Set<String>> getPageMultiTrees(final IHTMLPage page, final boolean liveMode)
            throws DotDataException, DotSecurityException {

        
        final Optional<Table<String, String, Set<String>>> pageContentsOpt =
                CacheLocator.getMultiTreeCache().getPageMultiTrees(page.getIdentifier(), liveMode);
        
        if(pageContentsOpt.isPresent()) {
            return pageContentsOpt.get();
        }

        final Table<String, String, Set<String>> pageContents = HashBasedTable.create();
        final List<MultiTree> multiTrees = this.getMultiTrees(page.getIdentifier());

        for (final MultiTree multiTree : multiTrees) {
            final ContainerAPI containerAPI = APILocator.getContainerAPI();
            final ContentletAPI contentletAPI = APILocator.getContentletAPI();
            final User systemUser = APILocator.systemUser();
            Container container = null;

            try {

                //container = containerAPI.getWorkingContainerById(multiTree.getContainer(), systemUser, false);
                container = liveMode? containerAPI.getLiveContainerById(multiTree.getContainer(), systemUser, false)
                        : containerAPI.getWorkingContainerById(multiTree.getContainer(), systemUser, false);

                if (container == null && !liveMode) {
                    continue;
                }
            } catch (NotFoundInDbException e) {
                Logger.debug(this, e.getMessage(), e);
                continue;
            }

            Contentlet contentlet = null;
            try {
                contentlet = contentletAPI.findContentletByIdentifierAnyLanguage(multiTree.getContentlet());;
            } catch (DotDataException | DotSecurityException | DotContentletStateException e) {
                Logger.warn(this.getClass(), "invalid contentlet on multitree:" + multiTree);
            }
            if (contentlet != null) {
                final Set<String> myContents = pageContents.contains(multiTree.getContainer(), multiTree.getRelationType())
                        ? pageContents.get(multiTree.getContainer(), multiTree.getRelationType())
                        : new LinkedHashSet<>();
                if (container != null && myContents.size() < container.getMaxContentlets()) {
                    myContents.add(multiTree.getContentlet());
                }

                pageContents.put(multiTree.getContainer(), multiTree.getRelationType(), myContents);
            } ;

        }

        this.addEmptyContainers(page, pageContents, liveMode);
        
        CacheLocator.getMultiTreeCache().putPageMultiTrees(page.getIdentifier(), liveMode, pageContents);
        return pageContents;
    }


    private void addEmptyContainers(final IHTMLPage page, Table<String, String, Set<String>> pageContents, final boolean liveMode)
            throws DotDataException, DotSecurityException {

        try {
            
            
            final Template template =
                    APILocator.getTemplateAPI().findWorkingTemplate(page.getTemplateId(), APILocator.getUserAPI().getSystemUser(), false);
            if (!template.isDrawed()) {
                return;
            }

            final TemplateLayout layout = DotTemplateTool.themeLayout(page.getTemplateId(), APILocator.getUserAPI().getSystemUser(), false);
            final List<ContainerUUID> containersUUID = APILocator.getTemplateAPI().getContainersUUID(layout);

            for (final ContainerUUID containerUUID : containersUUID) {

                Container container = null;
                try {
                    container = (liveMode) ? APILocator.getContainerAPI().getLiveContainerById(containerUUID.getIdentifier(), APILocator.systemUser(), false)
                            : APILocator.getContainerAPI().getWorkingContainerById(containerUUID.getIdentifier(), APILocator.systemUser(), false);

                    if (container == null && !liveMode) {
                        continue;
                    }
                } catch (NotFoundInDbException e) {
                    Logger.debug(this, e.getMessage(), e);
                    continue;
                }


                if (!pageContents.contains(containerUUID.getIdentifier(), containerUUID.getUUID())) {
                    final boolean isLegacyValue = ContainerUUID.UUID_LEGACY_VALUE.equals(containerUUID.getUUID());

                    if (!isLegacyValue || !pageContents.contains(containerUUID.getIdentifier(), ContainerUUID.UUID_START_VALUE)) {
                        pageContents.put(containerUUID.getIdentifier(), containerUUID.getUUID(), new LinkedHashSet<>());
                    }
                }
            }
        } catch (RuntimeException e) {
            Logger.error(this, e.getMessage(), e);
        }
    }


}
