package com.dotcms.uuid.shorty;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

public class ShortyIdAPIImpl implements ShortyIdAPI {

    public long getDbHits() {
        return dbHits;
    }

    long dbHits = 0;
    public static final int MINIMUM_SHORTY_ID_LENGTH = Config.getIntProperty("MINIMUM_SHORTY_ID_LENGTH", 10);
    
    
    @Override
    public ShortyId noShorty(String shorty) {
        return new ShortyId(shorty, ShortType.CACHE_MISS.toString(), ShortType.CACHE_MISS,
                ShortType.CACHE_MISS);
    }

    @Override
    public String shortify(final String shortStr) {
        try{
            validShorty(shortStr);
            return shortStr.replaceAll("-", "").substring(0, MINIMUM_SHORTY_ID_LENGTH);
        } catch (ShortyException se) {
            return null;
        }
    }

    
    
    @Override
    public Optional<ShortyId> getShorty(final String shortStr) {
        try {
            validShorty(shortStr);
            ShortyId shortyId = null;
            Optional<ShortyId> opt = new ShortyIdCache().get(shortStr);
            if (opt.isPresent()) {
                shortyId = opt.get();
            } else {
                shortyId = viaDb(shortStr);
                new ShortyIdCache().add(shortyId);
            }
            return shortyId.type == ShortType.CACHE_MISS ? Optional.empty() : Optional.of(shortyId);
        } catch (ShortyException se) {
            
            Logger.debug(this.getClass(), se.getMessage());
            return Optional.empty();
        }
    }


    /*
     * ShortyId viaIndex(final String shorty) {
     * 
     * 
     * ContentletAPI capi = APILocator.getContentletAPI(); ContentletSearch con = null; ShortyId
     * shortyId = new ShortyId(shorty, "CACHE_MISS", ShortType.CACHE_MISS);
     * 
     * // if we have a shorty, use the index
     * 
     * StringBuilder query = new StringBuilder("+(identifier:").append(shorty).append("* inode:")
     * .append(shorty).append("*) ");
     * 
     * 
     * query.append("+working:true ");
     * 
     * List<ContentletSearch> cons; try { cons = capi.searchIndex(query.toString(), 1, 0, "score",
     * APILocator.getUserAPI().getSystemUser(), false); if (cons.size() > 0) { con = cons.get(0);
     * ShortType type = (con.getIdentifier().startsWith(shorty)) ? ShortType.IDENTIFIER :
     * ShortType.CONTENTLET; String id = (con.getIdentifier().startsWith(shorty)) ?
     * con.getIdentifier() : con.getInode(); shortyId = new ShortyId(shorty, id, type); } } catch
     * (Exception e) { // we should not add to the cache if something went wrong throw new
     * ShortyException("somthing went wrong in the index", e); }
     * 
     * return shortyId; }
     */

    String unUidIfy(String shorty){
        while(shorty.indexOf('-')>-1){
            shorty = shorty.replace("-", "");
        }
        return shorty;
    }


    public String uuidIfy(String shorty) {
        StringBuilder newShorty = new StringBuilder();
        shorty = unUidIfy(shorty);
        char[] chars =shorty.toCharArray();
        for (int i=0;i< chars.length;i++) {
            char c = chars[i];
            if(i==8 || i==12|| i==16 || i==20){
                newShorty.append('-');
            }            
            newShorty.append(c);
        }
        return newShorty.toString();
    }
    
    

    ShortyId viaDb(final String shorty) {

        this.dbHits++;
        ShortyId shortyId = noShorty(shorty);


        DotConnect db = new DotConnect();
        db.setSQL(ShortyIdSql.SELECT_SHORTY_SQL);
        String uuid = uuidIfy(shorty);
        db.addParam(uuid + "%");
        db.addParam(uuid + "%");


        List<Map<String, Object>> results;
        try {
            results = db.loadObjectResults();

            if (results.size() > 1) {
                throw new ShortyException("Shorty ID collision:" + uuid );
            } else if (results.size() == 1) {
                String id = (String) results.get(0).get("id");
                String type = (String) results.get(0).get("type");
                String subType = (String) results.get(0).get("subtype");

                shortyId = new ShortyId(shorty, id, ShortType.fromString(type),
                        ShortType.fromString(subType));
            }
        } catch (DotDataException e) {
            Logger.warn(this.getClass(), "db exception:" + e.getMessage());
        }


        return shortyId;
    }
    
    public String shortUri(Contentlet c){
        
        
        return null;
    }
    
    public String shortInodeUri(Contentlet c){
        
        
        return null;
    }

    public void validShorty(final String test) {
        if (test == null || test.length() < 8 || test.length()>36) {
            throw new ShortyException(
                    "shorty " + test + " is not a short id.  Short Ids should be " + MINIMUM_SHORTY_ID_LENGTH + " chars in length");
        }
        
        for (char c : test.toCharArray()) {
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c=='-')) {
                throw new ShortyException(
                        "shorty " + test + " is not an alpha numeric id.  Short Ids should be " + MINIMUM_SHORTY_ID_LENGTH + " alpha/numeric chars in length");
            }
        }
    }
}
