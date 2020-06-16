package com.dotcms.vanityurl.model;

import static com.liferay.util.StringUtil.GROUP_REPLACEMENT_PREFIX;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletResponse;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.vanityurl.util.VanityUrlUtil;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;

/**
 * This class construct a reduced version of the {@link VanityUrl}
 * object to be saved on cache
 *
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 22, 2017
 */
public class CachedVanityUrl implements Serializable {

    static final long serialVersionUID = 1L;
    final public Pattern pattern;
    final public String vanityUrlId;
    final public String url;
    final public String siteId;
    final public long languageId;
    final public String forwardTo;
    final public int response;
    final public int order;

    /**
     * Generate a cached Vanity URL object
     *
     * @param vanityUrl The vanityurl Url to cache
     */

    public CachedVanityUrl(final VanityUrl vanityUrl) {
        this(vanityUrl.getIdentifier(),vanityUrl.getURI(),vanityUrl.getLanguageId(),vanityUrl.getSite(),vanityUrl.getForwardTo(),vanityUrl.getAction());
    }

    public CachedVanityUrl(final String vanityUrlId, final String url, final long languageId, final String siteId, final String forwardTo, final int response) {
        final String regex = normalize(url);
        this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        this.vanityUrlId = vanityUrlId;
        this.url = url;
        this.languageId = languageId;
        this.siteId = siteId;
        this.forwardTo = forwardTo;
        this.response = response;
        this.order    = 0;
    }
    
    /**
     * rewrites the vanity with the matching groups if needed, returns
     * the rewritten url, parameters from the request
     * @param urlIn
     * @return
     */
    final Tuple2<String, String> processForward(final String urlIn) {
      String newForward = this.forwardTo;
      String queryString = null;
      if(pattern!=null) {
        Matcher matcher = pattern.matcher(urlIn);
        if (matcher.matches() && forwardTo.indexOf(GROUP_REPLACEMENT_PREFIX)>-1) {
          for(int i=1;i<=matcher.groupCount();i++) {
            newForward=newForward.replace("$"+i, matcher.group(i));
          }
        }
      }
      if (UtilMethods.isSet(newForward) && newForward.contains("?")) {
          String[] arr = newForward.split("\\?", 2);
          newForward = arr[0];
          if (arr.length > 1) {
              queryString = arr[1];
          }
      }
      return Tuple.of(newForward, queryString);
    }
    
    

    /**
     * This comes as fix for https://github.com/dotCMS/core/issues/16433
     * If the uri ends with forward slash `/`
     * This method will make that piece optional.
     * @param uri the uri stored in the contentlet.
     * @return the uri regexp with the optional forward slash support added.
     */
    private String addOptionalForwardSlashSupport(final String uri){
        if(uri.endsWith(StringPool.FORWARD_SLASH)){
            String regex = uri;
            regex = regex.substring(0, regex.length() -1 );
            return regex + "(/)*";
        }
        return uri;
    }

    /**
     * This takes the uir that was originally stored in the contentlet adds validates it.
     * @param uri the uri stored in the contentlet.
     * @return normalized uri.
     */
    private String normalize(final String uri){
        final String uriRegEx = addOptionalForwardSlashSupport(uri);
        return VanityUrlUtil.isValidRegex(uriRegEx) ? uriRegEx : StringPool.BLANK;
    }
    
    
    public VanityUrlResult handle(final String uriIn,
                    final HttpServletResponse response) {
        
        final Tuple2<String,String> rewritten = processForward(uriIn);
        final String rewrite = rewritten._1;
        final String queryString = rewritten._2;


        // if the vanity is a redirect
        if (this.response==301 || this.response==302 ) {
            response.setStatus(this.response);
            response.setHeader("Location", rewrite);
            return new VanityUrlResult(rewrite, queryString, true);
        }
        
        // if the vanity is a proxy request
        if (this.response==200 && UtilMethods.isSet(rewrite) && rewrite.contains("//")) {
            Try.run(()-> new CircuitBreakerUrl(rewrite + queryString!=null ? "?" + queryString : "").doOut(response)).onFailure(e->{throw new DotRuntimeException(e);});
            return new VanityUrlResult(rewrite, queryString, true);
        }

        return new VanityUrlResult(rewrite, queryString, false);
        
        
    }
    
    


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((forwardTo == null) ? 0 : forwardTo.hashCode());
        result = prime * result + (int) (languageId ^ (languageId >>> 32));
        result = prime * result + response;
        result = prime * result + ((siteId == null) ? 0 : siteId.hashCode());
        result = prime * result + ((url == null) ? 0 : url.hashCode());
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
        CachedVanityUrl other = (CachedVanityUrl) obj;
        if (forwardTo == null) {
            if (other.forwardTo != null)
                return false;
        } else if (!forwardTo.equals(other.forwardTo))
            return false;
        if (languageId != other.languageId)
            return false;
        if (response != other.response)
            return false;
        if (siteId == null) {
            if (other.siteId != null)
                return false;
        } else if (!siteId.equals(other.siteId))
            return false;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        return true;
    }



    @Override
    public String toString() {
        return "CachedVanityUrl{" +
                "pattern=" + pattern +
                ", vanityUrlId='" + vanityUrlId + '\'' +
                ", url='" + url + '\'' +
                ", siteId='" + siteId + '\'' +
                ", languageId=" + languageId +
                ", forwardTo='" + forwardTo + '\'' +
                ", response=" + response +
                ", order=" + order +
                '}';
    }
    
    
}