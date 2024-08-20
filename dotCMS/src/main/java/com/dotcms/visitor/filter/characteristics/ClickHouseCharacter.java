package com.dotcms.visitor.filter.characteristics;

import com.dotcms.clickhouse.rules.DotScore;
import com.dotcms.clickhouse.util.ULID;
import com.dotcms.visitor.domain.Visitor.AccruedTag;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import eu.bitwalker.useragentutils.UserAgent;
import io.vavr.control.Try;
import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class takes all the information collected by the other Characters and 
 * stick it into a map - mapOut - that will be logged as a json object for injestion
 * by clickhouse
 * @author will
 *
 */
public class ClickHouseCharacter extends AbstractCharacter {

  private ULID ulid = new ULID();

  final Map<String, Serializable> mapOut = new HashMap<>();
  
  
  
  public ClickHouseCharacter(AbstractCharacter incomingCharacter) {
    super(incomingCharacter);
    mapOut.clear();

    // add a unique sortable id
    mapOut.put("id", ulid.nextULID());
    String hostId = (String) getMap().get("hostId");
    final int isBot = Try.of(() -> ((Boolean) getMap().get(IsBotCharacter.IS_BOT)) ? 1 : 0).getOrElse(0);

    try {
      hostId = hostId != null ? hostId : WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request).getIdentifier();
    } catch (Throwable t) {
      Logger.debug(this.getClass().getName(), "only here so tests work", t);
    }
    
    final String url = getMap().get("url") != null ? String.valueOf(getMap().get("url"))
        : request.getRequestURL() + (request.getQueryString() != null ? "?" + request.getQueryString() : "");

    mapOut.put("host_id", hostId);
    mapOut.put("ts", Instant.now().atOffset(ZoneOffset.UTC).toInstant().getEpochSecond());
    
    // only calculate score if the request is not a bot
    if (isBot == 0) {
      mapOut.put("dot_score", new DotScore(request).value);
    }
    
    mapOut.put("url", url);
    mapOut.put("status", getMap().get("status"));
    mapOut.put("method", request.getMethod());
    mapOut.put("request_ms", getMap().get("ms"));
    mapOut.put("cluster_id", getMap().get("cluster"));
    mapOut.put("server_id", getMap().get("server"));
    mapOut.put("session_id", getMap().get("session"));
    mapOut.put("session_new", Try.of(() -> ((Boolean) getMap().get("sessionNew")) ? 1 : 0).getOrElse(1));
    mapOut.put("referer", getMap().get("referer"));
    mapOut.put("mime", getMap().get("mime"));
    mapOut.put("asset_id", UtilMethods.isSet(getMap().get("assetId")) ? getMap().get("assetId") : null);
    mapOut.put("robot", isBot);

    mapOut.put("lang", getMap().get("lang"));
    mapOut.put("i_am", getMap().get("iAm"));
    mapOut.put("dmid", getMap().get("dmid"));

    if (getMap().get("contentId") != null) {
      mapOut.put("content_id", getMap().get("contentId"));
      mapOut.put("content_type",
          Try.of(() -> APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage((String) getMap().get("contentId"))
              .getContentType().variable().toLowerCase()).getOrNull());
    }

    mapOut.put("persona", getMap().get("persona"));
    mapOut.put("vanity_url", getMap().get("vanityUrl"));
    mapOut.put("ip_addr", getMap().get("ip"));

    UserAgent agent = (UserAgent) getMap().get("agent");
    if (agent != null) {
      mapOut.put("agent_os", agent.getOperatingSystem());
      mapOut.put("agent_browser", agent.getBrowser());
      mapOut.put("device", agent.getOperatingSystem().getDeviceType());
    }
    mapOut.put("pages_viewed", getMap().get("pagesViewed"));
    mapOut.put("content_id", UtilMethods.isSet(getMap().get("contentId")) ? getMap().get("contentId") : null);
    mapOut.put("user_id", getMap().get("userId"));
    mapOut.put("query_string", getMap().get("queryString"));

    final ArrayList<String> keys = new ArrayList<>();
    final ArrayList<Serializable> values = new ArrayList<>();

    if (getMap().get("weightedTags") != null) {
      ((List<AccruedTag>) getMap().get("weightedTags")).stream().forEach(e -> {
        keys.add(e.getTag());
        values.add(e.getCount());
      });
      mapOut.put("tags.t_name", ImmutableList.copyOf(keys));
      mapOut.put("tags.t_weight", ImmutableList.copyOf(values));
      keys.clear();
      values.clear();
    }

    //headers
    getMap().entrySet().stream().filter(e -> e.getKey().startsWith("h.") && e.getValue()!=null).forEach(e -> {
      keys.add(e.getKey().substring(2, e.getKey().length()));
      values.add(e.getValue());
    });
    mapOut.put("headers.h_name", ImmutableList.copyOf(keys));
    mapOut.put("headers.h_value", ImmutableList.copyOf(values));
    keys.clear();
    values.clear();

    //cookies
    getMap().entrySet().stream().filter(e -> e.getKey().startsWith("c.") && e.getValue()!=null).forEach(e -> {
      keys.add(e.getKey().substring(2, e.getKey().length()));
      values.add(e.getValue());
    });
    mapOut.put("cookies.c_name", ImmutableList.copyOf(keys));
    mapOut.put("cookies.c_value", ImmutableList.copyOf(values));
    keys.clear();
    values.clear();

    //parameters
    getMap().entrySet().stream().filter(e -> e.getKey().startsWith("p.") && e.getValue()!=null).forEach(e -> {
      keys.add(e.getKey().substring(2, e.getKey().length()));
      values.add(e.getValue());
    });
    mapOut.put("params.p_name", ImmutableList.copyOf(keys));
    mapOut.put("params.p_value", ImmutableList.copyOf(values));
    keys.clear();
    values.clear();

    mapOut.put("rules_request", Try.<String[]>of(() -> ((String) getMap().get("rulesRequest")).split(", ")).getOrNull());
    mapOut.put("rules_session", Try.<String[]>of(() -> ((String) getMap().get("rulesSession")).split(", ")).getOrNull());


    clearMap();
    getMap().putAll(mapOut);
  }


}
