package com.dotmarketing.factories;

import com.dotcms.business.WrapInTransaction;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.services.PageServices;
import com.dotmarketing.util.Logger;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

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
public class MultiTreeFactory {

    final static String DELETE_SQL = "delete from multi_tree where parent1=? and parent2=? and child=? and  relation_type = ?";
    final static String SELECT_SQL =
            "select * from multi_tree where parent1 = ? and parent2 = ? and child = ? and  relation_type = ?";

    final static String INSERT_SQL =
            "insert into multi_tree (parent1, parent2, child, relation_type, tree_order ) values (?,?,?,?,?)  ";

    final static String SELECT_BY_ONE_PARENT = "select * from multi_tree where parent1 = ? or parent2 = ? ";
    final static String SELECT_BY_TWO_PARENTS = "select * from multi_tree where parent1 = ? and parent2 = ?  order by tree_order";
    final static String SELECT_ALL = "select * from multi_tree  ";
    final static String SELECT_BY_CHILD = "select * from multi_tree where child = ?  order by parent1, parent2, relation_type ";
    final static String SELECT_BY_PARENTS_AND_RELATIONS =
            " select * from multi_tree where parent1 = ? and parent2 = ? and relation_type = ? order by tree_order";


    public static void deleteMultiTree(final MultiTree mTree) throws DotDataException {
        _dbDelete(mTree);
        updateHTMLPageVersionTS(mTree.getHtmlPage());
        refreshPageInCache(mTree.getHtmlPage());
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
    private static void _dbDelete(final MultiTree mTree) throws DotDataException {

        DotConnect db = new DotConnect().setSQL(DELETE_SQL)
            .addParam(mTree.getHtmlPage())
            .addParam(mTree.getContainer())
            .addParam(mTree.getContentlet())
            .addParam(mTree.getRelationType());
        db.loadResult();

    }

    /**
     * use the relationType for specificity{link
     * {@link #getMultiTree(String, String, String, String)}
     * 
     * @param htmlPage
     * @param container
     * @param childContent
     * @return
     * @throws DotDataException
     */
    @Deprecated
    public static MultiTree getMultiTree(Identifier htmlPage, Identifier container, Identifier childContent)
            throws DotDataException {
        return getMultiTree(htmlPage, container, childContent, MultiTree.LEGACY_RELATION_TYPE);
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

        return dbToMultiTree(db.loadObjectResults()).stream()
            .findFirst()
            .orElse(null);

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

        return dbToMultiTree(db.loadObjectResults());

    }

    public static java.util.List<MultiTree> getAllMultiTrees() {
        try {
            DotConnect db = new DotConnect().setSQL(SELECT_ALL);

            return dbToMultiTree(db.loadObjectResults());

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
            return dbToMultiTree(db.loadObjectResults());
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
        return dbToMultiTree(db.loadObjectResults());

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

        return dbToMultiTree(db.loadObjectResults());

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
     * @param o - The multi-tree structure.
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @WrapInTransaction
    public static void saveMultiTrees(List<MultiTree> mTrees) throws DotDataException {
        if (mTrees == null || mTrees.isEmpty())
            throw new DotDataException("empty list passed in");
        int i = 0;
        for (MultiTree tree : mTrees) {
            _dbUpsert(tree.setTreeOrder(i++));
        }
        MultiTree mTree = mTrees.get(0);
        updateHTMLPageVersionTS(mTree.getHtmlPage());
        refreshPageInCache(mTree.getHtmlPage());

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

    /**
     * Refresh cached objects for all versions of the HTMLPage with the given pageIdentifier.
     * 
     * @param pageIdentifier The HTMLPage Identifier to pass in
     * @throws DotContentletStateException
     * @throws DotDataException
     * 
     */
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
            for (Contentlet pageContent : contentlets) {
                IHTMLPage htmlPage = APILocator.getHTMLPageAssetAPI()
                    .fromContentlet(pageContent);
                PageServices.invalidateAll(htmlPage);
            }
        } catch (DotStateException | DotSecurityException e) {
            Logger.warn(MultiTreeFactory.class, "unable to refresh page cache:" + e.getMessage());
        }
    }


    private static MultiTree dbToMultiTree(Map<String, Object> row) {
        final String relationType = (String) row.getOrDefault("relation_type", null);
        final String parent1 = (String) row.getOrDefault("parent1", null);
        final String parent2 = (String) row.getOrDefault("parent2", null);
        final String child = (String) row.getOrDefault("child", null);
        final int order = Integer.valueOf((Integer) row.getOrDefault("tree_order", 0));
        return new MultiTree(parent1, parent2, child, relationType, order);
    }

    public static List<MultiTree> dbToMultiTree(List<Map<String, Object>> dbRows) {
        return (List<MultiTree>) dbRows.stream()
            .map(row -> dbToMultiTree(row))
            .collect(Collectors.toList());
    }

    /**
     * {link {@link #saveMultiTree(MultiTree)} The multitree does not respect language
     * 
     * @param multiTreeEN
     * @param english
     * @throws DotDataException
     */
    @Deprecated
    public static void saveMultiTree(MultiTree multiTree, long lang) throws DotDataException {
        saveMultiTree(multiTree);
    }


}
