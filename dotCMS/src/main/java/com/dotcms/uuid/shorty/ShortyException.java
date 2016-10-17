package com.dotcms.uuid.shorty;

import com.dotmarketing.business.DotStateException;

public class ShortyException extends DotStateException {

    public ShortyException(String x) {
        super(x);

    }

    public ShortyException(String x, Exception e) {
        super(x,e);

    }


}
