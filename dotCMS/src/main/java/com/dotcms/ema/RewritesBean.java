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
 */
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
