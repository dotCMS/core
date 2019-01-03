package com.dotmarketing.factories;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.google.common.collect.Table;

import java.util.List;
import java.util.Set;

/**
 * API for {@link com.dotmarketing.beans.MultiTree}
 */
public interface MultiTreeAPI {

    /**
     * Saves multitrees for a given page
     * 
     * @param mTrees
     * @throws DotDataException
     */
    void saveMultiTrees(String pageId, List<MultiTree> mTrees) throws DotDataException;

    /**
     * Saves a specific multitree
     * 
     * @param multiTree
     * @throws DotDataException
     */
    void saveMultiTree(MultiTree multiTree) throws DotDataException;

    /**
     * Deletes a specific multitree
     * 
     * @param multiTree
     * @throws DotDataException
     */
    void deleteMultiTree(MultiTree multiTree) throws DotDataException;

    /**
     * Deletes a multitrees related to the identifier
     *
     * @param identifier {@link Identifier}
     * @throws DotDataException
     */
    void deleteMultiTreeByIdentifier(Identifier identifier) throws DotDataException;

    /**
     * Removes the references to these inodes on child and parents.
     * @param inodes
     * @throws DotDataException
     */
    void deleteMultiTreesForInodes(List<String> inodes) throws DotDataException;


    /**
     * deletes all the multi tress related to the identifier, including parents and child
     * in addition for the pages related refresh the cache and publish relationships
     * @param identifier String
     * @throws DotDataException
     */
    void deleteMultiTreesRelatedToIdentifier(final String identifier) throws DotDataException;

    /**
     * This method returns ALL multitree entries (in all languages) for a given page. It is up to
     * what ever page renderer to properly choose which multitree children to show for example, show
     * an english content on a spanish page when language fallback=true or specific content for a
     * given persona.
     * 
     * @param page
     * @param liveMode
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    Table<String, String, Set<String>>  getPageMultiTrees(final IHTMLPage page, final boolean liveMode)
            throws DotDataException, DotSecurityException;

    void updateMultiTree(final String pageId, final String containerId, final String oldRelationType,
            final String newRelationType) throws DotDataException;

    void saveMultiTrees(List<MultiTree> mTrees) throws DotDataException;

    void deleteMultiTree(List<MultiTree> mTree) throws DotDataException;

    void deleteMultiTreeByParent(String pageOrContainer) throws DotDataException;

    void deleteMultiTreeByChild(String contentIdentifier) throws DotDataException;

    List<MultiTree> getAllMultiTrees();

    List<MultiTree> getMultiTreesByPage(String parentInode) throws DotDataException;

    List<MultiTree> getMultiTrees(String htmlPage, String container, String relationType);

    List<MultiTree> getMultiTrees(IHTMLPage htmlPage, Container container) throws DotDataException;

    List<MultiTree> getMultiTrees(String htmlPage, String container) throws DotDataException;

    List<MultiTree> getMultiTrees(IHTMLPage htmlPage, Container container, String relationType);

    List<MultiTree> getContainerMultiTrees(String containerIdentifier) throws DotDataException;

    List<MultiTree> getMultiTreesByChild(String contentIdentifier) throws DotDataException;

    List<MultiTree> getContainerStructureMultiTree(String containerIdentifier, String structureInode);

    List<String> getContainersId(String pageId) throws DotDataException;

    MultiTree getMultiTree(Identifier htmlPage, Identifier container, Identifier childContent, String relationType) throws DotDataException;

    MultiTree getMultiTree(String htmlPage, String container, String childContent, String relationType) throws DotDataException;

    MultiTree getMultiTree(String htmlPage, String container, String childContent) throws DotDataException;

    List<MultiTree> getMultiTrees(Identifier parent) throws DotDataException;

    List<MultiTree> getMultiTrees(Identifier htmlPage, Identifier container) throws DotDataException;

    List<MultiTree> getMultiTrees(String parentInode) throws DotDataException;

}
