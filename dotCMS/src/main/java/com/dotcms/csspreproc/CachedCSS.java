package com.dotcms.csspreproc;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class CachedCSS implements Serializable {
    private static final long serialVersionUID = 9133484979864956939L;
    
    public static class ImportedAsset implements Serializable {
        private static final long serialVersionUID = -3564108514822292137L;
        public String uri;
        public Date modDate;
    }
    
    public String uri;
    public String hostId;
    public byte[] data;
    public Date modDate;
    public List<ImportedAsset> imported;
    public boolean live;
    
    /**
     * Calculates max date between asset modDate and imported asset modDates.
     * Useful for building expiration headers, etag, modified-since, etc
     * @return
     */
    public Date getMaxDate() {
        Date max = modDate;
        for(ImportedAsset asset : imported) {
            max = max.before(asset.modDate) ? asset.modDate : max;
        }
        return max;
    }
}
