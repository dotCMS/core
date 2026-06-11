package com.dotmarketing.factories;

import com.dotcms.util.transform.TransformerLocator;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.Params;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author maria
 */
public class TreeFactory {



    public static Tree getTree(final Inode parent, final Inode child) {
        return getTree(parent.getInode(), child.getInode());
    }

    public static Tree getTree(final String parent, final String child) {
        String relationType = "child";
        return getTree(parent, child, relationType);

    }

    public static Tree getTree(final String parent, final String child, final String relationType) {
        try {
            DotConnect dc = new DotConnect();
            String query = "select * from tree where parent = ? and child = ? ";
            if (relationType != null)
                query += " and relation_type = ?";
            dc.setSQL(query);
            dc.addParam(parent);
            dc.addParam(child);
            if (relationType != null)
                dc.addParam(relationType);

            return TransformerLocator.createTreeTransformer(dc.loadObjectResults()).findFirst();
        } catch (DotDataException e) {
            Logger.warn(TreeFactory.class, "getTree failed:" + e, e);
        }

        return new Tree();
    }

    public static Tree getTree(final Tree tree) {
        return getTree(tree.getParent(), tree.getChild(), tree.getRelationType());
    }

    public static List<Tree> getAllTrees(final int limit, final int offset) throws DotDataException {

        DotConnect dc = new DotConnect().setSQL("select * from tree order by parent,child,relation_type ").setMaxRows(limit)
                .setStartRow(offset);

        return TransformerLocator.createTreeTransformer(dc.loadObjectResults()).asList();
    }


    public static Tree getTree(final Inode parent, final Inode child, final String relationType) {
        return getTree(parent.getInode(), child.getInode(), relationType);
    }



    public static List<Tree> getTreesByParentAndRelationType(Identifier parent, String relationType) {
        return getTreesByParentAndRelationType(parent.getId(), relationType);
    }


    public static List<Tree> getTreesByParentAndRelationType(Inode parent, String relationType) {
        return getTreesByParentAndRelationType(parent.getInode(), relationType);
    }


    private static List<Tree> getTreesByParentAndRelationType(String parent, String relationType) {
        try {
            DotConnect dc = new DotConnect();
            dc.setSQL("select * from tree where  parent = ? and relation_type = ? order by tree_order asc");
            dc.addParam(parent);
            dc.addParam(relationType);

            return TransformerLocator.createTreeTransformer(dc.loadObjectResults()).asList();
        } catch (Exception e) {
            Logger.warn(TreeFactory.class, "getTree failed:" + e, e);
        }

        return new ArrayList<>();
    }



    private static List<Tree> getTreesByChildAndRelationType(String child, String relationType) {
        try {
            DotConnect dc = new DotConnect();
            dc.setSQL("select * from tree where  child = ? and relation_type = ? order by tree_order asc");
            dc.addParam(child);
            dc.addParam(relationType);

            return TransformerLocator.createTreeTransformer(dc.loadObjectResults()).asList();
        } catch (Exception e) {
            Logger.warn(TreeFactory.class, "getTree failed:" + e, e);
        }

        return new ArrayList<>();
    }


    public static List<Tree> getTreesByChildAndRelationType(Inode child, String relationType) {
        return getTreesByChildAndRelationType(child.getInode(), relationType);
    }

    public static List<Tree> getTreesByChildAndRelationType(Identifier child, String relationType) {
        return getTreesByChildAndRelationType(child.getId(), relationType);
    }


    public static List<Tree> getTreesByRelationType(String relationType) {
        try {
            DotConnect dc = new DotConnect();
            dc.setSQL("select * from tree where  relation_type = ?");
            dc.addParam(relationType);

            return TransformerLocator.createTreeTransformer(dc.loadObjectResults()).asList();
        } catch (Exception e) {
            Logger.warn(TreeFactory.class, "getTree failed:" + e, e);
        }

        return new ArrayList<>();
    }


    public static List<Tree> getTreesByParent(Inode inode) {
        return getTreesByParent(inode.getInode());
    }


