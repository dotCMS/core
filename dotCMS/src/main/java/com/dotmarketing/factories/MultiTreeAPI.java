package com.dotmarketing.factories;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
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

}
