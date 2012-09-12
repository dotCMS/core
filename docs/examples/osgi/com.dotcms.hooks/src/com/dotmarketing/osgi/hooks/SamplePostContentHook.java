package com.dotmarketing.osgi.hooks;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPIPostHookAbstractImp;

public class SamplePostContentHook extends ContentletAPIPostHookAbstractImp {

    public SamplePostContentHook () {
        super();
    }

    @Override
    public long contentletCount ( long returnValue ) throws DotDataException {

        System.out.println( "+++++++++++++++++++++++++++++++++++++++++++++++" );
        System.out.println( "INSIDE SamplePostContentHook.contentletCount()" );
        System.out.println( "+++++++++++++++++++++++++++++++++++++++++++++++" );

        return super.contentletCount( returnValue );
    }

}