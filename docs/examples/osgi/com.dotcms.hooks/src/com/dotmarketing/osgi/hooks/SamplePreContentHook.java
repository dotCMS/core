package com.dotmarketing.osgi.hooks;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHookAbstractImp;

public class SamplePreContentHook extends ContentletAPIPreHookAbstractImp {

    public SamplePreContentHook () {
        super();
    }

    @Override
    public boolean contentletCount () throws DotDataException {

        System.out.println( "+++++++++++++++++++++++++++++++++++++++++++++++" );
        System.out.println( "INSIDE SamplePreContentHook.contentletCount()" );
        System.out.println( "+++++++++++++++++++++++++++++++++++++++++++++++" );

        return super.contentletCount();
    }

}