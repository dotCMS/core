package com.dotcms.business;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.categories.business.CategorySQL;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.UtilMethods;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Default implementation of {@link SystemTableFactory}
 */
public class SystemTableFactoryImpl extends SystemTableFactory {

    private final SystemCache systemCache;

    public SystemTableFactoryImpl () {

        this.systemCache = CacheLocator.getSystemCache();
    }
    @Override
    protected Optional<String> find(final String key) throws DotDataException {

        if(UtilMethods.isSet(key)) {

            Object value = this.systemCache.get(key);
            if(value == null) {

                final List<Map<String, Object>> result = new DotConnect()
                        .setSQL(" SELECT * FROM category WHERE inode = ? ")
                        .addParam(id)
                        .loadObjectResults();

                category = result.isEmpty() ? null : convertForCategory(result.get(0));

                if(category != null)
                    try {
                        catCache.put(category);
                    } catch (DotCacheException e) {
                        throw new DotDataException(e.getMessage(), e);
                    }
            }
        }

        return Optional.empty();
    }

    @Override
    protected Map<String, String> findAll() throws DotDataException {
        return null;
    }

    @Override
    protected void save(final String key, final String value) throws DotDataException {

    }

    @Override
    protected void update(final String key, final String value) throws DotDataException {

    }

    @Override
    protected void delete(final String key) throws DotDataException {

    }

    @Override
    protected void clearCache() {

    }
} // E:O:F:SystemTableFactoryImpl
