package com.dotmarketing.portlets.categories.business;

import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.categories.model.ShortCategory;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This utility class is used to build queries for finding instances of {@link Category} using the {@link CategoryFactoryImpl#findAll(CategorySearchCriteria)} method.
 *
 * There are two different SQL queries that can be utilized, each with a separate implementation based on the {@link CategorySearchCriteria}:
 *
 * Recursive Query: This query uses the WITH RECURSIVE clause to navigate through the {@link Category} tree. It is used in two scenarios:
 *
 * When you need to calculate the entire path of parent categories for the results.
 * When you need to search only within the descendants of a specific category.
 * This query is constructed using the {@link CategoryRecursiveQueryBuilder} class.
 * Simple Query: This query traverses the category and tree tables. It is used when:
 *
 * You need to search across the entire tree.
 * You want to search within a specific level, such as the children of a specific {@link Category} or the top-level categories.
 * This query is constructed using the {@link CategorySimpleQueryBuilder} class.
 */
public abstract class CategoryQueryBuilder {

    protected String rootInode;
    protected Level level;
    protected boolean countChildren;
    protected CategorySearchCriteria searchCriteria;

    public CategoryQueryBuilder(final CategorySearchCriteria searchCriteria) {
        this.rootInode = searchCriteria.rootInode;
        this.level = getLevel(searchCriteria);
        this.searchCriteria = searchCriteria;
        this.countChildren = searchCriteria.isCountChildren();
    }

    public Level getLevel(final CategorySearchCriteria searchCriteria) {
        if (searchCriteria.searchAllLevels) {
            return Level.ALL_LEVELS;
        } else if (UtilMethods.isSet(searchCriteria.rootInode)) {
            return Level.CHILDREN;
        } else {
            return Level.TOP;
        }
    }

    public abstract String build() throws DotDataException, DotSecurityException;

    protected String getChildrenCount() {
        return this.countChildren ?
                ", (SELECT COUNT(*) FROM tree WHERE parent = inode) as childrenCount" : StringPool.BLANK;
    }

    public enum Level {
        TOP,
        CHILDREN,
        ALL_LEVELS;
    }

}
