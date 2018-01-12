package com.dotmarketing.factories;

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
    void saveMultiTrees(String pageId, List<MultiTree> mTrees) throws DotDataException;

    void saveMultiTree(MultiTree multiTree) throws DotDataException;

    Table<String, String, Set<String>> getPageMultiTrees(final IHTMLPage page, final boolean liveMode)
            throws DotDataException, DotSecurityException;
}
