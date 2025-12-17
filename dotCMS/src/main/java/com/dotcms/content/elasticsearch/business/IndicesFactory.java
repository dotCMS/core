package com.dotcms.content.elasticsearch.business;

import com.dotcms.content.opensearch.business.IndicesInfoImpl;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;

import io.vavr.control.Try;
import java.util.Optional;
import org.apache.commons.beanutils.PropertyUtils;

public class IndicesFactory {


    protected static IndicesCache cache = CacheLocator.getIndiciesCache();


    public LegacyIndicesInfo loadLegacyIndices() throws DotDataException {
        return loadLegacyIndices(null);
    }

    @CloseDBIfOpened
    public LegacyIndicesInfo loadLegacyIndices(Connection conn) throws DotDataException {
        LegacyIndicesInfo info = cache.get();
        if (info == null) {
            // build it once
            synchronized (IndicesFactory.class) {
                if (conn == null) {
                    conn = DbConnectionFactory.getConnection();
                }
                info = cache.get();
                if (info == null) {
                    final LegacyIndicesInfo.Builder builder = new LegacyIndicesInfo.Builder();
                    final DotConnect dc = new DotConnect();
                    dc.setSQL("SELECT index_name,index_type FROM indicies WHERE index_version is NULL");
                    final List<Map<String, Object>> results = dc.loadResults(conn);
                    for (Map<String, Object> rr : results) {
                        String name = (String) rr.get("index_name");
                        String type = (String) rr.get("index_type");
                        if (type.equalsIgnoreCase(IndexType.WORKING.toString())) {
                            builder.setWorking(name);
                        } else if (type.equalsIgnoreCase(IndexType.LIVE.toString())) {
                            builder.setLive(name);
                        } else if (type.equalsIgnoreCase(IndexType.REINDEX_LIVE.toString())) {
                            builder.setReindexLive(name);
                        } else if (type.equalsIgnoreCase(IndexType.REINDEX_WORKING.toString())) {
                            builder.setReindexWorking(name);
                        } else if (type.equalsIgnoreCase(IndexType.SITE_SEARCH.toString())) {
                            builder.setSiteSearch(name);
                        }
                    }

                    info = builder.build();
                    cache.put(info);
                }
            }
        }
        return info;
    }

    @CloseDBIfOpened
    public Optional<IndicesInfo> loadIndices() throws DotDataException {
        IndicesInfoImpl info = null;
        // build it once
        synchronized (IndicesFactory.class) {
            final IndicesInfoImpl.Builder builder = new IndicesInfoImpl.Builder();
            final DotConnect dc = new DotConnect();
            dc.setSQL("SELECT index_name,index_type FROM indicies WHERE index_version = '"
                    + IndicesInfo.OPEN_SEARCH_VERSION + "' ");
            @SuppressWarnings("unchecked") final List<Map<String, Object>> results = dc.loadResults();
            if(results.isEmpty()){
               return Optional.empty();
            }
            for (Map<String, Object> rr : results) {
                String name = (String) rr.get("index_name");
                String type = (String) rr.get("index_type");
                if (type.equalsIgnoreCase(IndexType.WORKING.toString())) {
                    builder.working(name);
                } else if (type.equalsIgnoreCase(IndexType.LIVE.toString())) {
                    builder.live(name);
                } else if (type.equalsIgnoreCase(IndexType.REINDEX_LIVE.toString())) {
                    builder.reindexLive(name);
                } else if (type.equalsIgnoreCase(IndexType.REINDEX_WORKING.toString())) {
                    builder.reindexWorking(name);
                } else if (type.equalsIgnoreCase(IndexType.SITE_SEARCH.toString())) {
                    builder.siteSearch(name);
                }
            }
            info = builder.build();
        }
        return Optional.of(info);
    }

    public void point(final IndicesInfo newInfo) throws DotDataException {
      Connection conn = null;

      try {
        conn = DbConnectionFactory.getDataSource().getConnection();
        conn.setAutoCommit(false);

        if(newInfo==null || newInfo.equals(loadLegacyIndices(conn))) {
            return;
        }
        DotConnect dc = new DotConnect();
        final String insertSQL = "INSERT INTO indicies VALUES(?,?,?)";
        final String deleteSQL = "DELETE from indicies where index_type=? or index_name=?";
        for (IndexType type : IndexType.values()) {
            final String indexType = type.toString().toLowerCase();
            final String newValue = Try.of(() -> (String) PropertyUtils
                    .getProperty(newInfo, type.getPropertyName())).getOrNull();

            dc.setSQL(deleteSQL).addParam(indexType).addParam(newValue).loadResult(conn);
            if (newValue != null) {
                dc.setSQL(insertSQL).addParam(newValue).addParam(indexType).addParam(newInfo.version()).loadResult(conn);
            }

        }
        conn.commit();
        cache.clearCache();
        CacheLocator.getESQueryCache().clearCache();
        
      } catch (Exception e) {
        if (conn != null) {
          try {
            conn.rollback();
          } catch (SQLException e1) {
            Logger.debug(this.getClass(), e1.getMessage(), e1);
          }
        }
        throw new DotRuntimeException(e);
      } finally {
        if (conn != null) {
          try {
            conn.close();
          } catch (Exception ex) {
            Logger.debug(this.getClass(), ex.getMessage(), ex);
          }
        }
      }

    }

}
