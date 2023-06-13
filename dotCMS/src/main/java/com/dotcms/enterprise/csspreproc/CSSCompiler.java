package com.dotcms.enterprise.csspreproc;

import com.dotmarketing.beans.Host;

public abstract class CSSCompiler extends DotCSSCompiler {

    public CSSCompiler(Host host, String uri, boolean live) {
        super(host, uri, live);
    }
    
}
