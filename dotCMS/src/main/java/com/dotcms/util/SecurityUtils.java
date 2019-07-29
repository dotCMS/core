package com.dotcms.util;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.liferay.util.Xss;

import io.vavr.control.Try;

/**
 * This class exposes utility routines related to security aspects of dotCMS that can be used by
 * several pieces of the system.
 * 
 * @author Jorge Urdaneta
 * @version 2.5.4, 3.7
 * @since Feb 24, 2014
 *
 */
public class SecurityUtils {

  /**
   * Contains the different delay strategies that can be used to halt the normal flow of a request or
   * thread:
   * <ul>
   * <li>{@code POW}: (Default strategy) Causes the thread to sleep for the <b>seconds</b> specified
   * by raising the seed value to the power of 2.</li>
   * <li>{@code TIME_MIN}: Causes the thread to sleep for the <b>minutes</b> specified in the seed
   * value.</li>
   * <li>{@code TIME_SEC}: Causes the thread to sleep for the <b>seconds</b> specified in the seed
   * value.</li>
   * <li>{@code TIME_MILLS}: Causes the thread to sleep for the <b>milliseconds</b> specified in the
   * seed value.</li>
   * </ul>
   * 
   * @author Jose Castro
   * @version 3.7
   * @since Aug 11, 2016
   *
   */
  public enum DelayStrategy {
    POW, TIME_MILLS, TIME_SEC, TIME_MIN
  }

  /**
   * 
   * @param request
   * @param referer
   * @return
   * @throws IllegalArgumentException
   */
  public static String stripReferer(HttpServletRequest request, String referer) throws IllegalArgumentException {

    if (referer == null)
      return referer;

    String ref = referer;

    ref = Xss.strip(ref);

    if (ref.contains("%0d") || ref.contains("%0a"))
      ref = "/";

    return ref;
  }

  /**
   * This is a useful mechanism for dealing with DoS and similar security attacks in which it might be
   * adequate to temporarily pause a request for a specific time before letting it continue. This is
   * particularly useful to deal with multiple failed login attempts, in which the attacker can be
   * penalized for it. Different security strategies can be added in order to counter-attack these
   * threats.
   * <p>
   * By default, the approach consists of taking a seed value and raising it to the power of 2. The
   * result will represent the seconds that a given request will have to wait before moving on. For
   * example, during a failed authentication, the {@code seed} can be the number of failed login
   * attempts. This way, the more times hackers fail to authenticate, the more time they will have to
   * wait to try it again.
   * 
   * @param seed - The number of times that a user has tried to log in and failed.
   * @param delayStrategy - The delay strategy used after a failed login.
   */
  public static void delayRequest(long seed, final DelayStrategy delayStrategy) {
    seed = Math.abs(seed);
    if (delayStrategy.equals(DelayStrategy.TIME_MIN)) {
      try {

        Logger.debug(SecurityUtils.class, "Sleeping " + seed + " minutes");

        TimeUnit.MINUTES.sleep(seed);
      } catch (NumberFormatException e) {
        // Invalid number, defaults to no thread sleep
      } catch (InterruptedException e) {
        // Sleep was interrupted, just ignore it
      }
    } else if (delayStrategy.equals(DelayStrategy.TIME_SEC)) {
      try {

        Logger.debug(SecurityUtils.class, "Sleeping " + seed + " seconds");

        TimeUnit.SECONDS.sleep(seed);
      } catch (NumberFormatException e) {
        // Invalid number, defaults to no thread sleep
      } catch (InterruptedException e) {
        // Sleep was interrupted, just ignore it
      }
    } else if (delayStrategy.equals(DelayStrategy.TIME_MILLS)) {
      try {

        Logger.debug(SecurityUtils.class, "Sleeping " + seed + " milliseconds");

        TimeUnit.MILLISECONDS.sleep(seed);
      } catch (NumberFormatException e) {
        // Invalid number, defaults to no thread sleep
      } catch (InterruptedException e) {
        // Sleep was interrupted, just ignore it
      }
    } else {
      // Default strategy: DelayStrategy.POW
      if (seed > 0) {
        final long sleepTime = (long) Math.pow(seed, 2);
        try {

          Logger.debug(SecurityUtils.class, "Sleeping " + sleepTime + " seconds");
          TimeUnit.SECONDS.sleep(sleepTime);
        } catch (InterruptedException e) {
          // Sleep was interrupted, just ignore it
        }
      }
    }

    Logger.debug(SecurityUtils.class, "Leaving the delayRequest");
  }

