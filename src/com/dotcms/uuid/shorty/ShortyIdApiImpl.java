package com.dotcms.uuid.shorty;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.util.Logger;

public class ShortyIdApiImpl implements ShortyIdApi {

    long getDbHits(){
        return dbHits;
    }
    long dbHits=0;
    
    @Override
    public ShortyId noShorty(String shorty){ 
        return  new ShortyId(shorty, ShortType.CACHE_MISS.toString(), ShortType.CACHE_MISS, ShortType.CACHE_MISS);
    }
    
    @Override
    public Optional<ShortyId> getShorty(final String shorty) {

        try{
            validShorty(shorty);
        }
        catch(ShortyException se){
            return Optional.empty();
        }
        
        
        ShortyId shortyId = null;
        Optional<ShortyId> opt = new ShortyIdCache().get(shorty);
        if (opt.isPresent()) {
            shortyId = opt.get();
        } else {
            shortyId = viaDb(shorty);
            new ShortyIdCache().add(shortyId);
        }
        return shortyId.type == ShortType.CACHE_MISS ? Optional.empty() : Optional.of(shortyId);
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
    
    
    ShortyId viaDb(final String shorty) {

        this.dbHits++;
        ShortyId shortyId =noShorty(shorty);

        
        DotConnect db = new DotConnect();
        db.setSQL(ShortyIdSql.SELECT_SHORTY_SQL);
        db.addParam(shorty + "%");
        db.addParam(shorty + "%");
        
        try {
            List<Map<String, Object>> results = db.loadObjectResults();
            if (results.size() > 0) {
                String id = (String) results.get(0).get("id");
                String type = (String) results.get(0).get("type");
                String subType = (String) results.get(0).get("subtype");

                shortyId = new ShortyId(shorty, id, ShortType.fromString(type),
                        ShortType.fromString(subType));
            }
        } catch (Exception e) {
            Logger.warn(this.getClass(), e.getMessage());
        }

        return shortyId;
    }
    
     void validShorty(final String test){
        if (test == null) {
            throw new ShortyException(
                    "this is not a short id.  Short Ids should be 8 chars in length");
        }

        for(char c : test.toCharArray()){
            if (!(
                    (c >= 'a' && c <= 'z') ||
                    (c >= 'A' && c <= 'Z') ||
                    (c >= '0' && c <= '9') ||
                    (c=='-')
            )){
                throw new ShortyException(
                        "this is not an alpha numeric id.  Short Ids should be 8 alpha/numeric chars in length");
            }
            
        }

    }
}
