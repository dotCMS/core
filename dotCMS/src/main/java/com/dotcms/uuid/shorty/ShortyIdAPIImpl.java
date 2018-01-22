package com.dotcms.uuid.shorty;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UUIDUtil;

public class ShortyIdAPIImpl implements ShortyIdAPI {

  public long getDbHits() {
    return dbHits;
  }

  long dbHits = 0;
  public static final int MINIMUM_SHORTY_ID_LENGTH =
      Config.getIntProperty("MINIMUM_SHORTY_ID_LENGTH", 10);

  @Override
  public Optional<ShortyId> getShorty(final String shortStr) {
    try {
      validShorty(shortStr);
      ShortyId shortyId = null;
      Optional<ShortyId> opt = new ShortyIdCache().get(shortStr);
      if (opt.isPresent()) {
        shortyId = opt.get();
      } else if (shortStr.length() == 36) {
        shortyId = viaDbEquals(shortStr);
        new ShortyIdCache().add(shortyId);
      } else {
        shortyId = viaDbLike(shortStr);
        new ShortyIdCache().add(shortyId);
      }
      return shortyId.type == ShortType.CACHE_MISS ? Optional.empty() : Optional.of(shortyId);
    } catch (ShortyException se) {

      Logger.warn(this.getClass(), se.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public ShortyId noShorty(String shorty) {
    return new ShortyId(shorty, ShortType.CACHE_MISS.toString(), ShortType.CACHE_MISS,
        ShortType.CACHE_MISS);
  }
  
  @Override
  public String randomShorty() {
      return shortify(UUIDGenerator.generateUuid());
  }
  
  @Override
  public String shortify(final String shortStr) {
    try {
      validShorty(shortStr);
      return shortStr.replaceAll("-", "").substring(0, MINIMUM_SHORTY_ID_LENGTH);
    } catch (ShortyException se) {
      return null;
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

  String unUidIfy(String shorty) {
    return UUIDUtil.unUidIfy(shorty);
  }

  public String uuidIfy(String shorty) {
    return UUIDUtil.uuidIfy(shorty);
  }

  @CloseDBIfOpened
  private ShortyId viaDbEquals(final String shorty) {
    this.dbHits++;
    DotConnect db = new DotConnect();
    db.setSQL(ShortyIdSql.SELECT_SHORTY_SQL_EQUALS);
    db.addParam(shorty);
    db.addParam(shorty);
    try {
      return transformMap(shorty, db.loadObjectResults());
    } catch (DotDataException e) {
      Logger.warn(this.getClass(), "db exception:" + e.getMessage());
      return noShorty(shorty);
    }
  }


  @CloseDBIfOpened
  private ShortyId viaDbLike(final String shorty) {
    this.dbHits++;
    DotConnect db = new DotConnect();
    db.setSQL(ShortyIdSql.SELECT_SHORTY_SQL_LIKE);
    String uuid = uuidIfy(shorty);
    db.addParam(uuid + "%");
    db.addParam(uuid + "%");

    try {
      return transformMap(shorty, db.loadObjectResults());
    } catch (DotDataException e) {
      Logger.warn(this.getClass(), "db exception:" + e.getMessage());
      return noShorty(shorty);
    }

  }

  
  
  
  
  
  private ShortyId transformMap(final String shorty, final List<Map<String, Object>> results) {
    if (results == null || results.size() < 1) {
      return noShorty(shorty);
    } else if (results.size() > 1) {
      throw new ShortyException("Shorty ID collision:" + shorty);
    } else {
      String id = (String) results.get(0).get("id");
      String type = (String) results.get(0).get("type");
      String subType = (String) results.get(0).get("subtype");
      return new ShortyId(shorty, id, ShortType.fromString(type), ShortType.fromString(subType));
    }

  }

  public String shortUri(Contentlet c) {

    return null;
  }

  public String shortInodeUri(Contentlet c) {

    return null;
  }

  public void validShorty(final String test) {
    if (test == null || test.length() < MINIMUM_SHORTY_ID_LENGTH || test.length() > 36) {
      throw new ShortyException("shorty " + test + " is not a short id.  Short Ids should be "
          + MINIMUM_SHORTY_ID_LENGTH + " chars in length");
    }

    for (char c : test.toCharArray()) {
      if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
          || c == '-')) {
        throw new ShortyException(
            "shorty " + test + " is not an alpha numeric id.  Short Ids should be "
                + MINIMUM_SHORTY_ID_LENGTH + " alpha/numeric chars in length");
      }
    }
  }
}