  /**
   * This method takes a request and makes sure that it is coming the same origin as
   * the referer or origin headers, e.g. a known source. It is
   * intended to help mitigate agsinst XSS and CSRF attacks
   * 
   * @param request
   * @return
   */
  public boolean validateReferer(HttpServletRequest request) {
    
    final String uri = request.getRequestURI() ==null ? "/" : request.getRequestURI().toLowerCase();
    final String url = request.getServerName() + uri;
    final String urlHost = hostFromUrl(url);

    final String incomingReferer = (request.getHeader("Origin")!=null) ? request.getHeader("Origin") :  request.getHeader("referer");
    final String refererHost = hostFromUrl(incomingReferer);
    
    // good: we allow CSS because css @import statements do not send referers
    if(refererHost == null && uri.endsWith(".css")) {
      return true;
    }
    
    // good: the url host == the refererHost
    if (urlHost.equalsIgnoreCase(refererHost)) {
        return true;
    }
    
    // good: uri is on the ignore list
    if(this.loadIgnorePaths()
        .stream()
        .anyMatch(
            path->(path.endsWith("*") && uri.startsWith(path.substring(0, path.lastIndexOf('*'))) || 
            uri.equals(path)))) {
      return true;
    }

    
    // good: the referer is a host that is being served from dotCMS
    if(isRefererOneOfOurHosts(refererHost)) {
      return true;
    }

    // good: if the urlHost should be ignored (this should almost never happen)
    if(this.loadIgnoreHosts().contains(urlHost)) {
      return true;
    }

    Try.run(()-> SecurityLogger.logInfo(SecurityUtils.class, "InvalidReferer, ip:" + request.getRemoteAddr() +", url:" + request.getRequestURL() + ", referer:" + incomingReferer));
    Try.run(()-> Logger.info(SecurityUtils.class, "InvalidReferer, ip:" + request.getRemoteAddr() +", url:" + request.getRequestURL() + ", referer:" + incomingReferer));
    return false;
  }
  
  @VisibleForTesting
  protected String getPortalHost() {
    return hostFromUrl(APILocator.getCompanyAPI().getDefaultCompany().getPortalURL());
  }
  
  @VisibleForTesting
  protected boolean resolveHost(String refererHost) {
    Host foundHost = Try.of(()->APILocator.getHostAPI().findByName(refererHost, APILocator.getUserAPI().getSystemUser(), false)).getOrNull();
    if (!UtilMethods.isSet(foundHost)) {
      foundHost = Try.of(()->APILocator.getHostAPI().findByAlias(refererHost, APILocator.getUserAPI().getSystemUser(), false)).getOrNull();
    }
    return UtilMethods.isSet(foundHost);
  }
  
  /**
   * Checks if the referer matches a hostname that is being served by dotCMS. 
   * As a security precaution, this will return false for the default host shipped
   * with dotCMS : demo.dotcms.com
   * @param refererHost
   * @return
   */
  private boolean isRefererOneOfOurHosts(final String refererHost) {
    // disallow links from our open demo
    if(refererHost==null) {
      return false;
    }
    // allow referers 
    if (refererHost.equalsIgnoreCase(hostFromUrl(getPortalHost()))) {
      return true;
    }

    return resolveHost(refererHost) ;
  }
  
  
  /**
   * get the hostname portion of a url string
   * @param url
   * @return
   */
  public String hostFromUrl(final String url) {
    if(url==null) return null;
    return Try.of(() -> {
      return (url.contains("://")) ? new URL(url.trim()).getHost().toLowerCase() : new URL("http://" + url.trim()).getHost().toLowerCase();
    }).getOrNull();
  }
  
  
  private static List<String> IGNORE_REFERER_FOR_HOSTS = null;

  private static List<String> IGNORE_REFERER_FOR_PATHS = null;
  /**
   * Load Ignore paths
   * @return
   */
  @VisibleForTesting
  protected List<String> loadIgnorePaths() {
    if (IGNORE_REFERER_FOR_PATHS == null) {
      List<String> ignorePathsStartingWith = new ArrayList<>();
      String[] paths = Config.getStringArrayProperty("IGNORE_REFERER_FOR_PATHS", new String[0] );
      for (String path : paths) {
        if (UtilMethods.isSet(path)) {
          ignorePathsStartingWith.add(path.toLowerCase().trim());
        }
      }
      IGNORE_REFERER_FOR_PATHS = ImmutableList.copyOf(ignorePathsStartingWith);
    }
    return IGNORE_REFERER_FOR_PATHS;
  }
  
  /**
   * Load Ignore hosts
   * @return
   */
  @VisibleForTesting
  protected List<String> loadIgnoreHosts() {
    if (IGNORE_REFERER_FOR_HOSTS == null) {
      List<String> ignoreHostsStartingWith = new ArrayList<>();
      String[] paths = Config.getStringArrayProperty("IGNORE_REFERER_FOR_HOSTS", new String[0] );
      for (String path : paths) {
        if (UtilMethods.isSet(path)) {
          ignoreHostsStartingWith.add(path.toLowerCase().trim());
        }
      }
      IGNORE_REFERER_FOR_HOSTS = ImmutableList.copyOf(ignoreHostsStartingWith);
    }
    return IGNORE_REFERER_FOR_HOSTS;
  }
  
}
