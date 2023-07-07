package com.dotcms.business;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.categories.model.Category;

import java.util.Map;

/**
 * Encapsulates the persistance class for the System table
 * @author jsanca
 */
public abstract class SystemTableFactory  {

    /**
     * 
     * @param key
     * @throws DotDataException
     */
    protected abstract void find(String key) throws DotDataException;

    protected abstract Map<String, String> findAll() throws DotDataException;
    protected abstract void save(String key, String value) throws DotDataException;
    protected abstract void update(String key, String value) throws DotDataException;
    protected abstract void delete(Category object) throws DotDataException;

    abstract protected  void clearCache();
}
