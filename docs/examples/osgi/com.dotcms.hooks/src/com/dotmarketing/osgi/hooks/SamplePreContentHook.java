package com.dotmarketing.osgi.hooks;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHookAbstractImp;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.categories.model.Category;
import com.liferay.portal.model.User;
import java.util.List;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.util.Logger;


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

    @Override
	public boolean checkin(Contentlet currentContentlet, ContentletRelationships relationshipsData, List<Category> cats, List<Permission> selectedPermissions, User user,	boolean respectFrontendRoles) {
        Logger.info(this,"+++++++++++++++++++++++++++++++++++++++++++++++" );
        Logger.info(this,"INSIDE SamplePreContentHook.checkin()" );
        Logger.info(this,"+++++++++++++++++++++++++++++++++++++++++++++++" );
		return super.checkin(currentContentlet, relationshipsData, cats,selectedPermissions, user, respectFrontendRoles);
	}

}