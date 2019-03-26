package com.dotmarketing.logConsole.model;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.DotCacheException;
import java.util.List;

/** Created by Jonathan Gamba. Date: 5/21/12 Time: 4:25 PM */
public interface LogMapperCache extends Cachable {

  /**
   * Return all the records stored on cache for this primary group
   *
   * @return
   * @throws DotCacheException
   */
  public List<LogMapperRow> get() throws DotCacheException;

  /**
   * Puts a LogMapperRow collection in a cache.
   *
   * @param logMapperRows
   * @throws DotCacheException
   */
  public void put(List<LogMapperRow> logMapperRows) throws DotCacheException;
}
