package com.dotcms.content.elasticsearch.util;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.content.elasticsearch.business.ESContentletIndexAPI;
import com.dotcms.content.elasticsearch.business.IndiciesAPI.IndiciesInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import java.io.Serializable;
import java.sql.Connection;
import java.util.Hashtable;
import java.util.Map;

public class ESReindexationProcessStatus implements Serializable {
  private static final ESContentletIndexAPI indexAPI = new ESContentletIndexAPI();

  public static synchronized boolean inFullReindexation() throws DotDataException {
    return inFullReindexation(DbConnectionFactory.getConnection());
  }

  public static synchronized boolean inFullReindexation(Connection conn) throws DotDataException {
    return indexAPI.isInFullReindex(conn);
  }

  @CloseDBIfOpened
  public static synchronized int getContentCountToIndex() throws DotDataException {

    DotConnect dc = new DotConnect();
    dc.setSQL("select count(*) as cc from contentlet_version_info");
    return Integer.parseInt(dc.loadObjectResults().get(0).get("cc").toString());
  }

  @CloseDBIfOpened
  public static synchronized int getLastIndexationProgress() throws DotDataException {
    return getLastIndexationProgress(getContentCountToIndex());
  }

  public static synchronized int getLastIndexationProgress(int countToIndex)
      throws DotDataException {
    long left = APILocator.getDistributedJournalAPI().recordsLeftToIndexForServer();
    int x = (int) (countToIndex - left);

    return (x < 0) ? 0 : x;
  }

  public static synchronized String currentIndexPath() throws DotDataException {
    IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();
    return "[" + info.working + "," + info.live + "]";
  }

  public static synchronized String getNewIndexPath() throws DotDataException {
    IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();
    return "[" + info.reindex_working + "," + info.reindex_live + "]";
  }

  @CloseDBIfOpened
  public static synchronized Map getProcessIndexationMap() throws DotDataException {
    Map<String, Object> theMap = new Hashtable<String, Object>();

    theMap.put("inFullReindexation", inFullReindexation());
    // no reason to hit db if not needed
    if (inFullReindexation()) {
      final int countToIndex = getContentCountToIndex();
      theMap.put("contentCountToIndex", countToIndex);
      theMap.put("lastIndexationProgress", getLastIndexationProgress(countToIndex));
      theMap.put("currentIndexPath", currentIndexPath());
      theMap.put("newIndexPath", getNewIndexPath());
    }
    return theMap;
  }
}
