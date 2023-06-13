package com.dotcms.enterprise.csspreproc;

import java.io.IOException;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

@Deprecated
public class LessCompiler extends DotLessCompiler {

    public LessCompiler(Host host, String uri, boolean live) {
        super(host, uri, live);
    }

    @Override
    public void compile() throws DotSecurityException, DotStateException, DotDataException, IOException {
        
        super.compile();
    }
}
