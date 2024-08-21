package com.dotmarketing.portlets.categories.business;


import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;

import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;


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

    private final String COUNT_CHILDREN_QUERY = "SELECT COUNT(*) " +
            "FROM category LEFT JOIN tree ON category.inode = tree.child WHERE tree.parent = c.inode";

    protected final String rootInode;
    protected final Level level;
    protected final boolean countChildren;
    protected final CategorySearchCriteria searchCriteria;

    protected CategoryQueryBuilder(final CategorySearchCriteria searchCriteria) {
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
                String.format(", (%s) as childrenCount", COUNT_CHILDREN_QUERY) : StringPool.BLANK;
    }

    public enum Level {
        TOP,
        CHILDREN,
        ALL_LEVELS;
    }

}
