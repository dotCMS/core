package com.dotmarketing.factories;

import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Table;

/**
 * API for {@link com.dotmarketing.beans.MultiTree}
 */
public interface MultiTreeAPI {
    void saveMultiTrees(String pageId, List<MultiTree> mTrees) throws DotDataException;

    void saveMultiTree(MultiTree multiTree) throws DotDataException;

    void deleteMultiTree(MultiTree multiTree) throws DotDataException;

    Table<String, String, Set<Contentlet>> getPageMultiTrees(final IHTMLPage page, final boolean liveMode)
            throws DotDataException, DotSecurityException;

    void updateMultiTree(final String pageId, final String containerId, final String oldRelationType,
                                       final String newRelationType) throws DotDataException;
}
