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
