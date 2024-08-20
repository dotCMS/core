package com.dotcms.visitor.filter.characteristics;


import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import eu.bitwalker.useragentutils.Browser;
import eu.bitwalker.useragentutils.UserAgent;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;

public class IsBotCharacter extends AbstractCharacter {
  
  
  /**
   * <a href="https://github.com/monperrus/crawler-user-agents">User Agent list</a>
   */
  
  private static final String FILE_NAME = "crawler-user-agents.json";

  private static volatile Set<Pattern> patterns = null;

  public final static String IS_BOT="isBot";
  public IsBotCharacter(AbstractCharacter incomingCharacter) {
    super(incomingCharacter);
    
    if(request.getSession(false)!=null && request.getSession().getAttribute(IS_BOT)!=null) {
        accrue(IS_BOT, (Boolean)request.getSession().getAttribute(IS_BOT));
      return;
    }
    if( request.getAttribute(IS_BOT)!=null) {
      accrue(IS_BOT, (Boolean)request.getAttribute(IS_BOT));
      return;
    }
    
    boolean isBot=false;
    final String userAgent = request.getHeader("User-Agent");
    if (userAgent == null) {
      accrue(IS_BOT, true);
      return;
    }

    if(!isBot) {
      UserAgent agent = (UserAgent) getMap().get("agent");
      if(agent!=null) {
        isBot = agent.getBrowser() == Browser.BOT;
      }
    }
    
    if(!isBot) {
      isBot = loadPatterns().parallelStream().anyMatch(p->p.matcher(userAgent).find());
    }
    request.setAttribute(IS_BOT, isBot);
    if(request.getSession(false)!=null) {
      request.getSession().setAttribute(IS_BOT, isBot);
    }
    accrue(IS_BOT, isBot);

  }

  private Set<Pattern> loadPatterns() {
    if (IsBotCharacter.patterns == null) {
      synchronized (this.getClass()) {
        if (IsBotCharacter.patterns == null) {
          final Set<Pattern> patterns = new HashSet<Pattern>();
          try (InputStream in = IsBotCharacter.class.getResourceAsStream("/" + FILE_NAME)) {
            String bots = IOUtils.toString(in, "UTF8");
            JSONArray jarr = new JSONArray(bots);
            jarr.iterator().forEachRemaining(jo -> {
              String patternStr = ((JSONObject) jo).optString("pattern", null);
              if (patternStr != null) {
                patterns.add(Pattern.compile(patternStr));
              }
            });
          } catch (Exception e) {
            Logger.warn(this.getClass(),"Error loading bot patterns from file:" + FILE_NAME, e);
          }
          IsBotCharacter.patterns = Set.copyOf(patterns);
        }
      }
    }
    return IsBotCharacter.patterns;

  }

}