    public static List<Tree> getTreesByParent(String inode) {
        try {
            DotConnect dc = new DotConnect();
            dc.setSQL("select * from tree where  parent = ?");
            dc.addParam(inode);

            return TransformerLocator.createTreeTransformer(dc.loadObjectResults()).asList();
        } catch (Exception e) {
            Logger.warn(TreeFactory.class, "getTree failed:" + e, e);
        }

        return new ArrayList<>();
    }

    public static List<Tree> getTreesByChild(Inode inode) {
        return getTreesByChild(inode.getInode());
    }


    public static List<Tree> getTreesByChild(String inode) {
        try {
            DotConnect dc = new DotConnect();
            dc.setSQL("select * from tree where  child = ?");
            dc.addParam(inode);

            return TransformerLocator.createTreeTransformer(dc.loadObjectResults()).asList();
        } catch (Exception e) {
            Logger.warn(TreeFactory.class, "getTree failed:" + e, e);
        }

        return new ArrayList<>();
    }

    public static void swapTrees(final Inode i1, final Inode i2) throws DotDataException {

        List<Tree> newTrees = new ArrayList<>();

        // Removing actual trees and creating the new ones
        Iterator<Tree> it = getTreesByParent(i1).iterator();
        while (it.hasNext()) {
            Tree tree = (Tree) it.next();
            newTrees.add(new Tree(i2.getInode(), tree.getChild(), tree.getRelationType(), tree.getTreeOrder()));
            deleteTree(tree);
        }

        it = getTreesByChild(i1).iterator();
        while (it.hasNext()) {
            Tree tree = (Tree) it.next();
            newTrees.add(new Tree(tree.getParent(), i2.getInode(), tree.getRelationType(), tree.getTreeOrder()));
            deleteTree(tree);
        }

        it = getTreesByParent(i2).iterator();
        while (it.hasNext()) {
            Tree tree = (Tree) it.next();
            newTrees.add(new Tree(i1.getInode(), tree.getChild(), tree.getRelationType(), tree.getTreeOrder()));
            deleteTree(tree);
        }

        it = getTreesByChild(i2).iterator();
        while (it.hasNext()) {
            Tree tree = (Tree) it.next();
            newTrees.add(new Tree(tree.getParent(), i1.getInode(), tree.getRelationType(), tree.getTreeOrder()));
            deleteTree(tree);
        }

        // Saving new trees
        it = newTrees.iterator();
        while (it.hasNext()) {
            Tree tree = (Tree) it.next();
            saveTree(tree);
        }
    }

    public static void deleteTree(Tree tree) {
        try {
            DotConnect dc = new DotConnect();

            dc.setSQL("delete from tree where parent = ? and child = ? and relation_type = ?");

            dc.addParam(tree.getParent());
            dc.addParam(tree.getChild());
            dc.addParam(tree.getRelationType());
            dc.loadResult();
        } catch (DotDataException e) {

            throw new DotStateException(e);

        }
    }

    public static void deleteTreesByParent(Inode parent) {
        try {
            DotConnect dc = new DotConnect();
            dc.setSQL("delete from tree where parent = ?");
            dc.addParam(parent.getInode());
            dc.loadResult();
        } catch (DotDataException e) {
            throw new DotStateException(e);
        }
    }

