package com.dotcms.content.elasticsearch.business;

import com.dotmarketing.exception.DotDataException;

import java.io.Serializable;
import java.sql.Connection;

/**
 * An API to store and retrieve information about current Elastic Search Indicies
 * 
 * @author Jorge Urdaneta
 */
public interface IndiciesAPI {
    public static class IndiciesInfo implements Serializable {
        public String live, working, reindex_live, reindex_working, site_search;

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((live == null) ? 0 : live.hashCode());
            result = prime * result + ((reindex_live == null) ? 0 : reindex_live.hashCode());
            result = prime * result + ((reindex_working == null) ? 0 : reindex_working.hashCode());
            result = prime * result + ((site_search == null) ? 0 : site_search.hashCode());
            result = prime * result + ((working == null) ? 0 : working.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            IndiciesInfo other = (IndiciesInfo) obj;
            if (live == null) {
                if (other.live != null)
                    return false;
            } else if (!live.equals(other.live))
                return false;
            if (reindex_live == null) {
                if (other.reindex_live != null)
                    return false;
            } else if (!reindex_live.equals(other.reindex_live))
                return false;
            if (reindex_working == null) {
                if (other.reindex_working != null)
                    return false;
            } else if (!reindex_working.equals(other.reindex_working))
                return false;
            if (site_search == null) {
                if (other.site_search != null)
                    return false;
            } else if (!site_search.equals(other.site_search))
                return false;
            if (working == null) {
                if (other.working != null)
                    return false;
            } else if (!working.equals(other.working))
                return false;
            return true;
        }
        
        
        
    }

    /**
     * Returns IndiciesInfo instance with index names stored.
     * 
     * @return IndiciesInfo instance
     */
    public IndiciesInfo loadIndicies() throws DotDataException;

    public IndiciesInfo loadIndicies(Connection conn) throws DotDataException;

    /**
     * Updates the información about ES indicies.
     * 
     * @param info
     */
    public void point(IndiciesInfo newInfo) throws DotDataException;

}
