package com.dotcms.ema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Encapsulates the EMA rewrites information
 * {
 *    "rewrites":[
 *       {
 *          "source":"/index*",
 *          "destination":"proxy.demo.com"
 *       }
 *    ]
 * }
 * @author jsanca
 *
 * @deprecated The ability to map incoming URLs with specific EMA Proxy URLs has been implemented via the new JSON
 * configuration format. This class is not necessary anymore and should be deleted in the future.
 */
@Deprecated
public class RewritesBean {
    private final List<RewriteBean> rewrites;

    @JsonCreator
    public RewritesBean(@JsonProperty("rewrites") final List<RewriteBean> rewrites) {
        this.rewrites = rewrites;
    }

    public List<RewriteBean> getRewrites() {
        return rewrites;
    }
}
