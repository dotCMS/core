package com.dotcms.uuid.shorty;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UUIDUtil;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation class for the {@link ShortyIdAPI}.
 *
 * @author Will Ezell
 * @since Sep 20, 2016
 */
public class ShortyIdAPIImpl implements ShortyIdAPI {

  public long getDbHits() {
    return dbHits;
  }

  private final Map<ShortyInputType, DBEqualsStrategy> dbEqualsStrategyMap =
          Map.of(
                  ShortyInputType.CONTENT,         (final DotConnect db, final String shorty) ->  db.setSQL(ShortyIdSql.SELECT_SHORTY_SQL_EQUALS).addParam(shorty).addParam(shorty),
                  ShortyInputType.WORKFLOW_SCHEME, (final DotConnect db, final String shorty) ->  db.setSQL(ShortyIdSql.SELECT_WF_SCHEME_SHORTY_SQL_EQUALS).addParam(shorty),
                  ShortyInputType.WORKFLOW_STEP,   (final DotConnect db, final String shorty) ->  db.setSQL(ShortyIdSql.SELECT_WF_STEP_SHORTY_SQL_EQUALS).addParam(shorty),
                  ShortyInputType.WORKFLOW_ACTION, (final DotConnect db, final String shorty) ->  db.setSQL(ShortyIdSql.SELECT_WF_ACTION_SHORTY_SQL_EQUALS).addParam(shorty)
             );

  private final Map<ShortyInputType, DBLikeStrategy> dbLikeStrategyMap =
          Map.of(
                  ShortyInputType.CONTENT,         (final DotConnect db, final String uuidIfy) -> {
                    final String sqlUnion = "(" + ShortyIdSql.SELECT_SHORTY_SQL_LIKE + " UNION ALL " + ShortyIdSql.SELECT_SHORTY_SQL_LIKE + ")" + " limit 1";
                    final String deterministicId = uuidIfy.replaceAll("-","");
                    db.setSQL(sqlUnion).addParam(uuidIfy + "%").addParam(uuidIfy + "%").addParam(deterministicId + "%").addParam(deterministicId + "%");
                  },
                  ShortyInputType.WORKFLOW_SCHEME, (final DotConnect db, final String uuidIfy) -> db.setSQL(ShortyIdSql.SELECT_WF_SCHEME_SHORTY_SQL_LIKE).addParam(uuidIfy + "%"),
                  ShortyInputType.WORKFLOW_STEP,   (final DotConnect db, final String uuidIfy) -> db.setSQL(ShortyIdSql.SELECT_WF_STEP_SHORTY_SQL_LIKE).addParam(uuidIfy + "%"),
                  ShortyInputType.WORKFLOW_ACTION, (final DotConnect db, final String uuidIfy) -> db.setSQL(ShortyIdSql.SELECT_WF_ACTION_SHORTY_SQL_LIKE).addParam(uuidIfy + "%")
          );

  long dbHits = 0;
  public static int MINIMUM_SHORTY_ID_LENGTH =
      Config.getIntProperty("MINIMUM_SHORTY_ID_LENGTH", 10);

  @Override
  public Optional<ShortyId> getShorty(final String shortStr) {

    return getShorty(shortStr, ShortyInputType.CONTENT);
  }

  @Override
  public Optional<ShortyId> getShorty(final String shortStr, final ShortyInputType shortyType) {


    try {
      if(shortStr.startsWith(TempFileAPI.TEMP_RESOURCE_PREFIX) && APILocator.getTempFileAPI().isTempResource(shortStr)) {
        return Optional.of(new ShortyId(shortStr, shortStr, ShortType.TEMP_FILE, ShortType.TEMP_FILE));
      }

      validShorty(shortStr);
      ShortyId shortyId = null;
      final Optional<ShortyId> opt = new ShortyIdCache().get(shortStr);
      if (opt.isPresent()) {
        shortyId = opt.get();
      } else if (shortStr.length() == 32 || shortStr.length() == 36 ) {
        shortyId = viaDbEquals(shortStr, shortyType);
        new ShortyIdCache().add(shortyId);
      } else {
        shortyId = viaDbLike(shortStr, shortyType);
        new ShortyIdCache().add(shortyId);
      }
      return shortyId.type == ShortType.CACHE_MISS ? Optional.empty() : Optional.of(shortyId);
    } catch (ShortyException se) {

      Logger.warn(this.getClass(), String.format("An error occurred when getting shorty value for '%s' of type " +
              "'%s': %s", shortStr, shortyType, se.getMessage()));
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
      
      return StringUtils.shortify(shortStr, MINIMUM_SHORTY_ID_LENGTH);
      
  }

  public String uuidIfy(String shorty) {
    return UUIDUtil.uuidIfy(shorty);
  }

  @FunctionalInterface
  interface DBEqualsStrategy {
    // applies the equals strategy
    void apply (final DotConnect dotConnect, final String shorty);
  }

  @CloseDBIfOpened
  protected ShortyId viaDbEquals(final String shorty, final ShortyInputType shortyType) {
    this.dbHits++;
    final DotConnect db = new DotConnect();
    this.dbEqualsStrategyMap.get(shortyType).apply(db, shorty);
    try {
      return transformMap(shorty, db.loadObjectResults());
    } catch (DotDataException e) {
      Logger.warn(this.getClass(), "db exception:" + e.getMessage());
      throw new ShortyException("Unable to query for shorty:" + shorty, e);
    }
  }

  @FunctionalInterface
  interface DBLikeStrategy {
    // applies the equals strategy
    void apply (final DotConnect dotConnect, final String uuidIfy);
  }

  @CloseDBIfOpened
  protected ShortyId viaDbLike(final String shorty, final ShortyInputType shortyType) {
    this.dbHits++;
    final DotConnect db = new DotConnect();
    final String uuid = uuidIfy(shorty);
    this.dbLikeStrategyMap.get(shortyType).apply(db, uuid);
    try {
      return transformMap(shorty, db.loadObjectResults());
    } catch (DotDataException e) {
      Logger.warn(this.getClass(), "db exception:" + e.getMessage());
      throw new ShortyException("Unable to query for shorty:" + shorty, e);
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

  @Override
  public void validShorty(final String incoming) {

    final String test = shortify(incoming);

    if (test == null || test.length() < MINIMUM_SHORTY_ID_LENGTH || test.length() > 36) {

      throw new ShortyException("shorty " + test + " is not a short id.  Short Ids should be "
          + MINIMUM_SHORTY_ID_LENGTH + " chars in length");
    }

    for (final char character : test.toCharArray()) {

      if (!((character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z') || (character >= '0' && character <= '9')
          || character == '-')) {

        throw new ShortyException(
            "shorty " + test + " is not an alpha numeric id.  Short Ids should be "
                + MINIMUM_SHORTY_ID_LENGTH + " alpha/numeric chars in length");
      }
    }
  } // validShorty.

}