    public static void deleteTreesByParentById(final String parentId) throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL("DELETE FROM tree WHERE parent = ?").addParam(parentId).loadResult();
    }

    public static void deleteTreesByParentAndRelationType(Inode parent, String relationType) {
        try {
            DotConnect dc = new DotConnect();
            dc.setSQL("delete from tree where parent = ?  and relation_type = ?");
            dc.addParam(parent.getInode());
            dc.addParam(relationType);
            dc.loadResult();
        } catch (DotDataException e) {
            throw new DotStateException(e);
        }
    }


    public static void deleteTreesByParentAndChildAndRelationType(String parent, String child, String relationType) {
        try {

            DotConnect dc = new DotConnect();
            dc.setSQL("delete from tree where parent = ? and child = ? and relation_type = ?");

            dc.addParam(parent);
            dc.addParam(child);
            dc.addParam(relationType);
            dc.loadResult();
        } catch (DotDataException e) {
            throw new DotStateException(e);
        }
    }


    public static void deleteTreesByChildAndRelationType(Inode child, String relationType) {
        try {
            DotConnect dc = new DotConnect();
            dc.setSQL("delete from tree where child = ? and relation_type = ?");

            dc.addParam(child);
            dc.addParam(relationType);
            dc.loadResult();
        } catch (DotDataException e) {
            throw new DotStateException(e);
        }
    }

    public static void deleteTreesByChild(Inode child) {
        try {
            DotConnect dc = new DotConnect();
            dc.setSQL("delete from tree where child =?");

            dc.addParam(child);

            dc.loadResult();
        } catch (DotDataException e) {
            throw new DotStateException(e);
        }
    }

    public static void deleteTreesByChildId(final String childId) throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL("DELETE FROM tree WHERE child = ?").addParam(childId).loadResult();
    }

    public static void deleteTreesByRelationType(String relationType) {
        try {
            DotConnect dc = new DotConnect();
            dc.setSQL("delete from tree where relation_type = ?");
            dc.addParam(relationType);
            dc.loadResult();
        } catch (DotDataException e) {
            throw new DotStateException(e);
        }
    }

    /**
     * Does a upsert into the tree table
     * 
     * @param tree
     */
    public static void saveTree(Tree tree) {
        deleteTree(tree);
        insertTree(tree);
    }

    /**
     * Does an insert into the tree table
     * 
     * @param tree
     */
    public static void insertTree(Tree tree) {
        DotConnect dc = new DotConnect();
        dc.setSQL("insert into tree (child,parent,relation_type,tree_order) values (?,?,?,?)");
        dc.addParam(tree.getChild());
        dc.addParam(tree.getParent());
        dc.addParam(tree.getRelationType());
        dc.addParam(tree.getTreeOrder());
        try {
            dc.loadResult();
        } catch (DotDataException e) {
            throw new DotStateException(e);
        }
    }

    public static void deleteTreesByParentAndRelationType(final String parentId,
                                                          final String relationType) {
        try {
            new DotConnect()
                    .setSQL("DELETE FROM tree WHERE parent = ? AND relation_type = ?")
                    .addParam(parentId)
                    .addParam(relationType)
                    .loadResult();
        } catch (final DotDataException e) {
            throw new DotStateException(e);
        }
    }

    public static void deleteTreesByChildAndRelationType(final String childId,
                                                         final String relationType) {
        try {
            new DotConnect()
                    .setSQL("DELETE FROM tree WHERE child = ? AND relation_type = ?")
                    .addParam(childId)
                    .addParam(relationType)
                    .loadResult();
        } catch (final DotDataException e) {
            throw new DotStateException(e);
        }
    }

    /**
     * Returns the identifiers of the children related to the given parent under the given
     * relation type, ordered by tree order. Unlike content-returning lookups, this method never
     * hydrates contentlets — it reads the tree table only.
     *
     * @param parentId     Identifier of the parent content
     * @param relationType The relationship type value
     * @return The child identifiers, in tree order
     */
    public static List<String> getRelatedIdsByParentAndRelationType(final String parentId,
            final String relationType) {
        return getRelatedIds("child", "parent", parentId, relationType);
    }

    /**
     * Returns the identifiers of the parents related to the given child under the given relation
     * type, ordered by tree order. Unlike content-returning lookups, this method never hydrates
     * contentlets — it reads the tree table only.
     *
     * @param childId      Identifier of the child content
     * @param relationType The relationship type value
     * @return The parent identifiers, in tree order
     */
    public static List<String> getRelatedIdsByChildAndRelationType(final String childId,
            final String relationType) {
        return getRelatedIds("parent", "child", childId, relationType);
    }

    private static List<String> getRelatedIds(final String selectColumn, final String whereColumn,
            final String id, final String relationType) {
        try {
            return new DotConnect()
                    .setSQL("SELECT " + selectColumn + " FROM tree WHERE " + whereColumn
                            + " = ? AND relation_type = ? ORDER BY tree_order")
                    .addParam(id)
                    .addParam(relationType)
                    .loadObjectResults().stream()
                    .map(row -> (String) row.get(selectColumn))
                    .toList();
        } catch (final DotDataException e) {
            throw new DotStateException(e);
        }
    }

    /**
     * Deletes the tree rows for the given parent and relation type whose child is in the given
     * list. Unlike {@link #deleteTreesByParentAndRelationType(String, String)}, rows pointing to
     * children NOT included in the list are preserved.
     *
     * @param parentId     Identifier of the parent content
     * @param childIds     Identifiers of the child contents whose rows can be deleted
     * @param relationType The relationship type value
     */
    public static void deleteTreesByParentAndChildrenAndRelationType(final String parentId,
            final List<String> childIds, final String relationType) {
        deleteTreesScopedByRelationType("parent", "child", parentId, childIds, relationType);
    }

    /**
     * Deletes the tree rows for the given child and relation type whose parent is in the given
     * list. Unlike {@link #deleteTreesByChildAndRelationType(String, String)}, rows pointing to
     * parents NOT included in the list are preserved.
     *
     * @param childId      Identifier of the child content
     * @param parentIds    Identifiers of the parent contents whose rows can be deleted
     * @param relationType The relationship type value
     */
    public static void deleteTreesByChildAndParentsAndRelationType(final String childId,
            final List<String> parentIds, final String relationType) {
        deleteTreesScopedByRelationType("child", "parent", childId, parentIds, relationType);
    }

    private static final int DELETE_TREES_CHUNK_SIZE = 500;

    private static void deleteTreesScopedByRelationType(final String fixedColumn,
            final String scopedColumn, final String fixedId, final List<String> scopedIds,
            final String relationType) {
        if (scopedIds == null || scopedIds.isEmpty()) {
            return;
        }
        try {
            for (int from = 0; from < scopedIds.size(); from += DELETE_TREES_CHUNK_SIZE) {
                final List<String> chunk = scopedIds.subList(from,
                        Math.min(from + DELETE_TREES_CHUNK_SIZE, scopedIds.size()));
                final DotConnect dc = new DotConnect().setSQL(
                        "DELETE FROM tree WHERE " + fixedColumn + " = ? AND relation_type = ? AND "
                                + scopedColumn + " IN ("
                                + DotConnect.createParametersPlaceholder(chunk.size()) + ")");
                dc.addParam(fixedId);
                dc.addParam(relationType);
                chunk.forEach(dc::addParam);
                dc.loadResult();
            }
        } catch (final DotDataException e) {
            throw new DotStateException(e);
        }
    }

    /**
     * Saves the given trees in batch, preserving the upsert semantics of
     * {@link #saveTree(Tree)} (delete-then-insert): duplicates by the {@code (child, parent,
     * relation_type)} primary key collapse to the last occurrence, and pre-existing rows with
     * the same key are replaced instead of triggering a constraint violation.
     *
     * @param trees The {@link Tree} rows to save
     */
    public static void insertTrees(final List<Tree> trees) {
        if (trees == null || trees.isEmpty()) {
            return;
        }
        final Map<String, Tree> uniqueTrees = new LinkedHashMap<>();
        for (final Tree tree : trees) {
            uniqueTrees.put(tree.getChild() + "|" + tree.getParent() + "|"
                    + tree.getRelationType(), tree);
        }

        final List<Params> deleteParams = new ArrayList<>(uniqueTrees.size());
        final List<Params> insertParams = new ArrayList<>(uniqueTrees.size());
        for (final Tree tree : uniqueTrees.values()) {
            deleteParams.add(new Params(tree.getChild(), tree.getParent(),
                    tree.getRelationType()));
            insertParams.add(new Params(tree.getChild(), tree.getParent(),
                    tree.getRelationType(), tree.getTreeOrder()));
        }

        try {
            new DotConnect().executeBatch(
                    "DELETE FROM tree WHERE child = ? AND parent = ? AND relation_type = ?",
                    deleteParams);
            new DotConnect().executeBatch(
                    "INSERT INTO tree (child, parent, relation_type, tree_order) VALUES (?,?,?,?)",
                    insertParams);
        } catch (final DotDataException e) {
            throw new DotStateException(e);
        }
    }

}
