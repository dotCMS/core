package com.dotcms.api.tree;

import com.dotmarketing.business.Treeable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

import java.util.List;

/**
 * Created by jasontesser on 9/30/16.
 */
public interface Parentable {


    public boolean isParent();

    public List<Treeable> getChildren(User user, boolean live, boolean working, boolean archived, boolean respectFrontEndPermissions) throws DotSecurityException, DotDataException;

}
