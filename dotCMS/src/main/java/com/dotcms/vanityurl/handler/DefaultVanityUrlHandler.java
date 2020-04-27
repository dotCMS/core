package com.dotcms.vanityurl.handler;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.VanityUrlResult;
import com.dotmarketing.util.UtilMethods;
import io.vavr.Tuple2;

/**
 * This class implements the methods defined in the {@link VanityUrlHandler}
 *
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 16, 2017
 */
public class DefaultVanityUrlHandler implements VanityUrlHandler {





    @Override
    public VanityUrlResult handle(final CachedVanityUrl vanityUrl, final String uriIn,
                                  final HttpServletResponse response) throws IOException {
        
        
        final Tuple2<String,String> rewritten = vanityUrl.processForward(uriIn);
        final String rewrite = rewritten._1;
        final String queryString = rewritten._2;


        if (vanityUrl.getResponse()==301 || vanityUrl.getResponse()==302 ) {
            response.setStatus(vanityUrl.getResponse());
            response.setHeader("Location", rewrite);
            return new VanityUrlResult(rewrite, queryString, true);
        }
        
        if (vanityUrl.getResponse()==200 && UtilMethods.isSet(rewrite) && rewrite.contains("//")) {
            new CircuitBreakerUrl(rewrite).doOut(response);
            return new VanityUrlResult(rewrite, queryString, true);
        }

        return new VanityUrlResult(rewrite, queryString, false);
    }


}