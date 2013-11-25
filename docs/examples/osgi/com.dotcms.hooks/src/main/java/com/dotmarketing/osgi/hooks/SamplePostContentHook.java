package com.dotmarketing.osgi.hooks;

import com.dotmarketing.beans.Permission;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPIPostHookAbstractImp;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import java.util.List;


public class SamplePostContentHook extends ContentletAPIPostHookAbstractImp {

    public SamplePostContentHook () {
        super();
    }

    @Override
    public long contentletCount ( long returnValue ) throws DotDataException {

        System.out.println( "+++++++++++++++++++++++++++++++++++++++++++++++" );
        System.out.println( "INSIDE SamplePostContentHook.contentletCount() -->" + String.valueOf( returnValue ) );
        System.out.println( "+++++++++++++++++++++++++++++++++++++++++++++++" );

        return super.contentletCount( returnValue );
    }

    @Override
    public void checkin ( Contentlet currentContentlet, ContentletRelationships relationshipsData, List<Category> cats, List<Permission> selectedPermissions, User user, boolean respectFrontendRoles, Contentlet returnValue ) {

        Logger.info( this, "+++++++++++++++++++++++++++++++++++++++++++++++" );
        Logger.info( this, "INSIDE SamplePostContentHook.checkin()" );
        Logger.info( this, "+++++++++++++++++++++++++++++++++++++++++++++++" );
        super.checkin( currentContentlet, relationshipsData, cats, selectedPermissions, user, respectFrontendRoles, returnValue );
    }

}